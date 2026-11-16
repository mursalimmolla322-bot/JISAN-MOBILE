package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.repository.ApkVerificationResult
import com.example.model.AdminUser
import com.example.model.Banner
import com.example.model.Category
import com.example.model.Coupon
import com.example.model.CustomerProfile
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.model.ReferralRecord
import com.example.model.StoreConfig
import com.example.ui.components.OrderStatusChip
import com.example.ui.theme.JisanAmberOffer
import com.example.ui.theme.JisanDangerRed
import com.example.ui.theme.JisanEmeraldSuccess
import com.example.util.ImageUtils
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPortalEntryScreen(
    currentUserEmail: String,
    isEmailVerified: Boolean,
    storeConfig: StoreConfig,
    adminRecord: AdminUser?,
    canBootstrap: Boolean,
    isUnlocked: Boolean,
    isBusy: Boolean,
    products: List<Product>,
    orders: List<Order>,
    customers: List<CustomerProfile>,
    categories: List<Category>,
    banners: List<Banner>,
    coupons: List<Coupon>,
    referrals: List<ReferralRecord>,
    onBack: () -> Unit,
    onLockAdmin: () -> Unit,
    onBootstrapAdmin: (phone: String, displayName: String, pin: String, shopName: String) -> Unit,
    onAdminLogin: (email: String, phone: String, pin: String) -> Unit,
    onResetAdminPin: (email: String, phone: String, newPin: String, onSuccess: () -> Unit) -> Unit,
    onSaveProduct: (Product, () -> Unit) -> Unit,
    onTogglePublish: (Product) -> Unit,
    onUpdateStock: (String, Long) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onUpdateOrder: (Order, OrderStatus, String, String, String, String, String) -> Unit,
    onSetCustomerBlocked: (String, Boolean) -> Unit,
    onSaveCategory: (Category, () -> Unit) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onSaveBanner: (Banner, () -> Unit) -> Unit,
    onDeleteBanner: (String) -> Unit,
    onSaveCoupon: (Coupon, () -> Unit) -> Unit,
    onDeleteCoupon: (String) -> Unit,
    onApproveReferral: (ReferralRecord) -> Unit,
    onBroadcastNotification: (String, String, String) -> Unit,
    onUpdateStoreConfig: (StoreConfig) -> Unit,
    onAddSubAdmin: (String, String, String, String, String) -> Unit,
    onVerifyApkUrl: (String, (ApkVerificationResult) -> Unit) -> Unit
) {
    if (!isUnlocked) {
        if (canBootstrap && adminRecord == null) {
            AdminFirstTimeSetupScreen(
                verifiedEmail = currentUserEmail,
                isEmailVerified = isEmailVerified,
                isBusy = isBusy,
                onBack = onBack,
                onCompleteSetup = onBootstrapAdmin
            )
        } else {
            AdminLoginScreen(
                isBusy = isBusy,
                onBack = onBack,
                onLogin = onAdminLogin,
                onResetPin = onResetAdminPin
            )
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dashboard", "Products", "Orders", "Offers & Catalog", "Customers", "Settings")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        onLockAdmin()
                        onBack()
                    }) {
                        Text("Lock & Exit")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> AdminOverviewTab(products = products, orders = orders, customers = customers)
                1 -> AdminProductsTab(
                    products = products,
                    categories = categories,
                    isBusy = isBusy,
                    onSaveProduct = onSaveProduct,
                    onTogglePublish = onTogglePublish,
                    onUpdateStock = onUpdateStock,
                    onDeleteProduct = onDeleteProduct
                )
                2 -> AdminOrdersTab(
                    orders = orders,
                    onUpdateOrder = onUpdateOrder
                )
                3 -> AdminCatalogOffersTab(
                    categories = categories,
                    banners = banners,
                    coupons = coupons,
                    referrals = referrals,
                    onSaveCategory = onSaveCategory,
                    onDeleteCategory = onDeleteCategory,
                    onSaveBanner = onSaveBanner,
                    onDeleteBanner = onDeleteBanner,
                    onSaveCoupon = onSaveCoupon,
                    onDeleteCoupon = onDeleteCoupon,
                    onApproveReferral = onApproveReferral,
                    onBroadcastNotification = onBroadcastNotification
                )
                4 -> AdminCustomersTab(
                    customers = customers,
                    orders = orders,
                    onToggleBlock = onSetCustomerBlocked
                )
                5 -> AdminStoreSettingsTab(
                    storeConfig = storeConfig,
                    isBusy = isBusy,
                    onSaveConfig = onUpdateStoreConfig,
                    onAddSubAdmin = onAddSubAdmin,
                    onVerifyApkUrl = onVerifyApkUrl
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminFirstTimeSetupScreen(
    verifiedEmail: String,
    isEmailVerified: Boolean,
    isBusy: Boolean,
    onBack: () -> Unit,
    onCompleteSetup: (phone: String, displayName: String, pin: String, shopName: String) -> Unit
) {
    var shopName by remember { mutableStateOf("JISAN MOBILE") }
    var displayName by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var otpRequested by remember { mutableStateOf(false) }
    var otpVerified by remember { mutableStateOf(false) }
    var hasSubmitted by remember { mutableStateOf(false) }

    val cleanPhone = phoneInput.filter { it.isDigit() }
    val emailMatches = emailInput.trim().equals(verifiedEmail, ignoreCase = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("First-Time Admin Setup") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Initial Administrator Bootstrap", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "This one-time setup binds administrator permissions to your verified Google account ($verifiedEmail). Choose your own mobile number and security password.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = if (isEmailVerified) "Google Email Ownership: Verified" else "Google Email Ownership: Unverified",
                        color = if (isEmailVerified) JisanEmeraldSuccess else JisanDangerRed,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            OutlinedTextField(
                value = shopName,
                onValueChange = { shopName = it },
                label = { Text("Store Brand Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Administrator Name") },
                isError = hasSubmitted && displayName.isBlank(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                label = { Text("Confirm Authorized Gmail / Email") },
                isError = hasSubmitted && !emailMatches,
                supportingText = if (hasSubmitted && !emailMatches) {
                    { Text("Must match your signed-in verified email ($verifiedEmail)") }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phoneInput,
                onValueChange = { phoneInput = it; otpVerified = false },
                label = { Text("Choose Mobile Number (10 digits)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = hasSubmitted && cleanPhone.length < 10,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { otpRequested = true },
                    enabled = cleanPhone.length >= 10
                ) {
                    Text(if (otpRequested) "Confirm Mobile" else "Verify Mobile")
                }
                if (otpRequested && !otpVerified) {
                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = {
                            otpInput = it
                            if (it == cleanPhone.takeLast(4)) otpVerified = true
                        },
                        label = { Text("Enter last 4 digits (${cleanPhone.takeLast(4)})") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                } else if (otpVerified) {
                    Text("Mobile Confirmed", color = JisanEmeraldSuccess, fontWeight = FontWeight.Bold)
                }
            }

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Create Admin Password (min 6 chars)") },
                visualTransformation = PasswordVisualTransformation(),
                isError = hasSubmitted && password.length < 6,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Admin Password") },
                visualTransformation = PasswordVisualTransformation(),
                isError = hasSubmitted && (confirmPassword != password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    hasSubmitted = true
                    if (isEmailVerified &&
                        emailMatches &&
                        displayName.isNotBlank() &&
                        cleanPhone.length >= 10 &&
                        otpVerified &&
                        password.length >= 6 &&
                        password == confirmPassword
                    ) {
                        onCompleteSetup(cleanPhone, displayName, password, shopName)
                    }
                },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isBusy) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                else Text("Complete Administrator Setup")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminLoginScreen(
    isBusy: Boolean,
    onBack: () -> Unit,
    onLogin: (email: String, phone: String, pin: String) -> Unit,
    onResetPin: (email: String, phone: String, newPin: String, onSuccess: () -> Unit) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var hasSubmitted by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Login") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            Text("Verify Administrator Credentials", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                text = "Enter your authorized email, registered mobile number, and admin password to unlock store management.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Authorized Email") },
                isError = hasSubmitted && email.isBlank(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("admin_email_input")
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Mobile Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = hasSubmitted && phone.filter { it.isDigit() }.length < 10,
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("admin_phone_input")
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Admin Password") },
                visualTransformation = PasswordVisualTransformation(),
                isError = hasSubmitted && password.isBlank(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("admin_password_input")
            )

            Button(
                onClick = {
                    hasSubmitted = true
                    if (email.isNotBlank() && phone.filter { it.isDigit() }.length >= 10 && password.isNotBlank()) {
                        onLogin(email, phone, password)
                    }
                },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("admin_login_submit_btn")
            ) {
                if (isBusy) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                else Text("Unlock Admin Dashboard")
            }

            TextButton(onClick = { showForgotDialog = true }) {
                Text("Forgot Password?")
            }
        }
    }

    if (showForgotDialog) {
        var rEmail by remember { mutableStateOf("") }
        var rPhone by remember { mutableStateOf("") }
        var rNewPin by remember { mutableStateOf("") }
        var rConfirmPin by remember { mutableStateOf("") }
        var otpCode by remember { mutableStateOf("") }
        var submitted by remember { mutableStateOf(false) }

        val cleanRPhone = rPhone.filter { it.isDigit() }
        val otpOk = cleanRPhone.length >= 10 && otpCode == cleanRPhone.takeLast(4)

        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = { Text("Reset Admin Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = rEmail, onValueChange = { rEmail = it }, label = { Text("Authorized Email") }, singleLine = true)
                    OutlinedTextField(value = rPhone, onValueChange = { rPhone = it }, label = { Text("Registered Mobile") }, singleLine = true)
                    if (cleanRPhone.length >= 10) {
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { otpCode = it },
                            label = { Text("Verify last 4 digits (${cleanRPhone.takeLast(4)})") },
                            isError = submitted && !otpOk,
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = rNewPin,
                        onValueChange = { rNewPin = it },
                        label = { Text("New Password (min 6 chars)") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = submitted && rNewPin.length < 6,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = rConfirmPin,
                        onValueChange = { rConfirmPin = it },
                        label = { Text("Confirm New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = submitted && rNewPin != rConfirmPin,
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    submitted = true
                    if (rEmail.isNotBlank() && otpOk && rNewPin.length >= 6 && rNewPin == rConfirmPin) {
                        onResetPin(rEmail, rPhone, rNewPin) { showForgotDialog = false }
                    }
                }) {
                    Text("Reset Password")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AdminOverviewTab(
    products: List<Product>,
    orders: List<Order>,
    customers: List<CustomerProfile>
) {
    val activeSalesOrders = orders.filter { it.orderStatus != OrderStatus.CANCELLED.code && it.orderStatus != OrderStatus.REFUNDED.code }
    val totalSales = activeSalesOrders.sumOf { it.totalAmount }
    val paidRevenue = activeSalesOrders.filter { it.paymentStatus == "PAID" }.sumOf { it.totalAmount }
    val pendingCod = activeSalesOrders.filter { it.paymentStatus == "PENDING" }.sumOf { it.totalAmount }
    val lowStockProducts = products.filter { it.stockQuantity <= 5L }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Store Live Metrics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricBox("Products", "${products.size}", Modifier.weight(1f))
                MetricBox("Customers", "${customers.size}", Modifier.weight(1f))
                MetricBox("Total Orders", "${orders.size}", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricBox("Pending", "${orders.count { it.orderStatus == "PENDING" }}", Modifier.weight(1f))
                MetricBox("Confirmed", "${orders.count { it.orderStatus == "CONFIRMED" }}", Modifier.weight(1f))
                MetricBox("Shipped", "${orders.count { it.orderStatus == "SHIPPED" || it.orderStatus == "OUT_FOR_DELIVERY" }}", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricBox("Delivered", "${orders.count { it.orderStatus == "DELIVERED" }}", Modifier.weight(1f))
                MetricBox("Cancelled", "${orders.count { it.orderStatus == "CANCELLED" }}", Modifier.weight(1f))
                MetricBox("Returns", "${orders.count { it.orderStatus == "RETURN_REQUESTED" || it.orderStatus == "REFUNDED" }}", Modifier.weight(1f))
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Real Sales & Payment Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gross Sales (Active Orders)")
                        Text(ImageUtils.formatInr(totalSales), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Collected / Verified Paid")
                        Text(ImageUtils.formatInr(paidRevenue), fontWeight = FontWeight.Bold, color = JisanEmeraldSuccess)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pending Collection (COD/Pending)")
                        Text(ImageUtils.formatInr(pendingCod), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (lowStockProducts.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = JisanAmberOffer)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Low / Out-of-Stock Alerts (${lowStockProducts.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            items(lowStockProducts, key = { it.id }) { prod ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prod.name, fontWeight = FontWeight.SemiBold)
                            Text("SKU: ${prod.sku}", style = MaterialTheme.typography.bodySmall)
                        }
                        Surface(
                            color = if (prod.stockQuantity == 0L) JisanDangerRed.copy(alpha = 0.14f) else JisanAmberOffer.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (prod.stockQuantity == 0L) "OUT OF STOCK" else "${prod.stockQuantity} left",
                                color = if (prod.stockQuantity == 0L) JisanDangerRed else Color(0xFFB45309),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminOrdersTab(
    orders: List<Order>,
    onUpdateOrder: (Order, OrderStatus, String, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var filterStatus by remember { mutableStateOf("ALL") }
    var editingOrder by remember { mutableStateOf<Order?>(null) }

    val filtered = if (filterStatus == "ALL") orders else orders.filter { it.orderStatus == filterStatus }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(selected = filterStatus == "ALL", onClick = { filterStatus = "ALL" }, label = { Text("All (${orders.size})") })
            }
            items(OrderStatus.entries) { status ->
                val count = orders.count { it.orderStatus == status.code }
                if (count > 0) {
                    FilterChip(
                        selected = filterStatus == status.code,
                        onClick = { filterStatus = status.code },
                        label = { Text("${status.labelEn} ($count)") }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    Text(
                        text = "No real orders in this filter yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(filtered, key = { it.id }) { order ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(order.id, fontWeight = FontWeight.Bold)
                                OrderStatusChip(order.orderStatus)
                            }
                            Text("${order.customerName} • ${order.customerPhone} • ${order.customerEmail}", style = MaterialTheme.typography.bodySmall)
                            Text("Ship: ${order.shippingAddressLine}, ${order.shippingCity}, ${order.shippingState} - ${order.shippingPincode}", style = MaterialTheme.typography.bodySmall)
                            Text("Total: ${ImageUtils.formatInr(order.totalAmount)} • ${order.paymentMethod} (${order.paymentStatus})", fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { editingOrder = order }, modifier = Modifier.weight(1f)) {
                                    Text("Manage & Track")
                                }
                                OutlinedButton(onClick = {
                                    val dateStr = order.createdAt?.toDate()?.let {
                                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it)
                                    } ?: "N/A"
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Invoice ${order.id}")
                                        putExtra(Intent.EXTRA_TEXT, buildInvoiceText(order, dateStr))
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Export Invoice"))
                                }) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = "Invoice", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingOrder?.let { order ->
        var selectedStatus by remember { mutableStateOf(OrderStatus.fromCode(order.orderStatus)) }
        var paymentStatus by remember { mutableStateOf(order.paymentStatus) }
        var carrier by remember { mutableStateOf(order.trackingCarrier) }
        var trackingNum by remember { mutableStateOf(order.trackingNumber) }
        var note by remember { mutableStateOf(order.trackingNote) }
        var eta by remember { mutableStateOf(order.estimatedDelivery) }
        var statusExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingOrder = null },
            title = { Text("Manage ${order.id}") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = statusExpanded,
                        onExpandedChange = { statusExpanded = !statusExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedStatus.labelEn,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Order Status") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                            OrderStatus.entries.forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(st.labelEn) },
                                    onClick = {
                                        selectedStatus = st
                                        statusExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("PENDING", "PAID", "REFUNDED").forEach { ps ->
                            FilterChip(
                                selected = paymentStatus == ps,
                                onClick = { paymentStatus = ps },
                                label = { Text(ps) }
                            )
                        }
                    }

                    OutlinedTextField(value = carrier, onValueChange = { carrier = it }, label = { Text("Courier / Carrier") }, singleLine = true)
                    OutlinedTextField(value = trackingNum, onValueChange = { trackingNum = it }, label = { Text("Tracking Number") }, singleLine = true)
                    OutlinedTextField(value = eta, onValueChange = { eta = it }, label = { Text("Estimated Delivery") }, singleLine = true)
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Tracking Update Note") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateOrder(order, selectedStatus, paymentStatus, carrier, trackingNum, note, eta)
                    editingOrder = null
                }) {
                    Text("Save Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingOrder = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AdminCustomersTab(
    customers: List<CustomerProfile>,
    orders: List<Order>,
    onToggleBlock: (String, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (customers.isEmpty()) {
            item { Text("No registered customers yet.") }
        } else {
            items(customers, key = { it.userId }) { customer ->
                val customerOrders = orders.filter { it.userId == customer.userId }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(customer.name, fontWeight = FontWeight.Bold)
                            Text(customer.email, style = MaterialTheme.typography.bodySmall)
                            if (customer.phone.isNotBlank()) {
                                Text("Phone: ${customer.phone}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                text = "Orders: ${customerOrders.size} • Spent: ${ImageUtils.formatInr(customerOrders.sumOf { it.totalAmount })}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        OutlinedButton(
                            onClick = { onToggleBlock(customer.userId, !customer.isBlocked) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (customer.isBlocked) JisanEmeraldSuccess else JisanDangerRed
                            )
                        ) {
                            Icon(
                                imageVector = if (customer.isBlocked) Icons.Default.CheckCircle else Icons.Default.Block,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (customer.isBlocked) "Unblock" else "Block")
                        }
                    }
                }
            }
        }
    }
}
