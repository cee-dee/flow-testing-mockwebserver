package com.example.flowtesting.utils

import com.example.flowtesting.Repository
import kotlinx.coroutines.test.UnconfinedTestDispatcher

fun createRepository(
    mockWebServerUrl: String,
    useInstantOkHttpDispatcher: Boolean = false
): Repository {
    return Repository(
        dataService = createRetrofitClient(mockWebServerUrl, useInstantOkHttpDispatcher),
        dispatcher = UnconfinedTestDispatcher()
    )
}
