package com.stripe.android.view

import android.content.Context
import android.text.Editable
import android.view.View

internal class CardElementWatchers(
    private val context: Context,
    private val cardElementAnalytics: CardElementAnalytics,
    private val invalidFieldProviders: () -> Set<CardValidCallback.Fields>,
    private val cardValidCallbackProvider: () -> CardValidCallback?,
) {
    val textFocusWatcher = View.OnFocusChangeListener { _, hasFocus ->
        if (hasFocus) {
            cardElementAnalytics.reportInteraction(context)
        }
    }

    val textInputWatcher = object : StripeTextWatcher() {
        override fun afterTextChanged(s: Editable?) {
            super.afterTextChanged(s)

            cardElementAnalytics.reportInteraction(context)

            val invalidFields = invalidFieldProviders()

            val isComplete = invalidFields.isEmpty()

            if (isComplete) {
                cardElementAnalytics.reportFormCompleted(context)
            }

            cardValidCallbackProvider()?.onInputChanged(invalidFields.isEmpty(), invalidFields)
        }
    }
}
