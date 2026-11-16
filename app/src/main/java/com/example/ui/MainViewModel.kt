package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AdminRepository
import com.example.data.repository.ApkVerificationResult
import com.example.data.repository.CustomerRepository
import com.example.data.repository.OrderRepository
import com.example.data.repository.StoreRepository
import com.example.model.AdminUser
import com.example.model.AppNotification
import com.example.model.Banner
import com.example.model.CartItem
import com.example.model.Category
import com.example.model.Coupon
import com.example.model.CustomerProfile
import com.example.model.DeliveryAddress
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.model.ProductReview
import com.example.model.ProductSortOption
import com.example.model.ReferralRecord
import com.example.model.StoreConfig
import com.example.model.UiState
import com.example.model.WishlistItem
import com.example.util.friendlyFirestoreMessage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.min

data class CartLineUi(
    val cartItem: CartItem,
    val product: Product
)

sealed interface AppEvent {
    data class Snackbar(val message: String, val isError: Boolean = false) : AppEvent
    data class OrderPlaced(val orderId: String) : AppEvent
    data class LocalNotification(val title: String, val message: String) : AppEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val storeRepo: StoreRepository,
    private val customerRepo: CustomerRepository,
    private val orderRepo: OrderRepository,
    private val adminRepo: AdminRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserName: String,
    val currentUserPhotoUrl: String,
    val isEmailVerified: Boolean
) : ViewModel() {

    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow("")
    val selectedCategoryId: StateFlow<String> = _selectedCategoryId.asStateFlow()

    private val _sortOption = MutableStateFlow(ProductSortOption.NEWEST)
    val sortOption: StateFlow<ProductSortOption> = _sortOption.asStateFlow()

    private val _pageSize = MutableStateFlow(20)
    val pageSize: StateFlow<Int> = _pageSize.asStateFlow()

    private val _appliedCouponCode = MutableStateFlow("")
    val appliedCouponCode: StateFlow<String> = _appliedCouponCode.asStateFlow()

    private val _isAdminSessionUnlocked = MutableStateFlow(false)
    val isAdminSessionUnlocked: StateFlow<Boolean> = _isAdminSessionUnlocked.asStateFlow()

    // Core reactive Firestore streams
    val storeConfig: StateFlow<StoreConfig> = storeRepo.observeStoreConfig()
        .catch { emit(StoreConfig()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), StoreConfig())

    val profile: StateFlow<CustomerProfile?> = customerRepo.observeProfile(currentUserId)
        .catch { error ->
            Log.w(TAG, "Profile stream error", unwrapFirestore(error))
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val adminRecord: StateFlow<AdminUser?> = adminRepo.observeAdminRecord(currentUserId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val categories: StateFlow<List<Category>> = storeRepo.observeCategories()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val banners: StateFlow<List<Banner>> = storeRepo.observeBanners()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val coupons: StateFlow<List<Coupon>> = storeRepo.observeCoupons()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val publishedProductsState: StateFlow<UiState<List<Product>>> = storeRepo.observePublishedProducts()
        .map<List<Product>, UiState<List<Product>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestore(error)
            Log.w(TAG, "Products stream error", root)
            emit(UiState.Error(friendlyFirestoreMessage(root)))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val filteredProducts: StateFlow<List<Product>> = combine(
        publishedProductsState,
        searchQuery,
        selectedCategoryId,
        sortOption,
        pageSize
    ) { state, query, catId, sort, limit ->
        val all = (state as? UiState.Success)?.data.orEmpty()
        val q = query.trim().lowercase()
        val filtered = all.filter { p ->
            val matchesCat = catId.isBlank() || p.categoryId == catId || p.categoryName.equals(catId, ignoreCase = true)
            val matchesQuery = q.isBlank() ||
                p.name.lowercase().contains(q) ||
                p.brand.lowercase().contains(q) ||
                p.categoryName.lowercase().contains(q) ||
                p.sku.lowercase().contains(q)
            matchesCat && matchesQuery
        }
        val sorted = when (sort) {
            ProductSortOption.NEWEST -> filtered.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            ProductSortOption.PRICE_LOW_TO_HIGH -> filtered.sortedBy { it.sellingPrice }
            ProductSortOption.PRICE_HIGH_TO_LOW -> filtered.sortedByDescending { it.sellingPrice }
            ProductSortOption.BEST_SELLING -> filtered.sortedByDescending { it.salesCount }
        }
        sorted.take(limit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val cartItems: StateFlow<List<CartItem>> = customerRepo.observeCart(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val cartLines: StateFlow<List<CartLineUi>> = combine(
        cartItems,
        publishedProductsState
    ) { items, productsState ->
        val products = (productsState as? UiState.Success)?.data.orEmpty().associateBy { it.id }
        items.mapNotNull { item ->
            products[item.productId]?.let { prod -> CartLineUi(item, prod) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val wishlist: StateFlow<List<WishlistItem>> = customerRepo.observeWishlist(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val addresses: StateFlow<List<DeliveryAddress>> = customerRepo.observeAddresses(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val notifications: StateFlow<List<AppNotification>> = customerRepo.observeNotifications(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val myReferrals: StateFlow<List<ReferralRecord>> = customerRepo.observeMyReferrals(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val myOrdersState: StateFlow<UiState<List<Order>>> = orderRepo.observeMyOrders(currentUserId)
        .map<List<Order>, UiState<List<Order>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestore(error)
            Log.w(TAG, "Orders stream error", root)
            emit(UiState.Error(friendlyFirestoreMessage(root)))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    // Admin-only reactive streams: strictly gated behind verified admin session
    val adminProducts: StateFlow<List<Product>> = isAdminSessionUnlocked
        .flatMapLatest { unlocked ->
            if (unlocked) storeRepo.observeAllProductsAdmin().catch { emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val adminOrders: StateFlow<List<Order>> = isAdminSessionUnlocked
        .flatMapLatest { unlocked ->
            if (unlocked) orderRepo.observeAllOrdersAdmin().catch { emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val adminCustomers: StateFlow<List<CustomerProfile>> = isAdminSessionUnlocked
        .flatMapLatest { unlocked ->
            if (unlocked) adminRepo.observeAllCustomers().catch { emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val adminReferrals: StateFlow<List<ReferralRecord>> = isAdminSessionUnlocked
        .flatMapLatest { unlocked ->
            if (unlocked) adminRepo.observeAllReferrals().catch { emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init {
        viewModelScope.launch {
            customerRepo.ensureProfileExists(
                userId = currentUserId,
                name = currentUserName,
                email = currentUserEmail,
                photoUrl = currentUserPhotoUrl
            )
        }
    }

    fun canCurrentUserBootstrapAdmin(): Boolean {
        val config = storeConfig.value
        return !config.bootstrapCompleted &&
            isEmailVerified &&
            currentUserEmail.equals(PRIMARY_OWNER_EMAIL, ignoreCase = true)
    }

    fun isAuthorizedAdminAccount(): Boolean {
        return adminRecord.value != null ||
            (isEmailVerified && currentUserEmail.equals(PRIMARY_OWNER_EMAIL, ignoreCase = true))
    }

    fun observeProductReviews(productId: String): Flow<List<ProductReview>> =
        storeRepo.observeProductReviews(productId).catch { emit(emptyList()) }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(categoryId: String) {
        _selectedCategoryId.value = categoryId
    }

    fun setSortOption(option: ProductSortOption) {
        _sortOption.value = option
    }

    fun loadMoreProducts() {
        _pageSize.value += 20
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = storeRepo.refreshPublishedProductsFromServer()
            _isRefreshing.value = false
            result.onFailure {
                _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true))
            }
        }
    }

    fun addToCart(product: Product) {
        viewModelScope.launch {
            if (product.stockQuantity <= 0L) {
                _events.tryEmit(AppEvent.Snackbar("This product is currently out of stock.", isError = true))
                return@launch
            }
            customerRepo.addToCart(currentUserId, product.id, product.stockQuantity)
                .onSuccess { _events.tryEmit(AppEvent.Snackbar("${product.name} added to cart")) }
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
        }
    }

    fun updateCartQuantity(productId: String, newQty: Long, maxStock: Long) {
        viewModelScope.launch {
            if (newQty > maxStock) {
                _events.tryEmit(AppEvent.Snackbar("Only $maxStock available in stock.", isError = true))
                return@launch
            }
            customerRepo.updateCartQuantity(currentUserId, productId, newQty)
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
        }
    }

    fun removeFromCart(productId: String) {
        viewModelScope.launch {
            customerRepo.removeFromCart(currentUserId, productId)
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
        }
    }

    fun toggleWishlist(productId: String) {
        viewModelScope.launch {
            customerRepo.toggleWishlist(currentUserId, productId)
                .onSuccess { added ->
                    _events.tryEmit(
                        AppEvent.Snackbar(if (added) "Saved to wishlist" else "Removed from wishlist")
                    )
                }
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
        }
    }

    fun applyCoupon(code: String) {
        val clean = code.trim().uppercase()
        if (clean.isBlank()) {
            _appliedCouponCode.value = ""
            return
        }
        val match = coupons.value.firstOrNull { it.code.equals(clean, ignoreCase = true) && it.isActive }
        if (match == null) {
            _events.tryEmit(AppEvent.Snackbar("Coupon '$clean' is invalid or expired.", isError = true))
            return
        }
        val subtotal = cartLines.value.sumOf { it.product.sellingPrice * it.cartItem.quantity }
        if (subtotal < match.minOrderAmount) {
            _events.tryEmit(
                AppEvent.Snackbar("Minimum order of ₹${match.minOrderAmount.toInt()} required for $clean.", isError = true)
            )
            return
        }
        _appliedCouponCode.value = match.code
        _events.tryEmit(AppEvent.Snackbar("Coupon ${match.code} applied (${match.discountPercent}% off)"))
    }

    fun clearCoupon() {
        _appliedCouponCode.value = ""
    }

    fun computeDiscountPreview(subtotal: Double): Double {
        val code = _appliedCouponCode.value
        if (code.isBlank()) return 0.0
        val coupon = coupons.value.firstOrNull { it.code == code && it.isActive } ?: return 0.0
        if (subtotal < coupon.minOrderAmount) return 0.0
        val raw = (subtotal * coupon.discountPercent.toDouble()) / 100.0
        return if (coupon.maxDiscountAmount > 0.0) min(raw, coupon.maxDiscountAmount) else raw
    }

    fun placeOrder(
        address: DeliveryAddress,
        paymentMethod: String,
        paymentStatus: String,
        transactionId: String,
        idempotencyKey: String
    ) {
        val prof = profile.value ?: CustomerProfile(
            userId = currentUserId,
            name = currentUserName,
            email = currentUserEmail
        )
        val items = cartItems.value
        viewModelScope.launch {
            _isBusy.value = true
            val result = orderRepo.placeOrder(
                profile = prof,
                address = address,
                cartItems = items,
                couponCode = _appliedCouponCode.value,
                paymentMethod = paymentMethod,
                paymentStatus = paymentStatus,
                transactionId = transactionId,
                idempotencyKey = idempotencyKey
            )
            _isBusy.value = false
            result.onSuccess { orderId ->
                _appliedCouponCode.value = ""
                _events.tryEmit(AppEvent.OrderPlaced(orderId))
                _events.tryEmit(
                    AppEvent.LocalNotification(
                        "Order Confirmed: $orderId",
                        "Thank you for shopping with JISAN MOBILE!"
                    )
                )
            }.onFailure {
                _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true))
            }
        }
    }

    fun cancelOrder(orderId: String, reason: String) {
        viewModelScope.launch {
            orderRepo.cancelMyOrder(orderId, reason)
                .onSuccess { _events.tryEmit(AppEvent.Snackbar("Order $orderId cancelled.")) }
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
        }
    }

    fun requestReturn(orderId: String, reason: String) {
        viewModelScope.launch {
            orderRepo.requestReturn(orderId, reason)
                .onSuccess { _events.tryEmit(AppEvent.Snackbar("Return request submitted for $orderId.")) }
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
        }
    }

    fun submitProductReview(productId: String, rating: Int, comment: String) {
        viewModelScope.launch {
            val name = profile.value?.name?.ifBlank { currentUserName } ?: currentUserName
            storeRepo.submitReview(productId, name, rating, comment)
                .onSuccess { _events.tryEmit(AppEvent.Snackbar("Review published. Thank you!")) }
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
        }
    }

    fun saveAddress(address: DeliveryAddress, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            customerRepo.saveAddress(currentUserId, address)
                .onSuccess {
                    _events.tryEmit(AppEvent.Snackbar("Delivery address saved."))
                    onDone()
                }
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
            _isBusy.value = false
        }
    }

    fun deleteAddress(addressId: String) {
        viewModelScope.launch {
            customerRepo.deleteAddress(currentUserId, addressId)
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
        }
    }

    fun updateProfilePreferences(
        name: String,
        phone: String,
        photoUrl: String,
        language: String,
        darkMode: Boolean
    ) {
        viewModelScope.launch {
            customerRepo.updateProfile(currentUserId, name, phone, photoUrl, language, darkMode)
                .onSuccess { _events.tryEmit(AppEvent.Snackbar("Profile & preferences updated.")) }
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
        }
    }

    fun setLanguage(lang: String) {
        val p = profile.value ?: return
        updateProfilePreferences(p.name, p.phone, p.photoUrl, lang, p.darkMode)
    }

    fun setDarkMode(enabled: Boolean) {
        val p = profile.value ?: return
        updateProfilePreferences(p.name, p.phone, p.photoUrl, p.preferredLanguage, enabled)
    }

    fun applyReferralCode(code: String) {
        viewModelScope.launch {
            customerRepo.applyReferralCode(currentUserId, currentUserEmail, code)
                .onSuccess { _events.tryEmit(AppEvent.Snackbar("Referral code applied successfully!")) }
                .onFailure { _events.tryEmit(AppEvent.Snackbar(it.localizedMessage ?: "Failed to apply code", isError = true)) }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            customerRepo.markNotificationRead(currentUserId, notificationId)
        }
    }

    // --- ADMIN OPERATIONS ---

    fun bootstrapFirstAdmin(phone: String, displayName: String, pin: String, shopName: String) {
        viewModelScope.launch {
            _isBusy.value = true
            adminRepo.bootstrapFirstAdmin(
                uid = currentUserId,
                email = currentUserEmail,
                phone = phone,
                displayName = displayName,
                pin = pin,
                shopName = shopName
            ).onSuccess {
                _isAdminSessionUnlocked.value = true
                _events.tryEmit(AppEvent.Snackbar("Administrator account initialized securely."))
            }.onFailure {
                _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true))
            }
            _isBusy.value = false
        }
    }

    fun loginAdmin(email: String, phone: String, pin: String) {
        viewModelScope.launch {
            _isBusy.value = true
            adminRepo.verifyAdminCredentials(currentUserId, email, phone, pin)
                .onSuccess { ok ->
                    if (ok) {
                        _isAdminSessionUnlocked.value = true
                        _events.tryEmit(AppEvent.Snackbar("Admin Portal unlocked."))
                    } else {
                        _events.tryEmit(AppEvent.Snackbar("Invalid admin email, mobile number, or password.", isError = true))
                    }
                }
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
            _isBusy.value = false
        }
    }

    fun lockAdminSession() {
        _isAdminSessionUnlocked.value = false
    }

    fun resetAdminPin(email: String, phone: String, newPin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            adminRepo.resetAdminPin(currentUserId, email, phone, newPin)
                .onSuccess {
                    _events.tryEmit(AppEvent.Snackbar("Admin password reset successfully."))
                    onSuccess()
                }
                .onFailure {
                    _events.tryEmit(AppEvent.Snackbar(it.localizedMessage ?: "Password reset failed.", isError = true))
                }
            _isBusy.value = false
        }
    }

    fun saveProductAdmin(product: Product, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            storeRepo.saveProduct(product)
                .onSuccess {
                    _events.tryEmit(AppEvent.Snackbar("Product saved to central catalog."))
                    onSaved()
                }
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
            _isBusy.value = false
        }
    }

    fun toggleProductPublishAdmin(product: Product) {
        viewModelScope.launch {
            storeRepo.toggleProductPublished(product.id, !product.isPublished)
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
        }
    }

    fun updateProductStockAdmin(productId: String, newStock: Long) {
        viewModelScope.launch {
            storeRepo.updateProductStock(productId, newStock)
                .onSuccess { _events.tryEmit(AppEvent.Snackbar("Stock updated.")) }
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
        }
    }

    fun deleteProductAdmin(productId: String) {
        viewModelScope.launch {
            storeRepo.deleteProduct(productId)
                .onSuccess { _events.tryEmit(AppEvent.Snackbar("Product deleted.")) }
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
        }
    }

    fun saveCategoryAdmin(category: Category, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            storeRepo.saveCategory(category)
                .onSuccess {
                    _events.tryEmit(AppEvent.Snackbar("Category saved."))
                    onDone()
                }
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
        }
    }

    fun deleteCategoryAdmin(id: String) {
        viewModelScope.launch {
            storeRepo.deleteCategory(id)
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
        }
    }

    fun saveBannerAdmin(banner: Banner, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            storeRepo.saveBanner(banner)
                .onSuccess {
                    _events.tryEmit(AppEvent.Snackbar("Banner saved."))
                    onDone()
                }
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
        }
    }

    fun deleteBannerAdmin(id: String) {
        viewModelScope.launch {
            storeRepo.deleteBanner(id)
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
        }
    }

    fun saveCouponAdmin(coupon: Coupon, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            storeRepo.saveCoupon(coupon)
                .onSuccess {
                    _events.tryEmit(AppEvent.Snackbar("Coupon saved."))
                    onDone()
                }
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
        }
    }

    fun deleteCouponAdmin(id: String) {
        viewModelScope.launch {
            storeRepo.deleteCoupon(id)
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
        }
    }

    fun updateOrderAdmin(
        order: Order,
        newStatus: OrderStatus,
        paymentStatus: String,
        trackingCarrier: String,
        trackingNumber: String,
        trackingNote: String,
        estimatedDelivery: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            orderRepo.adminUpdateOrder(
                order = order,
                newStatus = newStatus,
                paymentStatus = paymentStatus,
                trackingCarrier = trackingCarrier,
                trackingNumber = trackingNumber,
                trackingNote = trackingNote,
                estimatedDelivery = estimatedDelivery
            ).onSuccess {
                _events.tryEmit(AppEvent.Snackbar("Order ${order.id} updated to ${newStatus.labelEn}."))
            }.onFailure {
                _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true))
            }
            _isBusy.value = false
        }
    }

    fun setCustomerBlockedAdmin(userId: String, blocked: Boolean) {
        viewModelScope.launch {
            adminRepo.setCustomerBlocked(userId, blocked)
                .onSuccess {
                    _events.tryEmit(AppEvent.Snackbar(if (blocked) "Customer blocked." else "Customer unblocked."))
                }
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
        }
    }

    fun approveReferralRewardAdmin(referral: ReferralRecord) {
        viewModelScope.launch {
            adminRepo.approveReferralReward(referral)
                .onSuccess { _events.tryEmit(AppEvent.Snackbar("Referral reward credited.")) }
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
        }
    }

    fun updateStoreConfigAdmin(config: StoreConfig) {
        viewModelScope.launch {
            _isBusy.value = true
            adminRepo.updateStoreConfig(config)
                .onSuccess { _events.tryEmit(AppEvent.Snackbar("Store settings published.")) }
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
            _isBusy.value = false
        }
    }

    fun addSubAdmin(targetUid: String, email: String, phone: String, displayName: String, pin: String) {
        viewModelScope.launch {
            adminRepo.addAuthorizedAdmin(targetUid, email, phone, displayName, pin)
                .onSuccess { _events.tryEmit(AppEvent.Snackbar("Co-admin added.")) }
                .onFailure { _events.tryEmit(AppEvent.Snackbar(friendlyFirestoreMessage(it), isError = true)) }
        }
    }

    fun broadcastNotificationAdmin(title: String, message: String, type: String) {
        val customers = adminCustomers.value
        viewModelScope.launch {
            _isBusy.value = true
            var sentCount = 0
            customers.forEach { customer ->
                if (adminRepo.sendCustomerNotification(customer.userId, title, message, type).isSuccess) {
                    sentCount++
                }
            }
            _isBusy.value = false
            _events.tryEmit(AppEvent.Snackbar("Notification sent to $sentCount customer(s)."))
        }
    }

    fun verifyApkUrl(url: String, onResult: (ApkVerificationResult) -> Unit) {
        viewModelScope.launch {
            val res = adminRepo.verifyApkDownloadUrl(url)
            onResult(res)
        }
    }

    private fun unwrapFirestore(error: Throwable): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return firestoreError ?: error
    }

    private companion object {
        const val TAG = "MainViewModel"
        const val STOP_TIMEOUT_MS = 5000L
        const val PRIMARY_OWNER_EMAIL = "mursalimmolla322@gmail.com"
    }
}
