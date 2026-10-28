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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CategoryBreakdownItem
import com.example.data.model.EntryType
import com.example.data.model.LedgerCategory
import com.example.data.seed.KaliPujoSeedData
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.categoryIconForKey
import com.example.ui.components.parseCategoryColor
import com.example.ui.theme.TabularCurrencyFontFamily
import com.example.ui.viewmodel.LedgerDashboardData
import com.example.util.CurrencyFormatter

@Composable
fun CategoriesTabScreen(
    data: LedgerDashboardData,
    onAddCategoryClick: () -> Unit,
    onEditCategoryClick: (LedgerCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val incomeCategories = data.categories.filter { it.type == EntryType.INCOME.raw }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("categories_tab_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "donut_overview_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    CategoryDonutChart(
                        items = data.categoryBreakdown,
                        totalExpense = data.summary.totalExpense
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Updated Category List",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Includes Dhak, Thakur & Thakurmasai plus 8 structured puja categories derived from last year's report.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onAddCategoryClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("add_category_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.add_category))
                        }
                    }
                }
            }
        }

        item(key = "expense_categories_header") {
            Text(
                text = "Expense Categories (${data.categoryBreakdown.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(
            items = data.categoryBreakdown,
            key = { it.category.id }
        ) { item ->
            CategorySpendCard(
                item = item,
                onClick = { onEditCategoryClick(item.category) }
            )
        }

        if (incomeCategories.isNotEmpty()) {
            item(key = "income_categories_header") {
                Text(
                    text = "Income Categories (${incomeCategories.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(incomeCategories, key = { it.id }) { cat ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditCategoryClick(cat) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val badgeColor = parseCategoryColor(cat.colorHex)
                            Surface(
                                shape = CircleShape,
                                color = badgeColor.copy(alpha = 0.14f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = categoryIconForKey(cat.iconKey),
                                        contentDescription = cat.name,
                                        tint = badgeColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(cat.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Total Collected: ${CurrencyFormatter.formatInr(data.summary.totalIncome)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit category",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        item(key = "playbook_header") {
            Text(
                text = stringResource(R.string.recommendations_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        items(
            items = KaliPujoSeedData.accountManagerRecommendations,
            key = { it.title }
        ) { rec ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = rec.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Text(
                        text = rec.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = rec.actionHint,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun CategorySpendCard(
    item: CategoryBreakdownItem,
    onClick: () -> Unit
) {
    val cat = item.category
    val badgeColor = parseCategoryColor(cat.colorHex)
    val budgetCap = cat.budgetLimit
    val budgetProgress = if (budgetCap > 0L) {
        (item.currentAmount.toFloat() / budgetCap.toFloat()).coerceIn(0f, 1f)
    } else {
        item.shareFraction.coerceIn(0f, 1f)
    }
    val overBudget = budgetCap > 0L && item.currentAmount > budgetCap

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("category_card_${cat.name.lowercase().replace(' ', '_')}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = badgeColor.copy(alpha = 0.14f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = categoryIconForKey(cat.iconKey),
                                contentDescription = cat.name,
                                tint = badgeColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = buildString {
                                append("${item.transactionCount} item(s)")
                                if (item.previousYearAmount > 0L) {
                                    append(" • 2025: ${CurrencyFormatter.formatInr(item.previousYearAmount)}")
                                } else {
                                    append(" • New for 2026")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.formatInr(item.currentAmount),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = TabularCurrencyFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (overBudget) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (budgetCap > 0L) {
                        Text(
                            text = "Budget ${CurrencyFormatter.formatInr(budgetCap)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { budgetProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (overBudget) MaterialTheme.colorScheme.error else badgeColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
