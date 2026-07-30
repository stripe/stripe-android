package com.stripe.android.common.nfcscan

import com.stripe.android.core.injection.ViewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

internal interface NfcScanningTimeoutManager {
    val timeout: SharedFlow<Unit>

    fun start()

    fun reset()

    fun cancel()
}

internal class DefaultNfcScanningTimeoutManager @Inject constructor(
    @ViewModelScope private val coroutineScope: CoroutineScope,
) : NfcScanningTimeoutManager {
    private val _timeout = MutableSharedFlow<Unit>()
    override val timeout: SharedFlow<Unit> = _timeout.asSharedFlow()

    private var timeoutJob: Job? = null

    override fun start() {
        scheduleTimeout()
    }

    override fun reset() {
        scheduleTimeout()
    }

    override fun cancel() {
        timeoutJob?.cancel()
        timeoutJob = null
    }

    private fun scheduleTimeout() {
        timeoutJob?.cancel()
        timeoutJob = coroutineScope.launch {
            delay(INACTIVITY_TIMEOUT)
            _timeout.emit(Unit)
        }
    }

    private companion object {
        val INACTIVITY_TIMEOUT = 20.seconds
    }
}
