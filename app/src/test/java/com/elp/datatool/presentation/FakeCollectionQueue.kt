package com.elp.datatool.presentation

import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.QueueState
import com.elp.datatool.domain.repository.CollectionQueue
import kotlinx.coroutines.flow.MutableStateFlow

/** Records calls; tests drive [state] directly. */
class FakeCollectionQueue(
    private val supported: Set<DataSource> = setOf(DataSource.UNITY_ASSET_STORE),
) : CollectionQueue {
    override val state = MutableStateFlow(QueueState())
    val started = mutableListOf<Pair<DataSource, List<String>>>()
    val retried = mutableListOf<Pair<DataSource, String>>()
    var cancelCount = 0

    override fun isSupported(source: DataSource) = source in supported

    override fun start(source: DataSource, productIds: List<String>) {
        started += source to productIds
        state.value = QueueState(running = true, total = productIds.size)
    }

    override fun cancel() {
        cancelCount++
    }

    override fun retry(source: DataSource, productId: String) {
        retried += source to productId
    }
}
