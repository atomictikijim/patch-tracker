package com.prolocity.patchtracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Unit tests for the DB-free CSV parsing pieces used by the bulk import. The repository's
// importPlayersCsv/importTeamsCsv (which need Room) are exercised on-device instead.
class CsvImportTest {

    @Test
    fun parsesSimpleRowsAndDropsBlankLines() {
        val rows = parseCsv("a,b,c\n1,2,3\n\n4,5,6\n")
        assertEquals(
            listOf(listOf("a", "b", "c"), listOf("1", "2", "3"), listOf("4", "5", "6")),
            rows
        )
    }

    @Test
    fun handlesQuotedFieldsWithCommasNewlinesAndEscapedQuotes() {
        val rows = parseCsv("name,note\n\"Doe, Jane\",\"line1\nline2\"\n\"He said \"\"hi\"\"\",x")
        assertEquals(listOf("name", "note"), rows[0])
        assertEquals(listOf("Doe, Jane", "line1\nline2"), rows[1])
        assertEquals(listOf("He said \"hi\"", "x"), rows[2])
    }

    @Test
    fun handlesCrlfAndTrailingRowWithoutNewline() {
        val rows = parseCsv("a,b\r\n1,2")
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), rows)
    }

    @Test
    fun stripsLeadingBom() {
        val rows = parseCsv("﻿name,number\nAlex,10001")
        assertEquals("name", rows[0][0])
    }

    @Test
    fun headerMatchesAreCaseAndSpaceInsensitiveWithAliases() {
        val header = CsvHeader(listOf("Player Name", "PLAYER_NUMBER", "  Email  "))
        assertEquals(0, header.index("name", "playername"))
        assertEquals(1, header.index("playernumber", "number"))
        assertEquals(2, header.index("email"))
        assertNull(header.index("phone"))
    }

    @Test
    fun cellTrimsAndGuardsOutOfRange() {
        val row = listOf("  hi  ", "x")
        assertEquals("hi", row.cell(0))
        assertEquals("", row.cell(5))
        assertEquals("", row.cell(null))
    }
}
