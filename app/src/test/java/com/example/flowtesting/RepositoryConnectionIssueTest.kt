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

        /*
        While the RepositorySimpleCaseTest#`not flaky -- succeeding simple case using instant OkHttpDispatcher`
        test, where no exceptions on the Network layer a thrown, reliably succeed using the instant execution
        OkHttpDispatcher, in the exception case, this still doesn't suffice.

        With some nasty Thread.sleep(20) it seems to work "reliably on my machine". Still, that's
        a crappy half-baked solution.

        The reason for this could be related to some code I found in Retrofit2:

            /**
             * Force the calling coroutine to suspend before throwing [this].
             *
             * This is needed when a checked exception is synchronously caught in a [java.lang.reflect.Proxy]
             * invocation to avoid being wrapped in [java.lang.reflect.UndeclaredThrowableException].
             *
             * The implementation is derived from:
             * https://github.com/Kotlin/kotlinx.coroutines/pull/1667#issuecomment-556106349
             */
            internal suspend fun Exception.suspendAndThrow(): Nothing {
              suspendCoroutineUninterceptedOrReturn<Nothing> { continuation ->
                Dispatchers.Default.dispatch(continuation.context, Runnable {
                  continuation.intercepted().resumeWithException(this@suspendAndThrow)
                })
                COROUTINE_SUSPENDED
              }
            }

          It is called in HttpServiceMethod#adapt whenever an Exception occurs during a
          network request.
       */
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
