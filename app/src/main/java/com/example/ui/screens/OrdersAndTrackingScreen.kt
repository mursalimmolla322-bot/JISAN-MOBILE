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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.UiState
import com.example.ui.components.EmptyStateView
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.ProductPhoto
import com.example.ui.theme.JisanDangerRed
import com.example.ui.theme.JisanEmeraldSuccess
import com.example.util.AppStrings
import com.example.util.ImageUtils
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    ordersState: UiState<List<Order>>,
    strings: AppStrings,
    isBengali: Boolean,
    onOrderClick: (Order) -> Unit,
    onBrowseProducts: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(strings.orders) })
        }
    ) { padding ->
        when (ordersState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        title = "Unable to load orders",
                        subtitle = ordersState.message
                    )
                }
            }
            is UiState.Success -> {
                val orders = ordersState.data
                if (orders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateView(
                            title = strings.emptyOrders,
                            subtitle = "Your genuine purchase history and live delivery tracking will appear here.",
                            actionLabel = "Explore Store",
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
                        items(orders, key = { it.id }) { order ->
                            OrderSummaryCard(
                                order = order,
                                isBengali = isBengali,
                                onClick = { onOrderClick(order) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderSummaryCard(
    order: Order,
    isBengali: Boolean,
    onClick: () -> Unit
) {
    val dateStr = remember(order.createdAt) {
        order.createdAt?.toDate()?.let {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(it)
        } ?: "Just now"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("order_card_${order.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.id,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                OrderStatusChip(order.orderStatus, isBengali = isBengali)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$dateStr • ${order.paymentMethod} (${order.paymentStatus})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            order.items.take(2).forEach { item ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProductPhoto(
                        imageUrl = item.imageUrl,
                        contentDescription = item.productName,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${item.productName} × ${item.quantity}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Text(
                        text = ImageUtils.formatInr(item.lineTotal),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (order.items.size > 2) {
                Text(
                    text = "+ ${order.items.size - 2} more item(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Paid/Payable", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = ImageUtils.formatInr(order.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailTrackingScreen(
    order: Order,
    isBengali: Boolean,
    onBack: () -> Unit,
    onCancelOrder: (String) -> Unit,
    onRequestReturn: (String) -> Unit
) {
    val context = LocalContext.current
    var reasonDialogMode by remember { mutableStateOf<String?>(null) }

    val dateStr = remember(order.createdAt) {
        order.createdAt?.toDate()?.let {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(it)
        } ?: "Pending timestamp"
    }

    val trackingSteps = listOf(
        OrderStatus.PENDING,
        OrderStatus.CONFIRMED,
        OrderStatus.PROCESSING,
        OrderStatus.PACKED,
        OrderStatus.SHIPPED,
        OrderStatus.OUT_FOR_DELIVERY,
        OrderStatus.DELIVERED
    )
    val currentStatus = OrderStatus.fromCode(order.orderStatus)
    val currentStepIndex = trackingSteps.indexOf(currentStatus)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Track ${order.id}") },
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(order.id, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OrderStatusChip(order.orderStatus, isBengali)
                    }
                    Text("Order Date: $dateStr", style = MaterialTheme.typography.bodySmall)
                    Text("Payment: ${order.paymentMethod} • Status: ${order.paymentStatus}", style = MaterialTheme.typography.bodySmall)
                    if (order.transactionId.isNotBlank()) {
                        Text("Transaction ID: ${order.transactionId}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Live Tracking Progress
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Live Delivery Tracking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    if (currentStepIndex >= 0) {
                        trackingSteps.forEachIndexed { idx, step ->
                            val reached = idx <= currentStepIndex
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (reached) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (reached) JisanEmeraldSuccess else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isBengali) step.labelBn else step.labelEn,
                                    fontWeight = if (idx == currentStepIndex) FontWeight.Bold else FontWeight.Normal,
                                    color = if (reached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Current Status: ${if (isBengali) currentStatus.labelBn else currentStatus.labelEn}",
                            fontWeight = FontWeight.Bold,
                            color = JisanDangerRed
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    if (order.trackingCarrier.isNotBlank() || order.trackingNumber.isNotBlank()) {
                        Text("Courier: ${order.trackingCarrier.ifBlank { "Assigned" }}", fontWeight = FontWeight.SemiBold)
                        if (order.trackingNumber.isNotBlank()) {
                            Text("Tracking Number: ${order.trackingNumber}")
                        }
                    }
                    if (order.estimatedDelivery.isNotBlank()) {
                        Text("Estimated Delivery: ${order.estimatedDelivery}", color = JisanEmeraldSuccess)
                    }
                    if (order.trackingNote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Latest Update: ${order.trackingNote}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Delivery Address & Items
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Shipping Address", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("${order.customerName} • ${order.customerPhone}")
                    Text("${order.shippingAddressLine}, ${order.shippingCity}, ${order.shippingState} - ${order.shippingPincode}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Text("Items", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    order.items.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.productName} × ${item.quantity}", modifier = Modifier.weight(1f))
                            Text(ImageUtils.formatInr(item.lineTotal), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal")
                        Text(ImageUtils.formatInr(order.subtotal))
                    }
                    if (order.discountAmount > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Discount (${order.couponCode})")
                            Text("- ${ImageUtils.formatInr(order.discountAmount)}", color = JisanEmeraldSuccess)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Delivery")
                        Text(if (order.deliveryCharge == 0.0) "FREE" else ImageUtils.formatInr(order.deliveryCharge))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Amount", fontWeight = FontWeight.Bold)
                        Text(ImageUtils.formatInr(order.totalAmount), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Invoice Share/Download Action
            OutlinedButton(
                onClick = {
                    val invoiceText = buildInvoiceText(order, dateStr)
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "JISAN MOBILE Invoice ${order.id}")
                        putExtra(Intent.EXTRA_TEXT, invoiceText)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Save or Share Invoice"))
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download / Share Invoice")
            }

            if (order.orderStatus == OrderStatus.PENDING.code || order.orderStatus == OrderStatus.CONFIRMED.code) {
                Button(
                    onClick = { reasonDialogMode = "CANCEL" },
                    colors = ButtonDefaults.buttonColors(containerColor = JisanDangerRed),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Cancel Order")
                }
            } else if (order.orderStatus == OrderStatus.DELIVERED.code) {
                OutlinedButton(
                    onClick = { reasonDialogMode = "RETURN" },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Request Return / Refund")
                }
            }
        }
    }

    if (reasonDialogMode != null) {
        var reason by remember { mutableStateOf("") }
        val isCancel = reasonDialogMode == "CANCEL"
        AlertDialog(
            onDismissRequest = { reasonDialogMode = null },
            title = { Text(if (isCancel) "Cancel Order" else "Request Return") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (isCancel) onCancelOrder(reason) else onRequestReturn(reason)
                    reasonDialogMode = null
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { reasonDialogMode = null }) { Text("Close") }
            }
        )
    }
}

fun buildInvoiceText(order: Order, dateStr: String): String {
    val sb = StringBuilder()
    sb.appendLine("====================================")
    sb.appendLine("          JISAN MOBILE")
    sb.appendLine("         TAX INVOICE / RECEIPT")
    sb.appendLine("====================================")
    sb.appendLine("Order ID: ${order.id}")
    sb.appendLine("Date: $dateStr")
    sb.appendLine("Customer: ${order.customerName}")
    sb.appendLine("Phone: ${order.customerPhone}")
    sb.appendLine("Ship To: ${order.shippingAddressLine}, ${order.shippingCity}, ${order.shippingState} - ${order.shippingPincode}")
    sb.appendLine("------------------------------------")
    order.items.forEach { item ->
        sb.appendLine("${item.productName} x${item.quantity} = ${ImageUtils.formatInr(item.lineTotal)}")
    }
    sb.appendLine("------------------------------------")
    sb.appendLine("Subtotal: ${ImageUtils.formatInr(order.subtotal)}")
    if (order.discountAmount > 0.0) {
        sb.appendLine("Discount (${order.couponCode}): -${ImageUtils.formatInr(order.discountAmount)}")
    }
    sb.appendLine("Delivery: ${if (order.deliveryCharge == 0.0) "FREE" else ImageUtils.formatInr(order.deliveryCharge)}")
    sb.appendLine("TOTAL PAYABLE: ${ImageUtils.formatInr(order.totalAmount)}")
    sb.appendLine("Payment: ${order.paymentMethod} (${order.paymentStatus})")
    if (order.transactionId.isNotBlank()) {
        sb.appendLine("Txn ID: ${order.transactionId}")
    }
    sb.appendLine("====================================")
    return sb.toString()
}
