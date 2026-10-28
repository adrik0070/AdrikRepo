package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class EntryType(val raw: String, val displayLabel: String) {
    EXPENSE("EXPENSE", "Expense"),
    INCOME("INCOME", "Income");

    companion object {
        fun fromRaw(value: String): EntryType =
            entries.firstOrNull { it.raw.equals(value, ignoreCase = true) } ?: EXPENSE
    }
}

enum class PaymentMode(val raw: String, val displayLabel: String, val shortBadge: String) {
    FUND("FUND", "Paid from Fund / Cash", "Fund Pay"),
    MEMBER_ADVANCE("MEMBER_ADVANCE", "Paid by Member (Needs Reimbursement)", "Due to Member"),
    REIMBURSED("REIMBURSED", "Reimbursed to Member", "Reimbursed"),
    RESIDENT_PAID("RESIDENT_PAID", "Resident Contribution", "Contribution");

    companion object {
        fun fromRaw(value: String): PaymentMode =
            entries.firstOrNull { it.raw.equals(value, ignoreCase = true) } ?: FUND
    }
}

@IgnoreExtraProperties
data class LedgerAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val year: Long = 2026L,
    val defaultContribution: Long = 1800L,
    val targetBudget: Long = 40000L,
    val archived: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LedgerCategory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val type: String = EntryType.EXPENSE.raw,
    val budgetLimit: Long = 0L,
    val colorHex: String = "#B71C1C",
    val iconKey: String = "category",
    val sortOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LedgerEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val accountId: String = "",
    val type: String = EntryType.EXPENSE.raw,
    val amount: Long = 0L,
    val categoryId: String = "",
    val categoryName: String = "",
    val notes: String = "",
    val partyName: String = "",
    val paymentMode: String = PaymentMode.FUND.raw,
    val dateStr: String = "",
    val dateEpochMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class EntryWithBalance(
    val entry: LedgerEntry,
    val runningBalance: Long
)

data class AccountSummaryMetrics(
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val netBalance: Long = 0L,
    val pendingReimbursements: Long = 0L,
    val unreimbursedCount: Int = 0,
    val availableCashInHand: Long = 0L,
    val entryCount: Int = 0
)

data class ResidentContributionRow(
    val residentName: String,
    val lastYearContribution: Long,
    val currentYearContribution: Long,
    val pendingMemberAdvance: Long,
    val settledMemberAdvance: Long
)

data class CategoryBreakdownItem(
    val category: LedgerCategory,
    val currentAmount: Long,
    val previousYearAmount: Long,
    val transactionCount: Int,
    val shareFraction: Float
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
