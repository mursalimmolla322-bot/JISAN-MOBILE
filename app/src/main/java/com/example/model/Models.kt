package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class OrderStatus(val code: String, val labelEn: String, val labelBn: String) {
    PENDING("PENDING", "Pending", "অপেক্ষমাণ"),
    CONFIRMED("CONFIRMED", "Confirmed", "নিশ্চিত"),
    PROCESSING("PROCESSING", "Processing", "প্রক্রিয়াধীন"),
    PACKED("PACKED", "Packed", "প্যাক করা হয়েছে"),
    SHIPPED("SHIPPED", "Shipped", "পাঠানো হয়েছে"),
    OUT_FOR_DELIVERY("OUT_FOR_DELIVERY", "Out for Delivery", "ডেলিভারির জন্য বেরিয়েছে"),
    DELIVERED("DELIVERED", "Delivered", "ডেলিভারি সম্পন্ন"),
    CANCELLED("CANCELLED", "Cancelled", "বাতিল"),
    RETURN_REQUESTED("RETURN_REQUESTED", "Return Requested", "রিটার্ন অনুরোধ"),
    REFUNDED("REFUNDED", "Refunded", "রিফান্ড সম্পন্ন");

    companion object {
        fun fromCode(code: String): OrderStatus =
            entries.firstOrNull { it.code == code } ?: PENDING
    }
}

@IgnoreExtraProperties
data class AdminUser(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val phone: String = "",
    val displayName: String = "",
    val pinHash: String = "",
    val pinSalt: String = "",
    @get:JvmName("getIsPrimaryOwner")
    val isPrimaryOwner: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StoreConfig(
    @DocumentId val id: String = "",
    val shopName: String = "JISAN MOBILE",
    val shopTagline: String = "Smartphones, Accessories & Trusted Electronics",
    val supportWhatsapp: String = "",
    val supportEmail: String = "",
    val shopAddress: String = "",
    val deliveryCharge: Double = 49.0,
    val freeDeliveryAbove: Double = 999.0,
    val referralRewardAmount: Double = 50.0,
    val codEnabled: Boolean = true,
    val onlinePaymentEnabled: Boolean = false,
    val razorpayKeyId: String = "",
    val appVersion: String = "1.0.0",
    val apkDownloadUrl: String = "",
    val apkFileSizeMb: String = "",
    val iosAppStoreUrl: String = "",
    val releaseNotes: String = "Initial production release of JISAN MOBILE.",
    val bootstrapCompleted: Boolean = false,
    val privacyPolicy: String = "JISAN MOBILE respects customer privacy. Personal data, addresses, and order records are isolated per user account and encrypted in transit and at rest via Firebase.",
    val termsText: String = "All orders placed on JISAN MOBILE are subject to live central stock availability. Prices include applicable taxes unless stated otherwise.",
    val shippingPolicy: String = "Orders are packed and dispatched within 24-48 business hours. Live tracking details are updated directly by the store team.",
    val refundPolicy: String = "Eligible return requests can be submitted from My Orders within 7 days of delivery. Approved refunds are processed back to the original payment method or wallet.",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Category(
    @DocumentId val id: String = "",
    val name: String = "",
    val nameBn: String = "",
    val imageUrl: String = "",
    val displayOrder: Long = 0L,
    @get:JvmName("getIsActive")
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val brand: String = "",
    val sku: String = "",
    val mrp: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val discountPercent: Long = 0L,
    val stockQuantity: Long = 0L,
    @get:JvmName("getIsPublished")
    val isPublished: Boolean = true,
    @get:JvmName("getIsFeatured")
    val isFeatured: Boolean = false,
    val salesCount: Long = 0L,
    val ratingSum: Long = 0L,
    val ratingCount: Long = 0L,
    val imageUrls: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val averageRating: Double
        get() = if (ratingCount > 0) ratingSum.toDouble() / ratingCount.toDouble() else 0.0
}

@IgnoreExtraProperties
data class ProductReview(
    @DocumentId val id: String = "",
    val productId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val orderId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Banner(
    @DocumentId val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val badgeText: String = "",
    val imageUrl: String = "",
    val targetCategory: String = "",
    @get:JvmName("getIsActive")
    val isActive: Boolean = true,
    val displayOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Coupon(
    @DocumentId val id: String = "",
    val code: String = "",
    val discountPercent: Long = 10L,
    val maxDiscountAmount: Double = 500.0,
    val minOrderAmount: Double = 499.0,
    @get:JvmName("getIsActive")
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CustomerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val referralCode: String = "",
    val referredByCode: String = "",
    val referralRewardEarned: Double = 0.0,
    @get:JvmName("getIsBlocked")
    val isBlocked: Boolean = false,
    val preferredLanguage: String = "en",
    val darkMode: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DeliveryAddress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "Home",
    val recipientName: String = "",
    val phone: String = "",
    val line1: String = "",
    val line2: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    @get:JvmName("getIsDefault")
    val isDefault: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CartItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val quantity: Long = 1L,
    @ServerTimestamp val addedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WishlistItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    @ServerTimestamp val addedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "ORDER",
    val referenceId: String = "",
    @get:JvmName("getIsRead")
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val imageUrl: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Long = 1L,
    val lineTotal: Double = 0.0
)

@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val customerPhone: String = "",
    val shippingAddressLine: String = "",
    val shippingCity: String = "",
    val shippingState: String = "",
    val shippingPincode: String = "",
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val couponCode: String = "",
    val deliveryCharge: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "COD",
    val paymentStatus: String = "PENDING",
    val transactionId: String = "",
    val orderStatus: String = "PENDING",
    val trackingCarrier: String = "",
    val trackingNumber: String = "",
    val trackingNote: String = "",
    val estimatedDelivery: String = "",
    val idempotencyKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ReferralRecord(
    @DocumentId val id: String = "",
    val referrerUid: String = "",
    val referrerCode: String = "",
    val referredUid: String = "",
    val referredEmail: String = "",
    val status: String = "REGISTERED",
    val rewardAmount: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class ProductSortOption {
    NEWEST,
    PRICE_LOW_TO_HIGH,
    PRICE_HIGH_TO_LOW,
    BEST_SELLING
}
