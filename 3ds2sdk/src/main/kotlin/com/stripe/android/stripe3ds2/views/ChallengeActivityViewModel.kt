package com.stripe.android.stripe3ds2.views

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import com.stripe.android.stripe3ds2.transaction.ChallengeAction
import com.stripe.android.stripe3ds2.transaction.ChallengeActionHandler
import com.stripe.android.stripe3ds2.transaction.ChallengeRequestResult
import com.stripe.android.stripe3ds2.transaction.ChallengeResult
import com.stripe.android.stripe3ds2.transaction.TransactionTimer
import com.stripe.android.stripe3ds2.transactions.ChallengeResponseData
import com.stripe.android.stripe3ds2.utils.ImageCache
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal class ChallengeActivityViewModel(
    private val challengeActionHandler: ChallengeActionHandler,
    private val transactionTimer: TransactionTimer,
    errorReporter: ErrorReporter,
    private val imageCache: ImageCache = ImageCache.Default,
    workContext: CoroutineContext
) : ViewModel() {
    private val imageRepository = ImageRepository(errorReporter, workContext)

    private val _refreshUi = MutableLiveData<Unit>()
    val refreshUi: LiveData<Unit> = _refreshUi

    private val _submitClicked = MutableLiveData<ChallengeAction>()
    val submitClicked: LiveData<ChallengeAction> = _submitClicked

    private val _shouldFinish = MutableLiveData<ChallengeResult>()
    val shouldFinish: LiveData<ChallengeResult> = _shouldFinish

    private val _challengeText = MutableLiveData<String>()
    val challengeText: LiveData<String> = _challengeText

    private val _challengeRequestResult = OnInactiveAwareMutableLiveData<ChallengeRequestResult>()
    val challengeRequestResult: LiveData<ChallengeRequestResult> = _challengeRequestResult

    private val _nextScreen = OnInactiveAwareMutableLiveData<ChallengeResponseData>()
    val nextScreen: LiveData<ChallengeResponseData> = _nextScreen

    var shouldRefreshUi: Boolean = false
    var shouldAutoSubmitOOB: Boolean = false

    internal val transactionTimerJob: Job

    init {
        transactionTimerJob = viewModelScope.launch {
            transactionTimer.start()
        }
    }

    fun getTimeout() = liveData {
        emit(
            transactionTimer.timeout.firstOrNull { isTimeout -> isTimeout }
        )
    }

    fun getImage(
        imageData: ChallengeResponseData.Image?,
        densityDpi: Int
    ) = liveData {
        emit(
            imageRepository.getImage(
                imageData?.getUrlForDensity(densityDpi)
            )
        )
    }

    fun submit(action: ChallengeAction) {
        viewModelScope.launch {
            _challengeRequestResult.postValue(challengeActionHandler.submit(action))
        }
    }

    fun stopTimer() {
        transactionTimerJob.cancel()
    }

    fun onMemoryEvent() {
        imageCache.clear()
    }

    fun onRefreshUi() {
        _refreshUi.value = Unit
    }

    fun onSubmitClicked(challengeAction: ChallengeAction) {
        _submitClicked.postValue(challengeAction)
    }

    fun onFinish(
        challengeResult: ChallengeResult
    ) {
        _shouldFinish.postValue(challengeResult)
    }

    fun updateChallengeText(text: String) {
        _challengeText.value = text
    }

    fun onNextScreen(cres: ChallengeResponseData) {
        _nextScreen.value = cres
    }

    /**
     * A `MutableLiveData` that sets the value to `null` when `onInactive()` is invoked.
     */
    private class OnInactiveAwareMutableLiveData<T> : MutableLiveData<T>() {
        override fun onInactive() {
            super.onInactive()
            value = null
        }
    }

    internal class Factory(
        private val challengeActionHandler: ChallengeActionHandler,
        private val transactionTimer: TransactionTimer,
        private val errorReporter: ErrorReporter,
        private val workContext: CoroutineContext
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChallengeActivityViewModel(
                challengeActionHandler,
                transactionTimer,
                errorReporter,
                workContext = workContext
            ) as T
        }
    }
}
