package com.stripe.example.controller

import android.content.Context
import android.widget.Button
import com.jakewharton.rxbinding2.view.RxView
import com.stripe.android.Stripe
import com.stripe.android.view.CardInputWidget
import com.stripe.example.R
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/**
 * Class containing all the logic needed to create a token and listen for the results using
 * RxJava.
 */
class RxTokenController(
    button: Button,
    private var mCardInputWidget: CardInputWidget?,
    context: Context,
    private val mErrorDialogHandler: ErrorDialogHandler,
    private val mOutputListController: ListViewController,
    private val mProgressDialogController: ProgressDialogController,
    publishableKey: String
) {

    private val mStripe: Stripe
    private val mCompositeDisposable: CompositeDisposable = CompositeDisposable()

    init {
        mCompositeDisposable.add(
            RxView.clicks(button).subscribe { saveCard() }
        )

        mStripe = Stripe(context, publishableKey)
    }

    /**
     * Release subscriptions to prevent memory leaks.
     */
    fun detach() {
        mCompositeDisposable.dispose()
        mCardInputWidget = null
    }

    private fun saveCard() {
        val cardToSave = mCardInputWidget!!.card
        if (cardToSave == null) {
            mErrorDialogHandler.show("Invalid Card Data")
            return
        }

        // Note: using this style of Observable creation results in us having a method that
        // will not be called until we subscribe to it.
        val tokenObservable = Observable.fromCallable {
            mStripe.createTokenSynchronous(cardToSave)!!
        }

        mCompositeDisposable.add(tokenObservable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { mProgressDialogController.show(R.string.progressMessage) }
            .doOnComplete { mProgressDialogController.dismiss() }
            .subscribe(
                { mOutputListController.addToList(it) },
                { throwable -> mErrorDialogHandler.show(throwable.localizedMessage) }
            )
        )
    }
}
