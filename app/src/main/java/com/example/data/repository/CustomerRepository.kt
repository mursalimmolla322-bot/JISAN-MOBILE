package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.model.AppNotification
import com.example.model.CartItem
import com.example.model.CustomerProfile
import com.example.model.DeliveryAddress
import com.example.model.ReferralRecord
import com.example.model.WishlistItem
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

class CustomerRepository(
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
            ?: throw IllegalStateException("User must sign in with Google first.")

    suspend fun ensureProfileExists(
        userId: String,
        name: String,
        email: String,
        photoUrl: String
    ): Result<CustomerProfile> = runCatching {
        val userRef = db.collection("users").document(userId)
        val snap = userRef.get().await()
        if (!snap.exists()) {
            val code = SecurityUtils.generateReferralCode(userId, name)
            val batch = db.batch()
            batch.set(
                userRef,
                mapOf(
                    "userId" to userId,
                    "name" to name.ifBlank { "JISAN Shopper" },
                    "email" to email.ifBlank { "$userId@jisanmobile.app" },
                    "phone" to "",
                    "photoUrl" to photoUrl,
                    "referralCode" to code,
                    "referredByCode" to "",
                    "referralRewardEarned" to 0.0,
                    "isBlocked" to false,
                    "preferredLanguage" to "en",
                    "darkMode" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("referral_codes").document(code),
                mapOf(
                    "code" to code,
                    "ownerUid" to userId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            userRef.get().await().toObject(
                CustomerProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: CustomerProfile(userId = userId, name = name, email = email, referralCode = code)
        } else {
            snap.toObject(
                CustomerProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: CustomerProfile(userId = userId)
        }
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/$userId")
    }

    fun observeProfile(userId: String): Flow<CustomerProfile?> = flow {
        requireUserId()
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            CustomerProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    suspend fun updateProfile(
        userId: String,
        name: String,
        phone: String,
        photoUrl: String,
        preferredLanguage: String,
        darkMode: Boolean
    ): Result<Unit> = runCatching {
        requireUserId()
        db.collection("users").document(userId).update(
            mapOf(
                "name" to name.trim().ifBlank { "JISAN Shopper" },
                "phone" to phone.trim(),
                "photoUrl" to photoUrl,
                "preferredLanguage" to if (preferredLanguage == "bn") "bn" else "en",
                "darkMode" to darkMode,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/$userId")
    }

    fun observeAddresses(userId: String): Flow<List<DeliveryAddress>> = flow {
        requireUserId()
        val path = "users/$userId/addresses"
        emitAll(
            db.collection("users").document(userId).collection("addresses")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        DeliveryAddress::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.isDefault }
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun saveAddress(userId: String, address: DeliveryAddress): Result<String> = runCatching {
        requireUserId()
        val col = db.collection("users").document(userId).collection("addresses")
        val isNew = address.id.isBlank()
        val docRef = if (isNew) col.document() else col.document(address.id)
        val payload = mapOf(
            "userId" to userId,
            "label" to address.label.trim().ifBlank { "Home" },
            "recipientName" to address.recipientName.trim(),
            "phone" to address.phone.trim(),
            "line1" to address.line1.trim(),
            "line2" to address.line2.trim(),
            "city" to address.city.trim(),
            "state" to address.state.trim(),
            "pincode" to address.pincode.trim(),
            "isDefault" to address.isDefault,
            "createdAt" to (if (!isNew && address.createdAt != null) address.createdAt else FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        docRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/$userId/addresses")
    }

    suspend fun deleteAddress(userId: String, addressId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("users").document(userId).collection("addresses").document(addressId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "users/$userId/addresses/$addressId")
    }

    fun observeCart(userId: String): Flow<List<CartItem>> = flow {
        requireUserId()
        val path = "users/$userId/cart"
        emitAll(
            db.collection("users").document(userId).collection("cart")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        CartItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun addToCart(userId: String, productId: String, maxStock: Long): Result<Unit> = runCatching {
        requireUserId()
        if (maxStock <= 0L) throw IllegalStateException("Product is out of stock")
        val docRef = db.collection("users").document(userId).collection("cart").document(productId)
        val snap = docRef.get().await()
        if (snap.exists()) {
            val currentQty = snap.getLong("quantity") ?: 1L
            val nextQty = (currentQty + 1L).coerceAtMost(maxStock).coerceAtMost(50L)
            docRef.update(
                mapOf(
                    "quantity" to nextQty,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.set(
                mapOf(
                    "userId" to userId,
                    "productId" to productId,
                    "quantity" to 1L,
                    "addedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/$userId/cart/$productId")
    }

    suspend fun updateCartQuantity(userId: String, productId: String, quantity: Long): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("users").document(userId).collection("cart").document(productId)
        if (quantity <= 0L) {
            docRef.delete().await()
        } else {
            docRef.update(
                mapOf(
                    "quantity" to quantity.coerceIn(1L, 50L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/$userId/cart/$productId")
    }

    suspend fun removeFromCart(userId: String, productId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("users").document(userId).collection("cart").document(productId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "users/$userId/cart/$productId")
    }

    fun observeWishlist(userId: String): Flow<List<WishlistItem>> = flow {
        requireUserId()
        val path = "users/$userId/wishlist"
        emitAll(
            db.collection("users").document(userId).collection("wishlist")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        WishlistItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun toggleWishlist(userId: String, productId: String): Result<Boolean> = runCatching {
        requireUserId()
        val docRef = db.collection("users").document(userId).collection("wishlist").document(productId)
        val snap = docRef.get().await()
        if (snap.exists()) {
            docRef.delete().await()
            false
        } else {
            docRef.set(
                mapOf(
                    "userId" to userId,
                    "productId" to productId,
                    "addedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            true
        }
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/$userId/wishlist/$productId")
    }

    fun observeNotifications(userId: String): Flow<List<AppNotification>> = flow {
        requireUserId()
        val path = "users/$userId/notifications"
        emitAll(
            db.collection("users").document(userId).collection("notifications")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        AppNotification::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun markNotificationRead(userId: String, notificationId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("users").document(userId).collection("notifications").document(notificationId)
            .update("isRead", true)
            .await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/$userId/notifications/$notificationId")
    }

    suspend fun createNotification(
        userId: String,
        title: String,
        message: String,
        type: String,
        referenceId: String = ""
    ): Result<Unit> = runCatching {
        requireUserId()
        db.collection("users").document(userId).collection("notifications").document()
            .set(
                mapOf(
                    "userId" to userId,
                    "title" to title,
                    "message" to message,
                    "type" to type,
                    "referenceId" to referenceId,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "users/$userId/notifications")
    }

    suspend fun applyReferralCode(
        userId: String,
        userEmail: String,
        inputCode: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val code = inputCode.trim().uppercase()
        require(code.length >= 4) { "Invalid referral code format." }

        val codeSnap = db.collection("referral_codes").document(code).get().await()
        if (!codeSnap.exists()) throw IllegalStateException("Referral code not found.")
        val referrerUid = codeSnap.getString("ownerUid") ?: throw IllegalStateException("Invalid referral link.")
        if (referrerUid == userId) throw IllegalStateException("Self-referrals are not allowed.")

        val existingReferral = db.collection("referrals").document(userId).get().await()
        if (existingReferral.exists()) throw IllegalStateException("A referral code has already been applied to your account.")

        val configSnap = db.collection("store_config").document("public").get().await()
        val rewardAmount = configSnap.getDouble("referralRewardAmount") ?: 50.0

        val batch = db.batch()
        batch.set(
            db.collection("referrals").document(userId),
            mapOf(
                "referrerUid" to referrerUid,
                "referrerCode" to code,
                "referredUid" to userId,
                "referredEmail" to userEmail,
                "status" to "REGISTERED",
                "rewardAmount" to rewardAmount,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            db.collection("users").document(userId),
            mapOf(
                "referredByCode" to code,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "referrals/$userId")
    }

    fun observeMyReferrals(userId: String): Flow<List<ReferralRecord>> = flow {
        requireUserId()
        val path = "referrals"
        emitAll(
            db.collection("referrals")
                .whereEqualTo("referrerUid", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        ReferralRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    private fun unwrapAndLog(error: Throwable, op: OperationType, path: String, expectedUid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val toLog = firestoreError ?: (error as? Exception) ?: return
        if (auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(toLog, op, path)
        }
    }
}
