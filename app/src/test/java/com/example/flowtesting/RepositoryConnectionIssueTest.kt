package com.example.flowtesting

import app.cash.turbine.test
import com.example.flowtesting.utils.MainDispatcherRule
import com.example.flowtesting.utils.assertHasException
import com.example.flowtesting.utils.createRepository
import com.example.flowtesting.utils.url
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.ConnectException

@RunWith(JUnitParamsRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("UnusedPrivateMember", "BlockingMethodInNonBlockingContext")
class RepositoryConnectionIssueTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockWebServer = MockWebServer()

    @TestCaseName("[{index}] {method}")
    @Parameters(method = "provideIndexes")
    @Test
    fun `not flaky -- verify failure result due to connection error fails using plain FlowTurbine`(iteration: Int) {
        mockWebServer.start()
        mockWebServer.shutdown()
        runTest {

            val sut = createRepository(mockWebServer.url)

            sut.select(selector)

            sut.data.test {
                val item = awaitItem()
                verifyFailingDueToConnectionIssue(item)
            }
        }
    }

    @TestCaseName("[{index}] {method}")
    @Parameters(method = "provideIndexes")
    @Test
    fun `not flaky -- verify failure result due to connection error succeeds using Thread#sleep(50)`(iteration: Int) {
        mockWebServer.start()
        mockWebServer.shutdown()
        runTest {

            val sut = createRepository(mockWebServer.url)

            sut.select(selector)

            sut.data.test {
                Thread.sleep(50)
                val item = awaitItem()
                verifyFailingDueToConnectionIssue(item)
            }
        }
    }

    @TestCaseName("[{index}] {method}")
    @Parameters(method = "provideIndexes")
    @Test
    fun `flaky -- verify failure result due to connection error succeeds using Thread#sleep(1)`(iteration: Int) {
        mockWebServer.start()
        mockWebServer.shutdown()
        runTest {

            val sut = createRepository(mockWebServer.url)

            sut.select(selector)

            sut.data.test {
                Thread.sleep(1)
                val item = awaitItem()
                verifyFailingDueToConnectionIssue(item)
            }
        }
    }

    @TestCaseName("[{index}] {method}")
    @Parameters(method = "provideIndexes")
    @Test
    fun `not flaky -- verify failure result due to connection error fails using advanceUntilIdle`(iteration: Int) {
        mockWebServer.start()
        mockWebServer.shutdown()
        runTest {

            val sut = createRepository(mockWebServer.url)

            sut.select(selector)

            sut.data.test {
                testScheduler.advanceUntilIdle()
                val item = awaitItem()
                verifyFailingDueToConnectionIssue(item)
            }
        }
    }

    @Test
    @TestCaseName("[{index}] {method}")
    @Parameters(method = "provideIndexes")
    fun `flaky -- verify failure result due to connection error using instant OkHttp dispatcher and advanceUntilIdle`(iteration: Int) {
        mockWebServer.start()
        mockWebServer.shutdown()
        runTest {

            val sut = createRepository(
                mockWebServerUrl = mockWebServer.url,
                useInstantOkHttpDispatcher = true
            )

            sut.select(selector)

            sut.data.test {
                testScheduler.advanceUntilIdle()
                val item = awaitItem()
                verifyFailingDueToConnectionIssue(item)
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

    private fun verifyFailingDueToConnectionIssue(item: Result<Data>) {
        item.assertHasException(ConnectException::class.java, "Failed to connect to")
    }

    companion object {
        private const val selector = "1234567890"
    }
}
