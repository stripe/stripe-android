package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stripe.android.ApiResultCallback
import com.stripe.android.Stripe
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardParams
import com.stripe.android.model.Source
import com.stripe.android.model.SourceParams
import com.stripe.android.model.SourceTypeModel
import com.stripe.example.R
import com.stripe.example.StripeFactory
import com.stripe.example.adapter.SourcesAdapter
import com.stripe.example.databinding.CreateCardSourceActivityBinding

/**
 * Activity that lets you redirect for a 3DS source verification.
 */
class CreateCardSourceActivity : AppCompatActivity() {
    private val viewBinding: CreateCardSourceActivityBinding by lazy {
        CreateCardSourceActivityBinding.inflate(layoutInflater)
    }

    private val viewModel: SourceViewModel by viewModels()
    private val sourcesAdapter: SourcesAdapter by lazy {

        SourcesAdapter()
    }
    private val stripe: Stripe by lazy {
        StripeFactory(this).create()
    }
    private val keyboardController: KeyboardController by lazy {
        KeyboardController(this)
    }
    private val snackbarController: SnackbarController by lazy {
        SnackbarController(viewBinding.coordinator)
    }

    private var alertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.createButton.setOnClickListener {
            // If a field is invalid this will shift focus to that field
            viewBinding.cardWidget.cardParams?.let {
                createCardSource(it)
            } ?: showSnackbar("Enter a valid card.")
        }

        viewBinding.cardWidget.setCardValidCallback { isValid, invalidFields -> // We will not call cardParams unless it is valid because
            // this will cause the inFocus field to change.
            if (isValid) {
                Log.e("STRIPE", "Validity: $isValid, ${viewBinding.cardWidget.cardParams}")
            } else {
                Log.e("STRIPE", "Validity: $isValid, invalidField: $invalidFields")
            }
        }

        viewBinding.recyclerView.setHasFixedSize(true)
        viewBinding.recyclerView.layoutManager = LinearLayoutManager(this)
        viewBinding.recyclerView.adapter = sourcesAdapter
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null && stripe.isAuthenticateSourceResult(requestCode, data)) {
            stripe.onAuthenticateSourceResult(
                data,
                object : ApiResultCallback<Source> {
                    override fun onSuccess(result: Source) {
                        sourcesAdapter.addSource(result)
                    }

                    override fun onError(e: Exception) {
                        showSnackbar(e.message.orEmpty())
                    }
                }
            )
        }
    }

    override fun onPause() {
        alertDialog?.dismiss()
        alertDialog = null
        super.onPause()
    }

    /**
     * To start the 3DS cycle, create a [Source] out of the user-entered [CardParams].
     *
     * @param cardParams the [CardParams] used to create a source
     */
    private fun createCardSource(cardParams: CardParams) {
        keyboardController.hide()

        viewBinding.createButton.isEnabled = false
        viewBinding.progressBar.visibility = View.VISIBLE

        val params = SourceParams.createCardParams(cardParams)
        viewModel.createSource(params).observe(
            this,
            { result ->
                viewBinding.createButton.isEnabled = true
                viewBinding.progressBar.visibility = View.INVISIBLE

                result.fold(
                    onSuccess = ::onSourceCreated,
                    onFailure = {
                        showSnackbar(it.message.orEmpty())
                    }
                )
            }
        )
    }

    private fun onSourceCreated(source: Source) {
        // Because we've made the mapping above, we're now subscribing
        // to the result of creating a 3DS Source
        val cardData = source.sourceTypeModel as SourceTypeModel.Card

        // Making a note of the Card Source in our list.
        sourcesAdapter.addSource(source)

        // If we need to get 3DS verification for this card, we first create a 3DS Source.
        if (SourceTypeModel.Card.ThreeDSecureStatus.Required == cardData.threeDSecureStatus) {
            // The card Source can be used to create a 3DS Source
            createThreeDSecureSource(source)
        }
    }

    /**
     * Create the 3DS Source as a separate call to the API. This is what is needed
     * to verify the third-party approval. The only information from the Card source
     * that is used is the ID field.
     *
     * @param source the [CardParams]-created [Source].
     */
    private fun createThreeDSecureSource(source: Source) {
        // This represents a request for a 3DS purchase of 10.00 euro.
        val params = SourceParams.createThreeDSecureParams(
            amount = 1000L,
            currency = "EUR",
            returnUrl = RETURN_URL,
            cardId = source.id.orEmpty()
        )

        viewModel.createSource(params).observe(
            this,
            { result ->
                viewBinding.progressBar.visibility = View.INVISIBLE

                result.fold(
                    onSuccess = ::authenticateSource,
                    onFailure = {
                        showSnackbar(it.message.orEmpty())
                    }
                )
            }
        )
    }

    /**
     * Authenticate the [Source]
     */
    private fun authenticateSource(source: Source) {
        if (source.flow == Source.Flow.Redirect) {
            createAuthenticateSourceDialog(source).let {
                alertDialog = it
                it.show()
            }
        }
    }

    private fun createAuthenticateSourceDialog(source: Source): AlertDialog {
        val typeData = source.sourceTypeData.orEmpty()
        val cardBrand = CardBrand.fromCode(typeData["brand"] as String?)
        return MaterialAlertDialogBuilder(this)
            .setTitle(this.getString(R.string.authentication_dialog_title))
            .setMessage(
                getString(
                    R.string.authentication_dialog_message,
                    cardBrand.displayName,
                    typeData["last4"]
                )
            )
            .setIcon(cardBrand.icon)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                stripe.authenticateSource(this, source)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private fun showSnackbar(message: String) {
        snackbarController.show(message)
    }

    private companion object {
        private const val RETURN_URL = "stripe://source_activity"
    }
}
