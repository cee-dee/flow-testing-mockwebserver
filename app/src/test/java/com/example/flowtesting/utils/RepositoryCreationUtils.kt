package com.example.flowtesting.utils

import com.example.flowtesting.Data
import com.example.flowtesting.DataService
import com.example.flowtesting.Repository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

fun createRepository(
    mockWebServerUrl: String,
    useInstantOkHttpDispatcher: Boolean = false
): Repository {
    return Repository(
        dataService = createRetrofitClient(mockWebServerUrl, useInstantOkHttpDispatcher),
        dispatcher = UnconfinedTestDispatcher()
    )
}

fun createSucceedingStubBackedRepository(): Repository {
    val dataService = mock(DataService::class.java)
    runBlocking {
        `when`(dataService.loadData(anyString())).thenReturn(
            Data("value1")
        )
    }
    return Repository(
        dataService = dataService,
        dispatcher = UnconfinedTestDispatcher()
    )
}

fun createFailingStubBackedRepository(): Repository {
    val dataService = mock(DataService::class.java)
    runBlocking {
        `when`(dataService.loadData(anyString())).thenThrow(RuntimeException::class.java)
    }
    return Repository(
        dataService = dataService,
        dispatcher = UnconfinedTestDispatcher()
    )
}
