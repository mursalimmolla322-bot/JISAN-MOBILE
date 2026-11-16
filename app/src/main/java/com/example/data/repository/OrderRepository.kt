package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.model.CartItem
import com.example.model.CustomerProfile
import com.example.model.DeliveryAddress
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.util.OperationType
import com.example.util.SecurityUtils
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.min

class OrderRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must sign in with Google before placing or viewing orders.")

    fun observeMyOrders(userId: String): Flow<List<Order>> = flow {
        requireUserId()
        val path = "orders"
        emitAll(
            db.collection("orders")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Order::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeAllOrdersAdmin(): Flow<List<Order>> = flow {
        requireUserId()
        val path = "orders"
        emitAll(
            db.collection("orders")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Order::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, null)
                    throw error
                }
        )
    }

    /**
     * Places an order inside an atomic Firestore transaction:
     * - Re-reads product prices and stock from central Firestore (never trusts client-side totals)
     * - Re-validates coupon and delivery charges from central StoreConfig
     * - Guards against duplicate orders using [idempotencyKey]
     * - Atomically decrements stockQuantity and increments salesCount (prevents overselling)
     * - Clears ordered cart items
     */
    suspend fun placeOrder(
        profile: CustomerProfile,
        address: DeliveryAddress,
        cartItems: List<CartItem>,
        couponCode: String,
        paymentMethod: String,
        paymentStatus: String,
        transactionId: String,
        idempotencyKey: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(cartItems.isNotEmpty()) { "Your cart is empty." }
        if (profile.isBlocked) throw IllegalStateException("Your account is restricted from placing orders.")

        val orderId = SecurityUtils.generateOrderId(idempotencyKey)
        val orderRef = db.collection("orders").document(orderId)
        val storeConfigRef = db.collection("store_config").document("public")
        val normalizedCoupon = couponCode.trim().uppercase()
        val couponRef = if (normalizedCoupon.isNotBlank()) {
            db.collection("coupons").document(normalizedCoupon)
        } else null

        db.runTransaction { tx ->
            val existingOrder = tx.get(orderRef)
            if (existingOrder.exists()) {
                throw IllegalStateException("Duplicate order detected. Order $orderId is already placed.")
            }

            val configSnap = tx.get(storeConfigRef)
            val configDelivery = configSnap.getDouble("deliveryCharge") ?: 49.0
            val freeAbove = configSnap.getDouble("freeDeliveryAbove") ?: 999.0

            val couponSnap = couponRef?.let { tx.get(it) }

            val productSnaps = cartItems.map { item ->
                val pRef = db.collection("products").document(item.productId)
                item to tx.get(pRef)
            }

            var subtotal = 0.0
            val orderItemsMap = mutableListOf<Map<String, Any>>()

            for ((cartItem, pSnap) in productSnaps) {
                if (!pSnap.exists()) {
                    throw IllegalStateException("One of the products in your cart is no longer available.")
                }
                val isPublished = pSnap.getBoolean("isPublished") ?: false
                if (!isPublished) {
                    throw IllegalStateException("${pSnap.getString("name") ?: "Product"} is no longer published.")
                }
                val currentStock = pSnap.getLong("stockQuantity") ?: 0L
                val currentSales = pSnap.getLong("salesCount") ?: 0L
                if (currentStock < cartItem.quantity) {
                    val pname = pSnap.getString("name") ?: "Product"
                    throw IllegalStateException("Only $currentStock unit(s) of $pname left in stock.")
                }
                val price = pSnap.getDouble("sellingPrice") ?: 0.0
                val lineTotal = price * cartItem.quantity
                subtotal += lineTotal

                val images = (pSnap.get("imageUrls") as? List<*>)?.filterIsInstance<String>().orEmpty()
                orderItemsMap.add(
                    mapOf(
                        "productId" to cartItem.productId,
                        "productName" to (pSnap.getString("name") ?: "Product"),
                        "imageUrl" to (images.firstOrNull() ?: ""),
                        "unitPrice" to price,
                        "quantity" to cartItem.quantity,
                        "lineTotal" to lineTotal
                    )
                )

                tx.update(
                    pSnap.reference,
                    mapOf(
                        "stockQuantity" to (currentStock - cartItem.quantity),
                        "salesCount" to (currentSales + cartItem.quantity),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            var discountAmount = 0.0
            var appliedCode = ""
            if (couponSnap != null && couponSnap.exists() && (couponSnap.getBoolean("isActive") == true)) {
                val minOrder = couponSnap.getDouble("minOrderAmount") ?: 0.0
                if (subtotal >= minOrder) {
                    val pct = couponSnap.getLong("discountPercent") ?: 0L
                    val maxDisc = couponSnap.getDouble("maxDiscountAmount") ?: 0.0
                    val computed = (subtotal * pct.toDouble()) / 100.0
                    discountAmount = if (maxDisc > 0.0) min(computed, maxDisc) else computed
                    appliedCode = normalizedCoupon
                }
            }

            val afterDiscount = (subtotal - discountAmount).coerceAtLeast(0.0)
            val deliveryCharge = if (afterDiscount >= freeAbove || afterDiscount == 0.0) 0.0 else configDelivery
            val totalAmount = afterDiscount + deliveryCharge

            val fullAddressLine = listOf(address.line1, address.line2)
                .filter { it.isNotBlank() }
                .joinToString(", ")

            tx.set(
                orderRef,
                mapOf(
                    "userId" to uid,
                    "customerName" to address.recipientName.ifBlank { profile.name },
                    "customerEmail" to profile.email,
                    "customerPhone" to address.phone.ifBlank { profile.phone.ifBlank { "0000000000" } },
                    "shippingAddressLine" to fullAddressLine,
                    "shippingCity" to address.city,
                    "shippingState" to address.state,
                    "shippingPincode" to address.pincode,
                    "items" to orderItemsMap,
                    "subtotal" to subtotal,
                    "discountAmount" to discountAmount,
                    "couponCode" to appliedCode,
                    "deliveryCharge" to deliveryCharge,
                    "totalAmount" to totalAmount,
                    "paymentMethod" to paymentMethod,
                    "paymentStatus" to paymentStatus,
                    "transactionId" to transactionId,
                    "orderStatus" to OrderStatus.PENDING.code,
                    "trackingCarrier" to "",
                    "trackingNumber" to "",
                    "trackingNote" to "Order submitted and awaiting store confirmation.",
                    "estimatedDelivery" to "",
                    "idempotencyKey" to idempotencyKey,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            for (cartItem in cartItems) {
                val cartRef = db.collection("users").document(uid).collection("cart").document(cartItem.productId)
                tx.delete(cartRef)
            }
        }.await()

        // Create real notification record for customer
        db.collection("users").document(uid).collection("notifications").document()
            .set(
                mapOf(
                    "userId" to uid,
                    "title" to "Order Placed: $orderId",
                    "message" to "Your order $orderId has been submitted via $paymentMethod.",
                    "type" to "ORDER",
                    "referenceId" to orderId,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

        orderId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "orders")
    }

    suspend fun cancelMyOrder(orderId: String, reason: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("orders").document(orderId).update(
            mapOf(
                "orderStatus" to OrderStatus.CANCELLED.code,
                "trackingNote" to reason.ifBlank { "Cancelled by customer." },
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
    }

    suspend fun requestReturn(orderId: String, reason: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("orders").document(orderId).update(
            mapOf(
                "orderStatus" to OrderStatus.RETURN_REQUESTED.code,
                "trackingNote" to reason.ifBlank { "Return requested by customer." },
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
    }

    suspend fun adminUpdateOrder(
        order: Order,
        newStatus: OrderStatus,
        paymentStatus: String,
        trackingCarrier: String,
        trackingNumber: String,
        trackingNote: String,
        estimatedDelivery: String
    ): Result<Unit> = runCatching {
        requireUserId()
        db.collection("orders").document(order.id).update(
            mapOf(
                "orderStatus" to newStatus.code,
                "paymentStatus" to paymentStatus,
                "trackingCarrier" to trackingCarrier.trim(),
                "trackingNumber" to trackingNumber.trim(),
                "trackingNote" to trackingNote.trim(),
                "estimatedDelivery" to estimatedDelivery.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        // Notify customer about real status update
        db.collection("users").document(order.userId).collection("notifications").document()
            .set(
                mapOf(
                    "userId" to order.userId,
                    "title" to "Order ${order.id}: ${newStatus.labelEn}",
                    "message" to trackingNote.ifBlank { "Your order status is now ${newStatus.labelEn}." },
                    "type" to "ORDER_UPDATE",
                    "referenceId" to order.id,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "orders/${order.id}")
    }

    private fun unwrapAndLog(error: Throwable, op: OperationType, path: String, expectedUid: String?) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val toLog = firestoreError ?: (error as? Exception) ?: return
        if (expectedUid == null || auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(toLog, op, path)
        }
    }
}
