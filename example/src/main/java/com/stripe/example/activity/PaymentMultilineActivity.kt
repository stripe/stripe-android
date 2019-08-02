package com.stripe.example.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ListView
import android.widget.SimpleAdapter
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
import java.util.HashMap

class PaymentMultilineActivity : AppCompatActivity() {

    private var mProgressDialogController: ProgressDialogController? = null
    private var mErrorDialogHandler: ErrorDialogHandler? = null

    private var mCardMultilineWidget: CardMultilineWidget? = null

    private val mCompositeDisposable = CompositeDisposable()
    private var mSimpleAdapter: SimpleAdapter? = null
    private val mCardSources = ArrayList<Map<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_multiline)

        mCardMultilineWidget = findViewById(R.id.card_multiline_widget)

        mProgressDialogController = ProgressDialogController(supportFragmentManager,
            resources)

        mErrorDialogHandler = ErrorDialogHandler(this)

        val listView = findViewById<ListView>(R.id.card_list_pma)
        mSimpleAdapter = SimpleAdapter(
            this,
            mCardSources,
            R.layout.list_item_layout,
            arrayOf("last4", "tokenId"),
            intArrayOf(R.id.last4, R.id.tokenId))

        listView.adapter = mSimpleAdapter
        mCompositeDisposable.add(
            RxView.clicks(findViewById(R.id.save_payment)).subscribe { saveCard() })
    }

    private fun saveCard() {
        val card = mCardMultilineWidget!!.card ?: return

        val stripe = Stripe(applicationContext,
            PaymentConfiguration.getInstance().publishableKey)
        val cardSourceParams = PaymentMethodCreateParams.create(card.toPaymentMethodParamsCard(), null)
        // Note: using this style of Observable creation results in us having a method that
        // will not be called until we subscribe to it.
        val tokenObservable = Observable.fromCallable { stripe.createPaymentMethodSynchronous(cardSourceParams) }

        mCompositeDisposable.add(tokenObservable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { d -> mProgressDialogController!!.show(R.string.progressMessage) }
            .doOnComplete { mProgressDialogController!!.dismiss() }
            .subscribe(
                { addToList(it) },
                { throwable -> mErrorDialogHandler!!.show(throwable.localizedMessage) }
            )
        )
    }

    private fun addToList(paymentMethod: PaymentMethod?) {
        if (paymentMethod?.card == null) {
            return
        }

        val paymentMethodCard = paymentMethod.card
        val endingIn = getString(R.string.endingIn)
        val map = HashMap<String, String>()
        map["last4"] = endingIn + " " + paymentMethodCard!!.last4
        map["tokenId"] = paymentMethod.id!!
        mCardSources.add(map)
        mSimpleAdapter!!.notifyDataSetChanged()
    }

    override fun onDestroy() {
        mCompositeDisposable.dispose()
        super.onDestroy()
    }
}
