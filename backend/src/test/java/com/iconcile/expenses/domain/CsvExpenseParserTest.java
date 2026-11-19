package com.iconcile.expenses.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvExpenseParserTest {

    private final CsvExpenseParser parser = new CsvExpenseParser(LocalDate.of(2026, 11, 19));

    @Test
    void parsesValidRowsWithAliasesQuotesAndFormats() {
        String csv = "\uFEFFDate,Amount,Vendor Name,Description\r\n"
                + "2026-11-02,450.50,Swiggy,Dinner\r\n"
                + "05/11/2026,\"₹1,200\",Uber,\"Airport, late night\"\r\n"
                + "\r\n"
                + "7 Nov 2026,Rs. 99,Netflix,\"Plan \"\"Basic\"\"\"\n";
        var result = parser.parse(csv);

        assertEquals(0, result.errors().size());
        assertEquals(3, result.rows().size());
        var uber = result.rows().get(1);
        assertEquals(LocalDate.of(2026, 11, 5), uber.date());
        assertEquals(new BigDecimal("1200.00"), uber.amount());
        assertEquals("Airport, late night", uber.description());
        assertEquals("Plan \"Basic\"", result.rows().get(2).description());
        assertEquals(LocalDate.of(2026, 11, 7), result.rows().get(2).date());
    }

    @Test
    void collectsRowErrorsWithLineNumbersWithoutAbortingImport() {
        String csv = "date,amount,vendor\n"
                + "2026-11-01,100,Zomato\n"
                + "2026-13-01,100,Zomato\n"
                + "2026-11-01,-5,Zomato\n"
                + "2026-11-01,10.999,Zomato\n"
                + "2026-11-01,abc,\n"
                + "2027-01-01,100,Zomato\n";
        var result = parser.parse(csv);

        assertEquals(1, result.rows().size());
        assertEquals(5, result.errors().size());
        assertEquals(3, result.errors().get(0).lineNumber());
        assertTrue(result.errors().get(0).message().contains("invalid date"));
        assertTrue(result.errors().get(1).message().contains("greater than 0"));
        assertTrue(result.errors().get(2).message().contains("2 decimal"));
        assertTrue(result.errors().get(3).message().contains("vendor is required"));
        assertTrue(result.errors().get(3).message().contains("invalid amount"));
        assertTrue(result.errors().get(4).message().contains("future"));
    }

    @Test
    void optionalColumnsMayBeAbsentAndCategoryIsCaptured() {
        var result = parser.parse("vendor,amount,date,category\nCorner Shop,50,2026-10-10,Groceries\n");
        var row = result.rows().get(0);
        assertNull(row.description());
        assertEquals("Groceries", row.categoryName());
    }

    @Test
    void rejectsFilesWithMissingColumnsOrNoData() {
        assertThrows(CsvExpenseParser.CsvFormatException.class, () -> parser.parse("date,vendor\n2026-01-01,X\n"));
        assertThrows(CsvExpenseParser.CsvFormatException.class, () -> parser.parse("date,amount,vendor\n"));
        assertThrows(CsvExpenseParser.CsvFormatException.class, () -> parser.parse("   "));
        assertThrows(CsvExpenseParser.CsvFormatException.class,
                () -> parser.parse("date,amount,vendor\n2026-01-01,5,\"unterminated\n"));
    }

    @Test
    void multilineQuotedFieldKeepsCorrectLineNumbers() {
        String csv = "date,amount,vendor,description\n"
                + "2026-11-01,10,A,\"line one\nline two\"\n"
                + "bad,10,B,x\n";
        var result = parser.parse(csv);
        assertEquals("line one\nline two", result.rows().get(0).description());
        assertEquals(4, result.errors().get(0).lineNumber());
    }
}
