package com.exemplo.fluidez.system

/**
 * Abstração de um "executor privilegiado" — pode ser o Shizuku ou o ADB embutido.
 * Assim o resto do app não precisa saber qual backend está em uso.
 */
interface PrivShell {
    /** Está pronto para executar comandos privilegiados? */
    fun isReady(): Boolean

    /** Executa um comando shell. onResult(sucesso, saída). */
    fun exec(command: String, onResult: (Boolean, String) -> Unit)
}
