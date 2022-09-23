package com.stripe.android.financialconnections.utils

import kotlinx.coroutines.Job

/**
 * Job container that will cancel the previous job if a new one is set.
 *
 * Assign the new job with the += operator.
 */
internal class ConflatedJob {

    private var job: Job? = null
    private var prevJob: Job? = null

    val isActive get() = job?.isActive ?: false

    @Synchronized
    operator fun plusAssign(newJob: Job) {
        cancel()
        job = newJob
    }

    fun cancel() {
        job?.cancel()
        prevJob = job
    }

    fun start() {
        job?.start()
    }
}
