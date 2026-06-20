package com.exemplo.fluidez.ui

import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exemplo.fluidez.data.AppInfo
import com.exemplo.fluidez.system.AppActions
import com.exemplo.fluidez.system.Priv
import com.exemplo.fluidez.system.SettingsNavigator
import com.exemplo.fluidez.util.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(viewModel: AppsViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        if (!state.hasUsageAccess) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Libere o 'Acesso de uso' pra ver o tempo de cada app.")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        SettingsNavigator.open(context, Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    }) { Text("Abrir configuração") }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            label = { Text("Buscar app") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppFilter.values().forEach { f ->
                FilterChip(
                    selected = state.filter == f,
                    onClick = { viewModel.setFilter(f) },
                    label = { Text(f.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        if (state.loading) {
            CircularProgressIndicator()
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.visibleApps, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        onUninstall = { AppActions.uninstallUserApp(context, app.packageName) },
                        onForceStop = {
                            AppActions.forceStop(Priv.shell(context), app.packageName) { _, _ -> }
                        },
                        onDisable = {
                            AppActions.disableSystemApp(Priv.shell(context), app.packageName) { _, _ ->
                                viewModel.refresh()
                            }
                        },
                        onRemove = {
                            AppActions.uninstallForUser(Priv.shell(context), app.packageName) { _, _ ->
                                viewModel.refresh()
                            }
                        },
                        onRestore = {
                            AppActions.reinstallForUser(Priv.shell(context), app.packageName) { _, _ ->
                                viewModel.refresh()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    app: AppInfo,
    onUninstall: () -> Unit,
    onForceStop: () -> Unit,
    onDisable: () -> Unit,
    onRemove: () -> Unit,
    onRestore: () -> Unit
) {
    val context = LocalContext.current
    val icon = remember(app.packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(app.packageName).toBitmap().asImageBitmap()
        }.getOrNull()
    }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remover ${app.label}?") },
            text = {
                Text(
                    "Isso remove o app para o usuário (funciona até em apps de sistema). " +
                        "Dá para reverter em Restaurar, mas remover apps essenciais pode deixar o " +
                        "sistema instável. Tem certeza?"
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmRemove = false; onRemove() }) { Text("Remover") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text("Cancelar") }
            }
        )
    }

    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    app.label + if (!app.isInstalled) "  (removido)" else "",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    "Uso (7 dias): ${formatDuration(app.usageTimeMs)}" +
                        if (app.isSystemApp) " • sistema" else "",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Ações")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (!app.isInstalled) {
                        DropdownMenuItem(
                            text = { Text("Restaurar") },
                            onClick = { menuOpen = false; onRestore() }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Parar") },
                            onClick = { menuOpen = false; onForceStop() }
                        )
                        if (app.isSystemApp) {
                            DropdownMenuItem(
                                text = { Text("Desativar") },
                                onClick = { menuOpen = false; onDisable() }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Desinstalar") },
                                onClick = { menuOpen = false; onUninstall() }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Remover (forçado)") },
                            onClick = { menuOpen = false; confirmRemove = true }
                        )
                    }
                }
            }
        }
    }
}
