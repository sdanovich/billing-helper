package com.example.claims.android.scan

import com.example.claims.android.data.CLAIM_STATUSES

/** Editable form backing the scan-confirm screen; also the parser's output shape. */
data class ScanForm(
    val claimId: String = "",
    val patientName: String = "",
    val memberId: String = "",
    val payer: String = "",
    val cptCode: String = "",
    val icdCode: String = "",
    val billedAmount: String = "",
    val paidAmount: String = "",
    val balance: String = "",
    val status: String = "pending",
    val denialReason: String = ""
)

/**
 * Deterministic, on-device parse of the recognized text into claim fields. The synthetic claim
 * documents are a FIXED labelled-field format we define, so this is plain labelled-field/regex
 * extraction — no LLM, nothing leaves the phone. The clerk confirms/corrects the result before
 * it's submitted.
 *
 * Expected document lines (label: value), order-independent, e.g.:
 *   Claim ID: TEST-0001
 *   Patient: Alpha Tester
 *   Member ID: MBR-TEST-0001
 *   Payer: Synthetic Health Plan
 *   CPT: 99213
 *   ICD: E11.9
 *   Billed: 250.00
 *   Paid: 200.00
 *   Status: submitted
 *   Denial Reason: ...
 */
object ClaimParser {

    fun parse(text: String): ScanForm {
        fun field(vararg labels: String): String {
            for (label in labels) {
                val m = Regex("(?im)^\\s*${Regex.escape(label)}\\s*[:#\\-]\\s*(.+)$").find(text)
                if (m != null) return m.groupValues[1].trim()
            }
            return ""
        }
        fun money(s: String): String =
            s.replace(Regex("[^0-9.]"), "").let { if (it.toDoubleOrNull() != null) it else "" }

        val statusRaw = field("Status").lowercase()
        val status = CLAIM_STATUSES.firstOrNull { statusRaw.contains(it) } ?: "pending"

        return ScanForm(
            claimId = field("Claim ID", "ClaimID", "Claim"),
            patientName = field("Patient Name", "Patient"),
            memberId = field("Member ID", "MemberID", "Member"),
            payer = field("Payer", "Plan", "Insurer"),
            cptCode = field("CPT", "CPT Code"),
            icdCode = field("ICD", "ICD Code", "Diagnosis"),
            billedAmount = money(field("Billed", "Billed Amount", "Charge")),
            paidAmount = money(field("Paid", "Paid Amount")),
            balance = money(field("Balance")),
            status = status,
            denialReason = field("Denial Reason", "Denial")
        )
    }
}
