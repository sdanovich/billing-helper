package com.example.claims.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.claims.android.data.AppGraph
import com.example.claims.android.data.CLAIM_STATUSES
import com.example.claims.android.data.ClaimSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimsListScreen(
    graph: AppGraph,
    onOpen: (String) -> Unit,
    onScan: () -> Unit,
    onLogout: () -> Unit
) {
    val vm: ClaimsListViewModel = viewModel(factory = ClaimsListViewModel.factory(graph.repository))
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Claims") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onScan,
                icon = { Icon(Icons.Filled.DocumentScanner, contentDescription = null) },
                text = { Text("Scan") }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            FilterBar(state, vm)

            state.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (state.loading && state.claims.isEmpty()) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.claims, key = { it.claimId }) { claim ->
                    ClaimRow(claim) { onOpen(claim.claimId) }
                }
                if (state.claims.isEmpty() && !state.loading) {
                    item { Text("No claims match these filters.", Modifier.padding(8.dp)) }
                }
                if (state.hasMore) {
                    item {
                        TextButton(onClick = vm::loadMore, modifier = Modifier.fillMaxWidth()) {
                            Text(if (state.loading) "Loading…" else "Load more")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBar(state: ClaimsListViewModel.State, vm: ClaimsListViewModel) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::setQuery,
            label = { Text("Search patient or claim id") },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { vm.submitQuery() }),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = state.status == null,
                onClick = { vm.setStatus(null) },
                label = { Text("All") }
            )
            CLAIM_STATUSES.forEach { s ->
                FilterChip(
                    selected = state.status == s,
                    onClick = { vm.setStatus(if (state.status == s) null else s) },
                    label = { Text(s.replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        PayerDropdown(
            payers = state.payers,
            selected = state.payer,
            onSelect = vm::setPayer,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayerDropdown(
    payers: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected ?: "All payers",
            onValueChange = {},
            readOnly = true,
            label = { Text("Payer") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("All payers") }, onClick = { onSelect(null); expanded = false })
            payers.forEach { p ->
                DropdownMenuItem(text = { Text(p) }, onClick = { onSelect(p); expanded = false })
            }
        }
    }
}

@Composable
private fun ClaimRow(claim: ClaimSummary, onClick: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(claim.patientName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${claim.claimId} · ${claim.payer}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Balance: $" + String.format("%.2f", claim.balance),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            StatusChip(claim.status)
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val color = when (status) {
        "paid" -> MaterialTheme.colorScheme.primary
        "denied" -> MaterialTheme.colorScheme.error
        "pending" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(status.replaceFirstChar { it.uppercase() }) },
        colors = AssistChipDefaults.assistChipColors(
            disabledLabelColor = color,
            disabledContainerColor = color.copy(alpha = 0.12f)
        )
    )
}
