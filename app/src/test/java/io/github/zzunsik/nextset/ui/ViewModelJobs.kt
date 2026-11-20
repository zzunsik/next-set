package io.github.zzunsik.nextset.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * 테스트가 만든 화면 상태의 코루틴 범위(`viewModelScope`)를 모아 두고, 데이터베이스를 닫기 전에 모두 끝날 때까지 기다린다(#75).
 *
 * `ViewModelStore.clear()`는 viewModelScope를 취소만 하고 기다리지 않는다. 돌던 데이터베이스 작업은 끝나야 멈추므로 바로
 * 데이터베이스를 닫으면 남은 작업이 닫힌 데이터베이스를 읽는다. 그러면 뒤에 도는 다른 테스트가 UncaughtExceptionsBeforeTest로
 * 실패하거나(#66) 네이티브 SQLite가 테스트 프로세스를 죽인다. 시간을 정해 기다리지 않고 코루틴이 실제로 끝나기를 기다린다.
 */
internal class ViewModelJobs {
    private val jobs = mutableListOf<Job>()

    /** [viewModel]의 코루틴 범위를 기다릴 목록에 넣는다. */
    fun <T : ViewModel> track(viewModel: T): T = viewModel.also { jobs += it.viewModelScope.coroutineContext.job }

    /** [store]를 비우고(화면 상태 취소) 모든 화면 상태 코루틴이 끝날 때까지 기다린다. 제한 시간은 멈춘 경우 실패시키려는 것이다. */
    fun clearAndAwait(store: ViewModelStore, timeoutMillis: Long = 5_000) {
        store.clear()
        runBlocking { withContext(Dispatchers.Default) { withTimeout(timeoutMillis) { jobs.joinAll() } } }
    }
}
