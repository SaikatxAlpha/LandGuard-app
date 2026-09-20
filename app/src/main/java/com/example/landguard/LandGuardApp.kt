package com.example.landguard

import android.app.Application
import com.example.landguard.data.alerts.AlertIngestor
import com.example.landguard.data.alerts.AlertSyncManager
import com.example.landguard.offline.MeshService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LandGuardApp : Application() {

    @Inject lateinit var alertIngestor: AlertIngestor
    @Inject lateinit var alertSync: AlertSyncManager

    override fun onCreate() {
        super.onCreate()
        // One alert pipeline for FCM, backend sync and the offline mesh.
        alertIngestor.start()
        alertSync.start()
        // Whatever started this process — launcher, FCM, boot — the offline mesh
        // comes up with it, so the phone can receive and pass on alerts without
        // anybody opening the app.
        MeshService.ensureRunning(this)
    }
}
