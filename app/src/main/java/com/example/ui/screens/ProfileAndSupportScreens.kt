package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.ApkVerificationResult
import com.example.model.AppNotification
import com.example.model.CustomerProfile
import com.example.model.DeliveryAddress
import com.example.model.Product
import com.example.model.ReferralRecord
import com.example.model.StoreConfig
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ProductGridCard
import com.example.ui.components.ProductPhoto
import com.example.ui.theme.JisanDangerRed
import com.example.ui.theme.JisanEmeraldSuccess
import com.example.util.AppStrings
import com.example.util.ImageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: CustomerProfile?,
    storeConfig: StoreConfig,
    strings: AppStrings,
    unreadNotifications: Int,
    wishlistCount: Int,
    showAdminPortalOption: Boolean,
    onEditProfile: (name: String, phone: String, photoUrl: String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onOpenWishlist: () -> Unit,
    onOpenAddresses: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenReferrals: () -> Unit,
    onOpenUpdates: () -> Unit,
    onOpenSupportPolicies: () -> Unit,
    onOpenAdminPortal: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(strings.profile) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProductPhoto(
                        imageUrl = profile?.photoUrl.orEmpty(),
                        contentDescription = "Profile photo",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile?.name?.ifBlank { "JISAN Shopper" } ?: "JISAN Shopper",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = profile?.email.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!profile?.phone.isNullOrBlank()) {
                            Text(
                                text = "Phone: ${profile?.phone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit profile")
                    }
                }
            }

            // Preferences (Language & Dark Mode)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(strings.languageLabel, style = MaterialTheme.typography.titleSmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = (profile?.preferredLanguage ?: "en") == "en",
                                onClick = { onLanguageChange("en") },
                                label = { Text("English") }
                            )
                            FilterChip(
                                selected = profile?.preferredLanguage == "bn",
                                onClick = { onLanguageChange("bn") },
                                label = { Text("বাংলা") }
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(strings.darkModeLabel, style = MaterialTheme.typography.titleSmall)
                        }
                        Switch(
                            checked = profile?.darkMode == true,
                            onCheckedChange = onDarkModeChange
                        )
                    }
                }
            }

            // Account Sections
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    ProfileMenuRow(Icons.Default.Favorite, "${strings.wishlist} ($wishlistCount)", onOpenWishlist)
                    HorizontalDivider()
                    ProfileMenuRow(Icons.Default.LocationOn, strings.savedAddresses, onOpenAddresses)
                    HorizontalDivider()
                    ProfileMenuRow(
                        Icons.Default.Notifications,
                        if (unreadNotifications > 0) "${strings.notifications} ($unreadNotifications new)" else strings.notifications,
                        onOpenNotifications
                    )
                    HorizontalDivider()
                    ProfileMenuRow(Icons.Default.Share, strings.shareAndRefer, onOpenReferrals)
                    HorizontalDivider()
                    ProfileMenuRow(Icons.Default.SystemUpdate, "${strings.appUpdates} (v${storeConfig.appVersion})", onOpenUpdates)
                    HorizontalDivider()
                    ProfileMenuRow(Icons.Default.SupportAgent, strings.supportPolicies, onOpenSupportPolicies)
                }
            }

            if (showAdminPortalOption) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenAdminPortal)
                        .testTag("open_admin_portal_btn"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.adminPortal,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Authorized Store Owner Access",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sign_out_btn"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = JisanDangerRed)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.signOut, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showEditDialog) {
        var name by remember { mutableStateOf(profile?.name.orEmpty()) }
        var phone by remember { mutableStateOf(profile?.phone.orEmpty()) }
        var photoUrl by remember { mutableStateOf(profile?.photoUrl.orEmpty()) }

        val photoPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri ->
                uri?.let {
                    ImageUtils.compressUriToBase64DataUri(context, it, maxDimension = 360)
                        .onSuccess { dataUri -> photoUrl = dataUri }
                        .onFailure { err ->
                            Toast.makeText(context, err.localizedMessage ?: "Failed to load image", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        )

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProductPhoto(
                            imageUrl = photoUrl,
                            contentDescription = "Avatar preview",
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Text("Choose Photo")
                        }
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Mobile Number") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onEditProfile(name, phone, photoUrl)
                    showEditDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProfileMenuRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    products: List<Product>,
    wishlistIds: Set<String>,
    strings: AppStrings,
    onBack: () -> Unit,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onToggleWishlist: (Product) -> Unit
) {
    val saved = products.filter { wishlistIds.contains(it.id) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.wishlist) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (saved.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyStateView(
                    title = "Wishlist is empty",
                    subtitle = "Tap the heart icon on any product to save it here."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(saved.chunked(2)) { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEach { product ->
                            ProductGridCard(
                                product = product,
                                isWishlisted = true,
                                addToCartLabel = strings.addToCart,
                                outOfStockLabel = strings.outOfStock,
                                onClick = { onProductClick(product) },
                                onAddToCart = { onAddToCart(product) },
                                onToggleWishlist = { onToggleWishlist(product) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedAddressesScreen(
    addresses: List<DeliveryAddress>,
    onBack: () -> Unit,
    onSaveAddress: (DeliveryAddress, () -> Unit) -> Unit,
    onDeleteAddress: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Addresses") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showAddDialog = true }) { Text("+ Add") }
                }
            )
        }
    ) { padding ->
        if (addresses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyStateView(
                    title = "No saved addresses",
                    subtitle = "Add your home or office address for faster checkout.",
                    actionLabel = "Add Address",
                    onAction = { showAddDialog = true }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(addresses, key = { it.id }) { addr ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${addr.recipientName} (${addr.label})", fontWeight = FontWeight.Bold)
                                Text("${addr.line1}, ${addr.city}, ${addr.state} - ${addr.pincode}")
                                Text("Phone: ${addr.phone}", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { onDeleteAddress(addr.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = JisanDangerRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddAddressDialog(
            onDismiss = { showAddDialog = false },
            onSave = { addr -> onSaveAddress(addr) { showAddDialog = false } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    onBack: () -> Unit,
    onMarkRead: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyStateView(
                    title = "No notifications yet",
                    subtitle = "Order confirmations, shipping updates, and store offers will appear here."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications, key = { it.id }) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (!note.isRead) onMarkRead(note.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (note.isRead) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(note.title, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(note.message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralsScreen(
    profile: CustomerProfile?,
    referrals: List<ReferralRecord>,
    storeConfig: StoreConfig,
    onBack: () -> Unit,
    onApplyReferralCode: (String) -> Unit
) {
    val context = LocalContext.current
    val myCode = profile?.referralCode.orEmpty()
    val referralLink = "https://ais-pre-gtxzzzwq53kiy4xk46fzkv-438215615179.asia-east1.run.app/?ref=$myCode"
    var friendCodeInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share & Refer") },
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Invite Friends & Earn ${ImageUtils.formatInr(storeConfig.referralRewardAmount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your Unique Referral Code: $myCode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = referralLink,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Rewards Earned: ${ImageUtils.formatInr(profile?.referralRewardEarned ?: 0.0)} • Total Referrals: ${referrals.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = JisanEmeraldSuccess
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Referral Link", referralLink))
                                Toast.makeText(context, "Referral link copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Link")
                        }
                        Button(
                            onClick = {
                                val msg = "Shop genuine mobiles at JISAN MOBILE! Use my referral code $myCode: $referralLink"
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, msg)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share JISAN MOBILE"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val msg = Uri.encode("Shop genuine mobiles at JISAN MOBILE! Use code $myCode: $referralLink")
                                val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=$msg"))
                                context.startActivity(waIntent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("WhatsApp")
                        }
                        OutlinedButton(
                            onClick = {
                                val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply {
                                    putExtra("sms_body", "Shop at JISAN MOBILE with referral code $myCode: $referralLink")
                                }
                                context.startActivity(smsIntent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("SMS")
                        }
                    }
                }
            }

            if (profile?.referredByCode.isNullOrBlank()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Have a Friend's Referral Code?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = friendCodeInput,
                                onValueChange = { friendCodeInput = it.uppercase() },
                                label = { Text("Enter referral code") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onApplyReferralCode(friendCodeInput) },
                                enabled = friendCodeInput.isNotBlank()
                            ) {
                                Text("Apply")
                            }
                        }
                    }
                }
            } else {
                Surface(
                    color = JisanEmeraldSuccess.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Referred by code: ${profile?.referredByCode}",
                        color = JisanEmeraldSuccess,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Text("My Referred Registrations (${referrals.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (referrals.isEmpty()) {
                Text(
                    text = "No referrals attributed yet. Share your link to start earning.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                referrals.forEach { rec ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rec.referredEmail, fontWeight = FontWeight.SemiBold)
                                Text("Status: ${rec.status}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(ImageUtils.formatInr(rec.rewardAmount), fontWeight = FontWeight.Bold, color = JisanEmeraldSuccess)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdatesAndDistributionScreen(
    storeConfig: StoreConfig,
    onBack: () -> Unit,
    onVerifyApkUrl: (String, (ApkVerificationResult) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var verificationResult by remember { mutableStateOf<ApkVerificationResult?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    LaunchedEffect(storeConfig.apkDownloadUrl) {
        if (storeConfig.apkDownloadUrl.startsWith("https://")) {
            isChecking = true
            onVerifyApkUrl(storeConfig.apkDownloadUrl) { res ->
                verificationResult = res
                isChecking = false
            }
        } else {
            verificationResult = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Download & Updates") },
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Installed Version: 1.0.0", fontWeight = FontWeight.Bold)
                    Text("Latest Store Version: ${storeConfig.appVersion}")
                    if (storeConfig.releaseNotes.isNotBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Release Notes", fontWeight = FontWeight.SemiBold)
                        Text(storeConfig.releaseNotes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Android APK Distribution (Verified HTTPS only)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Android, contentDescription = null, tint = JisanEmeraldSuccess)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Android APK Distribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    when {
                        storeConfig.apkDownloadUrl.isBlank() -> {
                            Text(
                                text = "No hosted signed APK URL has been published by the store administrator yet. Admins can configure a verified HTTPS APK URL in Admin Portal > Store Settings.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        isChecking -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verifying hosted APK availability...")
                            }
                        }
                        verificationResult?.reachable == true -> {
                            val res = verificationResult!!
                            Text(
                                text = "Verified APK Ready • Size: ${res.fileSizeMb.ifBlank { storeConfig.apkFileSizeMb.ifBlank { "Verified" } }}",
                                color = JisanEmeraldSuccess,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Install instructions: Download the signed APK, open it from your Downloads folder, and allow installation from your browser if prompted.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(storeConfig.apkDownloadUrl)))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Download Android APK (${storeConfig.appVersion})")
                            }
                        }
                        else -> {
                            Text(
                                text = "APK URL verification failed: ${verificationResult?.message ?: "Unreachable link"}",
                                color = JisanDangerRed,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // iOS Distribution (Never APK!)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("iPhone / iOS Distribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (storeConfig.iosAppStoreUrl.startsWith("https://")) {
                        Text("Available on the Apple App Store:")
                        Button(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(storeConfig.iosAppStoreUrl)))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open on App Store")
                        }
                    } else {
                        Text(
                            text = "Note: Android APK files cannot run on iPhone. Until App Store publication is completed, iPhone shoppers can install JISAN MOBILE via Safari: open the store web link, tap Share, and select 'Add to Home Screen'.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportAndPoliciesScreen(
    storeConfig: StoreConfig,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support & Store Policies") },
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(10.dp),
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.jisan_mobile_logo_1794840059210),
                                contentDescription = "JISAN MOBILE Logo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Contact JISAN MOBILE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("মোবাইল মানেই জিসান মোবাইল", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (storeConfig.supportEmail.isNotBlank()) Text("Email: ${storeConfig.supportEmail}")
                    if (storeConfig.supportWhatsapp.isNotBlank()) Text("WhatsApp: ${storeConfig.supportWhatsapp}")
                    if (storeConfig.shopAddress.isNotBlank()) Text("Address: ${storeConfig.shopAddress}")

                    if (storeConfig.supportWhatsapp.isNotBlank()) {
                        Button(
                            onClick = {
                                val digits = storeConfig.supportWhatsapp.filter { it.isDigit() }
                                val formatted = if (digits.length == 10) "91$digits" else digits
                                val url = "https://wa.me/$formatted?text=${Uri.encode("Hello JISAN MOBILE support, I need help with my order.")}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Chat on WhatsApp Support")
                        }
                    }
                }
            }

            PolicyCard("About JISAN MOBILE", storeConfig.shopTagline)
            PolicyCard("Privacy Policy", storeConfig.privacyPolicy)
            PolicyCard("Terms & Conditions", storeConfig.termsText)
            PolicyCard("Shipping Policy", storeConfig.shippingPolicy)
            PolicyCard("Refund & Return Policy", storeConfig.refundPolicy)
        }
    }
}

@Composable
private fun PolicyCard(title: String, body: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
