package com.redvirtualcreations.locmock.networking

import com.redvirtualcreations.locmock.jsonData.Response
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ElevationService {
    @GET("api/v1/lookup/{lat},{lon}")
    fun getElevation(@Path("lat") lat:Double, @Path("lon") lon:Double) : Call<Response>


}