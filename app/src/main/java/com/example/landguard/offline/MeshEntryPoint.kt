package com.example.landguard.offline

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Lets the mesh service's static helpers reach the one [NearbyMeshManager]
 * singleton from a plain Context (a BroadcastReceiver, or before the service is
 * constructed) without building a second mesh.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface MeshEntryPoint {

    fun nearbyMeshManager(): NearbyMeshManager

    companion object {
        fun resolve(context: Context): NearbyMeshManager =
            EntryPointAccessors
                .fromApplication(context.applicationContext, MeshEntryPoint::class.java)
                .nearbyMeshManager()
    }
}
