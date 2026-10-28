package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ResidentContributionRow
import com.example.ui.components.semanticIncomeColor
import com.example.ui.theme.TabularCurrencyFontFamily
import com.example.ui.viewmodel.LedgerDashboardData
import com.example.util.CurrencyFormatter

@Composable
fun ResidentsTabScreen(
    data: LedgerDashboardData,
    onCollectForResident: (residentName: String, suggestedAmount: Long) -> Unit,
    onAddNewResidentPayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val paidCount = data.residentRows.count { it.currentYearContribution > 0L }
    val totalResidents = data.residentRows.size.coerceAtLeast(1)
    val defaultRate = data.activeAccount?.defaultContribution ?: 2400L
    val targetTotal = data.activeAccount?.targetBudget ?: 40000L
    val progress = (data.summary.totalIncome.toFloat() / targetTotal.coerceAtLeast(1L).toFloat())
        .coerceIn(0f, 1f)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("residents_tab_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "chanda_summary_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Apartment Chanda Tracker",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "$paidCount / ${data.residentRows.size} Paid",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Collected: ${CurrencyFormatter.formatInr(data.summary.totalIncome)}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = TabularCurrencyFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Target: ${CurrencyFormatter.formatInr(targetTotal)}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = TabularCurrencyFontFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "Tip: Last year 14 flats paid ₹1,800 + Ratan da ₹5,000 = ₹30,200 (ending ₹-219). With ₹9,000 new Dhak, Thakur & Thakurmasai costs this year, collecting ₹${defaultRate} per flat keeps the fund comfortably positive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onAddNewResidentPayment,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("add_new_resident_chanda_button")
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Collect from Another Flat / Resident")
                    }
                }
            }
        }

        items(
            items = data.residentRows,
            key = { it.residentName }
        ) { row ->
            ResidentChecklistCard(
                row = row,
                suggestedRate = if (row.lastYearContribution > defaultRate) row.lastYearContribution else defaultRate,
                onCollectClick = {
                    val suggested = if (row.lastYearContribution > defaultRate) {
                        row.lastYearContribution
                    } else {
                        defaultRate
                    }
                    onCollectForResident(row.residentName, suggested)
                }
            )
        }
    }
}

@Composable
private fun ResidentChecklistCard(
    row: ResidentContributionRow,
    suggestedRate: Long,
    onCollectClick: () -> Unit
) {
    val isPaid = row.currentYearContribution > 0L
    val incomeColor = semanticIncomeColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("resident_card_${row.residentName.lowercase().replace(' ', '_')}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isPaid) {
                        incomeColor.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPaid) Icons.Filled.CheckCircle else Icons.Filled.Schedule,
                            contentDescription = if (isPaid) "Paid" else "Pending",
                            tint = if (isPaid) incomeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = row.residentName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = buildString {
                            if (row.lastYearContribution > 0L) {
                                append("2025: ${CurrencyFormatter.formatInr(row.lastYearContribution)}")
                            } else {
                                append("New contributor")
                            }
                            if (row.pendingMemberAdvance > 0L) {
                                append(" • Due reimbursement: ${CurrencyFormatter.formatInr(row.pendingMemberAdvance)}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isPaid) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.formatInr(row.currentYearContribution),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = TabularCurrencyFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = incomeColor
                    )
                    Text(
                        text = "Paid",
                        style = MaterialTheme.typography.labelSmall,
                        color = incomeColor
                    )
                }
            } else {
                FilledTonalButton(
                    onClick = onCollectClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("collect_btn_${row.residentName.lowercase().replace(' ', '_')}")
                ) {
                    Text("Collect ${CurrencyFormatter.formatInr(suggestedRate)}")
                }
            }
        }
    }
}
