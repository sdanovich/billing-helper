package com.example.claims.android.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.claims.android.data.AppGraph
import com.example.claims.android.data.CLAIM_STATUSES
import com.example.claims.android.scan.ScanForm
import com.example.claims.android.scan.buildDocumentScanner
import com.example.claims.android.scan.findActivity
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(graph: AppGraph, onDone: () -> Unit, onCancel: () -> Unit) {
    val vm: ScanViewModel = viewModel(factory = ScanViewModel.factory(graph.repository))
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scanner = remember { buildDocumentScanner() }

    val launcher = rememberLauncherForActivityResult(StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                ?.pages?.firstOrNull()?.imageUri
                ?.let { vm.onScanned(context, it) }
        }
    }

    fun startScan() {
        val activity = context.findActivity() ?: return
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { sender ->
                launcher.launch(IntentSenderRequest.Builder(sender).build())
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan claim") },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, contentDescription = "Cancel") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(onClick = { startScan() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.DocumentScanner, contentDescription = null)
                Text("  Scan document", modifier = Modifier.padding(start = 4.dp))
            }

            state.imageUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Scanned page",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)
                )
            }

            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())

            if (state.hasOcr || state.imageUri != null) {
                Text(
                    "Confirm the parsed fields",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            val f = state.form
            FormField("Claim ID", f.claimId) { v -> vm.update { it.copy(claimId = v) } }
            FormField("Patient name", f.patientName) { v -> vm.update { it.copy(patientName = v) } }
            FormField("Member ID", f.memberId) { v -> vm.update { it.copy(memberId = v) } }
            FormField("Payer", f.payer) { v -> vm.update { it.copy(payer = v) } }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField("CPT", f.cptCode, Modifier.weight(1f)) { v -> vm.update { it.copy(cptCode = v) } }
                FormField("ICD", f.icdCode, Modifier.weight(1f)) { v -> vm.update { it.copy(icdCode = v) } }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField("Billed", f.billedAmount, Modifier.weight(1f), KeyboardType.Decimal) { v -> vm.update { it.copy(billedAmount = v) } }
                FormField("Paid", f.paidAmount, Modifier.weight(1f), KeyboardType.Decimal) { v -> vm.update { it.copy(paidAmount = v) } }
            }
            FormField("Balance (blank = billed − paid)", f.balance, keyboardType = KeyboardType.Decimal) { v -> vm.update { it.copy(balance = v) } }

            StatusDropdown(f.status) { v -> vm.update { it.copy(status = v) } }

            if (f.status == "denied") {
                FormField("Denial reason", f.denialReason) { v -> vm.update { it.copy(denialReason = v) } }
            }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = { vm.save(onDone) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(if (state.busy) "Saving…" else "Save claim")
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.replaceFirstChar { it.uppercase() },
            onValueChange = {},
            readOnly = true,
            label = { Text("Status") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CLAIM_STATUSES.forEach { s ->
                DropdownMenuItem(
                    text = { Text(s.replaceFirstChar { it.uppercase() }) },
                    onClick = { onSelect(s); expanded = false }
                )
            }
        }
    }
}
