package com.example.flowtesting

import app.cash.turbine.test
import com.example.flowtesting.utils.MainDispatcherRule
import com.example.flowtesting.utils.createRepository
import com.example.flowtesting.utils.url
import com.google.common.truth.Truth.assertThat
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("UnusedPrivateMember", "BlockingMethodInNonBlockingContext")
class RepositorySimpleCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockWebServer = MockWebServer()

    @TestCaseName("[{index}] {method}")
    @Parameters(method = "provideIndexes")
    @Test
    fun `no waiting -- not passing, not flaky`(iteration: Int) {
        runTest {

            val sut = createRepository(
                mockWebServerUrl = mockWebServer.url
            )
            mockWebServer.enqueue(
                response = MockResponse()
                    .setResponseCode(200)
                    .setBody("{ \"value\": \"value1\", \"counter\": 0 }")
            )

            sut.select(selector)

            sut.data.test {
                val item = awaitItem()
                assertThat(item)
                    .isEqualTo(Result.success(Data(value = "value1")))
            }
        }
    }

    @TestCaseName("[{index}] {method}")
    @Parameters(method = "provideIndexes")
    @Test
    fun `using advanceUntilIdle -- not passing, not flaky`(iteration: Int) {
        runTest {

            val sut = createRepository(
                mockWebServerUrl = mockWebServer.url
            )
            mockWebServer.enqueue(
                response = MockResponse()
                    .setResponseCode(200)
                    .setBody("{ \"value\": \"value1\", \"counter\": 0 }")
            )

            sut.select(selector)

            sut.data.test {
                testScheduler.advanceUntilIdle()
                val item = awaitItem()
                assertThat(item)
                    .isEqualTo(Result.success(Data(value = "value1")))
            }
        }
    }

    @TestCaseName("[{index}] {method}")
    @Parameters(method = "provideIndexes")
    @Test
    fun `using instant OkHttpDispatcher -- passing, not flaky`(iteration: Int) {
        runTest {

            val sut = createRepository(
                mockWebServerUrl = mockWebServer.url,
                useInstantOkHttpDispatcher = true
            )
            mockWebServer.enqueue(
                response = MockResponse()
                    .setResponseCode(200)
                    .setBody("{ \"value\": \"value1\", \"counter\": 0 }")
            )

            sut.select(selector)

            sut.data.test {
                val item = awaitItem()
                assertThat(item)
                    .isEqualTo(Result.success(Data(value = "value1")))
            }
        }
    }

    @TestCaseName("[{index}] {method}")
    @Parameters(method = "provideIndexes")
    @Test
    fun `using Thread#sleep but not instant OkHttpDispatcher -- flaky`(iteration: Int) {
        runTest {

            val sut = createRepository(
                mockWebServerUrl = mockWebServer.url
            )
            mockWebServer.enqueue(
                response = MockResponse()
                    .setResponseCode(200)
                    .setBody("{ \"value\": \"value1\", \"counter\": 0 }")
            )

            sut.select(selector)

            sut.data.test {
                Thread.sleep(1)
                val item = awaitItem()
                assertThat(item)
                    .isEqualTo(Result.success(Data(value = "value1")))
            }
        }
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    fun provideIndexes(): Collection<Int> {
        return (1..50).toList()
    }

    companion object {
        private const val selector = "999999"
    }
}
