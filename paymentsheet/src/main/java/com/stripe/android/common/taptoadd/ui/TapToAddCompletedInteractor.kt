package com.stripe.android.common.taptoadd.ui

import androidx.appcompat.app.AppCompatDelegate
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.Amount
import com.stripe.android.uicore.format.CurrencyFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Provider

internal interface TapToAddCompletedInteractor {
    val cardBrand: CardBrand
    val last4: String?
    val label: ResolvableString

    interface Factory {
        fun create(paymentMethod: PaymentMethod): TapToAddCompletedInteractor
    }
}

internal class DefaultTapToAddCompletedInteractor(
    private val coroutineScope: CoroutineScope,
    private val amount: Amount?,
    private val paymentMethod: PaymentMethod,
    private val onShown: () -> Unit,
) : TapToAddCompletedInteractor {
    override val cardBrand = paymentMethod.card?.brand ?: CardBrand.Unknown
    override val last4 = paymentMethod.card?.last4

    override val label: ResolvableString = resolvableString(
        R.string.stripe_tap_to_add_paid_label,
        amount?.let {
            CurrencyFormatter.format(
                amount = amount.value,
                amountCurrencyCode = amount.currencyCode,
                targetLocale = AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
            )
        }
    )

    init {
        coroutineScope.launch {
            delay(SHOWN_SCREEN_DELAY)
            onShown()
        }
    }

    class Factory @Inject constructor(
        @ViewModelScope private val coroutineScope: CoroutineScope,
        private val paymentMethodMetadata: PaymentMethodMetadata,
        private val navigator: Provider<TapToAddNavigator>,
    ) : TapToAddCompletedInteractor.Factory {
        override fun create(paymentMethod: PaymentMethod): TapToAddCompletedInteractor {
            return DefaultTapToAddCompletedInteractor(
                coroutineScope = coroutineScope,
                paymentMethod = paymentMethod,
                amount = paymentMethodMetadata.amount(),
                onShown = {
                    navigator.get().performAction(
                        action = TapToAddNavigator.Action.Complete,
                    )
                },
            )
        }
    }

    private companion object {
        const val SHOWN_SCREEN_DELAY = 2500L
    }
}
