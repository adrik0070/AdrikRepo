package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.EntryType
import com.example.data.model.LedgerCategory
import com.example.data.model.LedgerEntry
import com.example.data.model.PaymentMode
import com.example.data.seed.KaliPujoSeedData
import com.example.util.CurrencyFormatter

private val CATEGORY_COLOR_SWATCHES = listOf(
    "#C62828",
    "#AD1457",
    "#E65100",
    "#6A1B9A",
    "#F9A825",
    "#2E7D32",
    "#00838F",
    "#1565C0",
    "#455A64"
)

private val CATEGORY_ICON_OPTIONS = listOf(
    "music" to "Music / Dhak",
    "temple" to "Idol / Thakur",
    "person" to "Priest / Person",
    "festival" to "Pandal",
    "lightbulb" to "Lights",
    "spa" to "Samagri",
    "shopping" to "Groceries",
    "flower" to "Flowers / Sweets",
    "cleaning" to "Cleaning",
    "shipping" to "Transport",
    "people" to "Residents"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EntryEditorBottomSheet(
    existingEntry: LedgerEntry?,
    initialType: EntryType,
    categories: List<LedgerCategory>,
    defaultContribution: Long,
    onDismiss: () -> Unit,
    onSave: (
        type: EntryType,
        amount: Long,
        category: LedgerCategory,
        notes: String,
        partyName: String,
        paymentMode: PaymentMode,
        dateStr: String
    ) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedType by remember(existingEntry, initialType) {
        mutableStateOf(
            existingEntry?.let { EntryType.fromRaw(it.type) } ?: initialType
        )
    }

    val matchingCategories = remember(categories, selectedType) {
        val typed = categories.filter { it.type == selectedType.raw }
        typed.ifEmpty { categories }
    }

    var selectedCategoryId by remember(existingEntry, selectedType, matchingCategories) {
        mutableStateOf(
            existingEntry?.categoryId?.takeIf { id -> matchingCategories.any { it.id == id } }
                ?: matchingCategories.firstOrNull()?.id.orEmpty()
        )
    }

    var amountText by remember(existingEntry) {
        mutableStateOf(existingEntry?.amount?.toString().orEmpty())
    }
    var notesText by remember(existingEntry) {
        mutableStateOf(existingEntry?.notes.orEmpty())
    }
    var partyText by remember(existingEntry) {
        mutableStateOf(existingEntry?.partyName.orEmpty())
    }
    var paymentMode by remember(existingEntry, selectedType) {
        mutableStateOf(
            existingEntry?.let { PaymentMode.fromRaw(it.paymentMode) }
                ?: if (selectedType == EntryType.INCOME) PaymentMode.RESIDENT_PAID else PaymentMode.FUND
        )
    }
    var dateText by remember(existingEntry) {
        mutableStateOf(existingEntry?.dateStr ?: CurrencyFormatter.todayDateStr())
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingEntry != null) "Edit Ledger Entry" else "New Ledger Entry",
                    style = MaterialTheme.typography.headlineSmall
                )
                if (existingEntry != null && onDelete != null) {
                    OutlinedButton(
                        onClick = { onDelete(existingEntry.id) },
                        modifier = Modifier.testTag("delete_entry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete entry",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete")
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                EntryType.entries.forEachIndexed { idx, typeOption ->
                    SegmentedButton(
                        selected = selectedType == typeOption,
                        onClick = {
                            selectedType = typeOption
                            paymentMode = if (typeOption == EntryType.INCOME) {
                                PaymentMode.RESIDENT_PAID
                            } else {
                                PaymentMode.FUND
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = idx,
                            count = EntryType.entries.size
                        ),
                        modifier = Modifier.testTag("entry_type_${typeOption.raw.lowercase()}")
                    ) {
                        Text(typeOption.displayLabel)
                    }
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    amountText = input.filter { it.isDigit() }.take(9)
                    validationError = null
                },
                label = { Text("Amount in ₹") },
                placeholder = { Text("e.g. 1000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("entry_amount_input")
            )

            // Quick amount chips
            val presetAmounts = if (selectedType == EntryType.INCOME) {
                listOf(1800L, defaultContribution, 3000L, 5000L).distinct()
            } else {
                listOf(500L, 1000L, 3000L, 5000L)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presetAmounts) { preset ->
                    AssistChip(
                        onClick = { amountText = preset.toString() },
                        label = { Text(CurrencyFormatter.formatInr(preset)) }
                    )
                }
            }

            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                matchingCategories.forEach { cat ->
                    val isSelected = cat.id == selectedCategoryId
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCategoryId = cat.id
                            if (notesText.isBlank()) {
                                notesText = cat.name
                            }
                        },
                        label = { Text(cat.name) },
                        leadingIcon = {
                            Icon(
                                imageVector = categoryIconForKey(cat.iconKey),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it.take(300) },
                label = {
                    Text(
                        if (selectedType == EntryType.INCOME) {
                            "Notes / Flat Details"
                        } else {
                            "Item / Expense Notes"
                        }
                    )
                },
                placeholder = { Text("e.g. Dhak, Thakurmasai dakshina, Dashakarma…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("entry_notes_input")
            )

            OutlinedTextField(
                value = partyText,
                onValueChange = { partyText = it.take(100) },
                label = {
                    Text(
                        if (selectedType == EntryType.INCOME) {
                            "Resident Name"
                        } else {
                            "Payee / Paid by Member (Optional)"
                        }
                    )
                },
                placeholder = { Text("e.g. Anirban, Rahul, Ranjan…") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("entry_party_input")
            )

            // Quick resident chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(KaliPujoSeedData.apartmentResidents2025.take(8)) { resident ->
                    AssistChip(
                        onClick = {
                            partyText = resident.name
                            if (selectedType == EntryType.INCOME && notesText.isBlank()) {
                                notesText = resident.name
                            }
                        },
                        label = { Text(resident.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            if (selectedType == EntryType.EXPENSE) {
                Text(
                    text = "Payment Settlement Mode",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val expenseModes = listOf(
                    PaymentMode.FUND,
                    PaymentMode.MEMBER_ADVANCE,
                    PaymentMode.REIMBURSED
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    expenseModes.forEach { mode ->
                        FilterChip(
                            selected = paymentMode == mode,
                            onClick = { paymentMode = mode },
                            label = { Text(mode.shortBadge) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it.take(20) },
                label = { Text("Date (DD/MM/YYYY)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (validationError != null) {
                Text(
                    text = validationError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val parsedAmount = amountText.toLongOrNull()
                        val chosenCategory = matchingCategories.firstOrNull { it.id == selectedCategoryId }
                            ?: matchingCategories.firstOrNull()
                        if (parsedAmount == null || parsedAmount <= 0L) {
                            validationError = "Please enter a valid positive amount in ₹."
                            return@Button
                        }
                        if (chosenCategory == null) {
                            validationError = "Please add or select a category first."
                            return@Button
                        }
                        val finalNotes = notesText.trim().ifBlank {
                            partyText.trim().ifBlank { chosenCategory.name }
                        }
                        onSave(
                            selectedType,
                            parsedAmount,
                            chosenCategory,
                            finalNotes,
                            partyText.trim(),
                            paymentMode,
                            dateText.trim().ifBlank { CurrencyFormatter.todayDateStr() }
                        )
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("save_entry_button")
                ) {
                    Text(
                        text = if (existingEntry != null) "Update Entry" else "Save Entry",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryEditorDialog(
    existingCategory: LedgerCategory?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        type: EntryType,
        budgetLimit: Long,
        colorHex: String,
        iconKey: String
    ) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    var nameText by remember(existingCategory) { mutableStateOf(existingCategory?.name.orEmpty()) }
    var selectedType by remember(existingCategory) {
        mutableStateOf(existingCategory?.let { EntryType.fromRaw(it.type) } ?: EntryType.EXPENSE)
    }
    var budgetText by remember(existingCategory) {
        mutableStateOf(
            existingCategory?.budgetLimit?.takeIf { it > 0L }?.toString().orEmpty()
        )
    }
    var selectedColorHex by remember(existingCategory) {
        mutableStateOf(existingCategory?.colorHex ?: CATEGORY_COLOR_SWATCHES.first())
    }
    var selectedIconKey by remember(existingCategory) {
        mutableStateOf(existingCategory?.iconKey ?: "festival")
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existingCategory != null) "Edit Category" else "Add Category")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = {
                        nameText = it.take(60)
                        errorMsg = null
                    },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Dhak, Bhog Prasad, Immersion") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EntryType.entries.forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = { Text(t.displayLabel) }
                        )
                    }
                }

                if (selectedType == EntryType.EXPENSE) {
                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = { budgetText = it.filter { ch -> ch.isDigit() }.take(8) },
                        label = { Text("Planned Budget Cap ₹ (Optional)") },
                        placeholder = { Text("e.g. 5000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_budget_input")
                    )
                }

                Text("Icon", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CATEGORY_ICON_OPTIONS.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedIconKey == key,
                            onClick = { selectedIconKey = key },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(
                                    imageVector = categoryIconForKey(key),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

                Text("Badge Color", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CATEGORY_COLOR_SWATCHES.forEach { hex ->
                        val c = parseCategoryColor(hex)
                        val chosen = hex.equals(selectedColorHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (chosen) 2.5.dp else 0.dp,
                                    color = if (chosen) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (chosen) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected color",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                if (existingCategory != null && onDelete != null) {
                    TextButton(
                        onClick = { onDelete(existingCategory.id) },
                        modifier = Modifier.testTag("delete_category_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Category", color = MaterialTheme.colorScheme.error)
                    }
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameText.isBlank()) {
                        errorMsg = "Category name is required."
                        return@Button
                    }
                    onSave(
                        nameText.trim(),
                        selectedType,
                        budgetText.toLongOrNull() ?: 0L,
                        selectedColorHex,
                        selectedIconKey
                    )
                },
                modifier = Modifier.testTag("save_category_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NewFunctionAccountDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, year: Long, defaultContribution: Long, targetBudget: Long) -> Unit
) {
    var nameText by remember { mutableStateOf("Saraswati Pujo 2027") }
    var yearText by remember { mutableStateOf("2027") }
    var contributionText by remember { mutableStateOf("2000") }
    var budgetText by remember { mutableStateOf("35000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Function Ledger") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it.take(80) },
                    label = { Text("Function Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = yearText,
                    onValueChange = { yearText = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("Year") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contributionText,
                    onValueChange = { contributionText = it.filter { c -> c.isDigit() }.take(7) },
                    label = { Text("Standard Flat Contribution (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it.filter { c -> c.isDigit() }.take(8) },
                    label = { Text("Estimated Total Budget (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameText.isNotBlank()) {
                        onCreate(
                            nameText.trim(),
                            yearText.toLongOrNull() ?: 2026L,
                            contributionText.toLongOrNull() ?: 2000L,
                            budgetText.toLongOrNull() ?: 35000L
                        )
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CollectContributionDialog(
    residentName: String,
    suggestedAmount: Long,
    onDismiss: () -> Unit,
    onConfirm: (residentName: String, amount: Long) -> Unit
) {
    var nameInput by remember(residentName) { mutableStateOf(residentName) }
    var amountInput by remember(suggestedAmount) { mutableStateOf(suggestedAmount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Resident Chanda") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it.take(80) },
                    label = { Text("Resident / Flat Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { ch -> ch.isDigit() }.take(7) },
                    label = { Text("Contribution Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1800L, 2000L, 2400L, 5000L).forEach { preset ->
                        AssistChip(
                            onClick = { amountInput = preset.toString() },
                            label = { Text("₹$preset") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountInput.toLongOrNull() ?: 0L
                    if (nameInput.isNotBlank() && amt > 0L) {
                        onConfirm(nameInput.trim(), amt)
                    }
                },
                modifier = Modifier.testTag("confirm_collect_chanda_button")
            ) {
                Text("Record Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
