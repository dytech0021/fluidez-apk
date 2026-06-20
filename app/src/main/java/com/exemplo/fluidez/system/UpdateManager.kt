package com.exemplo.fluidez.system

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Verifica e instala atualizações do Fluidez via releases do GitHub. */
object UpdateManager {
    private const val API = "https://api.github.com/repos/dytech0021/fluidez-apk/releases/latest"

    data class Info(val version: String, val apkUrl: String)

    fun currentVersion(context: Context): String =
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0" }
        catch (e: Exception) { "0" }

    /** Consulta o GitHub. Retorna Info se houver versão MAIS NOVA, senão null. */
    fun check(context: Context, onResult: (Info?) -> Unit) {
        Thread {
            val info = try {
                val conn = (URL(API).openConnection() as HttpURLConnection)
                conn.setRequestProperty("User-Agent", "Fluidez")
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val text = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(text)
                val tag = json.getString("tag_name").removePrefix("v")
                var apkUrl = ""
                val assets = json.getJSONArray("assets")
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.getString("name").endsWith(".apk")) {
                        apkUrl = a.getString("browser_download_url"); break
                    }
                }
                if (apkUrl.isNotBlank() && isNewer(tag, currentVersion(context))) Info(tag, apkUrl) else null
            } catch (e: Exception) {
                null
            }
            Handler(Looper.getMainLooper()).post { onResult(info) }
        }.start()
    }

    /** Baixa o APK e instala (silencioso via Shizuku, ou pelo instalador do sistema). */
    fun downloadAndInstall(
        context: Context, shell: PrivShell, info: Info, onStatus: (String) -> Unit
    ) {
        Thread {
            try {
                val file = File(context.externalCacheDir, "fluidez-update.apk")
                URL(info.apkUrl).openStream().use { input ->
                    file.outputStream().use { out -> input.copyTo(out) }
                }
                Handler(Looper.getMainLooper()).post {
                    if (shell.isReady()) {
                        onStatus("Instalando...")
                        shell.exec("pm install -r \"${file.absolutePath}\"") { ok, _ ->
                            if (ok) onStatus("Atualizado! Reabra o app.")
                            else openInstaller(context, file, onStatus)
                        }
                    } else {
                        openInstaller(context, file, onStatus)
                    }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { onStatus("Falha no download: ${e.message}") }
            }
        }.start()
    }

    private fun openInstaller(context: Context, file: File, onStatus: (String) -> Unit) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            onStatus("Abrindo instalador...")
        } catch (e: Exception) {
            onStatus("Não consegui abrir o instalador: ${e.message}")
        }
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val l = latest.split(".").mapNotNull { it.toIntOrNull() }
        val c = current.split(".").mapNotNull { it.toIntOrNull() }
        val n = maxOf(l.size, c.size)
        for (i in 0 until n) {
            val a = l.getOrElse(i) { 0 }
            val b = c.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }
}
