package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Festival
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AccountSummaryMetrics
import com.example.data.model.CategoryBreakdownItem
import com.example.data.model.LedgerAccount
import com.example.ui.theme.ExpenseCrimsonDark
import com.example.ui.theme.ExpenseCrimsonLight
import com.example.ui.theme.HeroGoldAccent
import com.example.ui.theme.HeroGradientEnd
import com.example.ui.theme.HeroGradientStart
import com.example.ui.theme.IncomeGreenDark
import com.example.ui.theme.IncomeGreenLight
import com.example.ui.theme.TabularCurrencyFontFamily
import com.example.util.CurrencyFormatter

fun categoryIconForKey(iconKey: String): ImageVector = when (iconKey.lowercase()) {
    "music" -> Icons.Filled.MusicNote
    "temple" -> Icons.Filled.AccountBalance
    "person" -> Icons.Filled.Person
    "festival" -> Icons.Filled.Festival
    "lightbulb" -> Icons.Filled.Lightbulb
    "spa" -> Icons.Filled.Spa
    "shopping" -> Icons.Filled.ShoppingBag
    "flower" -> Icons.Filled.LocalFlorist
    "cleaning" -> Icons.Filled.CleaningServices
    "shipping" -> Icons.Filled.LocalShipping
    "people" -> Icons.Filled.Groups
    else -> Icons.Filled.Category
}

@Composable
fun parseCategoryColor(colorHex: String): Color {
    val fallback = MaterialTheme.colorScheme.primary
    return remember(colorHex, fallback) {
        try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (_: Exception) {
            fallback
        }
    }
}

@Composable
fun semanticIncomeColor(): Color =
    if (isSystemInDarkTheme()) IncomeGreenDark else IncomeGreenLight

@Composable
fun semanticExpenseColor(): Color =
    if (isSystemInDarkTheme()) ExpenseCrimsonDark else ExpenseCrimsonLight

@Composable
fun AccountSwitcherTopBar(
    accounts: List<LedgerAccount>,
    activeAccount: LedgerAccount?,
    userEmail: String?,
    onSelectAccount: (String) -> Unit,
    onCreateNewFunction: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { menuExpanded = true }
                        .testTag("account_switcher_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (activeAccount?.archived == true) {
                                    "REFERENCE ARCHIVE (${activeAccount.year})"
                                } else {
                                    "ACTIVE FUNCTION LEDGER"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = activeAccount?.name ?: stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            contentDescription = "Switch function account",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    accounts.forEach { acc ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = acc.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (acc.id == activeAccount?.id) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        }
                                    )
                                    Text(
                                        text = if (acc.archived) {
                                            "Last year report • ${acc.year}"
                                        } else {
                                            "Active function • ${acc.year}"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                onSelectAccount(acc.id)
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(Icons.Filled.Add, contentDescription = null)
                        },
                        text = { Text(stringResource(R.string.new_function)) },
                        onClick = {
                            menuExpanded = false
                            onCreateNewFunction()
                        }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!userEmail.isNullOrBlank()) {
                    Text(
                        text = userEmail.substringBefore("@"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = stringResource(R.string.sign_out),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeroBalanceSummaryCard(
    accountName: String,
    summary: AccountSummaryMetrics,
    isArchived: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_balance_card"),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(HeroGradientStart, HeroGradientEnd)
                    )
                )
        ) {
            // Subtle decorative Alpana arcs drawn via DrawScope size
            Canvas(modifier = Modifier.matchParentSize()) {
                val radiusOuter = size.minDimension * 0.62f
                drawCircle(
                    color = HeroGoldAccent.copy(alpha = 0.08f),
                    radius = radiusOuter,
                    center = Offset(size.width * 0.92f, size.height * 0.16f)
                )
                drawCircle(
                    color = HeroGoldAccent.copy(alpha = 0.14f),
                    radius = radiusOuter * 0.68f,
                    center = Offset(size.width * 0.92f, size.height * 0.16f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.total_balance).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = HeroGoldAccent
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "${summary.entryCount} entries",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = CurrencyFormatter.formatInr(summary.netBalance),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = TabularCurrencyFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (summary.netBalance < 0L) HeroGoldAccent else Color.White
                )

                Text(
                    text = if (isArchived) {
                        "$accountName • Historical Final Statement"
                    } else if (summary.netBalance < 0L) {
                        "Expenses exceed collected chanda by ${CurrencyFormatter.formatInr(-summary.netBalance)}"
                    } else {
                        "Healthy surplus in $accountName fund"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.82f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeroMetricPill(
                        label = stringResource(R.string.total_income),
                        value = CurrencyFormatter.formatInr(summary.totalIncome),
                        accent = IncomeGreenDark,
                        modifier = Modifier.weight(1f)
                    )
                    HeroMetricPill(
                        label = stringResource(R.string.total_expense),
                        value = CurrencyFormatter.formatInr(summary.totalExpense),
                        accent = ExpenseCrimsonDark,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (summary.pendingReimbursements > 0L) {
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = HeroGoldAccent.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = HeroGoldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Due to Members (${summary.unreimbursedCount}): ${CurrencyFormatter.formatInr(summary.pendingReimbursements)}  •  Cash in Hand: ${CurrencyFormatter.formatInr(summary.availableCashInHand)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMetricPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = Color.White.copy(alpha = 0.12f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = TabularCurrencyFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CategoryDonutChart(
    items: List<CategoryBreakdownItem>,
    totalExpense: Long,
    modifier: Modifier = Modifier
) {
    val activeSlices = items.filter { it.currentAmount > 0L }
    val resolvedColors = activeSlices.map { parseCategoryColor(it.category.colorHex) }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(148.dp)) {
            val strokePx = size.minDimension * 0.16f
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx)
            )

            if (totalExpense > 0L && activeSlices.isNotEmpty()) {
                var startAngle = -90f
                activeSlices.forEachIndexed { idx, slice ->
                    val sweep = (slice.currentAmount.toFloat() / totalExpense.toFloat()) * 360f
                    drawArc(
                        color = resolvedColors[idx],
                        startAngle = startAngle,
                        sweepAngle = (sweep - 1.5f).coerceAtLeast(1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Spent",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = CurrencyFormatter.formatInr(totalExpense),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = TabularCurrencyFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
