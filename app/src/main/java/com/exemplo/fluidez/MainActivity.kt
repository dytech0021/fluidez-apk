package com.exemplo.fluidez

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.exemplo.fluidez.system.AnimationController
import com.exemplo.fluidez.system.AnimationPrefs
import com.exemplo.fluidez.ui.AppsScreen
import com.exemplo.fluidez.ui.DashboardScreen
import com.exemplo.fluidez.ui.StorageScreen
import com.exemplo.fluidez.ui.theme.FluidezTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Reaplica a trava de animação ao abrir, se o usuário ativou.
        if (AnimationPrefs.isLockEnabled(this)) {
            AnimationController.setZero(this)
        }
        // Prepara o ADB embutido (gera chaves em segundo plano).
        com.exemplo.fluidez.system.AdbManager.init(this)
        // Pede permissão de notificação (Android 13+) para o serviço-vigia.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        setContent {
            FluidezTheme {
                var tab by remember { mutableStateOf(0) }
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = tab == 0, onClick = { tab = 0 },
                                icon = { Icon(Icons.Default.Speed, contentDescription = null) },
                                label = { Text("Início") }
                            )
                            NavigationBarItem(
                                selected = tab == 1, onClick = { tab = 1 },
                                icon = { Icon(Icons.Default.Apps, contentDescription = null) },
                                label = { Text("Apps") }
                            )
                            NavigationBarItem(
                                selected = tab == 2, onClick = { tab = 2 },
                                icon = { Icon(Icons.Default.CleaningServices, contentDescription = null) },
                                label = { Text("Limpeza") }
                            )
                        }
                    }
                ) { padding ->
                    Box(Modifier.padding(padding)) {
                        when (tab) {
                            0 -> DashboardScreen()
                            1 -> AppsScreen()
                            2 -> StorageScreen()
                        }
                    }
                }
            }
        }
    }
}
