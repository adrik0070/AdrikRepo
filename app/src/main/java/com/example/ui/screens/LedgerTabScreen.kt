package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.EntryType
import com.example.data.model.EntryWithBalance
import com.example.data.model.PaymentMode
import com.example.ui.components.HeroBalanceSummaryCard
import com.example.ui.components.categoryIconForKey
import com.example.ui.components.parseCategoryColor
import com.example.ui.components.semanticExpenseColor
import com.example.ui.components.semanticIncomeColor
import com.example.ui.theme.TabularCurrencyFontFamily
import com.example.ui.viewmodel.EntryFilterTab
import com.example.ui.viewmodel.LedgerDashboardData
import com.example.util.CurrencyFormatter

@Composable
fun LedgerTabScreen(
    data: LedgerDashboardData,
    onAddExpenseClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onEditEntryClick: (EntryWithBalance) -> Unit,
    onMarkReimbursedClick: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterTabChange: (EntryFilterTab) -> Unit,
    onCategoryFilterClick: (String?) -> Unit,
    onToggleSortOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeAccount = data.activeAccount
    val categoryIconMap = data.categories.associateBy { it.id }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("ledger_tab_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "hero_summary") {
            HeroBalanceSummaryCard(
                accountName = activeAccount?.name ?: stringResource(R.string.app_name),
                summary = data.summary,
                isArchived = activeAccount?.archived == true
            )
        }

        item(key = "quick_actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onAddExpenseClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("quick_add_expense_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.add_expense))
                }
                OutlinedButton(
                    onClick = onAddIncomeClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("quick_add_income_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.add_income))
                }
            }
        }

        item(key = "search_and_filters") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = data.filterState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.search_entries_hint)) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "Search entries")
                    },
                    trailingIcon = {
                        if (data.filterState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ledger_search_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(EntryFilterTab.entries) { tab ->
                            FilterChip(
                                selected = data.filterState.filterTab == tab,
                                onClick = { onFilterTabChange(tab) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("filter_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    AssistChip(
                        onClick = onToggleSortOrder,
                        label = {
                            Text(
                                text = if (data.filterState.newestFirst) {
                                    stringResource(R.string.latest_order)
                                } else {
                                    stringResource(R.string.chronological_order)
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.SwapVert,
                                contentDescription = "Toggle sort order",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                // Category filter chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(data.categories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = data.filterState.selectedCategoryId == cat.id,
                            onClick = { onCategoryFilterClick(cat.id) },
                            label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        if (data.filteredRows.isEmpty()) {
            item(key = "empty_state") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.empty_entries_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.empty_entries_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(
                items = data.filteredRows,
                key = { it.entry.id }
            ) { row ->
                val catObj = categoryIconMap[row.entry.categoryId]
                LedgerEntryRowCard(
                    row = row,
                    iconKey = catObj?.iconKey ?: if (row.entry.type == EntryType.INCOME.raw) "people" else "festival",
                    colorHex = catObj?.colorHex ?: if (row.entry.type == EntryType.INCOME.raw) "#1B5E20" else "#C62828",
                    onClick = { onEditEntryClick(row) },
                    onMarkReimbursed = { onMarkReimbursedClick(row.entry.id) }
                )
            }
        }
    }
}

@Composable
private fun LedgerEntryRowCard(
    row: EntryWithBalance,
    iconKey: String,
    colorHex: String,
    onClick: () -> Unit,
    onMarkReimbursed: () -> Unit
) {
    val entry = row.entry
    val isIncome = entry.type == EntryType.INCOME.raw
    val amountColor = if (isIncome) semanticIncomeColor() else semanticExpenseColor()
    val badgeColor = parseCategoryColor(colorHex)
    val paymentMode = PaymentMode.fromRaw(entry.paymentMode)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("entry_card_${entry.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = badgeColor.copy(alpha = 0.14f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = categoryIconForKey(iconKey),
                            contentDescription = entry.categoryName,
                            tint = badgeColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.notes,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = entry.dateStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = entry.categoryName,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (entry.partyName.isNotBlank() || !isIncome) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (entry.partyName.isNotBlank()) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = if (isIncome) "Resident: ${entry.partyName}" else "Payee: ${entry.partyName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (!isIncome) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = if (paymentMode == PaymentMode.MEMBER_ADVANCE) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    }
                                ) {
                                    Text(
                                        text = paymentMode.shortBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (paymentMode == PaymentMode.MEMBER_ADVANCE) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = (if (isIncome) "+ " else "- ") + CurrencyFormatter.formatInr(entry.amount),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = TabularCurrencyFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = amountColor
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Bal ${CurrencyFormatter.formatInr(row.runningBalance)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = TabularCurrencyFontFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (paymentMode == PaymentMode.MEMBER_ADVANCE) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(
                        onClick = onMarkReimbursed,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("reimburse_btn_${entry.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.mark_reimbursed),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
