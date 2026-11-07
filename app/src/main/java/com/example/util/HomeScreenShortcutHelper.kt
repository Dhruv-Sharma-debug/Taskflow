package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.widget.Toast
import com.example.MainActivity
import com.example.R

object HomeScreenShortcutHelper {

    private const val SHORTCUT_ID = "taskflow_home_shortcut_v2"

    fun requestPinToHomeScreen(context: Context) {
        try {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_MAIN
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                val pinShortcutInfo = ShortcutInfo.Builder(context, SHORTCUT_ID)
                    .setShortLabel(context.getString(R.string.app_name))
                    .setLongLabel(context.getString(R.string.app_name))
                    .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
                    .setIntent(launchIntent)
                    .build()

                val requested = shortcutManager.requestPinShortcut(pinShortcutInfo, null)
                if (!requested) {
                    Toast.makeText(
                        context,
                        "TaskFlow is already pinned or check your App Drawer.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    context,
                    "Your launcher does not support direct pinning. Look for TaskFlow in the App Drawer.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Unable to pin shortcut: ${e.localizedMessage ?: "Check App Drawer"}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
