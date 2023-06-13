package com.example.flowtesting.utils

import java.util.concurrent.*

class InstantExecutorService : ExecutorService {

    override fun execute(command: Runnable) = command.run()

    override fun shutdown() {
        // intentionally do nothing
    }

    override fun shutdownNow() = mutableListOf<Runnable>()

    override fun isShutdown() = false

    override fun isTerminated() = false

    override fun awaitTermination(timeout: Long, unit: TimeUnit?) = false

    override fun <T> submit(taskFn: Callable<T>): Future<T> {
        return provideLambdaExecutionAsCompletableFuture {
            taskFn.call()
        }
    }

    override fun <T> submit(task: Runnable, result: T): Future<T> {
        return provideLambdaExecutionAsCompletableFuture {
            task.run()
            result
        }
    }

    override fun submit(task: Runnable): Future<*> {
        return provideLambdaExecutionAsCompletableFuture {
            task.run()
        }
    }

    override fun <T> invokeAll(tasks: MutableCollection<out Callable<T>>): MutableList<Future<T>> {
        return tasks.map { task ->
            provideLambdaExecutionAsCompletableFuture {
                task.call()
            }
        }.toMutableList()
    }

    override fun <T> invokeAll(tasks: MutableCollection<out Callable<T>>, timeout: Long, unit: TimeUnit): MutableList<Future<T>> {
        return tasks.map { task ->
            provideLambdaExecutionAsCompletableFuture {
                task.call()
            }
        }.toMutableList()
    }

    override fun <T> invokeAny(tasks: MutableCollection<out Callable<T>>): T {
        val futures = tasks.map { task ->
            provideLambdaExecutionAsCompletableFuture {
                task.call()
            }
        }
        @Suppress("UNCHECKED_CAST")
        return CompletableFuture.anyOf(*futures.toTypedArray())
            .get() as T
    }

    override fun <T> invokeAny(tasks: MutableCollection<out Callable<T>>, timeout: Long, unit: TimeUnit): T {
        val futures = tasks.map { task ->
            provideLambdaExecutionAsCompletableFuture {
                task.call()
            }
        }
        @Suppress("UNCHECKED_CAST")
        return CompletableFuture.anyOf(*futures.toTypedArray())
            .get(timeout, unit) as T
    }

    private fun <T> provideLambdaExecutionAsCompletableFuture(lambda: () -> T): CompletableFuture<T> {

        val future = CompletableFuture<T>()

        execute {
            try {
                val result = lambda()
                future.complete(result)
            } catch (e: Throwable) {
                future.completeExceptionally(e)
            }
        }

        return future
    }
}
