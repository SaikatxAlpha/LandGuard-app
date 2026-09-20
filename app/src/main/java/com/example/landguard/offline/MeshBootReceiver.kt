package com.example.landguard.offline

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Brings the offline mesh back after a reboot without the user having to open
 * the app — a phone that restarts mid-disaster must still relay alerts.
 * Receiving BOOT_COMPLETED is one of the exemptions that allows starting a
 * foreground service from the background on Android 12+.
 */
class MeshBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        Log.i("LandGuardMesh", "Restoring offline mesh after ${intent.action}")
        MeshService.ensureRunning(context)
    }
}
