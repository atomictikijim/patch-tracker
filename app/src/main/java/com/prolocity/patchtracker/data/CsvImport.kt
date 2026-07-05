package com.prolocity.patchtracker.data

// Bulk CSV import for Players and Teams. This file holds the DB-free pieces: a tolerant CSV
// parser, a header-name column mapper, and the result model. The actual validation + inserts
// live in PatchRepository.importPlayersCsv / importTeamsCsv, which need DAO access.

// Outcome of an import, surfaced to the user in a summary dialog. `added` counts rows that were
// inserted; `skipped` holds one human-readable reason per row that was rejected outright; `warnings`
// holds non-fatal notes (e.g. a team was created but one of its listed players couldn't be added).
data class ImportSummary(
    val added: Int,
    val skipped: List<String>,
    val warnings: List<String> = emptyList()
)

// A minimal RFC-4180-style parser: comma-separated, double-quoted fields (with "" as an escaped
// quote inside a quoted field), tolerating both CRLF and LF line endings and quoted newlines.
// Returns rows of raw string cells; blank lines are dropped. A leading UTF-8 BOM is stripped.
fun parseCsv(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false
    var i = 0
    val s = text.removePrefix("﻿")

    fun endField() {
        row.add(field.toString())
        field.setLength(0)
    }
    fun endRow() {
        endField()
        // Drop rows that are entirely empty (e.g. a trailing newline).
        if (row.any { it.isNotBlank() }) rows.add(row)
        row = mutableListOf()
    }

    while (i < s.length) {
        val c = s[i]
        when {
            inQuotes -> when {
                c == '"' && i + 1 < s.length && s[i + 1] == '"' -> { field.append('"'); i++ }
                c == '"' -> inQuotes = false
                else -> field.append(c)
            }
            c == '"' -> inQuotes = true
            c == ',' -> endField()
            c == '\r' -> { /* swallow; the \n (or end) closes the row */ }
            c == '\n' -> endRow()
            else -> field.append(c)
        }
        i++
    }
    // Flush the final field/row if the file didn't end with a newline.
    if (field.isNotEmpty() || row.isNotEmpty()) endRow()
    return rows
}

// Maps a header row to column indices by normalized name (lowercased, spaces/underscores removed),
// so column order and minor header spelling differences don't matter. Each logical column accepts a
// set of aliases; the first matching header wins. Returns index-or-null per requested key.
class CsvHeader(headerRow: List<String>) {
    private val indexByName: Map<String, Int> =
        headerRow.mapIndexed { i, h -> normalize(h) to i }
            .filter { it.first.isNotEmpty() }
            .toMap()

    fun index(vararg aliases: String): Int? =
        aliases.asSequence().map { normalize(it) }.mapNotNull { indexByName[it] }.firstOrNull()

    companion object {
        fun normalize(s: String): String =
            s.trim().lowercase().replace(" ", "").replace("_", "")
    }
}

// Safe cell accessor: trims, returns "" for out-of-range/absent columns.
fun List<String>.cell(index: Int?): String =
    if (index != null && index in indices) this[index].trim() else ""
