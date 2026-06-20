package com.exemplo.fluidez.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exemplo.fluidez.data.AppStorage
import com.exemplo.fluidez.system.Priv
import com.exemplo.fluidez.system.StorageActions
import com.exemplo.fluidez.util.formatBytes

@Composable
fun StorageScreen(viewModel: StorageViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var cleanStatus by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Armazenamento", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { state.overview.usedFraction },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "${formatBytes(state.overview.used)} usados • " +
                        "${formatBytes(state.overview.free)} livres"
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                StorageActions.trimAllCaches(Priv.shell(context)) { ok, _ ->
                    cleanStatus = if (ok) "Caches limpos!" else "Falhou (Shizuku ativo?)"
                    viewModel.refresh()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Limpar todos os caches") }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                StorageActions.stopUserApps(Priv.shell(context)) { ok, _ ->
                    cleanStatus = if (ok) "Apps de usuário parados!" else "Falhou (Shizuku ativo?)"
                    viewModel.refresh()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Parar apps de usuário") }

        if (cleanStatus.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(cleanStatus, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(12.dp))
        Text("Apps que mais ocupam", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (!state.hasUsageAccess) {
            Text("Libere o 'Acesso de uso' (na aba Apps) pra ver os tamanhos.")
        } else if (state.loading) {
            CircularProgressIndicator()
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.apps, key = { it.packageName }) { app ->
                    StorageRow(app) { StorageActions.openAppDetails(context, app.packageName) }
                }
            }
        }
    }
}

@Composable
private fun StorageRow(app: AppStorage, onDetails: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(app.label, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    "Total: ${formatBytes(app.totalBytes)} • cache: ${formatBytes(app.cacheBytes)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onDetails) { Text("Detalhes") }
        }
    }
}
