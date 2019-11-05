package com.stripe.example.controller

import android.content.Context
import android.widget.Button
import com.stripe.android.ApiResultCallback
import com.stripe.android.Stripe
import com.stripe.android.model.Token
import com.stripe.android.view.CardInputWidget
import com.stripe.example.R

/**
 * Logic needed to create a Card Token asynchronously using [Stripe.createCardToken].
 */
class AsyncTaskTokenController(
    button: Button,
    private var cardInputWidget: CardInputWidget?,
    context: Context,
    private val errorDialogHandler: ErrorDialogHandler,
    private val outputListController: ListViewController,
    private val progressDialogController: ProgressDialogController,
    publishableKey: String
) {
    private val stripe: Stripe = Stripe(context, publishableKey, enableLogging = true)

    init {
        button.setOnClickListener {
            saveCard()
        }
    }

    fun detach() {
        cardInputWidget = null
    }

    private fun saveCard() {
        cardInputWidget?.card?.let { card ->
            progressDialogController.show(R.string.progressMessage)
            stripe.createCardToken(card, callback = TokenCallbackImpl(
                errorDialogHandler,
                outputListController,
                progressDialogController
            ))
        } ?: errorDialogHandler.show("Invalid Card Data")
    }

    private class TokenCallbackImpl constructor(
        private val errorDialogHandler: ErrorDialogHandler,
        private val outputListController: ListViewController,
        private val progressDialogController: ProgressDialogController
    ) : ApiResultCallback<Token> {
        override fun onSuccess(result: Token) {
            outputListController.addToList(result)
            progressDialogController.dismiss()
        }

        override fun onError(e: Exception) {
            errorDialogHandler.show(e.localizedMessage.orEmpty())
            progressDialogController.dismiss()
        }
    }
}
