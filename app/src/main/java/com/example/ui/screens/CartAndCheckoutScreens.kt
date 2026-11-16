package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Coupon
import com.example.model.DeliveryAddress
import com.example.model.StoreConfig
import com.example.ui.CartLineUi
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ProductPhoto
import com.example.ui.theme.JisanAmberOffer
import com.example.ui.theme.JisanDangerRed
import com.example.ui.theme.JisanEmeraldSuccess
import com.example.util.AppStrings
import com.example.util.ImageUtils
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartLines: List<CartLineUi>,
    storeConfig: StoreConfig,
    strings: AppStrings,
    onUpdateQuantity: (String, Long, Long) -> Unit,
    onRemove: (String) -> Unit,
    onProceedToCheckout: () -> Unit,
    onBrowseProducts: () -> Unit
) {
    val subtotal = cartLines.sumOf { it.product.sellingPrice * it.cartItem.quantity }
    val delivery = if (subtotal == 0.0 || subtotal >= storeConfig.freeDeliveryAbove) 0.0 else storeConfig.deliveryCharge
    val total = subtotal + delivery

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${strings.cart} (${cartLines.sumOf { it.cartItem.quantity }})") }
            )
        },
        bottomBar = {
            if (cartLines.isNotEmpty()) {
                Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = strings.totalPayable,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = ImageUtils.formatInr(total),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = onProceedToCheckout,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("proceed_checkout_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = JisanAmberOffer)
                        ) {
                            Text(strings.checkout, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (cartLines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    title = strings.emptyCart,
                    subtitle = "Add smartphones or accessories from the store to begin checkout.",
                    actionLabel = "Shop Now",
                    onAction = onBrowseProducts
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (subtotal < storeConfig.freeDeliveryAbove) {
                    item {
                        val remaining = storeConfig.freeDeliveryAbove - subtotal
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Add ${ImageUtils.formatInr(remaining)} more for FREE delivery!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                items(cartLines, key = { it.cartItem.productId }) { line ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProductPhoto(
                                imageUrl = line.product.imageUrls.firstOrNull().orEmpty(),
                                contentDescription = line.product.name,
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = line.product.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2
                                )
                                Text(
                                    text = ImageUtils.formatInr(line.product.sellingPrice),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (line.product.stockQuantity < line.cartItem.quantity) {
                                    Text(
                                        text = "Only ${line.product.stockQuantity} left in stock",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = JisanDangerRed
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(
                                        onClick = {
                                            onUpdateQuantity(
                                                line.product.id,
                                                line.cartItem.quantity - 1L,
                                                line.product.stockQuantity
                                            )
                                        },
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        text = "${line.cartItem.quantity}",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            onUpdateQuantity(
                                                line.product.id,
                                                line.cartItem.quantity + 1L,
                                                line.product.stockQuantity
                                            )
                                        },
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(onClick = { onRemove(line.product.id) }) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Remove",
                                            tint = JisanDangerRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    PriceBreakdownCard(
                        subtotal = subtotal,
                        discount = 0.0,
                        deliveryCharge = delivery,
                        total = total,
                        strings = strings
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cartLines: List<CartLineUi>,
    addresses: List<DeliveryAddress>,
    coupons: List<Coupon>,
    appliedCouponCode: String,
    discountPreview: Double,
    storeConfig: StoreConfig,
    strings: AppStrings,
    isBusy: Boolean,
    onBack: () -> Unit,
    onApplyCoupon: (String) -> Unit,
    onClearCoupon: () -> Unit,
    onSaveAddress: (DeliveryAddress, () -> Unit) -> Unit,
    onPlaceOrder: (
        address: DeliveryAddress,
        paymentMethod: String,
        paymentStatus: String,
        transactionId: String,
        idempotencyKey: String
    ) -> Unit
) {
    var selectedAddressId by remember(addresses) {
        mutableStateOf(addresses.firstOrNull { it.isDefault }?.id ?: addresses.firstOrNull()?.id.orEmpty())
    }
    var showAddAddressDialog by remember { mutableStateOf(false) }
    var couponInput by remember(appliedCouponCode) { mutableStateOf(appliedCouponCode) }
    var paymentMethod by remember { mutableStateOf(if (storeConfig.codEnabled) "COD" else "ONLINE") }
    var showSandboxPaymentSheet by remember { mutableStateOf(false) }
    val idempotencyKey = remember(cartLines.size) { "ord_${UUID.randomUUID()}" }

    val subtotal = cartLines.sumOf { it.product.sellingPrice * it.cartItem.quantity }
    val afterDiscount = (subtotal - discountPreview).coerceAtLeast(0.0)
    val delivery = if (afterDiscount == 0.0 || afterDiscount >= storeConfig.freeDeliveryAbove) 0.0 else storeConfig.deliveryCharge
    val finalTotal = afterDiscount + delivery

    val selectedAddress = addresses.firstOrNull { it.id == selectedAddressId }
    val hasValidRazorpayKey = storeConfig.onlinePaymentEnabled && storeConfig.razorpayKeyId.isNotBlank()
    val isSandboxMode = storeConfig.razorpayKeyId.startsWith("rzp_test_", ignoreCase = true) ||
        storeConfig.razorpayKeyId.isBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Delivery Address Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delivery Address", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { showAddAddressDialog = true }) {
                            Text("+ Add Address")
                        }
                    }
                    if (addresses.isEmpty()) {
                        Text(
                            text = "No saved delivery address. Please add an address to continue.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        addresses.forEach { addr ->
                            val isSelected = addr.id == selectedAddressId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedAddressId = addr.id },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = isSelected, onClick = { selectedAddressId = addr.id })
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${addr.recipientName} (${addr.label})", fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${addr.line1}${if (addr.line2.isNotBlank()) ", ${addr.line2}" else ""}, ${addr.city}, ${addr.state} - ${addr.pincode}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text("Phone: ${addr.phone}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Coupon Code Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalOffer, contentDescription = null, tint = JisanAmberOffer)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Coupons & Offers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = couponInput,
                            onValueChange = { couponInput = it.uppercase() },
                            placeholder = { Text(strings.couponHint) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (appliedCouponCode.isNotBlank()) {
                            OutlinedButton(onClick = {
                                couponInput = ""
                                onClearCoupon()
                            }) {
                                Text("Remove")
                            }
                        } else {
                            Button(onClick = { onApplyCoupon(couponInput) }) {
                                Text(strings.applyCoupon)
                            }
                        }
                    }
                    val activeCoupons = coupons.filter { it.isActive }
                    if (activeCoupons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(activeCoupons, key = { it.id }) { c ->
                                AssistChip(
                                    onClick = {
                                        couponInput = c.code
                                        onApplyCoupon(c.code)
                                    },
                                    label = { Text("${c.code} (${c.discountPercent}% OFF)") }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Payment Method Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Payment Method (India)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (storeConfig.codEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { paymentMethod = "COD" }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = paymentMethod == "COD", onClick = { paymentMethod = "COD" })
                            Column {
                                Text(strings.cashOnDelivery, fontWeight = FontWeight.SemiBold)
                                Text("Pay in cash or UPI upon delivery", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = hasValidRazorpayKey) { paymentMethod = "ONLINE" }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = paymentMethod == "ONLINE",
                            onClick = { if (hasValidRazorpayKey) paymentMethod = "ONLINE" },
                            enabled = hasValidRazorpayKey
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.onlinePayment,
                                fontWeight = FontWeight.SemiBold,
                                color = if (hasValidRazorpayKey) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = when {
                                    !hasValidRazorpayKey -> "Requires merchant Razorpay Key ID activation in Admin Settings. COD remains active."
                                    isSandboxMode -> "Sandbox Test Mode active (${storeConfig.razorpayKeyId})"
                                    else -> "Live Razorpay UPI / Cards / Netbanking"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasValidRazorpayKey) JisanEmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 4. Price Breakdown
            PriceBreakdownCard(
                subtotal = subtotal,
                discount = discountPreview,
                deliveryCharge = delivery,
                total = finalTotal,
                strings = strings
            )

            Button(
                onClick = {
                    val addr = selectedAddress ?: return@Button
                    if (paymentMethod == "COD") {
                        onPlaceOrder(addr, "COD", "PENDING", "", idempotencyKey)
                    } else {
                        showSandboxPaymentSheet = true
                    }
                },
                enabled = !isBusy && selectedAddress != null && cartLines.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("place_order_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "${strings.placeOrder} • ${ImageUtils.formatInr(finalTotal)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showAddAddressDialog) {
        AddAddressDialog(
            onDismiss = { showAddAddressDialog = false },
            onSave = { newAddr ->
                onSaveAddress(newAddr) {
                    showAddAddressDialog = false
                }
            }
        )
    }

    if (showSandboxPaymentSheet && selectedAddress != null) {
        AlertDialog(
            onDismissRequest = { showSandboxPaymentSheet = false },
            title = {
                Text(if (isSandboxMode) "Razorpay Sandbox Gateway" else "Online Payment Verification")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Merchant Key: ${storeConfig.razorpayKeyId}")
                    Text("Payable Amount: ${ImageUtils.formatInr(finalTotal)}", fontWeight = FontWeight.Bold)
                    Text(
                        text = "Select sandbox payment outcome to test verified callback handling:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            showSandboxPaymentSheet = false
                            val txnId = "rzp_test_pay_${UUID.randomUUID().toString().take(8)}"
                            onPlaceOrder(selectedAddress, "ONLINE", "PAID", txnId, idempotencyKey)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simulate Sandbox Success (PAID)")
                    }
                    OutlinedButton(
                        onClick = {
                            showSandboxPaymentSheet = false
                            val txnId = "rzp_test_pend_${UUID.randomUUID().toString().take(8)}"
                            onPlaceOrder(selectedAddress, "ONLINE", "PENDING", txnId, idempotencyKey)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Record Payment Pending (PENDING)")
                    }
                    TextButton(
                        onClick = { showSandboxPaymentSheet = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel / Payment Failed")
                    }
                }
            }
        )
    }
}

@Composable
fun PriceBreakdownCard(
    subtotal: Double,
    discount: Double,
    deliveryCharge: Double,
    total: Double,
    strings: AppStrings
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(strings.subtotal)
                Text(ImageUtils.formatInr(subtotal))
            }
            if (discount > 0.0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.discount, color = JisanEmeraldSuccess)
                    Text("- ${ImageUtils.formatInr(discount)}", color = JisanEmeraldSuccess)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(strings.deliveryCharge)
                Text(
                    text = if (deliveryCharge == 0.0) strings.free else ImageUtils.formatInr(deliveryCharge),
                    color = if (deliveryCharge == 0.0) JisanEmeraldSuccess else MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(strings.totalPayable, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(ImageUtils.formatInr(total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AddAddressDialog(
    existing: DeliveryAddress? = null,
    onDismiss: () -> Unit,
    onSave: (DeliveryAddress) -> Unit
) {
    var label by remember { mutableStateOf(existing?.label ?: "Home") }
    var name by remember { mutableStateOf(existing?.recipientName ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var line1 by remember { mutableStateOf(existing?.line1 ?: "") }
    var line2 by remember { mutableStateOf(existing?.line2 ?: "") }
    var city by remember { mutableStateOf(existing?.city ?: "") }
    var state by remember { mutableStateOf(existing?.state ?: "") }
    var pincode by remember { mutableStateOf(existing?.pincode ?: "") }
    var interacted by remember { mutableStateOf(false) }

    val valid = name.isNotBlank() && phone.filter { it.isDigit() }.length >= 10 &&
        line1.isNotBlank() && city.isNotBlank() && state.isNotBlank() && pincode.length >= 6

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Delivery Address" else "Edit Address") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label (Home / Work)") }, singleLine = true)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; interacted = true },
                    label = { Text("Recipient Name*") },
                    isError = interacted && name.isBlank(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; interacted = true },
                    label = { Text("10-digit Mobile*") },
                    isError = interacted && phone.filter { c -> c.isDigit() }.length < 10,
                    singleLine = true
                )
                OutlinedTextField(
                    value = line1,
                    onValueChange = { line1 = it; interacted = true },
                    label = { Text("House / Street / Area*") },
                    isError = interacted && line1.isBlank()
                )
                OutlinedTextField(value = line2, onValueChange = { line2 = it }, label = { Text("Landmark (Optional)") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it; interacted = true },
                        label = { Text("City*") },
                        isError = interacted && city.isBlank(),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pincode,
                        onValueChange = { pincode = it; interacted = true },
                        label = { Text("PIN*") },
                        isError = interacted && pincode.length < 6,
                        modifier = Modifier.weight(0.8f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = state,
                    onValueChange = { state = it; interacted = true },
                    label = { Text("State*") },
                    isError = interacted && state.isBlank(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                interacted = true
                if (valid) {
                    onSave(
                        DeliveryAddress(
                            id = existing?.id.orEmpty(),
                            label = label,
                            recipientName = name,
                            phone = phone,
                            line1 = line1,
                            line2 = line2,
                            city = city,
                            state = state,
                            pincode = pincode,
                            isDefault = existing?.isDefault ?: true,
                            createdAt = existing?.createdAt
                        )
                    )
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun OrderConfirmationScreen(
    orderId: String,
    onTrackOrder: () -> Unit,
    onContinueShopping: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Order Confirmed",
                    tint = JisanEmeraldSuccess,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Order Placed Successfully!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Order ID: $orderId",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Stock has been reserved and your order is now tracked live in My Orders.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onTrackOrder,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Track Order")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onContinueShopping,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Continue Shopping")
                }
            }
        }
    }
}
