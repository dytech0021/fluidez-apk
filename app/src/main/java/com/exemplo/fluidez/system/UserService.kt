package com.exemplo.fluidez.system

import com.exemplo.fluidez.IUserService
import kotlin.system.exitProcess

/**
 * Este código roda DENTRO do processo do Shizuku (com poderes de ADB/shell).
 * É aqui que o comando privilegiado é executado de verdade.
 */
class UserService : IUserService.Stub() {

    override fun destroy() = exitProcess(0)

    override fun exit() = destroy()

    override fun exec(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val out = process.inputStream.bufferedReader().readText()
            val err = process.errorStream.bufferedReader().readText()
            process.waitFor()
            if (err.isBlank()) "OK:$out" else "ERR:$err"
        } catch (e: Exception) {
            "ERR:${e.message}"
        }
    }
}
