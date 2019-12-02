package com.stripe.example.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.Card
import com.stripe.android.model.Source
import com.stripe.android.model.SourceCardData
import com.stripe.android.model.SourceParams
import com.stripe.example.R
import com.stripe.example.adapter.SourcesAdapter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_card_sources.*

/**
 * Activity that lets you redirect for a 3DS source verification.
 */
class CreateCardSourceActivity : AppCompatActivity() {
    private val compositeDisposable = CompositeDisposable()

    private val sourcesAdapter: SourcesAdapter by lazy { SourcesAdapter() }
    private val redirectDialogController: RedirectDialogController by lazy {
        RedirectDialogController(this)
    }
    private val stripe: Stripe by lazy {
        Stripe(applicationContext,
            PaymentConfiguration.getInstance(this).publishableKey)
    }

    private var redirectSource: Source? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_sources)

        btn_three_d_secure.setOnClickListener { beginSequence() }
        btn_three_d_secure_sync.setOnClickListener { beginSequence() }

        recycler_view.setHasFixedSize(true)
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = sourcesAdapter
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        intent.data?.let { data ->
            if (data.query != null) {
                // The client secret and source ID found here is identical to
                // that of the source used to get the redirect URL.
                val clientSecret = data.getQueryParameter(QUERY_CLIENT_SECRET)
                val sourceId = data.getQueryParameter(QUERY_SOURCE_ID)
                if (clientSecret != null && sourceId != null &&
                    clientSecret == redirectSource?.clientSecret &&
                    sourceId == redirectSource?.id) {
                    updateSourceList(redirectSource)
                    redirectSource = null
                }
                redirectDialogController.dismissDialog()
            }
        }
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
    }

    private fun beginSequence() {
        card_widget_three_d.card?.let {
            createCardSource(it)
        } ?: showSnackbar("Enter a valid card.")
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
            .doOnSubscribe {
                btn_three_d_secure.isEnabled = false
                btn_three_d_secure_sync.isEnabled = false
                progress_bar.visibility = View.VISIBLE
            }
            .doOnComplete {
                btn_three_d_secure.isEnabled = true
                btn_three_d_secure_sync.isEnabled = true
                progress_bar.visibility = View.INVISIBLE
            }
            .subscribe({ source ->
                // Because we've made the mapping above, we're now subscribing
                // to the result of creating a 3DS Source
                val sourceCardData = source.sourceTypeModel as SourceCardData?
                val sourceId = source.id

                val threeDSecureStatus = sourceCardData?.threeDSecureStatus

                // Making a note of the Card Source in our list.
                sourcesAdapter.addItem(
                    source.status,
                    threeDSecureStatus,
                    sourceId,
                    source.type
                )
                // If we need to get 3DS verification for this card, we
                // first create a 3DS Source.
                if (sourceId != null &&
                    SourceCardData.ThreeDSecureStatus.REQUIRED == threeDSecureStatus) {
                    // The card Source can be used to create a 3DS Source
                    createThreeDSecureSource(sourceId)
                } else {
                    progress_bar.visibility = View.INVISIBLE
                }
            }, {
                showSnackbar(it.message.orEmpty())
            })
        )
    }

    /**
     * Create the 3DS Source as a separate call to the API. This is what is needed
     * to verify the third-party approval. The only information from the Card source
     * that is used is the ID field.
     *
     * @param sourceId the [Source.id] from the [Card]-created [Source].
     */
    private fun createThreeDSecureSource(sourceId: String) {
        // This represents a request for a 3DS purchase of 10.00 euro.
        val threeDParams = SourceParams.createThreeDSecureParams(
            1000L,
            "EUR",
            getUrl(true),
            sourceId
        )

        val threeDSecureObservable = Observable.fromCallable {
            stripe.createSourceSynchronous(threeDParams)
        }

        compositeDisposable.add(threeDSecureObservable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnComplete { progress_bar.visibility = View.INVISIBLE }
            .subscribe(
                { showVerifyDialog(it) },
                { showSnackbar(it.message.orEmpty()) }
            ))
    }

    /**
     * Show a dialog with a link to the external verification site.
     *
     * @param source the [Source] to verify
     */
    private fun showVerifyDialog(source: Source?) {
        // Caching the source object here because this app makes a lot of them.
        redirectSource = source
        source?.redirect?.url?.let { redirectUrl ->
            redirectDialogController.showDialog(redirectUrl, source.sourceTypeData.orEmpty())
        }
    }

    private fun updateSourceList(source: Source?) {
        if (source == null) {
            sourcesAdapter.addItem(
                "No source found",
                "Stopped",
                "Error",
                "None"
            )
        } else {
            sourcesAdapter.addItem(
                source.status,
                "complete",
                source.id,
                source.type
            )
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
            .show()
    }

    /**
     * Controller for the redirect dialog used to direct users out of the application.
     */
    internal class RedirectDialogController(private val activity: Activity) {
        private var alertDialog: AlertDialog? = null

        fun showDialog(redirectUrl: String, sourceCardData: Map<String, *>) {
            val brand = sourceCardData["brand"] as String?
            val alertDialog = AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                .setTitle(activity.getString(R.string.authentication_dialog_title))
                .setMessage(activity.getString(R.string.authentication_dialog_message,
                    brand, sourceCardData["last4"]))
                .setIcon(Card.getBrandIcon(brand))
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl)))
                }
                .setNegativeButton(android.R.string.no, null)
                .create()
            alertDialog.show()

            this.alertDialog = alertDialog
        }

        fun dismissDialog() {
            alertDialog?.dismiss()
            alertDialog = null
        }
    }

    private companion object {
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
