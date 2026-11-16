package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Banner
import com.example.model.Category
import com.example.model.Product
import com.example.model.ProductSortOption
import com.example.model.StoreConfig
import com.example.model.UiState
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ProductGridCard
import com.example.ui.components.ProductPhoto
import com.example.ui.theme.JisanAmberOffer
import com.example.ui.theme.JisanBluePrimary
import com.example.util.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    strings: AppStrings,
    isBengali: Boolean,
    storeConfig: StoreConfig,
    productsState: UiState<List<Product>>,
    filteredProducts: List<Product>,
    categories: List<Category>,
    banners: List<Banner>,
    wishlistIds: Set<String>,
    cartCount: Int,
    unreadNotificationsCount: Int,
    searchQuery: String,
    selectedCategoryId: String,
    sortOption: ProductSortOption,
    isRefreshing: Boolean,
    onSearchChange: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onSortSelected: (ProductSortOption) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onToggleWishlist: (Product) -> Unit,
    onOpenCart: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val allProducts = (productsState as? UiState.Success)?.data.orEmpty()
    val featured = allProducts.filter { it.isFeatured }.take(8)
    val bestSellers = allProducts.filter { it.salesCount > 0L }.sortedByDescending { it.salesCount }.take(8)
    val specialOffers = allProducts.filter { it.discountPercent >= 10L }.sortedByDescending { it.discountPercent }.take(8)

    Column(modifier = Modifier.fillMaxSize()) {
        // Branded Top Bar
        Surface(
            color = JisanBluePrimary,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(10.dp),
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.jisan_mobile_logo_1794840059210),
                                contentDescription = strings.appName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .testTag("home_brand_logo"),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = storeConfig.shopName.ifBlank { strings.appName },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = storeConfig.shopTagline.ifBlank { strings.tagline },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("home_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = strings.refresh,
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier.testTag("home_notifications_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotificationsCount > 0) {
                                        Badge { Text("$unreadNotificationsCount") }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = strings.notifications,
                                    tint = Color.White
                                )
                            }
                        }
                        IconButton(
                            onClick = onOpenCart,
                            modifier = Modifier.testTag("home_cart_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (cartCount > 0) {
                                        Badge { Text("$cartCount") }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = strings.cart,
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text(strings.searchHint, style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_input")
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val columns = when {
                    maxWidth >= 840.dp -> 4
                    maxWidth >= 600.dp -> 3
                    else -> 2
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Promotional Banners
                    item {
                        val activeBanners = banners.filter { it.isActive }
                        if (activeBanners.isNotEmpty()) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(activeBanners, key = { it.id }) { banner ->
                                    BannerCard(
                                        banner = banner,
                                        onClick = {
                                            if (banner.targetCategory.isNotBlank()) {
                                                onCategorySelected(banner.targetCategory)
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            DefaultHeroBanner(
                                shopName = storeConfig.shopName.ifBlank { strings.appName },
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }

                    // Categories Filter Rail
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategoryId.isBlank(),
                                    onClick = { onCategorySelected("") },
                                    label = { Text(strings.allProducts) },
                                    modifier = Modifier.testTag("cat_chip_all")
                                )
                            }
                            items(categories.filter { it.isActive }, key = { it.id }) { cat ->
                                FilterChip(
                                    selected = selectedCategoryId == cat.id,
                                    onClick = {
                                        onCategorySelected(if (selectedCategoryId == cat.id) "" else cat.id)
                                    },
                                    label = { Text(if (isBengali && cat.nameBn.isNotBlank()) cat.nameBn else cat.name) },
                                    modifier = Modifier.testTag("cat_chip_${cat.id}")
                                )
                            }
                        }
                    }

                    // Sort Filter Row
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = sortOption == ProductSortOption.NEWEST,
                                    onClick = { onSortSelected(ProductSortOption.NEWEST) },
                                    label = { Text(if (isBengali) "নতুন" else "Newest") }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = sortOption == ProductSortOption.BEST_SELLING,
                                    onClick = { onSortSelected(ProductSortOption.BEST_SELLING) },
                                    label = { Text(strings.bestSellers) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = sortOption == ProductSortOption.PRICE_LOW_TO_HIGH,
                                    onClick = { onSortSelected(ProductSortOption.PRICE_LOW_TO_HIGH) },
                                    label = { Text(if (isBengali) "কম দাম" else "Price: Low-High") }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = sortOption == ProductSortOption.PRICE_HIGH_TO_LOW,
                                    onClick = { onSortSelected(ProductSortOption.PRICE_HIGH_TO_LOW) },
                                    label = { Text(if (isBengali) "বেশি দাম" else "Price: High-Low") }
                                )
                            }
                        }
                    }

                    // Highlighted Rails when not searching
                    if (searchQuery.isBlank() && selectedCategoryId.isBlank()) {
                        if (featured.isNotEmpty()) {
                            item {
                                ProductHighlightSection(
                                    title = strings.featuredProducts,
                                    products = featured,
                                    wishlistIds = wishlistIds,
                                    strings = strings,
                                    onProductClick = onProductClick,
                                    onAddToCart = onAddToCart,
                                    onToggleWishlist = onToggleWishlist
                                )
                            }
                        }
                        if (bestSellers.isNotEmpty()) {
                            item {
                                ProductHighlightSection(
                                    title = strings.bestSellers,
                                    products = bestSellers,
                                    wishlistIds = wishlistIds,
                                    strings = strings,
                                    onProductClick = onProductClick,
                                    onAddToCart = onAddToCart,
                                    onToggleWishlist = onToggleWishlist
                                )
                            }
                        }
                        if (specialOffers.isNotEmpty()) {
                            item {
                                ProductHighlightSection(
                                    title = strings.specialOffers,
                                    products = specialOffers,
                                    wishlistIds = wishlistIds,
                                    strings = strings,
                                    onProductClick = onProductClick,
                                    onAddToCart = onAddToCart,
                                    onToggleWishlist = onToggleWishlist
                                )
                            }
                        }
                    }

                    // Main Catalog Heading
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strings.latestProducts,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${filteredProducts.size} items",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    when (productsState) {
                        is UiState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                        is UiState.Error -> {
                            item {
                                EmptyStateView(
                                    title = "Unable to load catalog",
                                    subtitle = productsState.message,
                                    actionLabel = strings.refresh,
                                    onAction = onRefresh
                                )
                            }
                        }
                        is UiState.Success -> {
                            if (filteredProducts.isEmpty()) {
                                item {
                                    EmptyStateView(
                                        title = strings.allProducts,
                                        subtitle = strings.emptyCatalog,
                                        actionLabel = strings.refresh,
                                        onAction = onRefresh
                                    )
                                }
                            } else {
                                val rows = filteredProducts.chunked(columns)
                                items(rows) { rowItems ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        rowItems.forEach { product ->
                                            ProductGridCard(
                                                product = product,
                                                isWishlisted = wishlistIds.contains(product.id),
                                                addToCartLabel = strings.addToCart,
                                                outOfStockLabel = strings.outOfStock,
                                                onClick = { onProductClick(product) },
                                                onAddToCart = { onAddToCart(product) },
                                                onToggleWishlist = { onToggleWishlist(product) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        repeat(columns - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }

                                if (allProducts.size > filteredProducts.size) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            OutlinedButton(onClick = onLoadMore) {
                                                Text("Load More Products")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultHeroBanner(shopName: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(165.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner_1794837912094),
                contentDescription = shopName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                        )
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Surface(color = JisanAmberOffer, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "LIVE STOREFRONT",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = shopName,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Genuine Mobiles • Fast Delivery • COD & Online",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BannerCard(banner: Banner, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(300.dp)
            .height(155.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (banner.imageUrl.isNotBlank()) {
                ProductPhoto(
                    imageUrl = banner.imageUrl,
                    contentDescription = banner.title,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_banner_1794837912094),
                    contentDescription = banner.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                        )
                    )
                    .padding(14.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    if (banner.badgeText.isNotBlank()) {
                        Surface(color = JisanAmberOffer, shape = RoundedCornerShape(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocalOffer,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = banner.badgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text(
                        text = banner.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (banner.subtitle.isNotBlank()) {
                        Text(
                            text = banner.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductHighlightSection(
    title: String,
    products: List<Product>,
    wishlistIds: Set<String>,
    strings: AppStrings,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onToggleWishlist: (Product) -> Unit
) {
    Column(modifier = Modifier.padding(top = 10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(products, key = { "${title}_${it.id}" }) { product ->
                ProductGridCard(
                    product = product,
                    isWishlisted = wishlistIds.contains(product.id),
                    addToCartLabel = strings.addToCart,
                    outOfStockLabel = strings.outOfStock,
                    onClick = { onProductClick(product) },
                    onAddToCart = { onAddToCart(product) },
                    onToggleWishlist = { onToggleWishlist(product) },
                    modifier = Modifier.width(175.dp)
                )
            }
        }
    }
}
