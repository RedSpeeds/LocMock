package com.redvirtualcreations.locmock.networking

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiConfig {
    companion object {
        fun getElevationApiService() : ElevationService{
            val loggingintercept : HttpLoggingInterceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
            val client = OkHttpClient.Builder().addInterceptor(loggingintercept).build()
            val retrofit : Retrofit = Retrofit.Builder().baseUrl("https://api.open-elevation.com/").addConverterFactory(GsonConverterFactory.create()).client(client).build()

            return retrofit.create(ElevationService::class.java)
        }
    }
}