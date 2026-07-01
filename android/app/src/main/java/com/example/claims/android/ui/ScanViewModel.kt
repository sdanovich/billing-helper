package com.example.claims.android.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.claims.android.data.ClaimsRepository
import com.example.claims.android.data.IngestClaim
import com.example.claims.android.scan.ClaimParser
import com.example.claims.android.scan.ScanForm
import com.example.claims.android.scan.ocrDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScanViewModel(private val repo: ClaimsRepository) : ViewModel() {

    data class State(
        val form: ScanForm = ScanForm(),
        val imageUri: Uri? = null,
        val hasOcr: Boolean = false,
        val busy: Boolean = false,
        val error: String? = null
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    /** A page was scanned or a file (image/PDF) imported: OCR on-device, then parse into the form. */
    fun onScanned(context: Context, uri: Uri) {
        _state.update { it.copy(imageUri = uri, busy = true, error = null) }
        val appContext = context.applicationContext
        viewModelScope.launch {
            try {
                val doc = ocrDocument(appContext, uri)
                _state.update {
                    it.copy(form = ClaimParser.parse(doc.text), imageUri = doc.imageUri, hasOcr = true, busy = false)
                }
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = "Couldn't read the document: ${e.message}") }
            }
        }
    }

    fun update(transform: (ScanForm) -> ScanForm) = _state.update { it.copy(form = transform(it.form)) }

    fun save(onSaved: () -> Unit) {
        val f = _state.value.form
        if (!f.claimId.trim().startsWith("TEST-")) {
            _state.update { it.copy(error = "Claim id must be synthetic (start with TEST-).") }
            return
        }
        if (f.patientName.isBlank() || f.memberId.isBlank() || f.payer.isBlank()) {
            _state.update { it.copy(error = "Patient, member id and payer are required.") }
            return
        }
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                repo.ingest(f.toIngest(), _state.value.imageUri)
                _state.update { it.copy(busy = false) }
                onSaved()
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = e.message ?: "Save failed") }
            }
        }
    }

    private fun ScanForm.toIngest() = IngestClaim(
        claimId = claimId.trim(),
        patientName = patientName.trim(),
        memberId = memberId.trim(),
        payer = payer.trim(),
        cptCode = cptCode.ifBlank { null },
        icdCode = icdCode.ifBlank { null },
        billedAmount = billedAmount.toDoubleOrNull(),
        paidAmount = paidAmount.toDoubleOrNull(),
        balance = balance.toDoubleOrNull(),
        status = status,
        denialReason = denialReason.ifBlank { null }
    )

    companion object {
        fun factory(repo: ClaimsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ScanViewModel(repo) as T
            }
    }
}
