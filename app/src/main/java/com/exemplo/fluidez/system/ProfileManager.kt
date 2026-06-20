package com.exemplo.fluidez.system

import android.content.Context
import com.exemplo.fluidez.data.Profile

object ProfileManager {

    val GAME    = Profile("game",    "Jogo",     animationScale = 0f, batterySaver = false)
    val ECONOMY = Profile("economy", "Economia", animationScale = 0f, batterySaver = true)
    val NORMAL  = Profile("normal",  "Normal",   animationScale = 1f, batterySaver = false)

    val all = listOf(GAME, ECONOMY, NORMAL)

    fun byId(id: String): Profile = all.firstOrNull { it.id == id } ?: NORMAL

    /**
     * Aplica o perfil: animação (nativo) + economia de bateria (via Shizuku).
     */
    fun apply(
        context: Context,
        shell: PrivShell,
        profile: Profile,
        onDone: (Boolean) -> Unit
    ) {
        val animOk = AnimationController.setScale(context, profile.animationScale)
        val value = if (profile.batterySaver) 1 else 0
        shell.exec("settings put global low_power $value") { saverOk, _ ->
            onDone(animOk && saverOk)
        }
    }
}
