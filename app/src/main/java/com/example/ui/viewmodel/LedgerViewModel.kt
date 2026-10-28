package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AccountSummaryMetrics
import com.example.data.model.CategoryBreakdownItem
import com.example.data.model.EntryType
import com.example.data.model.EntryWithBalance
import com.example.data.model.LedgerAccount
import com.example.data.model.LedgerCategory
import com.example.data.model.LedgerEntry
import com.example.data.model.PaymentMode
import com.example.data.model.ResidentContributionRow
import com.example.data.model.UiState
import com.example.data.repository.LedgerRepository
import com.example.data.seed.KaliPujoSeedData
import com.example.util.CurrencyFormatter
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class EntryFilterTab(val label: String) {
    ALL("All"),
    EXPENSES("Expenses"),
    INCOME("Income"),
    DUE_REIMBURSEMENT("Due to Member")
}

data class LedgerFilterState(
    val selectedAccountId: String = KaliPujoSeedData.ACCOUNT_2026_ID,
    val searchQuery: String = "",
    val filterTab: EntryFilterTab = EntryFilterTab.ALL,
    val selectedCategoryId: String? = null,
    val newestFirst: Boolean = true
)

data class LedgerDashboardData(
    val accounts: List<LedgerAccount>,
    val activeAccount: LedgerAccount?,
    val categories: List<LedgerCategory>,
    val chronologicalRows: List<EntryWithBalance>,
    val filteredRows: List<EntryWithBalance>,
    val summary: AccountSummaryMetrics,
    val previousYearSummary: AccountSummaryMetrics?,
    val categoryBreakdown: List<CategoryBreakdownItem>,
    val residentRows: List<ResidentContributionRow>,
    val filterState: LedgerFilterState
)

class LedgerViewModel(
    private val repository: LedgerRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _filterState = MutableStateFlow(LedgerFilterState())
    val filterState: StateFlow<LedgerFilterState> = _filterState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    init {
        initializeWorkspaceIfEmpty()
    }

    private fun initializeWorkspaceIfEmpty() {
        viewModelScope.launch {
            _isBusy.value = true
            val seedRes = repository.ensureSeededWorkspace()
            _isBusy.value = false
            seedRes.onFailure { err ->
                Log.w(TAG, "Seed workspace check failed", err)
                _statusMessage.value = err.localizedMessage ?: "Failed to initialize workspace"
            }
            if (seedRes.getOrNull() == true) {
                _statusMessage.value = "Loaded Kali Pujo 2026 (Dhak, Thakur, Thakurmasai) & 2025 report!"
            }
        }
    }

    private val accountsStream = repository.observeAccounts(currentUserId)
        .map<List<LedgerAccount>, UiState<List<LedgerAccount>>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing accounts", err)
            emit(UiState.Error(err.message ?: "Failed to load accounts"))
        }

    private val categoriesStream = repository.observeCategories(currentUserId)
        .map<List<LedgerCategory>, UiState<List<LedgerCategory>>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing categories", err)
            emit(UiState.Error(err.message ?: "Failed to load categories"))
        }

    private val entriesStream = repository.observeEntries(currentUserId)
        .map<List<LedgerEntry>, UiState<List<LedgerEntry>>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing entries", err)
            emit(UiState.Error(err.message ?: "Failed to load ledger entries"))
        }

    val dashboardState: StateFlow<UiState<LedgerDashboardData>> = combine(
        accountsStream,
        categoriesStream,
        entriesStream,
        _filterState
    ) { accState, catState, entState, filter ->
        when {
            accState is UiState.Error -> UiState.Error(accState.message)
            catState is UiState.Error -> UiState.Error(catState.message)
            entState is UiState.Error -> UiState.Error(entState.message)
            accState is UiState.Success && catState is UiState.Success && entState is UiState.Success -> {
                val accounts = accState.data
                val categories = catState.data
                val allEntries = entState.data

                val activeAccount = accounts.firstOrNull { it.id == filter.selectedAccountId }
                    ?: accounts.firstOrNull { !it.archived }
                    ?: accounts.firstOrNull()

                val activeAccountId = activeAccount?.id ?: filter.selectedAccountId
                val activeEntriesSorted = allEntries
                    .filter { it.accountId == activeAccountId }
                    .sortedWith(
                        compareBy<LedgerEntry> { it.dateEpochMillis }
                            .thenBy { (it.createdAt ?: Timestamp(0, 0)).seconds }
                            .thenBy { it.id }
                    )

                // Compute running balance chronologically
                var runningBal = 0L
                val chronologicalRows = activeEntriesSorted.map { entry ->
                    runningBal += if (entry.type == EntryType.INCOME.raw) entry.amount else -entry.amount
                    EntryWithBalance(entry = entry, runningBalance = runningBal)
                }

                val summary = computeMetrics(activeEntriesSorted)

                // Previous year comparison (e.g. Kali pujo 2025 when viewing 2026)
                val previousAccount = accounts
                    .filter { it.id != activeAccountId }
                    .maxByOrNull { it.year }
                val previousEntries = if (previousAccount != null) {
                    allEntries.filter { it.accountId == previousAccount.id }
                } else {
                    emptyList()
                }
                val previousSummary = if (previousAccount != null) computeMetrics(previousEntries) else null

                // Filtered rows for display
                val queryLower = filter.searchQuery.trim().lowercase()
                val filteredChrono = chronologicalRows.filter { row ->
                    val e = row.entry
                    val matchesTab = when (filter.filterTab) {
                        EntryFilterTab.ALL -> true
                        EntryFilterTab.EXPENSES -> e.type == EntryType.EXPENSE.raw
                        EntryFilterTab.INCOME -> e.type == EntryType.INCOME.raw
                        EntryFilterTab.DUE_REIMBURSEMENT -> e.paymentMode == PaymentMode.MEMBER_ADVANCE.raw
                    }
                    val matchesCategory = filter.selectedCategoryId == null || e.categoryId == filter.selectedCategoryId
                    val matchesSearch = queryLower.isEmpty() ||
                        e.notes.lowercase().contains(queryLower) ||
                        e.partyName.lowercase().contains(queryLower) ||
                        e.categoryName.lowercase().contains(queryLower) ||
                        e.dateStr.contains(queryLower)
                    matchesTab && matchesCategory && matchesSearch
                }

                val displayRows = if (filter.newestFirst) filteredChrono.reversed() else filteredChrono

                // Category breakdown for expenses
                val expenseCategories = categories.filter { it.type == EntryType.EXPENSE.raw }
                val totalExpenseForShare = summary.totalExpense.coerceAtLeast(1L)
                val categoryBreakdown = expenseCategories.map { cat ->
                    val currentEntriesForCat = activeEntriesSorted.filter {
                        it.type == EntryType.EXPENSE.raw && it.categoryId == cat.id
                    }
                    val currentSum = currentEntriesForCat.sumOf { it.amount }
                    val prevSum = previousEntries
                        .filter { it.type == EntryType.EXPENSE.raw && it.categoryId == cat.id }
                        .sumOf { it.amount }
                    CategoryBreakdownItem(
                        category = cat,
                        currentAmount = currentSum,
                        previousYearAmount = prevSum,
                        transactionCount = currentEntriesForCat.size,
                        shareFraction = if (summary.totalExpense > 0L) {
                            currentSum.toFloat() / totalExpenseForShare.toFloat()
                        } else {
                            0f
                        }
                    )
                }.sortedWith(
                    compareByDescending<CategoryBreakdownItem> { it.currentAmount }
                        .thenBy { it.category.sortOrder }
                )

                // Resident checklist combining 2025 contributors + any new contributors/payees in active account
                val residentNames = linkedMapOf<String, String>() // lowercase key -> display name
                KaliPujoSeedData.apartmentResidents2025.forEach { res ->
                    residentNames[res.name.lowercase()] = res.name
                }
                activeEntriesSorted.forEach { entry ->
                    if (entry.partyName.isNotBlank() &&
                        (entry.type == EntryType.INCOME.raw ||
                            entry.paymentMode == PaymentMode.MEMBER_ADVANCE.raw ||
                            entry.paymentMode == PaymentMode.REIMBURSED.raw)
                    ) {
                        val key = entry.partyName.trim().lowercase()
                        if (!residentNames.containsKey(key)) {
                            residentNames[key] = entry.partyName.trim()
                        }
                    }
                }

                val residentRows = residentNames.map { (key, displayName) ->
                    val histAmount = KaliPujoSeedData.apartmentResidents2025
                        .firstOrNull { it.name.lowercase() == key }?.contribution2025 ?: 0L
                    val currentPaid = activeEntriesSorted
                        .filter {
                            it.type == EntryType.INCOME.raw &&
                                (it.partyName.trim().lowercase() == key || it.notes.trim().lowercase() == key)
                        }
                        .sumOf { it.amount }
                    val pendingAdvance = activeEntriesSorted
                        .filter {
                            it.type == EntryType.EXPENSE.raw &&
                                it.paymentMode == PaymentMode.MEMBER_ADVANCE.raw &&
                                it.partyName.trim().lowercase() == key
                        }
                        .sumOf { it.amount }
                    val settledAdvance = activeEntriesSorted
                        .filter {
                            it.type == EntryType.EXPENSE.raw &&
                                it.paymentMode == PaymentMode.REIMBURSED.raw &&
                                it.partyName.trim().lowercase() == key
                        }
                        .sumOf { it.amount }

                    ResidentContributionRow(
                        residentName = displayName,
                        lastYearContribution = histAmount,
                        currentYearContribution = currentPaid,
                        pendingMemberAdvance = pendingAdvance,
                        settledMemberAdvance = settledAdvance
                    )
                }

                UiState.Success(
                    LedgerDashboardData(
                        accounts = accounts,
                        activeAccount = activeAccount,
                        categories = categories,
                        chronologicalRows = chronologicalRows,
                        filteredRows = displayRows,
                        summary = summary,
                        previousYearSummary = previousSummary,
                        categoryBreakdown = categoryBreakdown,
                        residentRows = residentRows,
                        filterState = filter.copy(selectedAccountId = activeAccountId)
                    )
                )
            }
            else -> UiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    private fun computeMetrics(entries: List<LedgerEntry>): AccountSummaryMetrics {
        val totalIncome = entries
            .filter { it.type == EntryType.INCOME.raw }
            .sumOf { it.amount }
        val totalExpense = entries
            .filter { it.type == EntryType.EXPENSE.raw }
            .sumOf { it.amount }
        val pendingReimbursements = entries
            .filter { it.type == EntryType.EXPENSE.raw && it.paymentMode == PaymentMode.MEMBER_ADVANCE.raw }
            .sumOf { it.amount }
        val unreimbursedCount = entries.count {
            it.type == EntryType.EXPENSE.raw && it.paymentMode == PaymentMode.MEMBER_ADVANCE.raw
        }
        val netBalance = totalIncome - totalExpense
        // Cash physically remaining in fund = Total Income - (Fund Paid + Reimbursed Expenses)
        val availableCash = netBalance + pendingReimbursements
        return AccountSummaryMetrics(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = netBalance,
            pendingReimbursements = pendingReimbursements,
            unreimbursedCount = unreimbursedCount,
            availableCashInHand = availableCash,
            entryCount = entries.size
        )
    }

    fun selectAccount(accountId: String) {
        _filterState.value = _filterState.value.copy(
            selectedAccountId = accountId,
            selectedCategoryId = null,
            searchQuery = ""
        )
    }

    fun updateSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun updateFilterTab(tab: EntryFilterTab) {
        _filterState.value = _filterState.value.copy(filterTab = tab)
    }

    fun updateCategoryFilter(categoryId: String?) {
        val next = if (_filterState.value.selectedCategoryId == categoryId) null else categoryId
        _filterState.value = _filterState.value.copy(selectedCategoryId = next)
    }

    fun toggleSortOrder() {
        _filterState.value = _filterState.value.copy(newestFirst = !_filterState.value.newestFirst)
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun postStatusMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun saveEntry(
        existingEntry: LedgerEntry?,
        accountId: String,
        type: EntryType,
        amount: Long,
        category: LedgerCategory,
        notes: String,
        partyName: String,
        paymentMode: PaymentMode,
        dateStr: String
    ) {
        viewModelScope.launch {
            val epoch = KaliPujoSeedData.parseDateMillis(
                dateStr,
                sequenceIndex = ((System.currentTimeMillis() / 1000L) % 86000L).toInt()
            )
            val result = if (existingEntry != null && existingEntry.id.isNotBlank()) {
                repository.updateEntry(
                    existingEntry.copy(
                        type = type.raw,
                        amount = amount,
                        categoryId = category.id,
                        categoryName = category.name,
                        notes = notes.ifBlank { category.name },
                        partyName = partyName.trim(),
                        paymentMode = paymentMode.raw,
                        dateStr = dateStr,
                        dateEpochMillis = epoch
                    )
                )
            } else {
                repository.createEntry(
                    accountId = accountId,
                    type = type.raw,
                    amount = amount,
                    categoryId = category.id,
                    categoryName = category.name,
                    notes = notes.ifBlank { category.name },
                    partyName = partyName.trim(),
                    paymentMode = paymentMode.raw,
                    dateStr = dateStr,
                    dateEpochMillis = epoch
                ).map { }
            }
            result.onSuccess {
                _statusMessage.value = if (existingEntry != null) "Entry updated" else "Entry added to ledger"
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to save entry"
            }
        }
    }

    fun collectResidentContribution(
        accountId: String,
        residentName: String,
        amount: Long,
        incomeCategory: LedgerCategory?
    ) {
        viewModelScope.launch {
            val catId = incomeCategory?.id ?: KaliPujoSeedData.CAT_CONTRIBUTION
            val catName = incomeCategory?.name ?: "Resident Contribution"
            val dateStr = CurrencyFormatter.todayDateStr()
            val epoch = KaliPujoSeedData.parseDateMillis(
                dateStr,
                sequenceIndex = ((System.currentTimeMillis() / 1000L) % 86000L).toInt()
            )
            val result = repository.createEntry(
                accountId = accountId,
                type = EntryType.INCOME.raw,
                amount = amount,
                categoryId = catId,
                categoryName = catName,
                notes = residentName.trim(),
                partyName = residentName.trim(),
                paymentMode = PaymentMode.RESIDENT_PAID.raw,
                dateStr = dateStr,
                dateEpochMillis = epoch
            )
            result.onSuccess {
                _statusMessage.value = "Recorded ${CurrencyFormatter.formatInr(amount)} from $residentName"
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to record contribution"
            }
        }
    }

    fun markReimbursed(entryId: String) {
        viewModelScope.launch {
            repository.markEntryReimbursed(entryId)
                .onSuccess { _statusMessage.value = "Marked member expense as reimbursed" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to update status" }
        }
    }

    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            repository.deleteEntry(entryId)
                .onSuccess { _statusMessage.value = "Entry removed" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to delete entry" }
        }
    }

    fun saveCategory(
        existingCategory: LedgerCategory?,
        name: String,
        type: EntryType,
        budgetLimit: Long,
        colorHex: String,
        iconKey: String,
        nextSortOrder: Long
    ) {
        viewModelScope.launch {
            val result = if (existingCategory != null && existingCategory.id.isNotBlank()) {
                repository.updateCategory(
                    existingCategory.copy(
                        name = name.trim(),
                        type = type.raw,
                        budgetLimit = budgetLimit,
                        colorHex = colorHex,
                        iconKey = iconKey
                    )
                )
            } else {
                repository.createCategory(
                    name = name.trim(),
                    type = type.raw,
                    budgetLimit = budgetLimit,
                    colorHex = colorHex,
                    iconKey = iconKey,
                    sortOrder = nextSortOrder
                ).map { }
            }
            result.onSuccess {
                _statusMessage.value = if (existingCategory != null) "Category updated" else "Added '$name' to category list"
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to save category"
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
                .onSuccess { _statusMessage.value = "Category deleted" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to delete category" }
        }
    }

    fun createNewFunctionAccount(
        name: String,
        year: Long,
        defaultContribution: Long,
        targetBudget: Long
    ) {
        viewModelScope.launch {
            repository.createAccount(
                name = name.trim(),
                year = year,
                defaultContribution = defaultContribution,
                targetBudget = targetBudget,
                archived = false
            ).onSuccess { newId ->
                _filterState.value = _filterState.value.copy(selectedAccountId = newId)
                _statusMessage.value = "Created '$name' ledger"
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to create function account"
            }
        }
    }

    private companion object {
        const val TAG = "LedgerVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
