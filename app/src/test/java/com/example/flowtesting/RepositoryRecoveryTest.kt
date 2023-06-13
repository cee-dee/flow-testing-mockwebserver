package com.example.flowtesting

import app.cash.turbine.test
import com.example.flowtesting.utils.MainDispatcherRule
import com.example.flowtesting.utils.MockWebServerWrapper
import com.example.flowtesting.utils.assertHasException
import com.example.flowtesting.utils.awaitItemWithRetry
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import java.net.ConnectException

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryRecoveryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    val mockWebServerWrapper = MockWebServerWrapper()

    @Test
    fun `throws ConnectException when there is no network connection`() {
        mockWebServerWrapper.immediatelyShutdown()
        runTest {

            val sut = createRepositoryImpl()

            sut.select(selector1)

            sut.data.test {
                val item1 = awaitItemWithRetry(testScheduler)
                verifyFailingDueToConnectionIssue(item1)
                mockWebServerWrapper.restart()
                mockWebServerWrapper.enqueueResponse(
                    response = MockResponse()
                        .setResponseCode(200)
                        .setBody("{ \"value\": \"value1\", \"counter\": 0 }")
                )
                sut.select(selector2)
                val item2 = awaitItemWithRetry(testScheduler)
                assertThat(item2)
                    .isEqualTo(Result.success(Data(value = "value1")))
            }
        }
    }

    @Test
    fun `success`() {
        runTest {

            val sut = createRepositoryImpl()
            mockWebServerWrapper.enqueueResponse(
                response = MockResponse()
                    .setResponseCode(200)
                    .setBody("{ \"value\": \"value1\", \"counter\": 0 }")
            )

            sut.select(selector2)

            sut.data.test {
                testScheduler.advanceUntilIdle()
                testScheduler.runCurrent()
                val item2 = awaitItem()
                assertThat(item2)
                    .isEqualTo(Result.success(Data(value = "value1")))
            }
        }
    }

    @After
    fun tearDown() {
        mockWebServerWrapper.finish()
    }

    private fun verifyFailingDueToConnectionIssue(item: Result<Data>) {
        item.assertHasException(ConnectException::class.java, "Failed to connect to")
    }

    private fun createRepositoryImpl(): Repository {
        return Repository(
            dataService = createRetrofitClient(),
            dispatcher = UnconfinedTestDispatcher()
        )
    }

    private fun createRetrofitClient(): DataService {
        val okHttpClient = OkHttpClient()
            .newBuilder()
            .build()

        val gsonConverterFactory = GsonConverterFactory.create(Gson())

        return Retrofit.Builder()
            .baseUrl(mockWebServerWrapper().url("/"))
            .client(okHttpClient)
            .addConverterFactory(gsonConverterFactory)
            .build()
            .create()
    }

    companion object {
        private const val selector1 = "561273"
        private const val selector2 = "999999"
    }
}
