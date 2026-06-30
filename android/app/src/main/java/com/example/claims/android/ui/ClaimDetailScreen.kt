package com.example.claims.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.claims.android.data.AppGraph
import com.example.claims.android.data.ClaimDetail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimDetailScreen(graph: AppGraph, claimId: String, onBack: () -> Unit) {
    val vm: ClaimDetailViewModel =
        viewModel(factory = ClaimDetailViewModel.factory(graph.repository, claimId))
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(claimId) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                state.loading -> CircularProgressIndicator()
                state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error)
                state.claim != null -> ClaimDetailBody(graph, state.claim!!)
            }
        }
    }
}

@Composable
private fun ClaimDetailBody(graph: AppGraph, c: ClaimDetail) {
    Text(c.patientName, style = MaterialTheme.typography.headlineSmall)
    Field("Status", c.status)
    Field("Member ID", c.memberId)
    Field("Payer", c.payer)
    Field("CPT", c.cptCode ?: "—")
    Field("ICD", c.icdCode ?: "—")
    Field("Billed", money(c.billedAmount))
    Field("Paid", money(c.paidAmount))
    Field("Balance", money(c.balance))
    if (c.status == "denied") Field("Denial reason", c.denialReason ?: "—")
    c.createdAt?.let { Field("Created", it) }

    if (c.imageId != null) {
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("Scanned document", style = MaterialTheme.typography.titleMedium)
        AsyncImage(
            model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(graph.imageUrl(c.claimId))
                .build(),
            imageLoader = graph.imageLoader,
            contentDescription = "Scanned claim document",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
        )
    }
}

@Composable
private fun Field(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun money(v: Double?): String = if (v == null) "—" else "$" + String.format("%.2f", v)
