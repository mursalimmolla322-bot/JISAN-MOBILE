package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.AdminRepository
import com.example.data.repository.CustomerRepository
import com.example.data.repository.OrderRepository
import com.example.data.repository.StoreRepository
import com.example.model.Category
import com.example.model.Product
import com.example.model.UiState
import com.example.ui.AppEvent
import com.example.ui.MainViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ProductPhoto
import com.example.ui.screens.AdminPortalEntryScreen
import com.example.ui.screens.AppUpdatesAndDistributionScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CartScreen
import com.example.ui.screens.CheckoutScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OrderConfirmationScreen
import com.example.ui.screens.OrderDetailTrackingScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReferralsScreen
import com.example.ui.screens.SavedAddressesScreen
import com.example.ui.screens.SupportAndPoliciesScreen
import com.example.ui.screens.WishlistScreen
import com.example.ui.theme.JisanBluePrimary
import com.example.ui.theme.JisanDangerRed
import com.example.ui.theme.JisanMobileTheme
import com.example.util.AppStrings
import com.example.util.NotificationHelper
import com.example.util.StringsProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannel(this)
        val initialReferralCode = intent?.data?.getQueryParameter("ref")?.trim()?.uppercase().orEmpty()

        setContent {
            AppNavigation(initialReferralCode = initialReferralCode)
        }
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
    addAuthStateListener(listener)
    trySend(currentUser)
    awaitClose { removeAuthStateListener(listener) }
}

private object AppRoutes {
    const val HOME = "home"
    const val CATEGORIES = "categories"
    const val CART = "cart"
    const val ORDERS = "orders"
    const val PROFILE = "profile"
    const val PRODUCT_DETAIL = "product/{productId}"
    const val CHECKOUT = "checkout"
    const val ORDER_CONFIRM = "order_confirm/{orderId}"
    const val ORDER_DETAIL = "order_detail/{orderId}"
    const val WISHLIST = "wishlist"
    const val ADDRESSES = "addresses"
    const val NOTIFICATIONS = "notifications"
    const val REFERRALS = "referrals"
    const val UPDATES = "updates"
    const val SUPPORT = "support"
    const val ADMIN = "admin"
}

private data class TopTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun AppNavigation(initialReferralCode: String = "") {
    val auth = remember { Firebase.auth }
    val currentUser by remember { auth.authStateFlow() }.collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        JisanMobileTheme {
            AuthScreen(onAuthSuccess = {})
        }
    } else {
        key(user.uid) {
            val context = LocalContext.current
            val db = remember(context) {
                FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id))
            }
            val viewModel: MainViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        MainViewModel(
                            storeRepo = StoreRepository(db, auth),
                            customerRepo = CustomerRepository(db, auth),
                            orderRepo = OrderRepository(db, auth),
                            adminRepo = AdminRepository(db, auth),
                            currentUserId = user.uid,
                            currentUserEmail = user.email.orEmpty(),
                            currentUserName = user.displayName.orEmpty(),
                            currentUserPhotoUrl = user.photoUrl?.toString().orEmpty(),
                            isEmailVerified = user.isEmailVerified
                        )
                    }
                }
            )

            val profile by viewModel.profile.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = profile?.darkMode ?: systemDark

            val notificationPermLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = {}
            )

            LaunchedEffect(user.uid) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            LaunchedEffect(initialReferralCode, profile?.userId) {
                val p = profile
                if (initialReferralCode.isNotBlank() && p != null && p.referredByCode.isBlank()) {
                    viewModel.applyReferralCode(initialReferralCode)
                }
            }

            JisanMobileTheme(darkTheme = useDarkTheme) {
                MainAppShell(
                    viewModel = viewModel,
                    onSignOut = {
                        viewModel.lockAdminSession()
                        withContext(NonCancellable) {
                            try {
                                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
                            } catch (e: Exception) {
                                Log.w("Auth", "clearCredentialState failed", e)
                            }
                            auth.signOut()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MainAppShell(
    viewModel: MainViewModel,
    onSignOut: suspend () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var lastSnackbarError by remember { mutableStateOf(false) }

    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val storeConfig by viewModel.storeConfig.collectAsStateWithLifecycle()
    val adminRecord by viewModel.adminRecord.collectAsStateWithLifecycle()
    val isAdminUnlocked by viewModel.isAdminSessionUnlocked.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val banners by viewModel.banners.collectAsStateWithLifecycle()
    val coupons by viewModel.coupons.collectAsStateWithLifecycle()
    val productsState by viewModel.publishedProductsState.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val cartLines by viewModel.cartLines.collectAsStateWithLifecycle()
    val wishlist by viewModel.wishlist.collectAsStateWithLifecycle()
    val addresses by viewModel.addresses.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val myReferrals by viewModel.myReferrals.collectAsStateWithLifecycle()
    val myOrdersState by viewModel.myOrdersState.collectAsStateWithLifecycle()

    val adminProducts by viewModel.adminProducts.collectAsStateWithLifecycle()
    val adminOrders by viewModel.adminOrders.collectAsStateWithLifecycle()
    val adminCustomers by viewModel.adminCustomers.collectAsStateWithLifecycle()
    val adminReferrals by viewModel.adminReferrals.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val appliedCouponCode by viewModel.appliedCouponCode.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val lang = profile?.preferredLanguage ?: "en"
    val isBengali = lang == "bn"
    val strings = remember(lang) { StringsProvider.forLang(lang) }

    val wishlistIds = remember(wishlist) { wishlist.map { it.productId }.toSet() }
    val cartBadgeCount = remember(cartLines) { cartLines.sumOf { it.cartItem.quantity }.toInt() }
    val unreadNotifCount = remember(notifications) { notifications.count { !it.isRead } }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AppEvent.Snackbar -> {
                    lastSnackbarError = event.isError
                    snackbarHostState.showSnackbar(event.message)
                }
                is AppEvent.OrderPlaced -> {
                    navController.navigate("order_confirm/${event.orderId}") {
                        popUpTo(AppRoutes.HOME) { saveState = false }
                        launchSingleTop = true
                    }
                }
                is AppEvent.LocalNotification -> {
                    NotificationHelper.showLocalNotification(context, event.title, event.message)
                }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topTabs = remember(strings, cartBadgeCount) {
        listOf(
            TopTabItem(AppRoutes.HOME, strings.home, Icons.Filled.Home, Icons.Outlined.Home),
            TopTabItem(AppRoutes.CATEGORIES, strings.categories, Icons.Filled.Category, Icons.Outlined.Category),
            TopTabItem(AppRoutes.CART, strings.cart, Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart, cartBadgeCount),
            TopTabItem(AppRoutes.ORDERS, strings.orders, Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
            TopTabItem(AppRoutes.PROFILE, strings.profile, Icons.Filled.Person, Icons.Outlined.Person)
        )
    }
    val topTabRoutes = remember(topTabs) { topTabs.map { it.route }.toSet() }
    val isTopLevelTab = currentRoute in topTabRoutes

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (isTopLevelTab && useNavRail) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    topTabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTab(navController, tab.route) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (tab.badgeCount > 0) {
                                            Badge { Text("${tab.badgeCount}") }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                }
                            },
                            label = { Text(tab.label, maxLines = 1) },
                            modifier = Modifier.testTag("nav_rail_${tab.route}")
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = if (lastSnackbarError) JisanDangerRed else MaterialTheme.colorScheme.inverseSurface,
                            contentColor = if (lastSnackbarError) Color.White else MaterialTheme.colorScheme.inverseOnSurface
                        )
                    }
                },
                bottomBar = {
                    if (isTopLevelTab && !useNavRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            topTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(navController, tab.route) },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (tab.badgeCount > 0) {
                                                    Badge { Text("${tab.badgeCount}") }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        }
                                    },
                                    label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.testTag("nav_bar_${tab.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = AppRoutes.HOME,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                ) {
                    composable(AppRoutes.HOME) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(JisanBluePrimary)
                                    .windowInsetsTopHeight(WindowInsets.statusBars)
                            )
                            HomeScreen(
                                strings = strings,
                                isBengali = isBengali,
                                storeConfig = storeConfig,
                                productsState = productsState,
                                filteredProducts = filteredProducts,
                                categories = categories,
                                banners = banners,
                                wishlistIds = wishlistIds,
                                cartCount = cartBadgeCount,
                                unreadNotificationsCount = unreadNotifCount,
                                searchQuery = searchQuery,
                                selectedCategoryId = selectedCategoryId,
                                sortOption = sortOption,
                                isRefreshing = isRefreshing,
                                onSearchChange = viewModel::setSearchQuery,
                                onCategorySelected = viewModel::setSelectedCategory,
                                onSortSelected = viewModel::setSortOption,
                                onRefresh = viewModel::refreshCatalog,
                                onLoadMore = viewModel::loadMoreProducts,
                                onProductClick = { product ->
                                    navController.navigate("product/${product.id}")
                                },
                                onAddToCart = viewModel::addToCart,
                                onToggleWishlist = { product -> viewModel.toggleWishlist(product.id) },
                                onOpenCart = { navigateToTab(navController, AppRoutes.CART) },
                                onOpenNotifications = { navController.navigate(AppRoutes.NOTIFICATIONS) },
                                onOpenProfile = { navigateToTab(navController, AppRoutes.PROFILE) }
                            )
                        }
                    }

                    composable(AppRoutes.CATEGORIES) {
                        val allPublished = (productsState as? UiState.Success)?.data.orEmpty()
                        CategoriesBrowserScreen(
                            categories = categories,
                            products = allPublished,
                            strings = strings,
                            isBengali = isBengali,
                            onSelectCategory = { catId ->
                                viewModel.setSelectedCategory(catId)
                                navigateToTab(navController, AppRoutes.HOME)
                            }
                        )
                    }

                    composable(AppRoutes.CART) {
                        CartScreen(
                            cartLines = cartLines,
                            storeConfig = storeConfig,
                            strings = strings,
                            onUpdateQuantity = viewModel::updateCartQuantity,
                            onRemove = viewModel::removeFromCart,
                            onProceedToCheckout = { navController.navigate(AppRoutes.CHECKOUT) },
                            onBrowseProducts = { navigateToTab(navController, AppRoutes.HOME) }
                        )
                    }

                    composable(AppRoutes.ORDERS) {
                        OrdersScreen(
                            ordersState = myOrdersState,
                            strings = strings,
                            isBengali = isBengali,
                            onOrderClick = { order ->
                                navController.navigate("order_detail/${order.id}")
                            },
                            onBrowseProducts = { navigateToTab(navController, AppRoutes.HOME) }
                        )
                    }

                    composable(AppRoutes.PROFILE) {
                        val showAdmin = viewModel.isAuthorizedAdminAccount() || viewModel.canCurrentUserBootstrapAdmin()
                        ProfileScreen(
                            profile = profile,
                            storeConfig = storeConfig,
                            strings = strings,
                            unreadNotifications = unreadNotifCount,
                            wishlistCount = wishlistIds.size,
                            showAdminPortalOption = showAdmin,
                            onEditProfile = { name, phone, photoUrl ->
                                viewModel.updateProfilePreferences(
                                    name = name,
                                    phone = phone,
                                    photoUrl = photoUrl,
                                    language = lang,
                                    darkMode = profile?.darkMode == true
                                )
                            },
                            onLanguageChange = viewModel::setLanguage,
                            onDarkModeChange = viewModel::setDarkMode,
                            onOpenWishlist = { navController.navigate(AppRoutes.WISHLIST) },
                            onOpenAddresses = { navController.navigate(AppRoutes.ADDRESSES) },
                            onOpenNotifications = { navController.navigate(AppRoutes.NOTIFICATIONS) },
                            onOpenReferrals = { navController.navigate(AppRoutes.REFERRALS) },
                            onOpenUpdates = { navController.navigate(AppRoutes.UPDATES) },
                            onOpenSupportPolicies = { navController.navigate(AppRoutes.SUPPORT) },
                            onOpenAdminPortal = { navController.navigate(AppRoutes.ADMIN) },
                            onSignOut = { scope.launch { onSignOut() } }
                        )
                    }

                    composable(
                        route = AppRoutes.PRODUCT_DETAIL,
                        arguments = listOf(navArgument("productId") { type = NavType.StringType })
                    ) { backStack ->
                        val productId = backStack.arguments?.getString("productId").orEmpty()
                        val allPublished = (productsState as? UiState.Success)?.data.orEmpty()
                        val product = allPublished.firstOrNull { it.id == productId }
                        if (product == null) {
                            Box(
                                modifier = Modifier.fillMaxSize().systemBarsPadding(),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyStateView(
                                    title = "Product unavailable",
                                    subtitle = "This product may have been unpublished or removed.",
                                    actionLabel = "Back to Store",
                                    onAction = { navController.popBackStack() }
                                )
                            }
                        } else {
                            val reviews by remember(product.id) {
                                viewModel.observeProductReviews(product.id)
                            }.collectAsStateWithLifecycle(initialValue = emptyList())
                            ProductDetailScreen(
                                product = product,
                                reviews = reviews,
                                isWishlisted = wishlistIds.contains(product.id),
                                strings = strings,
                                onBack = { navController.popBackStack() },
                                onAddToCart = { viewModel.addToCart(product) },
                                onBuyNow = {
                                    viewModel.addToCart(product)
                                    navController.navigate(AppRoutes.CHECKOUT)
                                },
                                onToggleWishlist = { viewModel.toggleWishlist(product.id) },
                                onSubmitReview = { rating, comment ->
                                    viewModel.submitProductReview(product.id, rating, comment)
                                }
                            )
                        }
                    }

                    composable(AppRoutes.CHECKOUT) {
                        val subtotal = cartLines.sumOf { it.product.sellingPrice * it.cartItem.quantity }
                        CheckoutScreen(
                            cartLines = cartLines,
                            addresses = addresses,
                            coupons = coupons,
                            appliedCouponCode = appliedCouponCode,
                            discountPreview = viewModel.computeDiscountPreview(subtotal),
                            storeConfig = storeConfig,
                            strings = strings,
                            isBusy = isBusy,
                            onBack = { navController.popBackStack() },
                            onApplyCoupon = viewModel::applyCoupon,
                            onClearCoupon = viewModel::clearCoupon,
                            onSaveAddress = viewModel::saveAddress,
                            onPlaceOrder = viewModel::placeOrder
                        )
                    }

                    composable(
                        route = AppRoutes.ORDER_CONFIRM,
                        arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                    ) { backStack ->
                        val orderId = backStack.arguments?.getString("orderId").orEmpty()
                        Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                            OrderConfirmationScreen(
                                orderId = orderId,
                                onTrackOrder = {
                                    navController.navigate("order_detail/$orderId") {
                                        popUpTo(AppRoutes.HOME) { saveState = false }
                                    }
                                },
                                onContinueShopping = {
                                    navigateToTab(navController, AppRoutes.HOME)
                                }
                            )
                        }
                    }

                    composable(
                        route = AppRoutes.ORDER_DETAIL,
                        arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                    ) { backStack ->
                        val orderId = backStack.arguments?.getString("orderId").orEmpty()
                        val orders = (myOrdersState as? UiState.Success)?.data.orEmpty()
                        val order = orders.firstOrNull { it.id == orderId }
                        if (order == null) {
                            Box(
                                modifier = Modifier.fillMaxSize().systemBarsPadding(),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyStateView(
                                    title = "Order not found",
                                    subtitle = "Unable to locate order $orderId.",
                                    actionLabel = "Back",
                                    onAction = { navController.popBackStack() }
                                )
                            }
                        } else {
                            OrderDetailTrackingScreen(
                                order = order,
                                isBengali = isBengali,
                                onBack = { navController.popBackStack() },
                                onCancelOrder = { reason -> viewModel.cancelOrder(order.id, reason) },
                                onRequestReturn = { reason -> viewModel.requestReturn(order.id, reason) }
                            )
                        }
                    }

                    composable(AppRoutes.WISHLIST) {
                        val allPublished = (productsState as? UiState.Success)?.data.orEmpty()
                        WishlistScreen(
                            products = allPublished,
                            wishlistIds = wishlistIds,
                            strings = strings,
                            onBack = { navController.popBackStack() },
                            onProductClick = { product -> navController.navigate("product/${product.id}") },
                            onAddToCart = viewModel::addToCart,
                            onToggleWishlist = { product -> viewModel.toggleWishlist(product.id) }
                        )
                    }

                    composable(AppRoutes.ADDRESSES) {
                        SavedAddressesScreen(
                            addresses = addresses,
                            onBack = { navController.popBackStack() },
                            onSaveAddress = viewModel::saveAddress,
                            onDeleteAddress = viewModel::deleteAddress
                        )
                    }

                    composable(AppRoutes.NOTIFICATIONS) {
                        NotificationsScreen(
                            notifications = notifications,
                            onBack = { navController.popBackStack() },
                            onMarkRead = viewModel::markNotificationRead
                        )
                    }

                    composable(AppRoutes.REFERRALS) {
                        ReferralsScreen(
                            profile = profile,
                            referrals = myReferrals,
                            storeConfig = storeConfig,
                            onBack = { navController.popBackStack() },
                            onApplyReferralCode = viewModel::applyReferralCode
                        )
                    }

                    composable(AppRoutes.UPDATES) {
                        AppUpdatesAndDistributionScreen(
                            storeConfig = storeConfig,
                            onBack = { navController.popBackStack() },
                            onVerifyApkUrl = viewModel::verifyApkUrl
                        )
                    }

                    composable(AppRoutes.SUPPORT) {
                        SupportAndPoliciesScreen(
                            storeConfig = storeConfig,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(AppRoutes.ADMIN) {
                        AdminPortalEntryScreen(
                            currentUserEmail = viewModel.currentUserEmail,
                            isEmailVerified = viewModel.isEmailVerified,
                            storeConfig = storeConfig,
                            adminRecord = adminRecord,
                            canBootstrap = viewModel.canCurrentUserBootstrapAdmin(),
                            isUnlocked = isAdminUnlocked,
                            isBusy = isBusy,
                            products = adminProducts,
                            orders = adminOrders,
                            customers = adminCustomers,
                            categories = categories,
                            banners = banners,
                            coupons = coupons,
                            referrals = adminReferrals,
                            onBack = { navController.popBackStack() },
                            onLockAdmin = viewModel::lockAdminSession,
                            onBootstrapAdmin = viewModel::bootstrapFirstAdmin,
                            onAdminLogin = viewModel::loginAdmin,
                            onResetAdminPin = viewModel::resetAdminPin,
                            onSaveProduct = viewModel::saveProductAdmin,
                            onTogglePublish = viewModel::toggleProductPublishAdmin,
                            onUpdateStock = viewModel::updateProductStockAdmin,
                            onDeleteProduct = viewModel::deleteProductAdmin,
                            onUpdateOrder = viewModel::updateOrderAdmin,
                            onSetCustomerBlocked = viewModel::setCustomerBlockedAdmin,
                            onSaveCategory = viewModel::saveCategoryAdmin,
                            onDeleteCategory = viewModel::deleteCategoryAdmin,
                            onSaveBanner = viewModel::saveBannerAdmin,
                            onDeleteBanner = viewModel::deleteBannerAdmin,
                            onSaveCoupon = viewModel::saveCouponAdmin,
                            onDeleteCoupon = viewModel::deleteCouponAdmin,
                            onApproveReferral = viewModel::approveReferralRewardAdmin,
                            onBroadcastNotification = viewModel::broadcastNotificationAdmin,
                            onUpdateStoreConfig = viewModel::updateStoreConfigAdmin,
                            onAddSubAdmin = viewModel::addSubAdmin,
                            onVerifyApkUrl = viewModel::verifyApkUrl
                        )
                    }
                }
            }
        }
    }
}

private fun navigateToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoriesBrowserScreen(
    categories: List<Category>,
    products: List<Product>,
    strings: AppStrings,
    isBengali: Boolean,
    onSelectCategory: (String) -> Unit
) {
    val activeCategories = remember(categories) { categories.filter { it.isActive } }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(strings.categories) })
        }
    ) { padding ->
        if (activeCategories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    title = strings.categories,
                    subtitle = "Store categories will appear here once published by the store owner.",
                    actionLabel = strings.allProducts,
                    onAction = { onSelectCategory("") }
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCategory("") }
                            .testTag("browse_cat_all"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = strings.allProducts,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strings.allProducts,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${products.size} items",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                items(activeCategories, key = { it.id }) { cat ->
                    val count = products.count {
                        it.categoryId == cat.id || it.categoryName.equals(cat.name, ignoreCase = true)
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCategory(cat.id) }
                            .testTag("browse_cat_${cat.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (cat.imageUrl.isNotBlank()) {
                                ProductPhoto(
                                    imageUrl = cat.imageUrl,
                                    contentDescription = cat.name,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = cat.name,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isBengali && cat.nameBn.isNotBlank()) cat.nameBn else cat.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$count items",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
