package com.exemplo.fluidez.system

import android.content.Context
import android.os.Handler
import android.os.Looper

/** Backend privilegiado usando o ADB embutido (sem Shizuku). */
object AdbManager : PrivShell {

    private lateinit var appContext: Context

    fun init(context: Context) {
        if (!::appContext.isInitialized) {
            appContext = context.applicationContext
            // Gera/carrega as chaves fora da thread principal.
            Thread { runCatching { AdbConnectionManager.getInstance(appContext) } }.start()
        }
    }

    override fun isReady(): Boolean = try {
        AdbConnectionManager.peek()?.isConnected == true
    } catch (e: Exception) {
        false
    }

    override fun exec(command: String, onResult: (Boolean, String) -> Unit) {
        Thread {
            val result = try {
                val mgr = AdbConnectionManager.getInstance(appContext)
                if (!mgr.isConnected) {
                    "ERR:ADB não conectado"
                } else {
                    // Acrescenta um marcador com o código de saída real do comando.
                    val full = "$command; echo __RC=\$?"
                    val stream = mgr.openStream("shell:$full")
                    val sb = StringBuilder()
                    try {
                        val input = stream.openInputStream()
                        val buf = ByteArray(4096)
                        while (true) {
                            // O fechamento do stream (fim do comando) pode vir como exceção.
                            val n = try { input.read(buf) } catch (e: Exception) { -1 }
                            if (n < 0) break
                            sb.append(String(buf, 0, n))
                        }
                    } finally {
                        runCatching { stream.close() }
                    }
                    val out = sb.toString()
                    val rc = Regex("__RC=(-?\\d+)").find(out)?.groupValues?.get(1)?.toIntOrNull()
                    val clean = out.substringBefore("__RC=").trim()
                    when {
                        rc == 0 -> "OK:$clean"
                        rc != null -> "ERR:${clean.ifBlank { "código $rc" }}"
                        else -> "OK:$clean" // sem marcador: o comando rodou e fechou o stream
                    }
                }
            } catch (e: Exception) {
                "ERR:${e.message}"
            }
            post { onResult(result.startsWith("OK"), result) }
        }.start()
    }

    /** Pareia com a depuração sem fio (host:porta + código). */
    fun pair(host: String, port: Int, code: String, onResult: (Boolean, String) -> Unit) {
        Thread {
            val result = try {
                val ok = AdbConnectionManager.getInstance(appContext).pair(host, port, code)
                if (ok) "OK:pareado" else "ERR:pareamento falhou"
            } catch (e: Exception) {
                "ERR:${e.message}"
            }
            post { onResult(result.startsWith("OK"), result) }
        }.start()
    }

    /** Conecta ao adbd local (descoberta automática via mDNS). */
    fun connect(onResult: (Boolean, String) -> Unit) {
        Thread {
            val result = try {
                val ok = AdbConnectionManager.getInstance(appContext).autoConnect(appContext, 15000)
                if (ok) "OK:conectado" else "ERR:conexão falhou"
            } catch (e: Exception) {
                "ERR:${e.message}"
            }
            post { onResult(result.startsWith("OK"), result) }
        }.start()
    }

    private fun post(block: () -> Unit) = Handler(Looper.getMainLooper()).post(block)
}
