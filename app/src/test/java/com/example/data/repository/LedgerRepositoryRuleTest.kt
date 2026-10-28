package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.EntryType
import com.example.data.model.PaymentMode
import com.example.data.seed.KaliPujoSeedData
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LedgerRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureSeededWorkspace_creates2026ExpensesUpdatedCategoriesAnd2025Archive() = runBlocking {
        val uid = signInTestUser("seed_${UUID.randomUUID()}@test.com")
        val repository = LedgerRepository(firestore, auth)

        val seedResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.ensureSeededWorkspace() }
        assertTrue("Expected seeding to succeed: ${seedResult.exceptionOrNull()}", seedResult.isSuccess)
        assertTrue(seedResult.getOrThrow())

        val categories = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCategories(uid).first { it.size >= 11 }
        }
        val categoryNames = categories.map { it.name }
        assertTrue(categoryNames.contains("Dhak"))
        assertTrue(categoryNames.contains("Thakur"))
        assertTrue(categoryNames.contains("Thakurmasai"))

        val entries = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeEntries(uid).first { it.size >= 43 }
        }
        val entries2026 = entries.filter { it.accountId == KaliPujoSeedData.ACCOUNT_2026_ID }
        assertEquals(3, entries2026.size)
        assertEquals(9000L, entries2026.sumOf { it.amount })

        val entries2025 = entries.filter { it.accountId == KaliPujoSeedData.ACCOUNT_2025_ID }
        assertEquals(40, entries2025.size)
        val income2025 = entries2025.filter { it.type == EntryType.INCOME.raw }.sumOf { it.amount }
        val expense2025 = entries2025.filter { it.type == EntryType.EXPENSE.raw }.sumOf { it.amount }
        assertEquals(30200L, income2025)
        assertEquals(30419L, expense2025)
        assertEquals(-219L, income2025 - expense2025)
    }

    @Test
    fun createUpdateAndReimburseEntry_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser("owner_${UUID.randomUUID()}@test.com")
        val repository = LedgerRepository(firestore, auth)

        val accountId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createAccount(
                name = "Kali Pujo Test",
                year = 2026L,
                defaultContribution = 2000L,
                targetBudget = 40000L
            ).getOrThrow()
        }

        val entryId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createEntry(
                accountId = accountId,
                type = EntryType.EXPENSE.raw,
                amount = 1250L,
                categoryId = KaliPujoSeedData.CAT_SAMAGRI,
                categoryName = "Puja Samagri",
                notes = "Dashakarma advance",
                partyName = "Rahul",
                paymentMode = PaymentMode.MEMBER_ADVANCE.raw,
                dateStr = "27/10/2026",
                dateEpochMillis = 1793059200000L
            ).getOrThrow()
        }

        val reimburseResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.markEntryReimbursed(entryId)
        }
        assertTrue(reimburseResult.isSuccess)

        val updatedEntry = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeEntries(uid).first { list ->
                list.any { it.id == entryId && it.paymentMode == PaymentMode.REIMBURSED.raw }
            }.first { it.id == entryId }
        }
        assertEquals(PaymentMode.REIMBURSED.raw, updatedEntry.paymentMode)
    }

    @Test
    fun crossUserRead_bobCannotReadAliceAccount_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_${UUID.randomUUID()}@test.com")
        val aliceRepo = LedgerRepository(firestore, auth)
        val accountId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createAccount("Alice Function", 2026L, 1800L, 35000L).getOrThrow()
        }

        signInTestUser("bob_${UUID.randomUUID()}@test.com")
        val bobRepo = LedgerRepository(firestore, auth)
        val readResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getAccountById(aliceUid, accountId)
        }
        assertTrue(readResult.isFailure)
        val fsEx = readResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observeEntries_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = LedgerRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeEntries("unauth_target_uid").first()
            }
            fail("Expected unauthenticated Flow collection to fail with PERMISSION_DENIED")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
