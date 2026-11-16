package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.CartItem
import com.example.model.DeliveryAddress
import com.example.model.Product
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class JisanMobileRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun observePublishedProducts_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val storeRepo = StoreRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                storeRepo.observePublishedProducts().first()
            }
            fail("Expected IllegalStateException or PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertTrue(
                e is IllegalStateException ||
                    firestoreEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            )
        }
    }

    @Test
    fun customerProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_${UUID.randomUUID()}@jisan.test")
        val customerRepo = CustomerRepository(firestore, auth)
        val ensureResult = withTimeout(OP_TIMEOUT_MS) {
            customerRepo.ensureProfileExists(aliceUid, "Alice", "alice@jisan.test", "")
        }
        assertTrue(ensureResult.isSuccess)

        // Sign in as Bob and attempt to observe Alice's private profile
        signInTestUser("bob_${UUID.randomUUID()}@jisan.test")
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                customerRepo.observeProfile(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when Bob reads Alice's profile")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun customerCannotWriteProductDirectly_rejectedBySecurityRules() = runBlocking {
        signInTestUser("shopper_${UUID.randomUUID()}@jisan.test")
        val storeRepo = StoreRepository(firestore, auth)

        val result = withTimeout(OP_TIMEOUT_MS) {
            storeRepo.saveProduct(
                Product(
                    name = "Unauthorized Phone",
                    description = "Should fail",
                    categoryId = "cat_1",
                    categoryName = "Smartphones",
                    brand = "Test",
                    sku = "SKU-${UUID.randomUUID()}",
                    mrp = 15000.0,
                    sellingPrice = 12999.0,
                    stockQuantity = 10L,
                    isPublished = true
                )
            )
        }
        assertTrue(result.isFailure)
        val firestoreEx = result.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun referral_selfReferral_isRejected() = runBlocking {
        val uid = signInTestUser("refuser_${UUID.randomUUID()}@jisan.test")
        val customerRepo = CustomerRepository(firestore, auth)
        val profile = withTimeout(OP_TIMEOUT_MS) {
            customerRepo.ensureProfileExists(uid, "Ref User", "ref@jisan.test", "").getOrThrow()
        }

        val selfApply = withTimeout(OP_TIMEOUT_MS) {
            customerRepo.applyReferralCode(uid, "ref@jisan.test", profile.referralCode)
        }
        assertTrue(selfApply.isFailure)
    }

    @Test
    fun referral_validCrossUserReferral_succeedsAndPreventsDuplicate() = runBlocking {
        val referrerUid = signInTestUser("referrer_${UUID.randomUUID()}@jisan.test")
        val customerRepo = CustomerRepository(firestore, auth)
        val referrerProfile = withTimeout(OP_TIMEOUT_MS) {
            customerRepo.ensureProfileExists(referrerUid, "Referrer", "referrer@jisan.test", "").getOrThrow()
        }

        val referredUid = signInTestUser("referred_${UUID.randomUUID()}@jisan.test")
        withTimeout(OP_TIMEOUT_MS) {
            customerRepo.ensureProfileExists(referredUid, "Referred", "referred@jisan.test", "").getOrThrow()
        }

        val firstApply = withTimeout(OP_TIMEOUT_MS) {
            customerRepo.applyReferralCode(referredUid, "referred@jisan.test", referrerProfile.referralCode)
        }
        assertTrue(
            "firstApply failed: ${firstApply.exceptionOrNull()?.message} / ${firstApply.exceptionOrNull()?.unwrapFirestoreException()}",
            firstApply.isSuccess
        )

        val secondApply = withTimeout(OP_TIMEOUT_MS) {
            customerRepo.applyReferralCode(referredUid, "referred@jisan.test", referrerProfile.referralCode)
        }
        assertTrue("secondApply unexpectedly succeeded", secondApply.isFailure)
    }

    @Test
    fun orderQuery_customerSeesOnlyOwnOrders() = runBlocking {
        val aliceUid = signInTestUser("order_alice_${UUID.randomUUID()}@jisan.test")
        val orderRepo = OrderRepository(firestore, auth)

        val myOrders = withTimeout(FLOW_TIMEOUT_MS) {
            orderRepo.observeMyOrders(aliceUid).first()
        }
        assertTrue(myOrders.isEmpty())

        // Cross-user order query must fail with PERMISSION_DENIED
        val bobUid = signInTestUser("order_bob_${UUID.randomUUID()}@jisan.test")
        assertTrue(aliceUid != bobUid)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                orderRepo.observeMyOrders(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when Bob queries Alice's orders")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    private companion object {
        const val OP_TIMEOUT_MS = 10000L
        const val FLOW_TIMEOUT_MS = 10000L
    }
}
