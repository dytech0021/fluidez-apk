package com.exemplo.fluidez.system

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast

/** Abre telas de configuração do sistema usando "Intents". */
object SettingsNavigator {

    fun open(context: Context, action: String, fallback: String = Settings.ACTION_SETTINGS) {
        try {
            val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            runCatching {
                context.startActivity(
                    Intent(fallback).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.onFailure {
                Toast.makeText(context, "Não foi possível abrir essa tela", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openDeveloperOptions(context: Context) =
        open(context, Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, Settings.ACTION_DEVICE_INFO_SETTINGS)

    fun openStorageSettings(context: Context) =
        open(context, Settings.ACTION_INTERNAL_STORAGE_SETTINGS)

    fun openBatterySettings(context: Context) =
        open(context, Settings.ACTION_BATTERY_SAVER_SETTINGS)

    fun openAppsList(context: Context) =
        open(context, Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
}
