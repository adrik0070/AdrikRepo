package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.LedgerAccount
import com.example.data.model.LedgerCategory
import com.example.data.model.LedgerEntry
import com.example.data.model.PaymentMode
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.data.remote.unwrapFirestoreException
import com.example.data.seed.KaliPujoSeedData
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LedgerRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before writing to Firestore.")
    }

    private fun sanitizeId(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(100)
        return cleaned.ifBlank { UUID.randomUUID().toString() }
    }

    // --- Real-time Observation Streams (delegating read authorization to Firestore rules) ---

    fun observeAccounts(userId: String): Flow<List<LedgerAccount>> {
        val path = "users/$userId/accounts"
        return db.collection("users").document(userId).collection("accounts")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    LedgerAccount::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(compareBy<LedgerAccount> { it.archived }.thenByDescending { it.year })
            }
            .catch { error ->
                val fsEx = error.unwrapFirestoreException() ?: (error as? Exception)
                if (fsEx != null) {
                    handleFirestoreError(fsEx, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeCategories(userId: String): Flow<List<LedgerCategory>> {
        val path = "users/$userId/categories"
        return db.collection("users").document(userId).collection("categories")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    LedgerCategory::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.sortOrder }
            }
            .catch { error ->
                val fsEx = error.unwrapFirestoreException() ?: (error as? Exception)
                if (fsEx != null) {
                    handleFirestoreError(fsEx, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeEntries(userId: String): Flow<List<LedgerEntry>> {
        val path = "users/$userId/entries"
        return db.collection("users").document(userId).collection("entries")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    LedgerEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.dateEpochMillis }
            }
            .catch { error ->
                val fsEx = error.unwrapFirestoreException() ?: (error as? Exception)
                if (fsEx != null) {
                    handleFirestoreError(fsEx, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun getAccountById(ownerUid: String, accountId: String): Result<LedgerAccount?> {
        val docRef = db.collection("users").document(ownerUid)
            .collection("accounts").document(accountId)
        return try {
            val snap = docRef.get().await()
            Result.success(
                snap.toObject(
                    LedgerAccount::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    // --- Idempotent First-Launch Seeding ---

    suspend fun ensureSeededWorkspace(): Result<Boolean> {
        val uid = requireUserId()
        val accountsPath = "users/$uid/accounts"
        return try {
            val existing = db.collection("users").document(uid)
                .collection("accounts")
                .whereEqualTo("userId", uid)
                .limit(1)
                .get()
                .await()

            if (!existing.isEmpty) {
                return Result.success(false)
            }

            val batch = db.batch()
            val userRef = db.collection("users").document(uid)

            KaliPujoSeedData.buildDefaultAccounts(uid).forEach { account ->
                val ref = userRef.collection("accounts").document(account.id)
                batch.set(ref, account.copy(createdAt = null, updatedAt = null))
            }

            KaliPujoSeedData.buildDefaultCategories(uid).forEach { category ->
                val ref = userRef.collection("categories").document(category.id)
                batch.set(ref, category.copy(createdAt = null, updatedAt = null))
            }

            (KaliPujoSeedData.buildInitial2026Entries(uid) +
                KaliPujoSeedData.buildHistorical2025Entries(uid)).forEach { entry ->
                val ref = userRef.collection("entries").document(entry.id)
                batch.set(ref, entry.copy(createdAt = null, updatedAt = null))
            }

            batch.commit().await()
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, accountsPath)
            Result.failure(e)
        }
    }

    // --- Account Mutations ---

    suspend fun createAccount(
        name: String,
        year: Long,
        defaultContribution: Long,
        targetBudget: Long,
        archived: Boolean = false,
        customId: String? = null
    ): Result<String> {
        val uid = requireUserId()
        val docId = customId?.let(::sanitizeId) ?: sanitizeId("acc_${UUID.randomUUID()}")
        val docRef = db.collection("users").document(uid).collection("accounts").document(docId)
        val account = LedgerAccount(
            id = docId,
            userId = uid,
            name = name.trim().take(100),
            year = year.coerceIn(2000L, 2100L),
            defaultContribution = defaultContribution.coerceIn(0L, 10000000L),
            targetBudget = targetBudget.coerceIn(0L, 100000000L),
            archived = archived,
            createdAt = null,
            updatedAt = null
        )
        return try {
            docRef.set(account).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateAccount(account: LedgerAccount): Result<Unit> {
        val uid = requireUserId()
        require(account.id.isNotBlank()) { "Account ID must not be blank for update" }
        requireNotNull(account.createdAt) { "Account createdAt must not be null for update" }
        val docRef = db.collection("users").document(uid).collection("accounts").document(account.id)
        return try {
            docRef.set(account.copy(userId = uid, updatedAt = null)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Category Mutations ---

    suspend fun createCategory(
        name: String,
        type: String,
        budgetLimit: Long,
        colorHex: String,
        iconKey: String,
        sortOrder: Long,
        customId: String? = null
    ): Result<String> {
        val uid = requireUserId()
        val docId = customId?.let(::sanitizeId) ?: sanitizeId("cat_${UUID.randomUUID()}")
        val docRef = db.collection("users").document(uid).collection("categories").document(docId)
        val category = LedgerCategory(
            id = docId,
            userId = uid,
            name = name.trim().take(60),
            type = type,
            budgetLimit = budgetLimit.coerceIn(0L, 100000000L),
            colorHex = colorHex,
            iconKey = iconKey.take(40),
            sortOrder = sortOrder.coerceIn(0L, 10000L),
            createdAt = null,
            updatedAt = null
        )
        return try {
            docRef.set(category).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCategory(category: LedgerCategory): Result<Unit> {
        val uid = requireUserId()
        require(category.id.isNotBlank()) { "Category ID must not be blank for update" }
        requireNotNull(category.createdAt) { "Category createdAt must not be null for update" }
        val docRef = db.collection("users").document(uid).collection("categories").document(category.id)
        return try {
            docRef.set(category.copy(userId = uid, updatedAt = null)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("categories").document(categoryId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Ledger Entry Mutations ---

    suspend fun createEntry(
        accountId: String,
        type: String,
        amount: Long,
        categoryId: String,
        categoryName: String,
        notes: String,
        partyName: String,
        paymentMode: String,
        dateStr: String,
        dateEpochMillis: Long
    ): Result<String> {
        val uid = requireUserId()
        val docId = sanitizeId("entry_${UUID.randomUUID()}")
        val docRef = db.collection("users").document(uid).collection("entries").document(docId)
        val entry = LedgerEntry(
            id = docId,
            userId = uid,
            accountId = accountId,
            type = type,
            amount = amount.coerceIn(1L, 100000000L),
            categoryId = categoryId,
            categoryName = categoryName.trim().take(60),
            notes = notes.trim().take(300),
            partyName = partyName.trim().take(100),
            paymentMode = paymentMode,
            dateStr = dateStr.trim().take(20),
            dateEpochMillis = dateEpochMillis,
            createdAt = null,
            updatedAt = null
        )
        return try {
            docRef.set(entry).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateEntry(entry: LedgerEntry): Result<Unit> {
        val uid = requireUserId()
        require(entry.id.isNotBlank()) { "Entry ID must not be blank for update" }
        requireNotNull(entry.createdAt) { "Entry createdAt must not be null for update" }
        val docRef = db.collection("users").document(uid).collection("entries").document(entry.id)
        return try {
            docRef.set(entry.copy(userId = uid, updatedAt = null)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markEntryReimbursed(entryId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("entries").document(entryId)
        return try {
            docRef.update(
                mapOf(
                    "paymentMode" to PaymentMode.REIMBURSED.raw,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteEntry(entryId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("entries").document(entryId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
