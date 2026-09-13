package com.vakitplus.app

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object AppLocation {
    @SuppressLint("MissingPermission")
    suspend fun getLastKnown(context: Context): Location? =
        suspendCancellableCoroutine { cont ->
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = lm.getProviders(true)
            val location = providers.asSequence()
                .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
                .maxByOrNull { it.time }
            cont.resume(location)
        }

    fun isLocationEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return runCatching {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }.getOrDefault(false)
    }

    fun settingsIntent(context: Context) =
        android.content.Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
}
