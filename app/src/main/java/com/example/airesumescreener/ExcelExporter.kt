package com.example.airesumescreener

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook

object ExcelExporter {
    suspend fun export(context: Context, uri: Uri, candidates: List<Candidate>, jobTitle: String) =
        withContext(Dispatchers.IO) {
            XSSFWorkbook().use { wb ->
                val sheet = wb.createSheet("Candidates")
                val headers = listOf(
                    "Rank", "Name", "Score (%)", "Email", "Phone", "Address",
                    "Matched Hard Skills", "Missing Hard Skills",
                    "Matched Soft Skills", "Missing Soft Skills",
                    "Experience", "Education", "AI Summary", "Formatting Issues"
                )
                val style = wb.createCellStyle().apply {
                    setFont(wb.createFont().apply { bold = true })
                    fillForegroundColor = IndexedColors.LIGHT_CORNFLOWER_BLUE.index
                    fillPattern = FillPatternType.SOLID_FOREGROUND
                }
                val row0 = sheet.createRow(0)
                headers.forEachIndexed { i, h ->
                    row0.createCell(i).apply { cellStyle = style; setCellValue(h) }
                }
                candidates.sortedByDescending { it.score }.forEachIndexed { i, c ->
                    val r = sheet.createRow(i + 1)
                    r.createCell(0).setCellValue((i + 1).toDouble())
                    r.createCell(1).setCellValue(c.name)
                    r.createCell(2).setCellValue(c.score.toDouble())
                    r.createCell(3).setCellValue(c.email)
                    r.createCell(4).setCellValue(c.phone)
                    r.createCell(5).setCellValue(c.address)
                    r.createCell(6).setCellValue(c.hardSkillsMatched.joinToString(", "))
                    r.createCell(7).setCellValue(c.hardSkillsMissing.joinToString(", "))
                    r.createCell(8).setCellValue(c.softSkillsMatched.joinToString(", "))
                    r.createCell(9).setCellValue(c.softSkillsMissing.joinToString(", "))
                    r.createCell(10).setCellValue(c.experience)
                    r.createCell(11).setCellValue(c.education)
                    r.createCell(12).setCellValue(c.summary)
                    r.createCell(13).setCellValue(c.formattingIssues.joinToString(", "))
                }
                headers.indices.forEach { sheet.setColumnWidth(it, 6000) }
                context.contentResolver.openOutputStream(uri)?.use { wb.write(it) }
            }
        }
}