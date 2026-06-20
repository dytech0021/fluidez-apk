package com.exemplo.fluidez.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.exemplo.fluidez.IUserService
import rikka.shizuku.Shizuku

class ShizukuManager(private val context: Context) : PrivShell {

    companion object {
        const val PERMISSION_CODE = 1001
        const val SHIZUKU_PKG = "moe.shizuku.privileged.api"
        const val DOWNLOAD_URL = "https://github.com/RikkaApps/Shizuku/releases/latest"
    }

    /** A Shizuku está instalada no aparelho? */
    fun isInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(SHIZUKU_PKG, 0)
        true
    } catch (e: Exception) {
        false
    }

    /** O Shizuku está instalado e ativo (serviço rodando)? */
    fun isRunning(): Boolean =
        try { Shizuku.pingBinder() } catch (e: Exception) { false }

    /** O usuário já autorizou o nosso app no Shizuku? */
    fun hasPermission(): Boolean =
        isRunning() && !Shizuku.isPreV11() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    override fun isReady(): Boolean = hasPermission()

    fun requestPermission() {
        if (isRunning() && !Shizuku.isPreV11()) {
            Shizuku.requestPermission(PERMISSION_CODE)
        }
    }

    /** Abre a página de download da Shizuku (GitHub). */
    fun openDownloadPage() {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(DOWNLOAD_URL))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** Abre o app Shizuku (ou o download, se não estiver instalado). */
    fun openShizukuApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PKG)
        if (intent != null) {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            openDownloadPage()
        }
    }

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, UserService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("shizuku-service")
        .debuggable(false)
        .version(1)

    /** Roda qualquer comando shell pelo Shizuku (fora da thread principal). */
    override fun exec(command: String, onResult: (Boolean, String) -> Unit) {
        if (!hasPermission()) { onResult(false, "Sem permissão do Shizuku"); return }

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder?) {
                val conn = this
                Thread {
                    val output = try {
                        if (binder != null && binder.pingBinder())
                            IUserService.Stub.asInterface(binder).exec(command)
                        else "ERR:binder inválido"
                    } catch (e: Exception) { "ERR:${e.message}" }

                    Handler(Looper.getMainLooper()).post {
                        onResult(output.startsWith("OK"), output)
                    }
                    runCatching { Shizuku.unbindUserService(userServiceArgs, conn, true) }
                }.start()
            }
            override fun onServiceDisconnected(name: ComponentName) {}
        }
        Shizuku.bindUserService(userServiceArgs, connection)
    }

    fun grantSecureSettings(onResult: (Boolean) -> Unit) =
        exec("pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS") { ok, _ ->
            onResult(ok)
        }
}
