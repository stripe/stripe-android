package com.stripe.example.controller

import android.content.Context
import android.widget.Button
import com.stripe.android.ApiResultCallback
import com.stripe.android.Stripe
import com.stripe.android.model.Token
import com.stripe.android.view.CardInputWidget
import com.stripe.example.R

/**
 * Logic needed to create tokens using the [android.os.AsyncTask] methods included in the
 * sdk: [Stripe.createToken].
 */
class AsyncTaskTokenController(
    button: Button,
    private var cardInputWidget: CardInputWidget?,
    context: Context,
    private val errorDialogHandler: ErrorDialogHandler,
    outputListController: ListViewController,
    private val progressDialogController: ProgressDialogController,
    publishableKey: String
) {
    private val stripe: Stripe = Stripe(context, publishableKey, enableLogging = true)

    init {
        button.setOnClickListener {
            saveCard(TokenCallbackImpl(
                errorDialogHandler,
                outputListController,
                progressDialogController
            ))
        }
    }

    fun detach() {
        cardInputWidget = null
    }

    private fun saveCard(tokenCallback: ApiResultCallback<Token>) {
        val cardToSave = cardInputWidget?.card
        if (cardToSave == null) {
            errorDialogHandler.show("Invalid Card Data")
            return
        }

        progressDialogController.show(R.string.progressMessage)
        stripe.createToken(cardToSave, tokenCallback)
    }

    private class TokenCallbackImpl constructor(
        private val errorDialogHandler: ErrorDialogHandler,
        private val outputListController: ListViewController,
        private val progressDialogController: ProgressDialogController
    ) : ApiResultCallback<Token> {
        override fun onSuccess(token: Token) {
            outputListController.addToList(token)
            progressDialogController.dismiss()
        }

        override fun onError(error: Exception) {
            errorDialogHandler.show(error.localizedMessage.orEmpty())
            progressDialogController.dismiss()
        }
    }
}
