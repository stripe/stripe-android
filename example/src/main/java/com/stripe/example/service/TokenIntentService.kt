package com.stripe.example.service

import android.app.Activity
import android.app.IntentService
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager

import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.exception.StripeException
import com.stripe.android.model.Card
import com.stripe.android.model.Token

/**
 * An [IntentService] subclass for handling the creation of a [Token] from
 * input [Card] information.
 */
class TokenIntentService : IntentService("TokenIntentService") {

    override fun onHandleIntent(intent: Intent?) {
        var errorMessage: String? = null
        var token: Token? = null
        if (intent != null) {
            val cardNumber = intent.getStringExtra(EXTRA_CARD_NUMBER)
            val month = intent.getIntExtra(EXTRA_MONTH, -1)
            val year = intent.getIntExtra(EXTRA_YEAR, -1)
            val cvc = intent.getStringExtra(EXTRA_CVC)

            val card = Card.create(cardNumber, month, year, cvc)

            val stripe = Stripe(applicationContext,
                PaymentConfiguration.getInstance(this).publishableKey)
            try {
                token = stripe.createTokenSynchronous(card)
            } catch (stripeEx: StripeException) {
                errorMessage = stripeEx.localizedMessage
            }
        }

        val localIntent = Intent(TOKEN_ACTION)
        if (token != null) {
            localIntent.putExtra(STRIPE_CARD_LAST_FOUR, token.card!!.last4)
            localIntent.putExtra(STRIPE_CARD_TOKEN_ID, token.id)
        }

        if (errorMessage != null) {
            localIntent.putExtra(STRIPE_ERROR_MESSAGE, errorMessage)
        }

        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent)
    }

    companion object {

        const val TOKEN_ACTION = "com.stripe.example.service.tokenAction"
        const val STRIPE_CARD_LAST_FOUR = "com.stripe.example.service.cardLastFour"
        const val STRIPE_CARD_TOKEN_ID = "com.stripe.example.service.cardTokenId"
        const val STRIPE_ERROR_MESSAGE = "com.stripe.example.service.errorMessage"

        private const val EXTRA_CARD_NUMBER = "com.stripe.example.service.extra.cardNumber"
        private const val EXTRA_MONTH = "com.stripe.example.service.extra.month"
        private const val EXTRA_YEAR = "com.stripe.example.service.extra.year"
        private const val EXTRA_CVC = "com.stripe.example.service.extra.cvc"

        fun createTokenIntent(
            launchingActivity: Activity,
            cardNumber: String?,
            month: Int?,
            year: Int?,
            cvc: String?
        ): Intent {
            return Intent(launchingActivity, TokenIntentService::class.java)
                .putExtra(EXTRA_CARD_NUMBER, cardNumber)
                .putExtra(EXTRA_MONTH, month)
                .putExtra(EXTRA_YEAR, year)
                .putExtra(EXTRA_CVC, cvc)
        }
    }
}
