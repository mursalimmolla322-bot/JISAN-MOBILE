package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.repository.ApkVerificationResult
import com.example.model.Banner
import com.example.model.Category
import com.example.model.Coupon
import com.example.model.Product
import com.example.model.ReferralRecord
import com.example.model.StoreConfig
import com.example.ui.components.ProductPhoto
import com.example.ui.theme.JisanDangerRed
import com.example.ui.theme.JisanEmeraldSuccess
import com.example.util.ImageUtils

@Composable
fun AdminProductsTab(
    products: List<Product>,
    categories: List<Category>,
    isBusy: Boolean,
    onSaveProduct: (Product, () -> Unit) -> Unit,
    onTogglePublish: (Product) -> Unit,
    onUpdateStock: (String, Long) -> Unit,
    onDeleteProduct: (String) -> Unit
) {
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingProduct = null
                    showEditor = true
                },
                modifier = Modifier.testTag("admin_add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (products.isEmpty()) {
                item {
                    Text(
                        text = "No products in central database yet. Tap + to publish your first product to all customers.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(products, key = { it.id }) { product ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProductPhoto(
                                    imageUrl = product.imageUrls.firstOrNull().orEmpty(),
                                    contentDescription = product.name,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Bold)
                                    Text("${product.brand} • SKU: ${product.sku}", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = "${ImageUtils.formatInr(product.sellingPrice)} (MRP ${ImageUtils.formatInr(product.mrp)}) • Stock: ${product.stockQuantity}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Published", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Switch(
                                        checked = product.isPublished,
                                        onCheckedChange = { onTogglePublish(product) }
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(
                                        onClick = { onUpdateStock(product.id, (product.stockQuantity + 5L)) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("+5 Stock")
                                    }
                                    IconButton(onClick = {
                                        editingProduct = product
                                        showEditor = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = { onDeleteProduct(product.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = JisanDangerRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        ProductEditorDialog(
            existing = editingProduct,
            categories = categories,
            isBusy = isBusy,
            onDismiss = { showEditor = false },
            onSave = { prod ->
                onSaveProduct(prod) { showEditor = false }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductEditorDialog(
    existing: Product?,
    categories: List<Category>,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var brand by remember { mutableStateOf(existing?.brand.orEmpty()) }
    var sku by remember { mutableStateOf(existing?.sku.orEmpty()) }
    var categoryId by remember { mutableStateOf(existing?.categoryId.orEmpty()) }
    var categoryName by remember { mutableStateOf(existing?.categoryName ?: "Smartphones") }
    var mrpStr by remember { mutableStateOf(existing?.mrp?.takeIf { it > 0 }?.toInt()?.toString().orEmpty()) }
    var priceStr by remember { mutableStateOf(existing?.sellingPrice?.takeIf { it > 0 }?.toInt()?.toString().orEmpty()) }
    var stockStr by remember { mutableStateOf(existing?.stockQuantity?.toString() ?: "10") }
    var isPublished by remember { mutableStateOf(existing?.isPublished ?: true) }
    var isFeatured by remember { mutableStateOf(existing?.isFeatured ?: false) }
    var urlInput by remember { mutableStateOf("") }
    var catExpanded by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    val images = remember {
        mutableStateListOf<String>().apply {
            existing?.imageUrls?.let { addAll(it) }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4),
        onResult = { uris ->
            uris.forEach { uri ->
                if (images.size < 4) {
                    ImageUtils.compressUriToBase64DataUri(context, uri)
                        .onSuccess { dataUri -> images.add(dataUri) }
                        .onFailure { err ->
                            Toast.makeText(context, err.localizedMessage ?: "Image upload failed", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    )

    val mrp = mrpStr.toDoubleOrNull() ?: 0.0
    val sellingPrice = priceStr.toDoubleOrNull() ?: 0.0
    val stock = stockStr.toLongOrNull() ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Central Catalog Product" else "Edit Product") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Real Product Photographs (${images.size}/4)", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        enabled = images.size < 4
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload Photo")
                    }
                }
                if (images.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(images.size) { idx ->
                            Box {
                                ProductPhoto(
                                    imageUrl = images[idx],
                                    contentDescription = "Product photo ${idx + 1}",
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                IconButton(
                                    onClick = { images.removeAt(idx) },
                                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove photo", tint = JisanDangerRed)
                                }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Or HTTPS image URL") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(onClick = {
                        if (urlInput.startsWith("https://") && images.size < 4) {
                            images.add(urlInput.trim())
                            urlInput = ""
                        }
                    }) {
                        Text("Add")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name*") },
                    isError = submitted && name.isBlank(),
                    singleLine = true
                )
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand (e.g. Samsung)") }, singleLine = true)
                OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU Code") }, singleLine = true)

                if (categories.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                        OutlinedTextField(
                            value = categoryName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        categoryId = cat.id
                                        categoryName = cat.name
                                        catExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        label = { Text("Category Name") },
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = mrpStr,
                        onValueChange = { mrpStr = it },
                        label = { Text("MRP (₹)*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = submitted && mrp <= 0,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Price (₹)*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = submitted && (sellingPrice <= 0 || sellingPrice > mrp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = stockStr,
                    onValueChange = { stockStr = it },
                    label = { Text("Stock Quantity*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Product Description") },
                    maxLines = 4
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Published to Customers")
                    Switch(checked = isPublished, onCheckedChange = { isPublished = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Featured Product")
                    Switch(checked = isFeatured, onCheckedChange = { isFeatured = it })
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy,
                onClick = {
                    submitted = true
                    if (name.isNotBlank() && mrp > 0.0 && sellingPrice > 0.0 && sellingPrice <= mrp) {
                        onSave(
                            Product(
                                id = existing?.id.orEmpty(),
                                name = name,
                                description = description,
                                categoryId = categoryId,
                                categoryName = categoryName,
                                brand = brand,
                                sku = sku,
                                mrp = mrp,
                                sellingPrice = sellingPrice,
                                stockQuantity = stock,
                                isPublished = isPublished,
                                isFeatured = isFeatured,
                                salesCount = existing?.salesCount ?: 0L,
                                ratingSum = existing?.ratingSum ?: 0L,
                                ratingCount = existing?.ratingCount ?: 0L,
                                imageUrls = images.toList(),
                                createdAt = existing?.createdAt
                            )
                        )
                    }
                }
            ) {
                Text("Publish / Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AdminCatalogOffersTab(
    categories: List<Category>,
    banners: List<Banner>,
    coupons: List<Coupon>,
    referrals: List<ReferralRecord>,
    onSaveCategory: (Category, () -> Unit) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onSaveBanner: (Banner, () -> Unit) -> Unit,
    onDeleteBanner: (String) -> Unit,
    onSaveCoupon: (Coupon, () -> Unit) -> Unit,
    onDeleteCoupon: (String) -> Unit,
    onApproveReferral: (ReferralRecord) -> Unit,
    onBroadcastNotification: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    var catName by remember { mutableStateOf("") }
    var catNameBn by remember { mutableStateOf("") }

    var bannerTitle by remember { mutableStateOf("") }
    var bannerSubtitle by remember { mutableStateOf("") }
    var bannerBadge by remember { mutableStateOf("") }
    var bannerImage by remember { mutableStateOf("") }

    var couponCode by remember { mutableStateOf("") }
    var couponPct by remember { mutableStateOf("") }
    var couponMaxDisc by remember { mutableStateOf("") }
    var couponMinOrder by remember { mutableStateOf("") }

    var notifTitle by remember { mutableStateOf("") }
    var notifMessage by remember { mutableStateOf("") }

    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                ImageUtils.compressUriToBase64DataUri(context, it, maxDimension = 900)
                    .onSuccess { dataUri -> bannerImage = dataUri }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Categories
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Categories (${categories.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = catName, onValueChange = { catName = it }, label = { Text("Name (EN)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = catNameBn, onValueChange = { catNameBn = it }, label = { Text("Name (বাংলা)") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Button(
                    onClick = {
                        if (catName.isNotBlank()) {
                            onSaveCategory(
                                Category(name = catName, nameBn = catNameBn.ifBlank { catName }, displayOrder = categories.size.toLong())
                            ) {
                                catName = ""
                                catNameBn = ""
                            }
                        }
                    },
                    enabled = catName.isNotBlank()
                ) {
                    Text("Add Category")
                }
                categories.forEach { cat ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${cat.name} / ${cat.nameBn}")
                        IconButton(onClick = { onDeleteCategory(cat.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete category", tint = JisanDangerRed)
                        }
                    }
                }
            }
        }

        // Banners
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Promotional Banners (${banners.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = bannerTitle, onValueChange = { bannerTitle = it }, label = { Text("Banner Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bannerSubtitle, onValueChange = { bannerSubtitle = it }, label = { Text("Subtitle") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = bannerBadge, onValueChange = { bannerBadge = it }, label = { Text("Badge (e.g. SALE)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedButton(onClick = {
                        bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Text(if (bannerImage.isBlank()) "Upload Photo" else "Photo Ready")
                    }
                }
                Button(
                    onClick = {
                        if (bannerTitle.isNotBlank()) {
                            onSaveBanner(
                                Banner(
                                    title = bannerTitle,
                                    subtitle = bannerSubtitle,
                                    badgeText = bannerBadge,
                                    imageUrl = bannerImage,
                                    displayOrder = banners.size.toLong()
                                )
                            ) {
                                bannerTitle = ""
                                bannerSubtitle = ""
                                bannerBadge = ""
                                bannerImage = ""
                            }
                        }
                    },
                    enabled = bannerTitle.isNotBlank()
                ) {
                    Text("Publish Banner")
                }
                banners.forEach { b ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(b.title, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onDeleteBanner(b.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete banner", tint = JisanDangerRed)
                        }
                    }
                }
            }
        }

        // Coupons
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Discount Coupons (${coupons.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = couponCode, onValueChange = { couponCode = it.uppercase() }, label = { Text("Code") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = couponPct, onValueChange = { couponPct = it }, label = { Text("% Off") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(0.7f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = couponMaxDisc, onValueChange = { couponMaxDisc = it }, label = { Text("Max ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = couponMinOrder, onValueChange = { couponMinOrder = it }, label = { Text("Min Order ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                }
                Button(
                    onClick = {
                        val pct = couponPct.toLongOrNull() ?: 0L
                        if (couponCode.length >= 2 && pct in 1..95) {
                            onSaveCoupon(
                                Coupon(
                                    code = couponCode,
                                    discountPercent = pct,
                                    maxDiscountAmount = couponMaxDisc.toDoubleOrNull() ?: 500.0,
                                    minOrderAmount = couponMinOrder.toDoubleOrNull() ?: 499.0
                                )
                            ) {
                                couponCode = ""
                                couponPct = ""
                                couponMaxDisc = ""
                                couponMinOrder = ""
                            }
                        }
                    },
                    enabled = couponCode.length >= 2 && (couponPct.toLongOrNull() ?: 0L) in 1..95
                ) {
                    Text("Save Coupon")
                }
                coupons.forEach { c ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${c.code} (${c.discountPercent}% off, max ₹${c.maxDiscountAmount.toInt()})")
                        IconButton(onClick = { onDeleteCoupon(c.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete coupon", tint = JisanDangerRed)
                        }
                    }
                }
            }
        }

        // Referrals Management
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Referral Attribution (${referrals.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (referrals.isEmpty()) {
                    Text("No referral registrations recorded yet.", style = MaterialTheme.typography.bodySmall)
                } else {
                    referrals.forEach { rec ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${rec.referredEmail} via ${rec.referrerCode}", style = MaterialTheme.typography.bodySmall)
                                Text("Status: ${rec.status} • ₹${rec.rewardAmount.toInt()}", style = MaterialTheme.typography.labelSmall)
                            }
                            if (rec.status != "REWARDED") {
                                OutlinedButton(onClick = { onApproveReferral(rec) }) { Text("Reward") }
                            } else {
                                Text("Credited", color = JisanEmeraldSuccess, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        // Broadcast Notification
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Send Customer Notification", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = notifTitle, onValueChange = { notifTitle = it }, label = { Text("Notification Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notifMessage, onValueChange = { notifMessage = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        if (notifTitle.isNotBlank() && notifMessage.isNotBlank()) {
                            onBroadcastNotification(notifTitle, notifMessage, "OFFER")
                            notifTitle = ""
                            notifMessage = ""
                        }
                    },
                    enabled = notifTitle.isNotBlank() && notifMessage.isNotBlank()
                ) {
                    Text("Broadcast to Customers")
                }
            }
        }
    }
}

@Composable
fun AdminStoreSettingsTab(
    storeConfig: StoreConfig,
    isBusy: Boolean,
    onSaveConfig: (StoreConfig) -> Unit,
    onAddSubAdmin: (String, String, String, String, String) -> Unit,
    onVerifyApkUrl: (String, (ApkVerificationResult) -> Unit) -> Unit
) {
    var shopName by remember(storeConfig) { mutableStateOf(storeConfig.shopName) }
    var tagline by remember(storeConfig) { mutableStateOf(storeConfig.shopTagline) }
    var whatsapp by remember(storeConfig) { mutableStateOf(storeConfig.supportWhatsapp) }
    var supportEmail by remember(storeConfig) { mutableStateOf(storeConfig.supportEmail) }
    var address by remember(storeConfig) { mutableStateOf(storeConfig.shopAddress) }
    var deliveryFee by remember(storeConfig) { mutableStateOf(storeConfig.deliveryCharge.toInt().toString()) }
    var freeAbove by remember(storeConfig) { mutableStateOf(storeConfig.freeDeliveryAbove.toInt().toString()) }
    var referralReward by remember(storeConfig) { mutableStateOf(storeConfig.referralRewardAmount.toInt().toString()) }
    var codEnabled by remember(storeConfig) { mutableStateOf(storeConfig.codEnabled) }
    var onlineEnabled by remember(storeConfig) { mutableStateOf(storeConfig.onlinePaymentEnabled) }
    var razorpayKey by remember(storeConfig) { mutableStateOf(storeConfig.razorpayKeyId) }
    var appVersion by remember(storeConfig) { mutableStateOf(storeConfig.appVersion) }
    var apkUrl by remember(storeConfig) { mutableStateOf(storeConfig.apkDownloadUrl) }
    var iosUrl by remember(storeConfig) { mutableStateOf(storeConfig.iosAppStoreUrl) }
    var releaseNotes by remember(storeConfig) { mutableStateOf(storeConfig.releaseNotes) }
    var apkStatusText by remember { mutableStateOf<String?>(null) }

    var subUid by remember { mutableStateOf("") }
    var subEmail by remember { mutableStateOf("") }
    var subPhone by remember { mutableStateOf("") }
    var subName by remember { mutableStateOf("") }
    var subPin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Store, Payment & App Release Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        OutlinedTextField(value = shopName, onValueChange = { shopName = it }, label = { Text("Shop Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = tagline, onValueChange = { tagline = it }, label = { Text("Tagline") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = { Text("Support WhatsApp Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = supportEmail, onValueChange = { supportEmail = it }, label = { Text("Support Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Store Address") }, modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = deliveryFee, onValueChange = { deliveryFee = it }, label = { Text("Delivery ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = freeAbove, onValueChange = { freeAbove = it }, label = { Text("Free Above ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = referralReward, onValueChange = { referralReward = it }, label = { Text("Ref Bonus ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
        }

        HorizontalDivider()
        Text("Payment Gateway (India)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Cash on Delivery (COD)")
            Switch(checked = codEnabled, onCheckedChange = { codEnabled = it })
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Online Payment (Razorpay)")
            Switch(checked = onlineEnabled, onCheckedChange = { onlineEnabled = it })
        }
        OutlinedTextField(
            value = razorpayKey,
            onValueChange = { razorpayKey = it },
            label = { Text("Razorpay Publishable Key ID (rzp_test_... or rzp_live_...)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()
        Text("App Distribution & Updates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = appVersion, onValueChange = { appVersion = it }, label = { Text("Latest Version (e.g. 1.0.0)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = apkUrl, onValueChange = { apkUrl = it }, label = { Text("Hosted HTTPS APK Download URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = {
                    onVerifyApkUrl(apkUrl) { res -> apkStatusText = res.message }
                },
                enabled = apkUrl.isNotBlank()
            ) {
                Text("Verify APK Link")
            }
            if (apkStatusText != null) {
                Text(apkStatusText.orEmpty(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            }
        }
        OutlinedTextField(value = iosUrl, onValueChange = { iosUrl = it }, label = { Text("iOS App Store URL (Optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = releaseNotes, onValueChange = { releaseNotes = it }, label = { Text("Release Notes") }, modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                onSaveConfig(
                    storeConfig.copy(
                        shopName = shopName,
                        shopTagline = tagline,
                        supportWhatsapp = whatsapp,
                        supportEmail = supportEmail,
                        shopAddress = address,
                        deliveryCharge = deliveryFee.toDoubleOrNull() ?: 49.0,
                        freeDeliveryAbove = freeAbove.toDoubleOrNull() ?: 999.0,
                        referralRewardAmount = referralReward.toDoubleOrNull() ?: 50.0,
                        codEnabled = codEnabled,
                        onlinePaymentEnabled = onlineEnabled,
                        razorpayKeyId = razorpayKey,
                        appVersion = appVersion,
                        apkDownloadUrl = apkUrl,
                        iosAppStoreUrl = iosUrl,
                        releaseNotes = releaseNotes
                    )
                )
            },
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Save Store Settings")
        }

        HorizontalDivider()
        Text("Add Authorized Co-Admin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = subUid, onValueChange = { subUid = it }, label = { Text("Google User UID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = subName, onValueChange = { subName = it }, label = { Text("Admin Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = subEmail, onValueChange = { subEmail = it }, label = { Text("Authorized Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = subPhone, onValueChange = { subPhone = it }, label = { Text("Mobile Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = subPin, onValueChange = { subPin = it }, label = { Text("Create Security Password (min 6)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedButton(
            onClick = {
                if (subUid.isNotBlank() && subEmail.isNotBlank() && subPin.length >= 6) {
                    onAddSubAdmin(subUid, subEmail, subPhone, subName, subPin)
                    subUid = ""
                    subEmail = ""
                    subPhone = ""
                    subName = ""
                    subPin = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Admin Access")
        }
    }
}
