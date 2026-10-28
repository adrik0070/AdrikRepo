package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.AccountSummaryMetrics
import com.example.data.model. CategoryBreakdownItem
import com.example.data.model.EntryType
import com.example.data.model.EntryWithBalance
import com.example.data.model.LedgerAccount
import com.example.data.model.PaymentMode
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LedgerReportExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 32f

    fun exportAndSharePdf(
        context: Context,
        account: LedgerAccount,
        summary: AccountSummaryMetrics,
        chronologicalRows: List<EntryWithBalance>,
        onError: (String) -> Unit
    ) {
        try {
            val pdfDocument = PdfDocument()
            val timestampText = SimpleDateFormat("dd MMMM yyyy hh:mm a", Locale.US).format(Date())

            val headerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FDF2F0")
                style = Paint.Style.FILL
            }
            val brandBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#9E1B1B")
                style = Paint.Style.FILL
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#EEF2FF")
                style = Paint.Style.FILL
            }
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E1B18")
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#57534E")
                textSize = 9.5f
            }
            val boldCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E1B18")
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E1B18")
                textSize = 9f
            }
            val incomePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#15803D")
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val expensePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#B91C1C")
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            // Column X coordinates (total width = 531f from 32f to 563f)
            val colDateX = MARGIN
            val colNotesX = MARGIN + 66f
            val colCatX = MARGIN + 246f
            val colIncomeX = MARGIN + 348f
            val colExpenseX = MARGIN + 414f
            val colBalanceX = MARGIN + 480f
            val tableRightX = PAGE_WIDTH - MARGIN

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            var currentY = drawReportHeader(
                canvas = canvas,
                account = account,
                summary = summary,
                totalEntries = chronologicalRows.size,
                timestampText = timestampText,
                isFirstPage = true,
                headerFillPaint = headerFillPaint,
                brandBadgePaint = brandBadgePaint,
                borderPaint = borderPaint,
                titlePaint = titlePaint,
                subTextPaint = subTextPaint,
                incomePaint = incomePaint,
                expensePaint = expensePaint
            )

            fun drawTableHeader(c: Canvas, startY: Float): Float {
                val rowH = 24f
                c.drawRect(colDateX, startY, tableRightX, startY + rowH, tableHeaderPaint)
                c.drawRect(colDateX, startY, tableRightX, startY + rowH, borderPaint)
                listOf(colNotesX, colCatX, colIncomeX, colExpenseX, colBalanceX).forEach { x ->
                    c.drawLine(x, startY, x, startY + rowH, borderPaint)
                }
                val textY = startY + 15.5f
                c.drawText("Date", colDateX + 5f, textY, boldCellPaint)
                c.drawText("Notes & Payee", colNotesX + 5f, textY, boldCellPaint)
                c.drawText("Category", colCatX + 5f, textY, boldCellPaint)
                c.drawText("Income", colIncomeX + 5f, textY, boldCellPaint)
                c.drawText("Expense", colExpenseX + 5f, textY, boldCellPaint)
                c.drawText("Balance", colBalanceX + 5f, textY, boldCellPaint)
                return startY + rowH
            }

            currentY = drawTableHeader(canvas, currentY)

            chronologicalRows.forEach { item ->
                val rowHeight = 24f
                if (currentY + rowHeight + 48f > PAGE_HEIGHT - MARGIN) {
                    drawFooter(canvas, pageNumber, timestampText, subTextPaint)
                    pdfDocument.finishPage(page)
                    pageNumber += 1
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = MARGIN + 12f
                    currentY = drawTableHeader(canvas, currentY)
                }

                val entry = item.entry
                canvas.drawRect(colDateX, currentY, tableRightX, currentY + rowHeight, borderPaint)
                listOf(colNotesX, colCatX, colIncomeX, colExpenseX, colBalanceX).forEach { x ->
                    canvas.drawLine(x, currentY, x, currentY + rowHeight, borderPaint)
                }

                val textY = currentY + 15f
                canvas.drawText(entry.dateStr.take(10), colDateX + 4f, textY, cellPaint)

                val noteLine = buildString {
                    append(entry.notes)
                    if (entry.partyName.isNotBlank() && !entry.notes.contains(entry.partyName, ignoreCase = true)) {
                        append(" (")
                        append(entry.partyName)
                        append(")")
                    }
                    if (entry.paymentMode == PaymentMode.MEMBER_ADVANCE.raw) {
                        append(" [Due]")
                    }
                }.take(34)
                canvas.drawText(noteLine, colNotesX + 4f, textY, cellPaint)
                canvas.drawText(entry.categoryName.take(18), colCatX + 4f, textY, cellPaint)

                if (entry.type == EntryType.INCOME.raw) {
                    val incStr = CurrencyFormatter.formatInr(entry.amount, showCurrencySymbol = false)
                    canvas.drawText(incStr, colIncomeX + 5f, textY, incomePaint)
                } else {
                    val expStr = CurrencyFormatter.formatInr(entry.amount, showCurrencySymbol = false)
                    canvas.drawText(expStr, colExpenseX + 5f, textY, expensePaint)
                }

                val balStr = CurrencyFormatter.formatInr(item.runningBalance, showCurrencySymbol = false)
                canvas.drawText(balStr, colBalanceX + 5f, textY, boldCellPaint)

                currentY += rowHeight
            }

            // Totals Row
            val totalRowH = 26f
            canvas.drawRect(colDateX, currentY, tableRightX, currentY + totalRowH, tableHeaderPaint)
            canvas.drawRect(colDateX, currentY, tableRightX, currentY + totalRowH, borderPaint)
            listOf(colNotesX, colCatX, colIncomeX, colExpenseX, colBalanceX).forEach { x ->
                canvas.drawLine(x, currentY, x, currentY + totalRowH, borderPaint)
            }
            val totalTextY = currentY + 16.5f
            canvas.drawText("Total", colDateX + 5f, totalTextY, boldCellPaint)
            canvas.drawText(
                CurrencyFormatter.formatInr(summary.totalIncome),
                colIncomeX + 4f,
                totalTextY,
                incomePaint
            )
            canvas.drawText(
                CurrencyFormatter.formatInr(summary.totalExpense),
                colExpenseX + 4f,
                totalTextY,
                expensePaint
            )
            canvas.drawText(
                CurrencyFormatter.formatInr(summary.netBalance),
                colBalanceX + 4f,
                totalTextY,
                boldCellPaint
            )

            drawFooter(canvas, pageNumber, timestampText, subTextPaint)
            pdfDocument.finishPage(page)

            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val safeName = account.name.replace(Regex("[^a-zA-Z0-9]+"), "_")
            val pdfFile = File(reportsDir, "Utsav_Ledger_${safeName}_Report.pdf")
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "${account.name} — Account Manager Report")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "Share ${account.name} PDF Report")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Unable to export PDF report")
        }
    }

    private fun drawReportHeader(
        canvas: Canvas,
        account: LedgerAccount,
        summary: AccountSummaryMetrics,
        totalEntries: Int,
        timestampText: String,
        isFirstPage: Boolean,
        headerFillPaint: Paint,
        brandBadgePaint: Paint,
        borderPaint: Paint,
        titlePaint: Paint,
        subTextPaint: Paint,
        incomePaint: Paint,
        expensePaint: Paint
    ): Float {
        if (!isFirstPage) return MARGIN
        val bannerRect = RectF(MARGIN, MARGIN, PAGE_WIDTH - MARGIN, MARGIN + 48f)
        canvas.drawRoundRect(bannerRect, 8f, 8f, headerFillPaint)
        val badgeRect = RectF(MARGIN + 10f, MARGIN + 10f, MARGIN + 38f, MARGIN + 38f)
        canvas.drawRoundRect(badgeRect, 6f, 6f, brandBadgePaint)

        val headerTitlePaint = Paint(titlePaint).apply { textSize = 14f }
        canvas.drawText("Utsav Ledger • Account Manager Report", MARGIN + 48f, MARGIN + 24f, headerTitlePaint)
        canvas.drawText("Generated $timestampText", MARGIN + 48f, MARGIN + 38f, subTextPaint)

        var y = MARGIN + 68f
        canvas.drawText("Account Name", MARGIN, y, subTextPaint)
        y += 20f
        canvas.drawText(account.name, MARGIN, y, titlePaint)
        y += 14f

        val boxTop = y
        val boxHeight = 46f
        val boxWidth = (PAGE_WIDTH - 2 * MARGIN) / 3f
        canvas.drawRect(MARGIN, boxTop, PAGE_WIDTH - MARGIN, boxTop + boxHeight, borderPaint)
        canvas.drawLine(MARGIN + boxWidth, boxTop, MARGIN + boxWidth, boxTop + boxHeight, borderPaint)
        canvas.drawLine(MARGIN + 2 * boxWidth, boxTop, MARGIN + 2 * boxWidth, boxTop + boxHeight, borderPaint)

        val bigIncomePaint = Paint(incomePaint).apply { textSize = 14f }
        val bigExpensePaint = Paint(expensePaint).apply { textSize = 14f }
        val bigBalancePaint = Paint(titlePaint).apply { textSize = 14f }

        canvas.drawText("Total Income:", MARGIN + 8f, boxTop + 16f, subTextPaint)
        canvas.drawText(CurrencyFormatter.formatInr(summary.totalIncome), MARGIN + 8f, boxTop + 36f, bigIncomePaint)

        canvas.drawText("Total Expense:", MARGIN + boxWidth + 8f, boxTop + 16f, subTextPaint)
        canvas.drawText(CurrencyFormatter.formatInr(summary.totalExpense), MARGIN + boxWidth + 8f, boxTop + 36f, bigExpensePaint)

        canvas.drawText("Total Balance:", MARGIN + 2 * boxWidth + 8f, boxTop + 16f, subTextPaint)
        canvas.drawText(CurrencyFormatter.formatInr(summary.netBalance), MARGIN + 2 * boxWidth + 8f, boxTop + 36f, bigBalancePaint)

        y = boxTop + boxHeight + 16f
        val metaLine = buildString {
            append("Total number of entries: $totalEntries")
            if (summary.pendingReimbursements > 0L) {
                append("   |   Pending Member Reimbursements: ${CurrencyFormatter.formatInr(summary.pendingReimbursements)}")
            }
        }
        canvas.drawText(metaLine, MARGIN, y, subTextPaint)
        return y + 10f
    }

    private fun drawFooter(
        canvas: Canvas,
        pageNumber: Int,
        timestampText: String,
        subTextPaint: Paint
    ) {
        canvas.drawText(
            "Utsav Ledger Statement • $timestampText • Page $pageNumber",
            MARGIN,
            PAGE_HEIGHT - 14f,
            subTextPaint
        )
    }

    fun shareGroupSummaryText(
        context: Context,
        account: LedgerAccount,
        summary: AccountSummaryMetrics,
        categoryBreakdown: List<CategoryBreakdownItem>
    ) {
        val text = buildString {
            appendLine("*${account.name} — Account Update*")
            appendLine("────────────────────")
            appendLine("Total Collection: ${CurrencyFormatter.formatInr(summary.totalIncome)}")
            appendLine("Total Expenses: ${CurrencyFormatter.formatInr(summary.totalExpense)}")
            appendLine("*Net Balance: ${CurrencyFormatter.formatInr(summary.netBalance)}*")
            if (summary.pendingReimbursements > 0L) {
                appendLine("Due for Member Reimbursement: ${CurrencyFormatter.formatInr(summary.pendingReimbursements)}")
                appendLine("Available Fund Cash: ${CurrencyFormatter.formatInr(summary.availableCashInHand)}")
            }
            val activeCategories = categoryBreakdown.filter { it.currentAmount > 0L }
            if (activeCategories.isNotEmpty()) {
                appendLine()
                appendLine("*Category-wise Expenses:*")
                activeCategories.forEach { cat ->
                    appendLine("• ${cat.category.name}: ${CurrencyFormatter.formatInr(cat.currentAmount)}")
                }
            }
            appendLine()
            append("Updated via Utsav Ledger")
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${account.name} Account Update")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(
            Intent.createChooser(sendIntent, "Share Account Summary")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
