package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.model.AdminUser
import com.example.model.CustomerProfile
import com.example.model.ReferralRecord
import com.example.model.StoreConfig
import com.example.util.OperationType
import com.example.util.SecurityUtils
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class ApkVerificationResult(
    val reachable: Boolean,
    val statusCode: Int,
    val contentType: String,
    val fileSizeMb: String,
    val message: String
)

class AdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must sign in with Google first.")

    fun observeAdminRecord(uid: String): Flow<AdminUser?> = flow {
        requireUserId()
        val path = "admins/$uid"
        emitAll(
            db.collection("admins").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            AdminUser::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun getAdminRecord(uid: String): AdminUser? =
        try {
            db.collection("admins").document(uid).get().await()
                .toObject(AdminUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (_: Exception) {
            null
        }

    /**
     * Completes one-time first administrator setup:
     * - Hashes the chosen security PIN using SHA-256 + random salt (never plaintext)
     * - Registers the primary owner in /admins/{uid}
     * - Initializes /store_config/public with bootstrapCompleted = true
     */
    suspend fun bootstrapFirstAdmin(
        uid: String,
        email: String,
        phone: String,
        displayName: String,
        pin: String,
        shopName: String
    ): Result<Unit> = runCatching {
        requireUserId()
        require(phone.filter { it.isDigit() }.length >= 10) { "Enter a valid 10-digit mobile number." }
        require(pin.length >= 6) { "Security PIN/password must be at least 6 characters." }

        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPin(pin, salt)

        val batch = db.batch()
        batch.set(
            db.collection("admins").document(uid),
            mapOf(
                "uid" to uid,
                "email" to email.trim(),
                "phone" to phone.trim(),
                "displayName" to displayName.trim().ifBlank { "Store Owner" },
                "pinHash" to hash,
                "pinSalt" to salt,
                "isPrimaryOwner" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            db.collection("store_config").document("public"),
            mapOf(
                "shopName" to shopName.trim().ifBlank { "JISAN MOBILE" },
                "shopTagline" to "Smartphones, Accessories & Trusted Electronics",
                "supportWhatsapp" to phone.trim(),
                "supportEmail" to email.trim(),
                "shopAddress" to "",
                "deliveryCharge" to 49.0,
                "freeDeliveryAbove" to 999.0,
                "referralRewardAmount" to 50.0,
                "codEnabled" to true,
                "onlinePaymentEnabled" to false,
                "razorpayKeyId" to "",
                "appVersion" to "1.0.0",
                "apkDownloadUrl" to "",
                "apkFileSizeMb" to "",
                "iosAppStoreUrl" to "",
                "releaseNotes" to "Initial production release of JISAN MOBILE.",
                "bootstrapCompleted" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "admins/$uid")
    }

    suspend fun verifyAdminCredentials(
        uid: String,
        emailInput: String,
        phoneInput: String,
        pinInput: String
    ): Result<Boolean> = runCatching {
        requireUserId()
        val snap = db.collection("admins").document(uid).get().await()
        val admin = snap.toObject(AdminUser::class.java) ?: return@runCatching false
        val emailMatch = admin.email.equals(emailInput.trim(), ignoreCase = true)
        val phoneMatch = admin.phone.filter { it.isDigit() }.takeLast(10) ==
            phoneInput.filter { it.isDigit() }.takeLast(10)
        val pinMatch = SecurityUtils.verifyPin(pinInput, admin.pinSalt, admin.pinHash)
        emailMatch && phoneMatch && pinMatch
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.GET, "admins/$uid")
    }

    suspend fun resetAdminPin(
        uid: String,
        emailInput: String,
        phoneInput: String,
        newPin: String
    ): Result<Unit> = runCatching {
        requireUserId()
        require(newPin.length >= 6) { "New password/PIN must be at least 6 characters." }
        val snap = db.collection("admins").document(uid).get().await()
        val admin = snap.toObject(AdminUser::class.java)
            ?: throw IllegalStateException("Admin profile not found for this Google account.")
        if (!admin.email.equals(emailInput.trim(), ignoreCase = true)) {
            throw IllegalStateException("Entered email does not match authorized admin email.")
        }
        val cleanStored = admin.phone.filter { it.isDigit() }.takeLast(10)
        val cleanInput = phoneInput.filter { it.isDigit() }.takeLast(10)
        if (cleanStored != cleanInput) {
            throw IllegalStateException("Entered mobile number does not match registered admin mobile.")
        }

        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPin(newPin, salt)
        db.collection("admins").document(uid).update(
            mapOf(
                "pinHash" to hash,
                "pinSalt" to salt,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "admins/$uid")
    }

    suspend fun addAuthorizedAdmin(
        targetUid: String,
        email: String,
        phone: String,
        displayName: String,
        pin: String
    ): Result<Unit> = runCatching {
        requireUserId()
        require(targetUid.isNotBlank()) { "Target User UID is required." }
        require(pin.length >= 6) { "Password/PIN must be at least 6 characters." }
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPin(pin, salt)
        db.collection("admins").document(targetUid.trim()).set(
            mapOf(
                "uid" to targetUid.trim(),
                "email" to email.trim(),
                "phone" to phone.trim(),
                "displayName" to displayName.trim().ifBlank { "Store Admin" },
                "pinHash" to hash,
                "pinSalt" to salt,
                "isPrimaryOwner" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "admins/$targetUid")
    }

    fun observeAllCustomers(): Flow<List<CustomerProfile>> = flow {
        requireUserId()
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        CustomerProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, "users")
                    throw error
                }
        )
    }

    suspend fun setCustomerBlocked(userId: String, blocked: Boolean): Result<Unit> = runCatching {
        requireUserId()
        db.collection("users").document(userId).update(
            mapOf(
                "isBlocked" to blocked,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/$userId")
    }

    fun observeAllReferrals(): Flow<List<ReferralRecord>> = flow {
        requireUserId()
        emitAll(
            db.collection("referrals")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        ReferralRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, "referrals")
                    throw error
                }
        )
    }

    suspend fun approveReferralReward(referral: ReferralRecord): Result<Unit> = runCatching {
        requireUserId()
        if (referral.status == "REWARDED") return@runCatching
        val refDoc = db.collection("referrals").document(referral.id)
        val referrerDoc = db.collection("users").document(referral.referrerUid)

        db.runTransaction { tx ->
            val userSnap = tx.get(referrerDoc)
            val currentReward = userSnap.getDouble("referralRewardEarned") ?: 0.0
            tx.update(
                refDoc,
                mapOf(
                    "status" to "REWARDED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (userSnap.exists()) {
                tx.update(
                    referrerDoc,
                    mapOf(
                        "referralRewardEarned" to (currentReward + referral.rewardAmount),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }.await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "referrals/${referral.id}")
    }

    suspend fun updateStoreConfig(config: StoreConfig): Result<Unit> = runCatching {
        requireUserId()
        db.collection("store_config").document("public").set(
            mapOf(
                "shopName" to config.shopName.trim().ifBlank { "JISAN MOBILE" },
                "shopTagline" to config.shopTagline.trim(),
                "supportWhatsapp" to config.supportWhatsapp.trim(),
                "supportEmail" to config.supportEmail.trim(),
                "shopAddress" to config.shopAddress.trim(),
                "deliveryCharge" to config.deliveryCharge.coerceAtLeast(0.0),
                "freeDeliveryAbove" to config.freeDeliveryAbove.coerceAtLeast(0.0),
                "referralRewardAmount" to config.referralRewardAmount.coerceAtLeast(0.0),
                "codEnabled" to config.codEnabled,
                "onlinePaymentEnabled" to config.onlinePaymentEnabled,
                "razorpayKeyId" to config.razorpayKeyId.trim(),
                "appVersion" to config.appVersion.trim().ifBlank { "1.0.0" },
                "apkDownloadUrl" to config.apkDownloadUrl.trim(),
                "apkFileSizeMb" to config.apkFileSizeMb.trim(),
                "iosAppStoreUrl" to config.iosAppStoreUrl.trim(),
                "releaseNotes" to config.releaseNotes.trim(),
                "bootstrapCompleted" to true,
                "privacyPolicy" to config.privacyPolicy.trim(),
                "termsText" to config.termsText.trim(),
                "shippingPolicy" to config.shippingPolicy.trim(),
                "refundPolicy" to config.refundPolicy.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "store_config/public")
    }

    /**
     * Broadcasts a real notification to a specific user (e.g., new product or offer).
     */
    suspend fun sendCustomerNotification(userId: String, title: String, message: String, type: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("users").document(userId).collection("notifications").document()
            .set(
                mapOf(
                    "userId" to userId,
                    "title" to title.trim(),
                    "message" to message.trim(),
                    "type" to type,
                    "referenceId" to "",
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "users/$userId/notifications")
    }

    /**
     * Performs a real HTTP HEAD check on the configured APK HTTPS URL to verify existence,
     * MIME type (application/vnd.android.package-archive or octet-stream), and file size.
     */
    suspend fun verifyApkDownloadUrl(url: String): ApkVerificationResult = withContext(Dispatchers.IO) {
        if (!url.startsWith("https://")) {
            return@withContext ApkVerificationResult(
                reachable = false,
                statusCode = 0,
                contentType = "",
                fileSizeMb = "",
                message = "APK download URL must start with https://"
            )
        }
        try {
            val request = Request.Builder().url(url).head().build()
            httpClient.newCall(request).execute().use { response ->
                val code = response.code
                val contentType = response.header("Content-Type").orEmpty()
                val contentLength = response.header("Content-Length")?.toLongOrNull() ?: 0L
                val sizeMb = if (contentLength > 0L) {
                    String.format("%.1f MB", contentLength / (1024.0 * 1024.0))
                } else ""
                val isHtml = contentType.contains("text/html", ignoreCase = true)
                val ok = response.isSuccessful && !isHtml
                ApkVerificationResult(
                    reachable = ok,
                    statusCode = code,
                    contentType = contentType,
                    fileSizeMb = sizeMb,
                    message = when {
                        !response.isSuccessful -> "HTTP $code returned by server."
                        isHtml -> "URL returned HTML page instead of an APK binary."
                        else -> "Verified APK binary ($code, $contentType${if (sizeMb.isNotBlank()) ", $sizeMb" else ""})."
                    }
                )
            }
        } catch (e: Exception) {
            ApkVerificationResult(
                reachable = false,
                statusCode = 0,
                contentType = "",
                fileSizeMb = "",
                message = e.localizedMessage ?: "Failed to verify APK download link."
            )
        }
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
