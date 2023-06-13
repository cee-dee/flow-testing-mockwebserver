package com.example.flowtesting

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class Repository @Inject constructor(
    private val dataService: DataService,
    dispatcher: CoroutineDispatcher
) {

    private val selectorFlow: MutableStateFlow<String?> =
        MutableStateFlow(null)

    val data: Flow<Result<Data>> =
        selectorFlow
            .filterNotNull()
            .flatMapLatest { selector ->
                fetch(selector)
            }
            .distinctUntilChanged()
            .flowOn(dispatcher)

    suspend fun select(selector: String) {
        selectorFlow.emit(
            selector
        )
    }

    private suspend fun fetch(selector: String): Flow<Result<Data>> {
        return MutableStateFlow(
            runCatching {
                dataService.loadData(selector)
            }
        )
    }
}
