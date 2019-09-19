package com.stripe.example.activity

import android.os.Bundle
import android.widget.ListView
import android.widget.SimpleAdapter
import androidx.appcompat.app.AppCompatActivity
import com.jakewharton.rxbinding2.view.RxView
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.view.CardMultilineWidget
import com.stripe.example.R
import com.stripe.example.controller.ErrorDialogHandler
import com.stripe.example.controller.ProgressDialogController
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.ArrayList

class PaymentMultilineActivity : AppCompatActivity() {
    private val compositeDisposable = CompositeDisposable()
    private val cardSources = ArrayList<Map<String, String>>()

    private lateinit var cardMultilineWidget: CardMultilineWidget
    private lateinit var progressDialogController: ProgressDialogController
    private lateinit var errorDialogHandler: ErrorDialogHandler
    private lateinit var simpleAdapter: SimpleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_multiline)

        cardMultilineWidget = findViewById(R.id.card_multiline_widget)
        progressDialogController = ProgressDialogController(supportFragmentManager, resources)

        errorDialogHandler = ErrorDialogHandler(this)

        val listView = findViewById<ListView>(R.id.card_list_pma)
        simpleAdapter = SimpleAdapter(
            this,
            cardSources,
            R.layout.list_item_layout,
            arrayOf("last4", "tokenId"),
            intArrayOf(R.id.last4, R.id.tokenId))

        listView.adapter = simpleAdapter
        compositeDisposable.add(
            RxView.clicks(findViewById(R.id.save_payment)).subscribe { saveCard() })
    }

    private fun saveCard() {
        val card = cardMultilineWidget.card ?: return

        val stripe = Stripe(
            applicationContext,
            PaymentConfiguration.getInstance(applicationContext).publishableKey
        )
        val cardSourceParams = PaymentMethodCreateParams.create(card.toPaymentMethodParamsCard(), null)
        // Note: using this style of Observable creation results in us having a method that
        // will not be called until we subscribe to it.
        val tokenObservable = Observable.fromCallable { stripe.createPaymentMethodSynchronous(cardSourceParams) }

        compositeDisposable.add(tokenObservable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { progressDialogController.show(R.string.progressMessage) }
            .doOnComplete { progressDialogController.dismiss() }
            .subscribe(
                { addToList(it) },
                { throwable -> errorDialogHandler.show(throwable.localizedMessage) }
            )
        )
    }

    private fun addToList(paymentMethod: PaymentMethod?) {
        if (paymentMethod?.card == null) {
            return
        }

        val paymentMethodCard = paymentMethod.card
        val endingIn = getString(R.string.endingIn)
        val map = mapOf(
            "last4" to endingIn + " " + paymentMethodCard!!.last4,
            "tokenId" to paymentMethod.id!!
        )
        cardSources.add(map)
        simpleAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
    }
}
