package com.exemplo.fluidez.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exemplo.fluidez.data.SystemSnapshot
import com.exemplo.fluidez.system.AdbManager
import com.exemplo.fluidez.system.AnimationController
import com.exemplo.fluidez.system.AnimationPrefs
import com.exemplo.fluidez.system.GuardController
import com.exemplo.fluidez.system.MaintenanceActions
import com.exemplo.fluidez.system.PairingService
import com.exemplo.fluidez.system.Priv
import com.exemplo.fluidez.system.ProfileManager
import com.exemplo.fluidez.system.RefreshRateController
import com.exemplo.fluidez.system.ScheduleManager
import com.exemplo.fluidez.system.SchedulePrefs
import com.exemplo.fluidez.system.SettingsNavigator
import com.exemplo.fluidez.system.ShizukuManager
import com.exemplo.fluidez.system.UpdateManager
import com.exemplo.fluidez.util.formatBytes
import com.exemplo.fluidez.util.formatEta
import rikka.shizuku.Shizuku

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val context = LocalContext.current
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Fluidez",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        UpdateCard()
        ProfilesCard()
        ScheduleCard()

        UsageCard(
            title = "Memória RAM",
            detail = "${formatBytes(snapshot.usedRam)} de ${formatBytes(snapshot.totalRam)}",
            fraction = snapshot.ramUsedFraction
        )
        UsageCard(
            title = "Armazenamento",
            detail = "${formatBytes(snapshot.usedStorage)} de ${formatBytes(snapshot.totalStorage)}",
            fraction = snapshot.storageUsedFraction
        )
        BatteryCard(snapshot)

        AdbSetupCard()
        ShizukuSetupCard()
        AnimationCard()
        RefreshRateCard()
        MaintenanceCard()

        Text("Atalhos rápidos", style = MaterialTheme.typography.titleMedium)
        ShortcutButton("Opções de Desenvolvedor") { SettingsNavigator.openDeveloperOptions(context) }
        ShortcutButton("Gerenciar Apps") { SettingsNavigator.openAppsList(context) }
        ShortcutButton("Armazenamento") { SettingsNavigator.openStorageSettings(context) }
        ShortcutButton("Bateria") { SettingsNavigator.openBatterySettings(context) }

        Button(onClick = { viewModel.refresh() }, modifier = Modifier.fillMaxWidth()) {
            Text("Atualizar")
        }
    }
}

@Composable
private fun UpdateCard() {
    val context = LocalContext.current
    var info by remember { mutableStateOf<UpdateManager.Info?>(null) }
    var status by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { UpdateManager.check(context) { info = it } }

    val update = info ?: return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Atualização disponível: v${update.version}", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    status = "Baixando..."
                    UpdateManager.downloadAndInstall(context, Priv.shell(context), update) { status = it }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Atualizar agora") }
            if (status.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ProfilesCard() {
    val context = LocalContext.current
    var status by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Perfis", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileManager.all.forEach { profile ->
                    Button(
                        onClick = {
                            ProfileManager.apply(context, Priv.shell(context), profile) { ok ->
                                status = if (ok) "${profile.name} aplicado"
                                else "${profile.name}: aplicado parcialmente"
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(profile.name) }
                }
            }
            if (status.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ScheduleCard() {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Agendar perfis", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            ScheduleSlot(context, 0)
            Spacer(Modifier.height(12.dp))
            ScheduleSlot(context, 1)
        }
    }
}

@Composable
private fun ScheduleSlot(context: Context, slot: Int) {
    var s by remember { mutableStateOf(SchedulePrefs.get(context, slot)) }

    fun save(newS: SchedulePrefs.Slot) {
        s = newS
        SchedulePrefs.set(context, slot, newS)
        ScheduleManager.reschedule(context)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Agendamento ${slot + 1}", modifier = Modifier.weight(1f))
        Switch(checked = s.enabled, onCheckedChange = { save(s.copy(enabled = it)) })
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = {
            android.app.TimePickerDialog(
                context,
                { _, h, m -> save(s.copy(hour = h, minute = m)) },
                s.hour, s.minute, true
            ).show()
        }) { Text(String.format("%02d:%02d", s.hour, s.minute)) }

        OutlinedButton(onClick = {
            val ids = ProfileManager.all.map { it.id }
            val idx = ids.indexOf(s.profileId)
            val next = ids[(idx + 1) % ids.size]
            save(s.copy(profileId = next))
        }) { Text(ProfileManager.byId(s.profileId).name) }
    }
}

@Composable
private fun AdbSetupCard() {
    val context = LocalContext.current
    var connected by remember { mutableStateOf(AdbManager.isReady()) }
    var endpoint by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { AdbManager.init(context) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { connected = AdbManager.isReady() }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Conexão ADB (sem Shizuku)", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            if (connected) {
                Text("Conectado — recursos avançados liberados.")
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        AdbManager.exec(
                            "pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
                        ) { ok, msg -> status = if (ok) "Permissões concedidas!" else "Falhou: $msg" }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Conceder permissões") }
            } else {
                Text(
                    "Toque em 'Parear por notificação', depois abra 'Parear com código' nas " +
                        "Configurações. Vai surgir uma notificação pra você digitar só o código.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { PairingService.start(context) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Parear por notificação") }
                Spacer(Modifier.height(12.dp))
                Text("Ou manualmente (IP:porta + código):", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = endpoint, onValueChange = { endpoint = it },
                    label = { Text("IP:porta de pareamento") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = code, onValueChange = { code = it },
                    label = { Text("Código de pareamento") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val parts = endpoint.trim().split(":")
                            val port = parts.getOrNull(1)?.toIntOrNull()
                            if (parts.size == 2 && port != null) {
                                status = "Pareando..."
                                AdbManager.pair(parts[0], port, code.trim()) { ok, msg ->
                                    status = if (ok) "Pareado! Agora toque em Conectar." else "Falha: $msg"
                                }
                            } else {
                                status = "Use o formato IP:porta"
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Parear") }
                    Button(
                        onClick = {
                            status = "Conectando..."
                            AdbManager.connect { ok, msg ->
                                connected = AdbManager.isReady()
                                status = if (ok) "Conectado!" else "Falha: $msg"
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Conectar") }
                }
            }
            if (status.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ShizukuSetupCard() {
    val context = LocalContext.current
    val manager = remember { ShizukuManager(context.applicationContext) }
    var status by remember { mutableStateOf("") }
    var installed by remember { mutableStateOf(manager.isInstalled()) }
    var running by remember { mutableStateOf(manager.isRunning()) }
    var ready by remember { mutableStateOf(AnimationController.hasPermission(context)) }

    fun refresh() {
        installed = manager.isInstalled()
        running = manager.isRunning()
        ready = AnimationController.hasPermission(context)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh() }

    DisposableEffect(Unit) {
        val listener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                manager.grantSecureSettings { ok ->
                    refresh()
                    status = if (ok) "Pronto! O app já controla o sistema."
                    else "Não consegui liberar a permissão."
                }
            } else {
                status = "Autorização do Shizuku negada."
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)
        onDispose { Shizuku.removeRequestPermissionResultListener(listener) }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Configuração (Shizuku)", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            when {
                ready -> Text("Tudo certo: permissão concedida.")
                !installed -> {
                    Text("A Shizuku não está instalada. Ela libera os recursos avançados.")
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { manager.openDownloadPage() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Baixar Shizuku") }
                }
                !running -> {
                    Text("Shizuku instalada, mas não iniciada. Abra-a e toque em Iniciar.")
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { manager.openShizukuApp() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Abrir Shizuku") }
                }
                else -> Button(
                    onClick = {
                        if (manager.hasPermission()) {
                            manager.grantSecureSettings { ok ->
                                refresh()
                                status = if (ok) "Pronto!" else "Falhou."
                            }
                        } else manager.requestPermission()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Configurar via Shizuku") }
            }
            if (status.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AnimationCard() {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(AnimationController.hasPermission(context)) }
    var adbReady by remember { mutableStateOf(AdbManager.isReady()) }
    var isZero by remember { mutableStateOf(AnimationController.areAllZero(context)) }
    var guardOn by remember { mutableStateOf(AnimationPrefs.isGuardEnabled(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasPermission = AnimationController.hasPermission(context)
        adbReady = AdbManager.isReady()
        isZero = AnimationController.areAllZero(context)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Animações", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            if (!hasPermission && !adbReady) {
                Text("Conecte o ADB ou configure o Shizuku acima.")
            } else {
                Text(if (isZero) "Estado: ZERO" else "Estado: ativas")
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        AnimationPrefs.setLockEnabled(context, true)
                        if (hasPermission) {
                            AnimationController.setZero(context)
                            isZero = AnimationController.areAllZero(context)
                        } else {
                            AnimationController.setScaleViaShell(Priv.shell(context), 0f) { _, _ ->
                                isZero = AnimationController.areAllZero(context)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Travar animações em ZERO") }

                if (hasPermission) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Vigia em tempo real", modifier = Modifier.weight(1f))
                        Switch(
                            checked = guardOn,
                            onCheckedChange = { on ->
                                guardOn = on
                                AnimationPrefs.setGuardEnabled(context, on)
                                if (on) {
                                    AnimationController.setZero(context)
                                    GuardController.start(context)
                                } else {
                                    GuardController.stop(context)
                                }
                            }
                        )
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Funciona via ADB. 'Conceder permissões' acima ativa o modo nativo " +
                            "(reaplica no boot e o vigia em tempo real).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun RefreshRateCard() {
    val context = LocalContext.current
    val maxHz = remember { RefreshRateController.maxSupported(context) }
    var forced by remember { mutableStateOf(AnimationPrefs.isRefreshForced(context)) }
    var status by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Taxa de atualização", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Máxima suportada: ${maxHz}Hz", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Forçar ${maxHz}Hz sempre", modifier = Modifier.weight(1f))
                Switch(
                    checked = forced,
                    onCheckedChange = { on ->
                        forced = on
                        AnimationPrefs.setRefreshForced(context, on)
                        if (on) {
                            RefreshRateController.forceMax(context, Priv.shell(context)) { ok, _ ->
                                status = if (ok) "Aplicado" else "Falhou (Shizuku?)"
                            }
                        } else {
                            RefreshRateController.restore(Priv.shell(context)) { ok, _ ->
                                status = if (ok) "Restaurado" else "Falhou"
                            }
                        }
                    }
                )
            }
            if (status.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MaintenanceCard() {
    val context = LocalContext.current
    var running by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(0) }
    var total by remember { mutableStateOf(0) }
    var current by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf(0L) }
    var status by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Manutenção", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Recompila todos os apps para abrirem mais rápido. Pode levar alguns minutos.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    running = true
                    done = 0
                    total = 0
                    current = ""
                    status = ""
                    startTime = System.currentTimeMillis()
                    MaintenanceActions.optimizeAllApps(
                        context,
                        Priv.shell(context),
                        onProgress = { d, t, cur -> done = d; total = t; current = cur },
                        onDone = { ok ->
                            running = false
                            status = if (ok) "Concluído! $total apps otimizados."
                            else "Falhou (ADB/Shizuku ativo?)"
                        }
                    )
                },
                enabled = !running,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (running) "Otimizando..." else "Otimizar apps") }

            if (running && total > 0) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { done.toFloat() / total },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                val pct = (done * 100) / total
                Text("$done de $total  ($pct%)", style = MaterialTheme.typography.bodyMedium)
                if (current.isNotBlank()) {
                    Text(current, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
                val elapsed = System.currentTimeMillis() - startTime
                val remaining = if (done > 0) {
                    ((elapsed.toDouble() / done) * (total - done)).toLong()
                } else -1L
                Text(
                    if (remaining >= 0) "≈ ${formatEta(remaining)} restantes" else "calculando tempo...",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (status.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun UsageCard(title: String, detail: String, fraction: Float) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun BatteryCard(snapshot: SystemSnapshot) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Bateria", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            val status = if (snapshot.isCharging) "carregando" else "na bateria"
            Text("${snapshot.batteryPercent}% • $status")
            Text("Temperatura: ${snapshot.batteryTemperature} °C")
        }
    }
}

@Composable
private fun ShortcutButton(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            Text(text)
        }
    }
}
