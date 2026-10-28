package com.example.data.seed

import com.example.data.model.EntryType
import com.example.data.model.LedgerAccount
import com.example.data.model.LedgerCategory
import com.example.data.model.LedgerEntry
import com.example.data.model.PaymentMode
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object KaliPujoSeedData {

    const val ACCOUNT_2026_ID = "kali_pujo_2026"
    const val ACCOUNT_2025_ID = "kali_pujo_2025"

    const val CAT_DHAK = "cat_dhak"
    const val CAT_THAKUR = "cat_thakur"
    const val CAT_THAKURMASAI = "cat_thakurmasai"
    const val CAT_CONTRIBUTION = "cat_resident_contribution"
    const val CAT_PANDAL = "cat_pandal_decor"
    const val CAT_LIGHTING = "cat_lighting_sound"
    const val CAT_SAMAGRI = "cat_puja_samagri"
    const val CAT_GROCERIES = "cat_groceries_bhog"
    const val CAT_FRUITS_SWEETS = "cat_fruits_flowers_sweets"
    const val CAT_CLEANING = "cat_cleaning_helpers"
    const val CAT_BISARJAN = "cat_bisarjan_transport"

    data class HistoricalResident(
        val name: String,
        val contribution2025: Long
    )

    val apartmentResidents2025: List<HistoricalResident> = listOf(
        HistoricalResident("Anirban", 1800L),
        HistoricalResident("Sanjay Das", 1800L),
        HistoricalResident("Avik chowdhury", 1800L),
        HistoricalResident("Subhasish kaku", 1800L),
        HistoricalResident("Reshmi di", 1800L),
        HistoricalResident("Kumar Da", 1800L),
        HistoricalResident("Piyush kaku", 1800L),
        HistoricalResident("Samiran da", 1800L),
        HistoricalResident("Soumen da", 1800L),
        HistoricalResident("Ratan da", 5000L),
        HistoricalResident("Sanjib Saha", 1800L),
        HistoricalResident("Sunirmal guriya", 1800L),
        HistoricalResident("Rahul Ground floor", 1800L),
        HistoricalResident("Rahul dey", 1800L),
        HistoricalResident("Avik Ganguly", 1800L)
    )

    data class SmartRecommendation(
        val title: String,
        val subtitle: String,
        val actionHint: String
    )

    val accountManagerRecommendations: List<SmartRecommendation> = listOf(
        SmartRecommendation(
            title = "Adjust 2026 Target for ₹9,000 New Core Costs",
            subtitle = "Last year spent ₹30,419 (ending at ₹-219) without separate Dhak, Thakur, and Thakurmasai lines. With Dhak (₹1,000), Thakur (₹5,000), and Thakurmasai (₹3,000) added this year, projected total spend is ~₹39,500.",
            actionHint = "Recommend ₹2,400 per flat across 15 flats (+ Ratan da's ₹5,000) or onboarding 5 more flats at ₹2,000 to keep a healthy ₹1,500 surplus."
        ),
        SmartRecommendation(
            title = "Separate 'Fund Pay' vs. 'Member Paid' Advances",
            subtitle = "In 2025, 16 out of 25 expenses were paid out-of-pocket by members (Anirban, Rahul, Ranjan, Mousumi, etc.) and noted in free text.",
            actionHint = "Tag out-of-pocket bills as 'Paid by Member' and tap 'Mark Reimbursed' once settled so cash-in-hand never diverges from net balance."
        ),
        SmartRecommendation(
            title = "Categorize Every Entry from Day 1",
            subtitle = "All 40 entries in the 2025 PDF had '-' under Category, making it hard to see that Pandal (₹10,585) and Lighting/Sound (₹5,600) were 53% of total spend.",
            actionHint = "Use the updated 11-category list (including Dhak, Thakur, Thakurmasai) for 1-tap category tagging and instant PDF reports."
        )
    )

    fun parseDateMillis(dateStr: String, sequenceIndex: Int = 0): Long {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val base = sdf.parse(dateStr)?.time ?: 1793059200000L
            base + (sequenceIndex * 1000L)
        } catch (_: Exception) {
            1793059200000L + (sequenceIndex * 1000L)
        }
    }

    fun buildDefaultAccounts(userId: String): List<LedgerAccount> = listOf(
        LedgerAccount(
            id = ACCOUNT_2026_ID,
            userId = userId,
            name = "Kali Pujo 2026",
            year = 2026L,
            defaultContribution = 2400L,
            targetBudget = 40000L,
            archived = false
        ),
        LedgerAccount(
            id = ACCOUNT_2025_ID,
            userId = userId,
            name = "Kali pujo 2025",
            year = 2025L,
            defaultContribution = 1800L,
            targetBudget = 30200L,
            archived = true
        )
    )

    fun buildDefaultCategories(userId: String): List<LedgerCategory> = listOf(
        LedgerCategory(
            id = CAT_DHAK,
            userId = userId,
            name = "Dhak",
            type = EntryType.EXPENSE.raw,
            budgetLimit = 1500L,
            colorHex = "#C62828",
            iconKey = "music",
            sortOrder = 1L
        ),
        LedgerCategory(
            id = CAT_THAKUR,
            userId = userId,
            name = "Thakur",
            type = EntryType.EXPENSE.raw,
            budgetLimit = 5000L,
            colorHex = "#AD1457",
            iconKey = "temple",
            sortOrder = 2L
        ),
        LedgerCategory(
            id = CAT_THAKURMASAI,
            userId = userId,
            name = "Thakurmasai",
            type = EntryType.EXPENSE.raw,
            budgetLimit = 3500L,
            colorHex = "#E65100",
            iconKey = "person",
            sortOrder = 3L
        ),
        LedgerCategory(
            id = CAT_PANDAL,
            userId = userId,
            name = "Pandal & Decor",
            type = EntryType.EXPENSE.raw,
            budgetLimit = 11000L,
            colorHex = "#6A1B9A",
            iconKey = "festival",
            sortOrder = 4L
        ),
        LedgerCategory(
            id = CAT_LIGHTING,
            userId = userId,
            name = "Lighting & Sound",
            type = EntryType.EXPENSE.raw,
            budgetLimit = 6000L,
            colorHex = "#F9A825",
            iconKey = "lightbulb",
            sortOrder = 5L
        ),
        LedgerCategory(
            id = CAT_SAMAGRI,
            userId = userId,
            name = "Puja Samagri",
            type = EntryType.EXPENSE.raw,
            budgetLimit = 3000L,
            colorHex = "#D84315",
            iconKey = "spa",
            sortOrder = 6L
        ),
        LedgerCategory(
            id = CAT_GROCERIES,
            userId = userId,
            name = "Groceries & Bhog",
            type = EntryType.EXPENSE.raw,
            budgetLimit = 5500L,
            colorHex = "#2E7D32",
            iconKey = "shopping",
            sortOrder = 7L
        ),
        LedgerCategory(
            id = CAT_FRUITS_SWEETS,
            userId = userId,
            name = "Fruits, Flowers & Sweets",
            type = EntryType.EXPENSE.raw,
            budgetLimit = 5500L,
            colorHex = "#00838F",
            iconKey = "flower",
            sortOrder = 8L
        ),
        LedgerCategory(
            id = CAT_CLEANING,
            userId = userId,
            name = "Cleaning & Helpers",
            type = EntryType.EXPENSE.raw,
            budgetLimit = 1000L,
            colorHex = "#455A64",
            iconKey = "cleaning",
            sortOrder = 9L
        ),
        LedgerCategory(
            id = CAT_BISARJAN,
            userId = userId,
            name = "Bisarjan & Transport",
            type = EntryType.EXPENSE.raw,
            budgetLimit = 600L,
            colorHex = "#1565C0",
            iconKey = "shipping",
            sortOrder = 10L
        ),
        LedgerCategory(
            id = CAT_CONTRIBUTION,
            userId = userId,
            name = "Resident Contribution",
            type = EntryType.INCOME.raw,
            budgetLimit = 0L,
            colorHex = "#1B5E20",
            iconKey = "people",
            sortOrder = 11L
        )
    )

    fun buildInitial2026Entries(userId: String): List<LedgerEntry> = listOf(
        LedgerEntry(
            id = "entry_2026_01_dhak",
            userId = userId,
            accountId = ACCOUNT_2026_ID,
            type = EntryType.EXPENSE.raw,
            amount = 1000L,
            categoryId = CAT_DHAK,
            categoryName = "Dhak",
            notes = "Dhak",
            partyName = "",
            paymentMode = PaymentMode.FUND.raw,
            dateStr = "27/10/2026",
            dateEpochMillis = parseDateMillis("27/10/2026", 1)
        ),
        LedgerEntry(
            id = "entry_2026_02_thakur",
            userId = userId,
            accountId = ACCOUNT_2026_ID,
            type = EntryType.EXPENSE.raw,
            amount = 5000L,
            categoryId = CAT_THAKUR,
            categoryName = "Thakur",
            notes = "Thakur",
            partyName = "",
            paymentMode = PaymentMode.FUND.raw,
            dateStr = "27/10/2026",
            dateEpochMillis = parseDateMillis("27/10/2026", 2)
        ),
        LedgerEntry(
            id = "entry_2026_03_thakurmasai",
            userId = userId,
            accountId = ACCOUNT_2026_ID,
            type = EntryType.EXPENSE.raw,
            amount = 3000L,
            categoryId = CAT_THAKURMASAI,
            categoryName = "Thakurmasai",
            notes = "Thakurmasai",
            partyName = "",
            paymentMode = PaymentMode.FUND.raw,
            dateStr = "27/10/2026",
            dateEpochMillis = parseDateMillis("27/10/2026", 3)
        )
    )

    private data class Raw2025Row(
        val dateStr: String,
        val notes: String,
        val isIncome: Boolean,
        val amount: Long,
        val categoryId: String,
        val categoryName: String,
        val partyName: String,
        val paymentMode: PaymentMode
    )

    fun buildHistorical2025Entries(userId: String): List<LedgerEntry> {
        val rows = listOf(
            Raw2025Row("11/10/2025", "Anirban", true, 1800L, CAT_CONTRIBUTION, "Resident Contribution", "Anirban", PaymentMode.RESIDENT_PAID),
            Raw2025Row("14/10/2025", "Sanjay Das", true, 1800L, CAT_CONTRIBUTION, "Resident Contribution", "Sanjay Das", PaymentMode.RESIDENT_PAID),
            Raw2025Row("14/10/2025", "Avik chowdhury", true, 1800L, CAT_CONTRIBUTION, "Resident Contribution", "Avik chowdhury", PaymentMode.RESIDENT_PAID),
            Raw2025Row("14/10/2025", "Subhasish kaku", true, 1800L, CAT_CONTRIBUTION, "Resident Contribution", "Subhasish kaku", PaymentMode.RESIDENT_PAID),
            Raw2025Row("15/10/2025", "Reshmi di", true, 1800L, CAT_CONTRIBUTION, "Resident Contribution", "Reshmi di", PaymentMode.RESIDENT_PAID),
            Raw2025Row("15/10/2025", "Bokhish pandle,payee -Anirban", false, 100L, CAT_PANDAL, "Pandal & Decor", "Anirban", PaymentMode.REIMBURSED),
            Raw2025Row("15/10/2025", "Kumar Da", true, 1800L, CAT_CONTRIBUTION, "Resident Contribution", "Kumar Da", PaymentMode.RESIDENT_PAID),
            Raw2025Row("15/10/2025", "Piyush kaku", true, 1800L, CAT_CONTRIBUTION, "Resident Contribution", "Piyush kaku", PaymentMode.RESIDENT_PAID),
            Raw2025Row("15/10/2025", "Samiran da", true, 1800L, CAT_CONTRIBUTION, "Resident Contribution", "Samiran da", PaymentMode.RESIDENT_PAID),
            Raw2025Row("15/10/2025", "Soumen da", true, 1800L, CAT_CONTRIBUTION, "Resident Contribution", "Soumen da", PaymentMode.RESIDENT_PAID),
            Raw2025Row("15/10/2025", "Ratan da", true, 5000L, CAT_CONTRIBUTION, "Resident Contribution", "Ratan da", PaymentMode.RESIDENT_PAID),
            Raw2025Row("16/10/2025", "Sanjib Saha", true, 1800L, CAT_CONTRIBUTION, "Resident Contribution", "Sanjib Saha", PaymentMode.RESIDENT_PAID),
            Raw2025Row("16/10/2025", "White distemper,payee-moumita", false, 140L, CAT_PANDAL, "Pandal & Decor", "Moumita", PaymentMode.REIMBURSED),
            Raw2025Row("16/10/2025", "White distemper +color, payee-Mousumi", false, 345L, CAT_PANDAL, "Pandal & Decor", "Mousumi", PaymentMode.REIMBURSED),
            Raw2025Row("17/10/2025", "Sunirmal guriya", true, 1800L, CAT_CONTRIBUTION, "Resident Contribution", "Sunirmal guriya", PaymentMode.RESIDENT_PAID),
            Raw2025Row("18/10/2025", "Tuni installation charge, payee:Roni roy", false, 1800L, CAT_LIGHTING, "Lighting & Sound", "Roni roy", PaymentMode.REIMBURSED),
            Raw2025Row("19/10/2025", "Rahul Ground floor", true, 1800L, CAT_CONTRIBUTION, "Resident Contribution", "Rahul Ground floor", PaymentMode.RESIDENT_PAID),
            Raw2025Row("19/10/2025", "Groceries part 2 ,payee-Ranjan", false, 3529L, CAT_GROCERIES, "Groceries & Bhog", "Ranjan", PaymentMode.REIMBURSED),
            Raw2025Row("19/10/2025", "Dhuti 2+Sari 1, fund pay", false, 600L, CAT_SAMAGRI, "Puja Samagri", "", PaymentMode.FUND),
            Raw2025Row("19/10/2025", "Thakur mosai punjabi,fund pay", false, 300L, CAT_THAKURMASAI, "Thakurmasai", "", PaymentMode.FUND),
            Raw2025Row("19/10/2025", "Five piece Gamcha,fund pay", false, 110L, CAT_SAMAGRI, "Puja Samagri", "", PaymentMode.FUND),
            Raw2025Row("20/10/2025", "Groceries part 1.Payee-Soumen da", false, 451L, CAT_GROCERIES, "Groceries & Bhog", "Soumen da", PaymentMode.REIMBURSED),
            Raw2025Row("20/10/2025", "Rahul dey", true, 1800L, CAT_CONTRIBUTION, "Resident Contribution", "Rahul dey", PaymentMode.RESIDENT_PAID),
            Raw2025Row("20/10/2025", "Scotchbite pitambori,cash pay", false, 156L, CAT_CLEANING, "Cleaning & Helpers", "", PaymentMode.FUND),
            Raw2025Row("20/10/2025", "Kajer didi bason dhoa,fund pay", false, 250L, CAT_CLEANING, "Cleaning & Helpers", "", PaymentMode.FUND),
            Raw2025Row("20/10/2025", "Avik Ganguly", true, 1800L, CAT_CONTRIBUTION, "Resident Contribution", "Avik Ganguly", PaymentMode.RESIDENT_PAID),
            Raw2025Row("20/10/2025", "Fol bazar pujo,payee-Ranjan", false, 1745L, CAT_FRUITS_SWEETS, "Fruits, Flowers & Sweets", "Ranjan", PaymentMode.REIMBURSED),
            Raw2025Row("20/10/2025", "Gongajol gonga mati ,payee-Samiran da", false, 290L, CAT_SAMAGRI, "Puja Samagri", "Samiran da", PaymentMode.REIMBURSED),
            Raw2025Row("20/10/2025", "Thakurer ghee,payee-Mousumi", false, 148L, CAT_SAMAGRI, "Puja Samagri", "Mousumi", PaymentMode.REIMBURSED),
            Raw2025Row("20/10/2025", "Dosokorma ,Payee-Rahul", false, 1120L, CAT_SAMAGRI, "Puja Samagri", "Rahul", PaymentMode.REIMBURSED),
            Raw2025Row("20/10/2025", "Pandel cost,payee-Azar Mondal", false, 10000L, CAT_PANDAL, "Pandal & Decor", "Azar Mondal", PaymentMode.REIMBURSED),
            Raw2025Row("21/10/2025", "Pujo basan clean,payee-puja", false, 400L, CAT_CLEANING, "Cleaning & Helpers", "Puja", PaymentMode.REIMBURSED),
            Raw2025Row("21/10/2025", "Ful ,payee-Rahul", false, 1490L, CAT_FRUITS_SWEETS, "Fruits, Flowers & Sweets", "Rahul", PaymentMode.REIMBURSED),
            Raw2025Row("22/10/2025", "Doshokorma additional,payee-rahul", false, 190L, CAT_SAMAGRI, "Puja Samagri", "Rahul", PaymentMode.REIMBURSED),
            Raw2025Row("22/10/2025", "Misti sil nora misc ,Payee-Anirban", false, 1896L, CAT_FRUITS_SWEETS, "Fruits, Flowers & Sweets", "Anirban", PaymentMode.REIMBURSED),
            Raw2025Row("23/10/2025", "Sabji ,Payee-Sanjay da", false, 860L, CAT_GROCERIES, "Groceries & Bhog", "Sanjay da", PaymentMode.REIMBURSED),
            Raw2025Row("23/10/2025", "Dodhikorma itmes", false, 284L, CAT_GROCERIES, "Groceries & Bhog", "", PaymentMode.FUND),
            Raw2025Row("23/10/2025", "Bisa da box electric,pay from fund", false, 3800L, CAT_LIGHTING, "Lighting & Sound", "", PaymentMode.FUND),
            Raw2025Row("23/10/2025", "Bisorjon misti,payee-Anirban", false, 315L, CAT_BISARJAN, "Bisarjan & Transport", "Anirban", PaymentMode.REIMBURSED),
            Raw2025Row("25/10/2025", "Bisorjon Van Vara,pay from fund", false, 100L, CAT_BISARJAN, "Bisarjan & Transport", "", PaymentMode.FUND)
        )

        return rows.mapIndexed { idx, row ->
            val seq = idx + 1
            LedgerEntry(
                id = "entry_2025_%02d".format(Locale.US, seq),
                userId = userId,
                accountId = ACCOUNT_2025_ID,
                type = if (row.isIncome) EntryType.INCOME.raw else EntryType.EXPENSE.raw,
                amount = row.amount,
                categoryId = row.categoryId,
                categoryName = row.categoryName,
                notes = row.notes,
                partyName = row.partyName,
                paymentMode = row.paymentMode.raw,
                dateStr = row.dateStr,
                dateEpochMillis = parseDateMillis(row.dateStr, seq)
            )
        }
    }
}
