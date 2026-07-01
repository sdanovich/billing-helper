package com.example.claims.android.scan

import com.example.claims.android.data.CLAIM_STATUSES
import java.util.Locale

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
 * Best-effort, on-device parse of recognized text into claim fields. Nothing leaves the phone,
 * no LLM — deterministic heuristics only. The clerk confirms/corrects every field before submit,
 * so the parser errs toward filling *something* plausible rather than staying blank.
 *
 * Two tiers, applied per field:
 *  1. **Strict labelled** — a `Label: value` line, for the synthetic documents we define
 *     (`Claim ID: TEST-0001`, `Patient: …`, `Billed: 250.00`, …). This always wins when present.
 *  2. **Heuristic fallback** — for real-world statements (hospital bills, EOBs) that have no such
 *     labels: money amounts are found by keyword proximity, a patient name by an all-caps name
 *     line, a payer by insurer keywords, an account/member id by a digit run near "account", etc.
 *     These are guesses; some fields will legitimately stay blank when nothing matches.
 */
object ClaimParser {

    private val NAME_STOPWORDS = setOf(
        "HEALTH", "HOSPITAL", "STATEMENT", "CENTER", "MEDICAL", "CLINIC", "PAYMENT", "PAYMENTS",
        "CHARGES", "CHARGE", "ACCOUNT", "NUMBER", "PATIENT", "NAME", "BALANCE", "AMOUNT", "DATE",
        "DUE", "PLEASE", "PAY", "THIS", "INSURANCE", "DISCOUNT", "SERVICES", "TOTAL", "BILLING",
        "QUESTIONS", "ADDRESSEE", "REMIT", "ONLINE", "CARD", "CREDIT", "CHECK", "CHECKS", "PAYABLE",
        "PAGE", "LABORATORY", "GENERAL", "DESCRIPTION", "PO", "BOX", "STREET", "AVENUE", "USA"
    )

    fun parse(text: String): ScanForm {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // --- Tier 1: strict "Label: value" on one line (the synthetic format) ---
        fun labelled(vararg labels: String): String {
            for (label in labels) {
                val m = Regex("(?im)^\\s*${Regex.escape(label)}\\s*[:#\\-]\\s*(.+)$").find(text)
                if (m != null) return m.groupValues[1].trim()
            }
            return ""
        }

        fun money(s: String): String =
            s.replace(Regex("[^0-9.]"), "").let { if (it.toDoubleOrNull() != null) it else "" }

        // --- Tier 2 helpers (heuristic fallbacks over the whole page) ---

        // Money tokens require cents (".dd") so we don't mistake zips / account numbers for amounts.
        fun moneyTokens(s: String): List<Double> =
            Regex("\\$?\\s*(\\d[\\d,]*\\.\\d{2})").findAll(s)
                .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
                .toList()

        fun fmt(d: Double) = String.format(Locale.US, "%.2f", d)

        // First amount at/after a line containing one of the keywords (keywords tried in priority
        // order, so "total charges" wins over a bare "charges" header elsewhere on the page).
        fun amountNear(vararg keywords: String): String {
            for (k in keywords) {
                val idx = lines.indexOfFirst { it.contains(k, ignoreCase = true) }
                if (idx < 0) continue
                for (i in idx..minOf(idx + 2, lines.lastIndex)) {
                    val toks = moneyTokens(lines[i])
                    if (toks.isNotEmpty()) return fmt(toks.last())
                }
            }
            return ""
        }

        // An identifier (digit run, or a XXX-1234 style code) at or just after an "account"-ish line.
        fun idNear(vararg keywords: String): String {
            val idToken = Regex("\\b(\\d{4,}|[A-Z]{2,5}-[A-Z0-9\\-]{3,})\\b")
            for (k in keywords) {
                val idx = lines.indexOfFirst { it.contains(k, ignoreCase = true) }
                if (idx < 0) continue
                for (i in idx..minOf(idx + 3, lines.lastIndex)) {
                    val line = lines[i]
                    if (i == idx) {
                        val after = Regex("(?i)${Regex.escape(k)}\\s*[:#\\-]?\\s*([A-Z0-9][A-Z0-9\\-]{3,})")
                            .find(line)
                        if (after != null) return after.groupValues[1]
                    } else {
                        val t = idToken.find(line)
                        if (t != null) return t.groupValues[1]
                    }
                }
            }
            return ""
        }

        // An all-caps 2–3 word line that reads like a person's name (no digits, no bill vocabulary).
        fun guessName(): String = lines.firstOrNull { ln ->
            val toks = ln.split(Regex("\\s+")).map { it.trim(',', '.') }.filter { it.isNotBlank() }
            toks.size in 2..3 &&
                toks.none { t -> t.any(Char::isDigit) } &&
                toks.none { t -> t.uppercase() in NAME_STOPWORDS } &&
                toks.all { t -> t.length >= 2 && t.all { c -> c.isLetter() || c == '\'' || c == '-' } } &&
                toks.all { t -> t == t.uppercase() }
        } ?: ""

        // A line naming an insurer / plan (no digits, so we skip address lines).
        fun guessPayer(): String {
            val keys = listOf(
                "health plan", "insurance", "health", "payer", "medicare", "medicaid",
                "blue cross", "blue shield", "aetna", "cigna", "unitedhealth", "united health", "humana"
            )
            return lines.firstOrNull { ln ->
                ln.none(Char::isDigit) && keys.any { ln.contains(it, ignoreCase = true) }
            }?.trim() ?: ""
        }

        // --- Assemble each field: strict label first, then a guess ---

        val claimId = labelled("Claim ID", "ClaimID", "Claim")

        var patientName = labelled("Patient Name", "Patient")
        if (patientName.isEmpty()) patientName = guessName()

        var memberId = labelled("Member ID", "MemberID", "Member")
        if (memberId.isEmpty()) memberId = idNear("Account Number", "Account #", "Account No", "Account")

        var payer = labelled("Payer", "Plan", "Insurer")
        if (payer.isEmpty()) payer = guessPayer()

        var cptCode = labelled("CPT", "CPT Code", "Procedure")
        if (cptCode.isEmpty()) {
            cptCode = Regex("(?i)\\bCPT\\b[^0-9]{0,6}(\\d{5})").find(text)?.groupValues?.get(1) ?: ""
        }

        var icdCode = labelled("ICD", "ICD Code", "Diagnosis")
        if (icdCode.isEmpty()) {
            icdCode = Regex("\\b([A-TV-Z]\\d{2}(?:\\.\\d{1,4})?)\\b").find(text)?.groupValues?.get(1) ?: ""
        }

        var billedAmount = money(labelled("Billed", "Billed Amount"))
        if (billedAmount.isEmpty()) billedAmount = amountNear("total charges", "amount billed", "charges")

        var paidAmount = money(labelled("Paid", "Paid Amount"))
        if (paidAmount.isEmpty()) paidAmount = amountNear("insurance payment", "amount paid", "paid")

        var balance = money(labelled("Balance"))
        if (balance.isEmpty()) balance = amountNear("balance", "please pay this amount", "amount due")

        val labelledStatus = labelled("Status").lowercase()
        val status = when {
            CLAIM_STATUSES.any { labelledStatus.contains(it) } ->
                CLAIM_STATUSES.first { labelledStatus.contains(it) }
            Regex("(?i)\\bdenied\\b|\\bdenial\\b").containsMatchIn(text) -> "denied"
            else -> "pending"
        }

        var denialReason = labelled("Denial Reason", "Denial")
        if (denialReason.isEmpty() && status == "denied") {
            denialReason = lines.firstOrNull { it.contains("denial", true) || it.contains("denied", true) } ?: ""
        }

        return ScanForm(
            claimId = claimId,
            patientName = patientName,
            memberId = memberId,
            payer = payer,
            cptCode = cptCode,
            icdCode = icdCode,
            billedAmount = billedAmount,
            paidAmount = paidAmount,
            balance = balance,
            status = status,
            denialReason = denialReason
        )
    }
}
