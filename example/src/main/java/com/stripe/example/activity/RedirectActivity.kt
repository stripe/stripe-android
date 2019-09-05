package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.Card
import com.stripe.android.model.Source
import com.stripe.android.model.SourceCardData
import com.stripe.android.model.SourceParams
import com.stripe.android.view.CardInputWidget
import com.stripe.example.R
import com.stripe.example.adapter.RedirectAdapter
import com.stripe.example.controller.ErrorDialogHandler
import com.stripe.example.controller.ProgressDialogController
import com.stripe.example.controller.RedirectDialogController
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/**
 * Activity that lets you redirect for a 3DS source verification.
 */
class RedirectActivity : AppCompatActivity() {
    private val compositeDisposable = CompositeDisposable()

    private lateinit var cardInputWidget: CardInputWidget
    private lateinit var mRedirectAdapter: RedirectAdapter
    private lateinit var errorDialogHandler: ErrorDialogHandler
    private lateinit var redirectDialogController: RedirectDialogController
    private lateinit var progressDialogController: ProgressDialogController
    private lateinit var stripe: Stripe

    private var redirectSource: Source? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_polling)

        cardInputWidget = findViewById(R.id.card_widget_three_d)
        errorDialogHandler = ErrorDialogHandler(this)
        progressDialogController = ProgressDialogController(supportFragmentManager,
            resources)
        redirectDialogController = RedirectDialogController(this)

        val publishableKey = PaymentConfiguration.getInstance().publishableKey
        stripe = Stripe(applicationContext, publishableKey)

        val threeDSecureButton = findViewById<Button>(R.id.btn_three_d_secure)
        threeDSecureButton.setOnClickListener { beginSequence() }

        val threeDSyncButton = findViewById<Button>(R.id.btn_three_d_secure_sync)
        threeDSyncButton.setOnClickListener { beginSequence() }

        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_view)
        val linearLayoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = linearLayoutManager
        mRedirectAdapter = RedirectAdapter()
        recyclerView.adapter = mRedirectAdapter
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.data != null && intent.data!!.query != null) {
            // The client secret and source ID found here is identical to
            // that of the source used to get the redirect URL.
            val clientSecret = intent.data!!.getQueryParameter(QUERY_CLIENT_SECRET)
            val sourceId = intent.data!!.getQueryParameter(QUERY_SOURCE_ID)
            if (clientSecret != null &&
                sourceId != null &&
                clientSecret == redirectSource!!.clientSecret &&
                sourceId == redirectSource!!.id) {
                updateSourceList(redirectSource)
                redirectSource = null
            }
            redirectDialogController.dismissDialog()
        }
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
    }

    private fun beginSequence() {
        val displayCard = cardInputWidget.card ?: return
        createCardSource(displayCard)
    }

    /**
     * To start the 3DS cycle, create a [Source] out of the user-entered [Card].
     *
     * @param card the [Card] used to create a source
     */
    private fun createCardSource(card: Card) {
        val cardSourceParams = SourceParams.createCardParams(card)
        val cardSourceObservable = Observable.fromCallable {
            stripe.createSourceSynchronous(cardSourceParams)!!
        }

        compositeDisposable.add(cardSourceObservable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { progressDialogController.show(R.string.createSource) }
            .subscribe(
                // Because we've made the mapping above, we're now subscribing
                // to the result of creating a 3DS Source
                { source ->
                    val sourceCardData = source.sourceTypeModel as SourceCardData?

                    val threeDSecureStatus = sourceCardData?.threeDSecureStatus

                    // Making a note of the Card Source in our list.
                    mRedirectAdapter.addItem(
                        source.status,
                        threeDSecureStatus,
                        source.id,
                        source.type)
                    // If we need to get 3DS verification for this card, we
                    // first create a 3DS Source.
                    if (SourceCardData.ThreeDSecureStatus.REQUIRED == threeDSecureStatus) {
                        // The card Source can be used to create a 3DS Source
                        createThreeDSecureSource(source.id!!)
                    } else {
                        progressDialogController.dismiss()
                    }
                },
                { throwable -> errorDialogHandler.show(throwable.message ?: "") }
            ))
    }

    /**
     * Create the 3DS Source as a separate call to the API. This is what is needed
     * to verify the third-party approval. The only information from the Card source
     * that is used is the ID field.
     *
     * @param sourceId the [Source.getId] from the [Card]-created [Source].
     */
    private fun createThreeDSecureSource(sourceId: String) {
        // This represents a request for a 3DS purchase of 10.00 euro.
        val threeDParams = SourceParams.createThreeDSecureParams(
            1000L,
            "EUR",
            getUrl(true),
            sourceId)

        val threeDSecureObservable = Observable.fromCallable {
            stripe.createSourceSynchronous(threeDParams)!!
        }

        compositeDisposable.add(threeDSecureObservable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnComplete { progressDialogController.dismiss() }
            .subscribe(
                { showVerifyDialog(it) },
                { throwable -> errorDialogHandler.show(throwable.message ?: "") }
            ))
    }

    /**
     * Show a dialog with a link to the external verification site.
     *
     * @param source the [Source] to verify
     */
    private fun showVerifyDialog(source: Source) {
        // Caching the source object here because this app makes a lot of them.
        redirectSource = source

        val sourceRedirect = source.redirect
        val redirectUrl = sourceRedirect?.url
        if (redirectUrl != null) {
            redirectDialogController.showDialog(redirectUrl, source.sourceTypeData!!)
        }
    }

    private fun updateSourceList(source: Source?) {
        if (source == null) {
            mRedirectAdapter.addItem(
                "No source found",
                "Stopped",
                "Error",
                "None")
            return
        }

        mRedirectAdapter.addItem(
            source.status,
            "complete",
            source.id,
            source.type)
    }

    companion object {

        private const val RETURN_SCHEMA = "stripe://"
        private const val RETURN_HOST_ASYNC = "async"
        private const val RETURN_HOST_SYNC = "sync"

        private const val QUERY_CLIENT_SECRET = "client_secret"
        private const val QUERY_SOURCE_ID = "source"

        /**
         * Helper method to determine the return URL we use. This is one way to return basic information
         * to the activity (via the return Intent's host field). Because polling has been deprecated,
         * we no longer use this parameter in the example application, but it is used here to see the
         * relationship with the returned value for any parameters you may want to send.
         *
         * @param isSync whether or not to use a URL that tells us to use a sync method when we come
         * back to the application
         * @return a return url to be sent to the vendor
         */
        private fun getUrl(isSync: Boolean): String {
            return if (isSync) {
                RETURN_SCHEMA + RETURN_HOST_SYNC
            } else {
                RETURN_SCHEMA + RETURN_HOST_ASYNC
            }
        }
    }
}
