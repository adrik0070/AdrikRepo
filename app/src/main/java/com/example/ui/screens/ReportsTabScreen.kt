package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.EntryType
import com.example.ui.components.semanticExpenseColor
import com.example.ui.components.semanticIncomeColor
import com.example.ui.theme.TabularCurrencyFontFamily
import com.example.ui.viewmodel.LedgerDashboardData
import com.example.util.CurrencyFormatter
import com.example.util.LedgerReportExporter

@Composable
fun ReportsTabScreen(
    data: LedgerDashboardData,
    onExportError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeAccount = data.activeAccount
    val incomeColor = semanticIncomeColor()
    val expenseColor = semanticExpenseColor()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_tab_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "export_actions_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Account Manager Statement & Export",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Generate a multi-page tabular PDF report styled like last year's Kali Pujo statement—complete with structured categories, payees, and running balance—or share a quick text summary with your apartment group.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (activeAccount != null) {
                                    LedgerReportExporter.exportAndSharePdf(
                                        context = context,
                                        account = activeAccount,
                                        summary = data.summary,
                                        chronologicalRows = data.chronologicalRows,
                                        onError = onExportError
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .minimumInteractiveComponentSize()
                                .testTag("export_pdf_button")
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.export_pdf_report))
                        }

                        OutlinedButton(
                            onClick = {
                                if (activeAccount != null) {
                                    LedgerReportExporter.shareGroupSummaryText(
                                        context = context,
                                        account = activeAccount,
                                        summary = data.summary,
                                        categoryBreakdown = data.categoryBreakdown
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("share_summary_button")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.share_text_summary))
                        }
                    }
                }
            }
        }

        if (data.previousYearSummary != null) {
            item(key = "yoy_comparison_card") {
                val prev = data.previousYearSummary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${activeAccount?.name ?: "Current"} vs. Reference Year Benchmark",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ComparisonColumn(
                                title = activeAccount?.name ?: "Current",
                                income = data.summary.totalIncome,
                                expense = data.summary.totalExpense,
                                balance = data.summary.netBalance
                            )
                            ComparisonColumn(
                                title = "Last Year (${prev.entryCount} entries)",
                                income = prev.totalIncome,
                                expense = prev.totalExpense,
                                balance = prev.netBalance
                            )
                        }
                    }
                }
            }
        }

        item(key = "statement_table_header") {
            Column {
                Text(
                    text = "Live Statement Preview (${data.chronologicalRows.size} entries)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.95f)
                    )
                    Text(
                        text = "Notes & Category",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.9f)
                    )
                    Text(
                        text = "Inc / Exp",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.05f)
                    )
                    Text(
                        text = "Balance",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.05f)
                    )
                }
            }
        }

        items(
            items = data.chronologicalRows,
            key = { "stmt_${it.entry.id}" }
        ) { row ->
            val e = row.entry
            val isInc = e.type == EntryType.INCOME.raw
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = e.dateStr.take(10),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.95f)
                    )
                    Column(modifier = Modifier.weight(1.9f)) {
                        Text(
                            text = e.notes,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = e.categoryName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = (if (isInc) "+" else "-") + CurrencyFormatter.formatInr(e.amount, showCurrencySymbol = false),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = TabularCurrencyFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isInc) incomeColor else expenseColor,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.05f)
                    )
                    Text(
                        text = CurrencyFormatter.formatInr(row.runningBalance, showCurrencySymbol = false),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = TabularCurrencyFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.05f)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }
        }
    }
}

@Composable
private fun ComparisonColumn(
    title: String,
    income: Long,
    expense: Long,
    balance: Long
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text(
            "Inc: ${CurrencyFormatter.formatInr(income)}",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = TabularCurrencyFontFamily),
            color = semanticIncomeColor()
        )
        Text(
            "Exp: ${CurrencyFormatter.formatInr(expense)}",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = TabularCurrencyFontFamily),
            color = semanticExpenseColor()
        )
        Text(
            "Bal: ${CurrencyFormatter.formatInr(balance)}",
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = TabularCurrencyFontFamily,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
