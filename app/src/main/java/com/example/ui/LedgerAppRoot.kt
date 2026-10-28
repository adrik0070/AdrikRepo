package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.EntryType
import com.example.data.model.LedgerCategory
import com.example.data.model.LedgerEntry
import com.example.data.model.UiState
import com.example.data.repository.LedgerRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.AccountSwitcherTopBar
import com.example.ui.components.CategoryEditorDialog
import com.example.ui.components.CollectContributionDialog
import com.example.ui.components.EntryEditorBottomSheet
import com.example.ui.components.NewFunctionAccountDialog
import com.example.ui.screens.CategoriesTabScreen
import com.example.ui.screens.LedgerTabScreen
import com.example.ui.screens.ReportsTabScreen
import com.example.ui.screens.ResidentsTabScreen
import com.example.ui.viewmodel.LedgerDashboardData
import com.example.ui.viewmodel.LedgerViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

enum class MainDestination(
    val routeId: String,
    val labelRes: Int,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
) {
    LEDGER(
        routeId = "ledger",
        labelRes = R.string.nav_ledger,
        filledIcon = Icons.Filled.AccountBalanceWallet,
        outlinedIcon = Icons.Outlined.AccountBalanceWallet
    ),
    RESIDENTS(
        routeId = "residents",
        labelRes = R.string.nav_residents,
        filledIcon = Icons.Filled.Groups,
        outlinedIcon = Icons.Outlined.Groups
    ),
    CATEGORIES(
        routeId = "categories",
        labelRes = R.string.nav_categories,
        filledIcon = Icons.Filled.Category,
        outlinedIcon = Icons.Outlined.Category
    ),
    REPORTS(
        routeId = "reports",
        labelRes = R.string.nav_reports,
        filledIcon = Icons.Filled.Assessment,
        outlinedIcon = Icons.Outlined.Assessment
    )
}

@Composable
fun LedgerAppRoot(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        AuthenticatedLedgerShell(
            currentUserId = user.uid,
            userEmail = user.email
        )
    }
}

@Composable
private fun AuthenticatedLedgerShell(
    currentUserId: String,
    userEmail: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val ledgerViewModel: LedgerViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                LedgerViewModel(LedgerRepository(db), currentUserId)
            }
        }
    )

    val dashboardUiState by ledgerViewModel.dashboardState.collectAsStateWithLifecycle()
    val statusMessage by ledgerViewModel.statusMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by rememberSaveable { mutableStateOf(MainDestination.LEDGER) }

    // Dialog / BottomSheet states
    var showEntrySheet by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<LedgerEntry?>(null) }
    var entrySheetInitialType by remember { mutableStateOf(EntryType.EXPENSE) }

    var showCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<LedgerCategory?>(null) }

    var showNewFunctionDialog by remember { mutableStateOf(false) }
    var collectDialogTarget by remember { mutableStateOf<Pair<String, Long>?>(null) }

    // BackHandler on secondary tabs per navigation guideline
    if (currentTab != MainDestination.LEDGER) {
        BackHandler {
            currentTab = MainDestination.LEDGER
        }
    }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            ledgerViewModel.clearStatusMessage()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp
        val activeData = (dashboardUiState as? UiState.Success)?.data

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                AccountSwitcherTopBar(
                    accounts = activeData?.accounts.orEmpty(),
                    activeAccount = activeData?.activeAccount,
                    userEmail = userEmail,
                    onSelectAccount = { ledgerViewModel.selectAccount(it) },
                    onCreateNewFunction = { showNewFunctionDialog = true },
                    onSignOut = {
                        signOutUser(
                            credentialManager = credentialManager,
                            onSignOutComplete = {},
                            scope = scope
                        )
                    }
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar {
                        MainDestination.entries.forEach { dest ->
                            val selected = currentTab == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = dest },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.filledIcon else dest.outlinedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag("nav_tab_${dest.routeId}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentTab == MainDestination.LEDGER && activeData != null) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            editingEntry = null
                            entrySheetInitialType = EntryType.EXPENSE
                            showEntrySheet = true
                        },
                        icon = {
                            Icon(Icons.Filled.Add, contentDescription = null)
                        },
                        text = { Text(stringResource(R.string.add_entry)) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_entry")
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        MainDestination.entries.forEach { dest ->
                            val selected = currentTab == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = dest },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.filledIcon else dest.outlinedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag("rail_tab_${dest.routeId}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val contentModifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 840.dp)

                    when (val state = dashboardUiState) {
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Text(
                                        text = "Loading function ledger…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 12.dp)
                                    )
                                }
                            }
                        }

                        is UiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        is UiState.Success -> {
                            DestinationBody(
                                currentTab = currentTab,
                                data = state.data,
                                viewModel = ledgerViewModel,
                                onOpenEntrySheet = { existing, type ->
                                    editingEntry = existing
                                    entrySheetInitialType = type
                                    showEntrySheet = true
                                },
                                onOpenCategoryDialog = { cat ->
                                    editingCategory = cat
                                    showCategoryDialog = true
                                },
                                onOpenCollectDialog = { name, amount ->
                                    collectDialogTarget = name to amount
                                },
                                modifier = contentModifier
                            )
                        }
                    }
                }
            }
        }

        // Modals
        if (showEntrySheet && activeData != null) {
            val activeAcc = activeData.activeAccount
            if (activeAcc != null) {
                EntryEditorBottomSheet(
                    existingEntry = editingEntry,
                    initialType = entrySheetInitialType,
                    categories = activeData.categories,
                    defaultContribution = activeAcc.defaultContribution,
                    onDismiss = {
                        showEntrySheet = false
                        editingEntry = null
                    },
                    onSave = { type, amount, category, notes, partyName, paymentMode, dateStr ->
                        ledgerViewModel.saveEntry(
                            existingEntry = editingEntry,
                            accountId = activeAcc.id,
                            type = type,
                            amount = amount,
                            category = category,
                            notes = notes,
                            partyName = partyName,
                            paymentMode = paymentMode,
                            dateStr = dateStr
                        )
                        showEntrySheet = false
                        editingEntry = null
                    },
                    onDelete = { id ->
                        ledgerViewModel.deleteEntry(id)
                        showEntrySheet = false
                        editingEntry = null
                    }
                )
            }
        }

        if (showCategoryDialog && activeData != null) {
            CategoryEditorDialog(
                existingCategory = editingCategory,
                onDismiss = {
                    showCategoryDialog = false
                    editingCategory = null
                },
                onSave = { name, type, budgetLimit, colorHex, iconKey ->
                    ledgerViewModel.saveCategory(
                        existingCategory = editingCategory,
                        name = name,
                        type = type,
                        budgetLimit = budgetLimit,
                        colorHex = colorHex,
                        iconKey = iconKey,
                        nextSortOrder = (activeData.categories.size + 1).toLong()
                    )
                    showCategoryDialog = false
                    editingCategory = null
                },
                onDelete = { catId ->
                    ledgerViewModel.deleteCategory(catId)
                    showCategoryDialog = false
                    editingCategory = null
                }
            )
        }

        if (showNewFunctionDialog) {
            NewFunctionAccountDialog(
                onDismiss = { showNewFunctionDialog = false },
                onCreate = { name, year, defaultContrib, budget ->
                    ledgerViewModel.createNewFunctionAccount(name, year, defaultContrib, budget)
                    showNewFunctionDialog = false
                }
            )
        }

        val collectTarget = collectDialogTarget
        if (collectTarget != null && activeData?.activeAccount != null) {
            val incomeCat = activeData.categories.firstOrNull { it.type == EntryType.INCOME.raw }
            CollectContributionDialog(
                residentName = collectTarget.first,
                suggestedAmount = collectTarget.second,
                onDismiss = { collectDialogTarget = null },
                onConfirm = { resName, amt ->
                    ledgerViewModel.collectResidentContribution(
                        accountId = activeData.activeAccount.id,
                        residentName = resName,
                        amount = amt,
                        incomeCategory = incomeCat
                    )
                    collectDialogTarget = null
                }
            )
        }
    }
}

@Composable
private fun DestinationBody(
    currentTab: MainDestination,
    data: LedgerDashboardData,
    viewModel: LedgerViewModel,
    onOpenEntrySheet: (LedgerEntry?, EntryType) -> Unit,
    onOpenCategoryDialog: (LedgerCategory?) -> Unit,
    onOpenCollectDialog: (String, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when (currentTab) {
        MainDestination.LEDGER -> LedgerTabScreen(
            data = data,
            onAddExpenseClick = { onOpenEntrySheet(null, EntryType.EXPENSE) },
            onAddIncomeClick = { onOpenEntrySheet(null, EntryType.INCOME) },
            onEditEntryClick = { row -> onOpenEntrySheet(row.entry, EntryType.fromRaw(row.entry.type)) },
            onMarkReimbursedClick = { entryId -> viewModel.markReimbursed(entryId) },
            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
            onFilterTabChange = { viewModel.updateFilterTab(it) },
            onCategoryFilterClick = { viewModel.updateCategoryFilter(it) },
            onToggleSortOrder = { viewModel.toggleSortOrder() },
            modifier = modifier
        )

        MainDestination.RESIDENTS -> ResidentsTabScreen(
            data = data,
            onCollectForResident = { name, amt -> onOpenCollectDialog(name, amt) },
            onAddNewResidentPayment = {
                onOpenCollectDialog("", data.activeAccount?.defaultContribution ?: 2400L)
            },
            modifier = modifier
        )

        MainDestination.CATEGORIES -> CategoriesTabScreen(
            data = data,
            onAddCategoryClick = { onOpenCategoryDialog(null) },
            onEditCategoryClick = { cat -> onOpenCategoryDialog(cat) },
            modifier = modifier
        )

        MainDestination.REPORTS -> ReportsTabScreen(
            data = data,
            onExportError = { err -> viewModel.postStatusMessage(err) },
            modifier = modifier
        )
    }
}
