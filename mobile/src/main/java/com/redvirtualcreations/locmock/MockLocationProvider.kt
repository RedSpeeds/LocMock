package com.redvirtualcreations.locmock

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.redvirtualcreations.locmock.jsonData.Response
import com.redvirtualcreations.locmock.networking.ApiConfig
import retrofit2.Call
import retrofit2.Callback

class MockLocationProvider(providerName: String, ctx: Context) {
    private var powerUsage = 1;
    private var accuracy = 2
    private val lm: LocationManager
    private val providerName: String
    private val ctx: Context
    private val pref : SharedPreferences

    init {
        if (Build.VERSION.SDK_INT >= 31) {
            powerUsage = ProviderProperties.POWER_USAGE_LOW
            accuracy = ProviderProperties.ACCURACY_FINE
        }
        lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        this.providerName = providerName
        this.ctx = ctx
        pref = PreferenceManager.getDefaultSharedPreferences(ctx)
        startup()
    }

    @SuppressLint("WrongConstant")
    private fun startup() {
        val hasAltitude = pref.getBoolean("enableAltitude", true)
        try {
            lm.addTestProvider(providerName, false, false, false, false, hasAltitude, true, true, powerUsage, accuracy)
            lm.setTestProviderEnabled(providerName, true)
        }
        catch (e : SecurityException){
            Toast.makeText(ctx, "Missing mock permissions", Toast.LENGTH_LONG).show()
        }
    }

    public fun pushLocation(lat : Double, lon : Double){
        val mockLocation : Location = Location(providerName)
        mockLocation.latitude = lat
        mockLocation.longitude = lon
        if(pref.getBoolean("enableAltitude", true)) {
            val request = ApiConfig.getElevationApiService().getElevation(lat, lon)
            request.enqueue(object : Callback<Response> {
                override fun onResponse(
                    call: Call<Response>,
                    response: retrofit2.Response<Response>
                ) {
                    if ((response.body()?.results?.size ?: 0) > 0){
                        mockLocation.altitude = response.body()!!.results!![0]!!.elevation!!.toDouble()
                    }
                }

                override fun onFailure(call: Call<Response>, t: Throwable) {
                    mockLocation.altitude = 0.toDouble()
                }
            })
        }
        mockLocation.time = System.currentTimeMillis()
        mockLocation.speed = 0.01F
        mockLocation.bearing = 1F
        val accuracy = pref.getString("accuracy", "1")!!.toInt()

        mockLocation.accuracy = accuracy.toFloat()
        mockLocation.bearingAccuracyDegrees = 0.1F
        mockLocation.verticalAccuracyMeters = 0.1F
        mockLocation.speedAccuracyMetersPerSecond = 0.01f
        mockLocation.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        lm.setTestProviderLocation(providerName, mockLocation)
    }
    public fun shutdown(){
        lm.removeTestProvider(providerName)
    }
}