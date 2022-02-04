package com.stripe.android.cards

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.stripe.android.view.CardWidget

/**
 * A [ViewModel] for [CardWidget] instances.
 */
internal class CardWidgetViewModel(
    application: Application,
    cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory
) : AndroidViewModel(application) {

    constructor(
        application: Application
    ) : this(
        application,
        DefaultCardAccountRangeRepositoryFactory(application)
    )

    private val cardAccountRangeRepository: CardAccountRangeRepository by lazy {
        cardAccountRangeRepositoryFactory.create()
    }

    fun getAccountRange(cardNumber: CardNumber.Unvalidated) = liveData {
        emit(cardAccountRangeRepository.getAccountRange(cardNumber))
    }
}
