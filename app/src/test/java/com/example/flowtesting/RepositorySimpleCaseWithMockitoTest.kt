package com.example.flowtesting

import app.cash.turbine.test
import com.example.flowtesting.utils.MainDispatcherRule
import com.example.flowtesting.utils.createFailingStubBackedRepository
import com.example.flowtesting.utils.createSucceedingStubBackedRepository
import com.google.common.truth.Truth.assertThat
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("UnusedPrivateMember", "BlockingMethodInNonBlockingContext")
class RepositorySimpleCaseWithMockitoTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @TestCaseName("[{index}] {method}")
    @Parameters(method = "provideIndexes")
    @Test
    fun `not flaky -- succeeding simple case`(iteration: Int) {
        runTest {

            val sut = createSucceedingStubBackedRepository()

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
    fun `not flaky -- succeeding exception case`(iteration: Int) {
        runTest {

            val sut = createFailingStubBackedRepository()

            sut.select(selector)

            sut.data.test {
                val item = awaitItem()
                assertThat(item.isFailure).isTrue()
                assertThat(item.exceptionOrNull()).isInstanceOf(RuntimeException::class.java)
            }
        }
    }

    fun provideIndexes(): Collection<Int> {
        return (1..50).toList()
    }

    companion object {
        private const val selector = "999999"
    }
}
