package com.example.flowtesting.utils

import app.cash.turbine.FlowTurbine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.TestCoroutineScheduler

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <T> FlowTurbine<T>.awaitItemWithRetry(scheduler: TestCoroutineScheduler): T {
    val turbine = this
    return coroutineScope {
        repeatAwait(scheduler, turbine, 3)
    }
}

@Suppress("SuspendFunctionOnCoroutineScope")
@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun <T> CoroutineScope.repeatAwait(
    scheduler: TestCoroutineScheduler,
    turbine: FlowTurbine<T>,
    allowedRecursiveCalls: Int
): T {
    val job = createAsyncJob(this, scheduler, turbine)
    return try {
        job.await()
    } catch (t: Throwable) {
        if (allowedRecursiveCalls > 0) {
            // This call is actually required, otherwise the test repetition will
            // fail, although testScheduler.runCurrent() is called.
            @Suppress("BlockingMethodInNonBlockingContext")
            Thread.sleep(20)
            repeatAwait(scheduler, turbine, allowedRecursiveCalls - 1)
        } else {
            throw t
        }
    }
}

@Suppress("DeferredIsResult")
@OptIn(ExperimentalCoroutinesApi::class)
private fun <T> createAsyncJob(
    coroutineScope: CoroutineScope,
    scheduler: TestCoroutineScheduler,
    flowTurbine: FlowTurbine<T>
) = coroutineScope.async {
    scheduler.runCurrent()
    val item = flowTurbine.awaitItem()
    item
}
