package com.callerinfo.reciever

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.callerinfo.service.CallDetectionService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Guard — check permissions before starting service
        val hasPhonePermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.READ_PHONE_STATE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(context)
        } else true

        if (!hasPhonePermission || !hasOverlayPermission) return  // bail silently

        val svc = Intent(context, CallDetectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(svc)
        else
            context.startService(svc)
    }
}