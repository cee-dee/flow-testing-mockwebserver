package com.example.flowtesting

import retrofit2.http.GET
import retrofit2.http.Path

interface DataService {

    @GET("some/endpoint/{selector}")
    suspend fun loadData(@Path("selector") selector: String): Data
}
