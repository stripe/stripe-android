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
    private var mCardInputWidget: CardInputWidget?,
    context: Context,
    private val mErrorDialogHandler: ErrorDialogHandler,
    outputListController: ListViewController,
    private val mProgressDialogController: ProgressDialogController,
    publishableKey: String
) {
    private val mStripe: Stripe = Stripe(context, publishableKey)

    init {
        button.setOnClickListener {
            saveCard(TokenCallbackImpl(
                mErrorDialogHandler,
                outputListController,
                mProgressDialogController
            ))
        }
    }

    fun detach() {
        mCardInputWidget = null
    }

    private fun saveCard(tokenCallback: ApiResultCallback<Token>) {
        val cardToSave = mCardInputWidget?.card
        if (cardToSave == null) {
            mErrorDialogHandler.show("Invalid Card Data")
            return
        }

        mProgressDialogController.show(R.string.progressMessage)
        mStripe.createToken(cardToSave, tokenCallback)
    }

    private class TokenCallbackImpl constructor(
        private val mErrorDialogHandler: ErrorDialogHandler,
        private val mOutputListController: ListViewController,
        private val mProgressDialogController: ProgressDialogController
    ) : ApiResultCallback<Token> {
        override fun onSuccess(token: Token) {
            mOutputListController.addToList(token)
            mProgressDialogController.dismiss()
        }

        override fun onError(error: Exception) {
            mErrorDialogHandler.show(error.localizedMessage)
            mProgressDialogController.dismiss()
        }
    }
}
