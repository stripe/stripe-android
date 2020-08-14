package com.stripe.android.cards

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import com.stripe.android.ApiRequest
import com.stripe.android.PaymentConfiguration
import com.stripe.android.StripeApiRepository
import com.stripe.android.view.CardWidget

/**
 * A [ViewModel] for [CardWidget] instances.
 */
internal class CardWidgetViewModel(
    private val cardAccountRangeRepository: CardAccountRangeRepository
) : ViewModel() {

    fun getAccountRange(cardNumber: String) = liveData {
        emit(cardAccountRangeRepository.getAccountRange(cardNumber))
    }

    internal class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val paymentConfiguration = PaymentConfiguration.getInstance(
                application.applicationContext
            )
            return CardWidgetViewModel(
                DefaultCardAccountRangeRepository(
                    localCardAccountRangeSource = LocalCardAccountRangeSource(),
                    remoteCardAccountRangeSource = RemoteCardAccountRangeSource(
                        StripeApiRepository(
                            application.applicationContext,
                            paymentConfiguration.publishableKey
                        ),
                        ApiRequest.Options(
                            paymentConfiguration.publishableKey
                        )
                    )
                )
            ) as T
        }
    }
}
