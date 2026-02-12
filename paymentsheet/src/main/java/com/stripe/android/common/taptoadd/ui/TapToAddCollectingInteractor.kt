package com.stripe.android.common.taptoadd.ui

import com.stripe.android.common.taptoadd.TapToAddCollectionHandler
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface TapToAddCollectingInteractor {
    interface Factory {
        fun create(): TapToAddCollectingInteractor
    }
}

internal class DefaultTapToAddCollectingInteractor(
    paymentMethodMetadata: PaymentMethodMetadata,
    coroutineScope: CoroutineScope,
    tapToAddCollectionHandler: TapToAddCollectionHandler,
) : TapToAddCollectingInteractor {
    init {
        coroutineScope.launch {
            tapToAddCollectionHandler.collect(paymentMethodMetadata)
        }
    }

    class Factory @Inject constructor(
        private val paymentMethodMetadata: PaymentMethodMetadata,
        @ViewModelScope private val coroutineScope: CoroutineScope,
        private val tapToAddCollectionHandler: TapToAddCollectionHandler
    ) : TapToAddCollectingInteractor.Factory {
        override fun create(): TapToAddCollectingInteractor {
            return DefaultTapToAddCollectingInteractor(
                paymentMethodMetadata = paymentMethodMetadata,
                coroutineScope = coroutineScope,
                tapToAddCollectionHandler = tapToAddCollectionHandler,
            )
        }
    }
}
