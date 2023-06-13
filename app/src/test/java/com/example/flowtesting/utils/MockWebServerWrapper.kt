package com.example.flowtesting.utils

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.QueueDispatcher

class MockWebServerWrapper {

    private val mockWebServerDispatcher = object : QueueDispatcher() {

        fun clearResponseQueue() {
            responseQueue.clear()
        }
    }

    private var state: State = State.Uninitialized

    operator fun invoke(): MockWebServer {
        return when (val immutableState = state) {
            is State.Active -> immutableState.mockWebServer
            is State.Shutdown -> immutableState.mockWebServer
            State.Uninitialized -> start()
            State.Finished -> throw IllegalStateException("Finish state reached, cannot provide MockWebServer")
        }
    }

    private fun start(): MockWebServer {
        return when (state) {
            State.Uninitialized -> {
                val newMockWebServer = createNewMockWebServer()
                newMockWebServer.start()
                state = State.Active(newMockWebServer)
                newMockWebServer
            }
            is State.Active -> {
                throw IllegalStateException("MockWebServer is already started, are you aware of that?")
            }
            is State.Shutdown -> {
                throw IllegalStateException("MockWebServer is shutdown, call restart() to restart it")
            }
            State.Finished -> {
                throw IllegalStateException("Finished state reached, cannot start MockWebServer")
            }
        }
    }

    private fun createNewMockWebServer(): MockWebServer {
        return MockWebServer().apply {
            dispatcher = mockWebServerDispatcher
        }
    }

    fun immediatelyShutdown() {
        // Ensure MockWebServer is started, in case of being still Uninitialized
        this()
        shutdown()
    }

    private fun shutdown() {
        when (val immutableState = state) {
            State.Uninitialized -> {
                throw IllegalStateException("A not even initialized MockWebServer cannot be shut down")
            }
            is State.Active -> {
                immutableState.mockWebServer.shutdown()
                clearEnqueuedResponses()
                state = State.Shutdown(
                    mockWebServer = immutableState.mockWebServer,
                    inactivePort = immutableState.mockWebServer.port
                )
            }
            is State.Shutdown -> {
                throw IllegalStateException("Already shut down, are you aware of that?")
            }
            State.Finished -> {
                throw IllegalStateException("Finished state reached, cannot perform another shutdown()")
            }
        }
    }

    private fun clearEnqueuedResponses() {
        mockWebServerDispatcher.clearResponseQueue()
    }

    fun restart() {
        when (val immutableState = state) {
            State.Uninitialized -> {
                throw IllegalStateException("You cannot restart a MockWebServer that has not yet been started")
            }
            is State.Active -> {
                throw IllegalStateException("MockWebServer is already started, are you aware of that?")
            }
            is State.Shutdown -> {
                val newMockWebServer = createNewMockWebServer()
                newMockWebServer.start(immutableState.inactivePort)
                clearEnqueuedResponses()
                state = State.Active(newMockWebServer)
            }
            State.Finished -> {
                throw IllegalStateException("Finished state reached, cannot restart")
            }
        }
    }

    fun finish() {
        when (state) {
            State.Uninitialized -> {
                // intentionally do nothing
            }
            is State.Active -> {
                this().shutdown()
                clearEnqueuedResponses()
                state = State.Finished
            }
            is State.Shutdown -> {
                // no need to shutdown again
                state = State.Finished
            }
            State.Finished -> {
                throw IllegalStateException("Already in Finished state")
            }
        }
    }

    fun enqueueResponse(
        response: MockResponse
    ) {
        mockWebServerDispatcher.enqueueResponse(
            response
        )
    }

    private sealed class State {

        object Uninitialized : State()

        data class Active(
            val mockWebServer: MockWebServer
        ) : State()

        data class Shutdown(
            val mockWebServer: MockWebServer,
            val inactivePort: Int
        ) : State()

        object Finished : State()
    }
}
