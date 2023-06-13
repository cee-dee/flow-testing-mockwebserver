package com.example.flowtesting.utils

import com.example.flowtesting.DataService
import com.google.gson.Gson
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

fun createRetrofitClient(
    baseUrl: String,
    useInstantOkHttpDispatcher: Boolean
): DataService {
    val okHttpClient = OkHttpClient()
        .newBuilder()
        .let {
            if (useInstantOkHttpDispatcher) {
                it.dispatcher(Dispatcher(InstantExecutorService()))
            } else {
                it
            }
        }
        .build()

    val gsonConverterFactory = GsonConverterFactory.create(Gson())

    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(gsonConverterFactory)
        .build()
        .create()
}
