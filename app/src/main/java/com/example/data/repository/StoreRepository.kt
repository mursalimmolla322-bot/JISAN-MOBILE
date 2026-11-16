package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.model.Banner
import com.example.model.Category
import com.example.model.Coupon
import com.example.model.Product
import com.example.model.ProductReview
import com.example.model.StoreConfig
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToLong

class StoreRepository(
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
            ?: throw IllegalStateException("User must sign in with Google before accessing store catalog.")

    fun observeStoreConfig(): Flow<StoreConfig> = flow {
        requireUserId()
        val path = "store_config/public"
        emitAll(
            db.collection("store_config").document("public")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            StoreConfig::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: StoreConfig()
                    } else {
                        StoreConfig()
                    }
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeCategories(): Flow<List<Category>> = flow {
        requireUserId()
        val path = "categories"
        emitAll(
            db.collection("categories")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Category::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.displayOrder }
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observePublishedProducts(): Flow<List<Product>> = flow {
        requireUserId()
        val path = "products"
        emitAll(
            db.collection("products")
                .whereEqualTo("isPublished", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Product::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun refreshPublishedProductsFromServer(): Result<List<Product>> = runCatching {
        requireUserId()
        val snapshot = db.collection("products")
            .whereEqualTo("isPublished", true)
            .get(Source.SERVER)
            .await()
        snapshot.toObjects(
            Product::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.LIST, "products")
    }

    fun observeAllProductsAdmin(): Flow<List<Product>> = flow {
        requireUserId()
        val path = "products"
        emitAll(
            db.collection("products")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Product::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeBanners(): Flow<List<Banner>> = flow {
        requireUserId()
        val path = "banners"
        emitAll(
            db.collection("banners")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Banner::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.displayOrder }
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeCoupons(): Flow<List<Coupon>> = flow {
        requireUserId()
        val path = "coupons"
        emitAll(
            db.collection("coupons")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Coupon::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeProductReviews(productId: String): Flow<List<ProductReview>> = flow {
        requireUserId()
        val path = "products/$productId/reviews"
        emitAll(
            db.collection("products").document(productId).collection("reviews")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ProductReview::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun submitReview(
        productId: String,
        userName: String,
        rating: Int,
        comment: String,
        orderId: String = ""
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(rating in 1..5) { "Rating must be between 1 and 5" }
        val productRef = db.collection("products").document(productId)
        val reviewRef = productRef.collection("reviews").document(uid)

        db.runTransaction { tx ->
            val productSnap = tx.get(productRef)
            if (!productSnap.exists()) throw IllegalStateException("Product not found")
            val existingReview = tx.get(reviewRef)
            val currentSum = productSnap.getLong("ratingSum") ?: 0L
            val currentCount = productSnap.getLong("ratingCount") ?: 0L

            if (!existingReview.exists()) {
                tx.update(
                    productRef,
                    mapOf(
                        "ratingSum" to currentSum + rating.toLong(),
                        "ratingCount" to currentCount + 1L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    reviewRef,
                    mapOf(
                        "productId" to productId,
                        "userId" to uid,
                        "userName" to userName.ifBlank { "Verified Customer" },
                        "rating" to rating.toLong(),
                        "comment" to comment.trim(),
                        "orderId" to orderId,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                tx.set(
                    reviewRef,
                    mapOf(
                        "productId" to productId,
                        "userId" to uid,
                        "userName" to userName.ifBlank { "Verified Customer" },
                        "rating" to rating.toLong(),
                        "comment" to comment.trim(),
                        "orderId" to orderId,
                        "createdAt" to (existingReview.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }.await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "products/$productId/reviews")
    }

    suspend fun saveProduct(product: Product): Result<String> = runCatching {
        requireUserId()
        val isNew = product.id.isBlank()
        val docRef = if (isNew) {
            db.collection("products").document()
        } else {
            db.collection("products").document(product.id)
        }
        val mrp = product.mrp.coerceAtLeast(product.sellingPrice)
        val discount = if (mrp > 0.0) {
            (((mrp - product.sellingPrice) / mrp) * 100.0).roundToLong().coerceIn(0L, 100L)
        } else 0L

        val payload = hashMapOf<String, Any>(
            "name" to product.name.trim(),
            "description" to product.description.trim(),
            "categoryId" to product.categoryId,
            "categoryName" to product.categoryName.ifBlank { "Mobiles" },
            "brand" to product.brand.trim(),
            "sku" to product.sku.trim(),
            "mrp" to mrp,
            "sellingPrice" to product.sellingPrice,
            "discountPercent" to discount,
            "stockQuantity" to product.stockQuantity.coerceAtLeast(0L),
            "isPublished" to product.isPublished,
            "isFeatured" to product.isFeatured,
            "salesCount" to product.salesCount.coerceAtLeast(0L),
            "ratingSum" to product.ratingSum.coerceAtLeast(0L),
            "ratingCount" to product.ratingCount.coerceAtLeast(0L),
            "imageUrls" to product.imageUrls.take(5),
            "createdAt" to (if (!isNew && product.createdAt != null) product.createdAt else FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        docRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "products")
    }

    suspend fun updateProductStock(productId: String, newStock: Long): Result<Unit> = runCatching {
        requireUserId()
        db.collection("products").document(productId).update(
            mapOf(
                "stockQuantity" to newStock.coerceAtLeast(0L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "products/$productId")
    }

    suspend fun toggleProductPublished(productId: String, isPublished: Boolean): Result<Unit> = runCatching {
        requireUserId()
        db.collection("products").document(productId).update(
            mapOf(
                "isPublished" to isPublished,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "products/$productId")
    }

    suspend fun deleteProduct(productId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("products").document(productId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "products/$productId")
    }

    suspend fun saveCategory(category: Category): Result<String> = runCatching {
        requireUserId()
        val isNew = category.id.isBlank()
        val docRef = if (isNew) db.collection("categories").document() else db.collection("categories").document(category.id)
        val payload = mapOf(
            "name" to category.name.trim(),
            "nameBn" to category.nameBn.ifBlank { category.name }.trim(),
            "imageUrl" to category.imageUrl,
            "displayOrder" to category.displayOrder,
            "isActive" to category.isActive,
            "createdAt" to (if (!isNew && category.createdAt != null) category.createdAt else FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        docRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "categories")
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("categories").document(categoryId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "categories/$categoryId")
    }

    suspend fun saveBanner(banner: Banner): Result<String> = runCatching {
        requireUserId()
        val isNew = banner.id.isBlank()
        val docRef = if (isNew) db.collection("banners").document() else db.collection("banners").document(banner.id)
        val payload = mapOf(
            "title" to banner.title.trim(),
            "subtitle" to banner.subtitle.trim(),
            "badgeText" to banner.badgeText.trim(),
            "imageUrl" to banner.imageUrl,
            "targetCategory" to banner.targetCategory.trim(),
            "isActive" to banner.isActive,
            "displayOrder" to banner.displayOrder,
            "createdAt" to (if (!isNew && banner.createdAt != null) banner.createdAt else FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        docRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "banners")
    }

    suspend fun deleteBanner(bannerId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("banners").document(bannerId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "banners/$bannerId")
    }

    suspend fun saveCoupon(coupon: Coupon): Result<String> = runCatching {
        requireUserId()
        val normalizedCode = coupon.code.trim().uppercase()
        val isNew = coupon.id.isBlank()
        val docRef = if (isNew) db.collection("coupons").document(normalizedCode) else db.collection("coupons").document(coupon.id)
        val payload = mapOf(
            "code" to normalizedCode,
            "discountPercent" to coupon.discountPercent.coerceIn(1L, 95L),
            "maxDiscountAmount" to coupon.maxDiscountAmount.coerceAtLeast(0.0),
            "minOrderAmount" to coupon.minOrderAmount.coerceAtLeast(0.0),
            "isActive" to coupon.isActive,
            "createdAt" to (if (!isNew && coupon.createdAt != null) coupon.createdAt else FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        docRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "coupons")
    }

    suspend fun deleteCoupon(couponId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("coupons").document(couponId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "coupons/$couponId")
    }

    private fun unwrapAndLog(error: Throwable, op: OperationType, path: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val toLog = firestoreError ?: (error as? Exception) ?: return
        if (auth.currentUser != null) {
            handleFirestoreError(toLog, op, path)
        }
    }
}
