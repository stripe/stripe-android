package com.stripe.android.financialconnections.repository

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.di.ActivityRetainedScope
import com.stripe.android.financialconnections.model.PaymentAccountParams
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Repository for attached payment accounts.
 */
@ActivityRetainedScope
internal class AttachedPaymentAccountRepository @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val logger: Logger,
) : PersistingRepository<AttachedPaymentAccountRepository.State>(
    savedStateHandle = savedStateHandle,
) {

    fun set(paymentAccount: PaymentAccountParams) {
        logger.debug("payment account set to $paymentAccount")
        set(State(paymentAccount))
    }

    @Parcelize
    data class State(
        val attachedPaymentAccount: PaymentAccountParams? = null
    ) : Parcelable
}
