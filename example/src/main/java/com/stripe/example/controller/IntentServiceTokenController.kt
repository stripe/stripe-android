package com.stripe.example.controller

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.content.LocalBroadcastManager
import android.widget.Button
import com.stripe.android.view.CardInputWidget
import com.stripe.example.R
import com.stripe.example.service.TokenIntentService

/**
 * Example class showing how to save tokens with an [android.app.IntentService] doing your
 * background I/O work.
 */
class IntentServiceTokenController(
    private var mActivity: Activity?,
    button: Button,
    private var mCardInputWidget: CardInputWidget?,
    private val mErrorDialogHandler: ErrorDialogHandler,
    private val mOutputListViewController: ListViewController,
    private val mProgressDialogController: ProgressDialogController
) {
    private var mTokenBroadcastReceiver: TokenBroadcastReceiver? = null

    init {
        button.setOnClickListener { saveCard() }
        registerBroadcastReceiver()
    }

    /**
     * Unregister the [BroadcastReceiver].
     */
    fun detach() {
        if (mTokenBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(mActivity!!)
                .unregisterReceiver(mTokenBroadcastReceiver!!)
            mTokenBroadcastReceiver = null
            mActivity = null
        }
        mCardInputWidget = null
    }

    private fun registerBroadcastReceiver() {
        mTokenBroadcastReceiver = TokenBroadcastReceiver()
        LocalBroadcastManager.getInstance(mActivity!!).registerReceiver(
            mTokenBroadcastReceiver!!,
            IntentFilter(TokenIntentService.TOKEN_ACTION))
    }

    private fun saveCard() {
        val cardToSave = mCardInputWidget!!.card
        if (cardToSave == null) {
            mErrorDialogHandler.show("Invalid Card Data")
            return
        }
        val tokenServiceIntent = TokenIntentService.createTokenIntent(
            mActivity!!,
            cardToSave.number,
            cardToSave.expMonth,
            cardToSave.expYear,
            cardToSave.cvc)
        mProgressDialogController.show(R.string.progressMessage)
        mActivity!!.startService(tokenServiceIntent)
    }

    // Prevent instantiation of a local broadcast receiver outside this class.
    private inner class TokenBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent?) {
            mProgressDialogController.dismiss()

            if (intent == null) {
                return
            }

            if (intent.hasExtra(TokenIntentService.STRIPE_ERROR_MESSAGE)) {
                mErrorDialogHandler.show(
                    intent.getStringExtra(TokenIntentService.STRIPE_ERROR_MESSAGE))
                return
            }

            if (intent.hasExtra(TokenIntentService.STRIPE_CARD_TOKEN_ID) && intent.hasExtra(TokenIntentService.STRIPE_CARD_LAST_FOUR)) {
                mOutputListViewController.addToList(
                    intent.getStringExtra(TokenIntentService.STRIPE_CARD_LAST_FOUR),
                    intent.getStringExtra(TokenIntentService.STRIPE_CARD_TOKEN_ID))
            }
        }
    }
}
