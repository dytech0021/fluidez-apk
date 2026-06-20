package com.exemplo.fluidez.system

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat

/**
 * Faz o pareamento ADB sem fio "estilo Shizuku":
 * descobre a porta de pareamento via mDNS e mostra uma notificação
 * com campo de texto pro usuário digitar só o código.
 */
class PairingService : Service() {

    companion object {
        private const val CHANNEL = "adb_pairing"
        private const val NID = 42
        private const val NID_DONE = 43
        private const val ACTION_SUBMIT = "com.exemplo.fluidez.SUBMIT_CODE"
        private const val SERVICE_TYPE = "_adb-tls-pairing._tcp"
        private const val KEY_CODE = "code"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context, Intent(context, PairingService::class.java)
            )
        }
    }

    private var nsd: NsdManager? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var resolved = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        goForeground(buildNotification("Pareamento ADB", "Procurando dispositivo...", null))
        if (intent?.action == ACTION_SUBMIT) {
            handleCode(intent)
        } else {
            startDiscovery()
        }
        return START_NOT_STICKY
    }

    private fun startDiscovery() {
        val manager = getSystemService(Context.NSD_SERVICE) as NsdManager
        nsd = manager
        val listener = object : NsdManager.DiscoveryListener {
            override fun onServiceFound(info: NsdServiceInfo) {
                if (resolved) return
                @Suppress("DEPRECATION")
                manager.resolveService(info, object : NsdManager.ResolveListener {
                    override fun onServiceResolved(resolved: NsdServiceInfo) {
                        @Suppress("DEPRECATION")
                        val host = resolved.host?.hostAddress ?: return
                        onPairingServiceFound(host, resolved.port)
                    }

                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {}
                })
            }

            override fun onServiceLost(info: NsdServiceInfo) {}
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                update("Falha ao procurar (erro $errorCode)")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }
        discoveryListener = listener
        try {
            manager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            update("Falha ao iniciar a busca: ${e.message}")
        }
    }

    private fun onPairingServiceFound(host: String, port: Int) {
        if (resolved) return
        resolved = true
        stopDiscovery()

        val remoteInput = RemoteInput.Builder(KEY_CODE)
            .setLabel("Código de pareamento")
            .build()
        val submitIntent = Intent(this, PairingService::class.java).apply {
            action = ACTION_SUBMIT
            putExtra("host", host)
            putExtra("port", port)
        }
        val pi = PendingIntent.getForegroundService(
            this, 1, submitIntent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val action = Notification.Action.Builder(
            android.R.drawable.ic_input_add, "Parear", pi
        ).addRemoteInput(remoteInput).build()

        val n = baseBuilder("Dispositivo encontrado", "Digite o código de pareamento aqui")
            .addAction(action)
            .build()
        notify(NID, n)
    }

    private fun handleCode(intent: Intent) {
        val code = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(KEY_CODE)?.toString()?.trim()
        val host = intent.getStringExtra("host")
        val port = intent.getIntExtra("port", 0)

        if (code.isNullOrBlank() || host == null || port == 0) {
            update("Dados de pareamento inválidos")
            return
        }

        update("Pareando...")
        AdbManager.init(applicationContext)
        AdbManager.pair(host, port, code) { ok, msg ->
            if (!ok) {
                finishWith("Falha no pareamento: $msg")
                return@pair
            }
            update("Pareado! Conectando...")
            AdbManager.connect { cok, cmsg ->
                if (!cok) {
                    finishWith("Pareou, mas não conectou: $cmsg")
                    return@connect
                }
                AdbManager.exec(
                    "pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
                ) { _, _ -> finishWith("Conectado! Recursos liberados.") }
            }
        }
    }

    private fun finishWith(message: String) {
        notify(NID_DONE, baseBuilder("Fluidez", message).build())
        stopDiscovery()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION") stopForeground(true)
        }
        stopSelf()
    }

    private fun update(text: String) {
        notify(NID, buildNotification("Pareamento ADB", text, null))
    }

    private fun buildNotification(title: String, text: String, ignored: Any?): Notification =
        baseBuilder(title, text).build()

    private fun baseBuilder(title: String, text: String): Notification.Builder =
        Notification.Builder(this, CHANNEL)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOnlyAlertOnce(true)
            .setOngoing(true)

    private fun goForeground(n: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NID, n)
        }
    }

    private fun notify(id: Int, n: Notification) {
        getSystemService(NotificationManager::class.java).notify(id, n)
    }

    private fun stopDiscovery() {
        try {
            discoveryListener?.let { nsd?.stopServiceDiscovery(it) }
        } catch (e: Exception) {
            // ignora
        }
        discoveryListener = null
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL, "Pareamento ADB", NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopDiscovery()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
