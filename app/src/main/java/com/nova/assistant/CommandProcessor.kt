package com.nova.assistant

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper
import java.util.Locale

class CommandProcessor(
    private val context: Context,
    private val onReply: (String) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())

    private val openTriggers = listOf("kholo", "khol do", "open", "chalao", "start karo")

    fun process(command: String) {
        val lower = command.lowercase(Locale.getDefault())
        val openTrigger = openTriggers.firstOrNull { lower.contains(it) }

        if (openTrigger != null) {
            val appName = lower.replace(openTrigger, "").trim()
            if (tryOpenApp(appName)) {
                onReply("$appName khol raha hoon")
                return
            } else {
                onReply("Mujhe '$appName' naam ki app nahi mili")
                return
            }
        }

        askAi(command)
    }

    private fun tryOpenApp(spokenName: String): Boolean {
        if (spokenName.isBlank()) return false
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(0)

        val match = installedApps.firstOrNull { app: ApplicationInfo ->
            val label = pm.getApplicationLabel(app).toString().lowercase(Locale.getDefault())
            label == spokenName || label.contains(spokenName) || spokenName.contains(label)
        } ?: return false

        val launchIntent = pm.getLaunchIntentForPackage(match.packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return true
    }

    private fun askAi(question: String) {
        Thread {
            val apiKey = context.getSharedPreferences("nova_prefs", Context.MODE_PRIVATE)
                .getString("api_key", "") ?: ""
            val answer = if (apiKey.isBlank()) {
                "API key set nahi hai. App kholain aur key save karein."
            } else {
                AiClient.chat(apiKey, question)
            }
            handler.post { onReply(answer) }
        }.start()
    }
}
