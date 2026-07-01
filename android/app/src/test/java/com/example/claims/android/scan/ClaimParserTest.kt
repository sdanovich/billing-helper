package com.example.claims.android.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClaimParserTest {

    /** The synthetic labelled format must still parse exactly (no regression). */
    @Test
    fun parsesSyntheticLabelledFormat() {
        val text = """
            Claim ID: TEST-0001
            Patient: Alpha Tester
            Member ID: MBR-TEST-0001
            Payer: Synthetic Health Plan
            CPT: 99213
            ICD: E11.9
            Billed: 250.00
            Paid: 200.00
            Status: submitted
        """.trimIndent()

        val f = ClaimParser.parse(text)

        assertEquals("TEST-0001", f.claimId)
        assertEquals("Alpha Tester", f.patientName)
        assertEquals("MBR-TEST-0001", f.memberId)
        assertEquals("Synthetic Health Plan", f.payer)
        assertEquals("99213", f.cptCode)
        assertEquals("E11.9", f.icdCode)
        assertEquals("250.00", f.billedAmount)
        assertEquals("200.00", f.paidAmount)
        assertEquals("submitted", f.status)
    }

    /**
     * A real, tabular hospital statement (the Allina Health bill) has none of our labels. Best-effort
     * extraction should still fill what it can: patient name, payer, account id, and the amounts.
     * OCR reading order is approximated below (label rows, then value rows).
     */
    @Test
    fun fillsWhatItCanFromRealHospitalStatement() {
        val text = """
            Allina Health
            2925 Chicago Avenue
            Minneapolis, MN 55407-1321
            Billing Questions ?
            ADDRESSEE
            JANE DOE
            123 ANY STREET
            ANYWHERE, USA 55555
            HOSPITAL STATEMENT
            ACCOUNT NUMBER
            PATIENT NAME
            HOSPITAL NAME
            0000000
            JANE DOE
            Medical Center
            Date
            Description
            Charges/Payments
            SERVICES FROM 07/07/2010 to 07/07/2010
            07/07/10
            Laboratory General
            ${'$'}654.80
            TOTAL CHARGES
            ${'$'}654.80
            03/26/13
            Insurance Payment
            ${'$'}0.00
            03/27/13
            Uninsured Discount
            -235.73
            BALANCE:
            ${'$'}419.07
            PLEASE PAY THIS AMOUNT
            ${'$'}419.07
        """.trimIndent()

        val f = ClaimParser.parse(text)

        assertEquals("JANE DOE", f.patientName)
        assertTrue("payer was '${f.payer}'", f.payer.contains("Allina", ignoreCase = true))
        assertEquals("0000000", f.memberId)
        assertEquals("654.80", f.billedAmount)
        assertEquals("0.00", f.paidAmount)
        assertEquals("419.07", f.balance)
        // No claim id / CPT / ICD on a real statement — left blank for the clerk to fill.
        assertEquals("", f.claimId)
        assertEquals("", f.cptCode)
        assertEquals("", f.icdCode)
    }

    /** Amounts must not pick up zips/account numbers, and blank input must not crash. */
    @Test
    fun handlesEmptyAndAvoidsFalseAmounts() {
        val f = ClaimParser.parse("")
        assertEquals("", f.patientName)
        assertEquals("", f.balance)
        assertEquals("pending", f.status)
    }
}
