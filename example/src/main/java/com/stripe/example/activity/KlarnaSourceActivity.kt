package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import com.stripe.android.Stripe
import com.stripe.android.getAuthenticateSourceResult
import com.stripe.android.model.Address
import com.stripe.android.model.DateOfBirth
import com.stripe.android.model.KlarnaSourceParams
import com.stripe.android.model.Source
import com.stripe.android.model.SourceParams
import com.stripe.example.StripeFactory
import com.stripe.example.databinding.KlarnaSourceActivityBinding
import kotlinx.coroutines.launch

class KlarnaSourceActivity : AppCompatActivity() {
    private val viewBinding: KlarnaSourceActivityBinding by lazy {
        KlarnaSourceActivityBinding.inflate(layoutInflater)
    }

    private val viewModel: SourceViewModel by viewModels()

    private val stripe: Stripe by lazy {
        StripeFactory(this).create()
    }

    private val buttons: Set<Button> by lazy {
        setOf(
            viewBinding.createButton,
            viewBinding.fetchButton
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(viewBinding.root)

        viewBinding.createButton.setOnClickListener {
            viewBinding.sourceResult.text = ""
            disableUi()
            createKlarnaSource().observe(
                this,
                { result ->
                    result.fold(
                        onSuccess = { source ->
                            logSource(source)
                            stripe.authenticateSource(this, source)
                        },
                        onFailure = ::logException
                    )
                }
            )
        }

        viewBinding.fetchButton.setOnClickListener {
            disableUi()
            viewModel.fetchSource(viewModel.source).observe(
                this,
                { result ->
                    enableUi()
                    result.fold(::logSource, ::logException)
                }
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (stripe.isAuthenticateSourceResult(requestCode, data)) {
            lifecycleScope.launch {
                runCatching {
                    stripe.getAuthenticateSourceResult(requestCode, data!!)
                }.fold(
                    onSuccess = {
                        viewModel.source = it
                        enableUi()
                    },
                    onFailure = {
                        enableUi()
                        logException(it)
                    }
                )
            }
        }
    }

    private fun logSource(source: Source) {
        viewBinding.sourceResult.text = listOf(
            "Source ID" to source.id,
            "Flow" to source.flow,
            "Status" to source.status,
            "Redirect Status" to source.redirect?.status,
            "Authenticate URL" to source.redirect?.url,
            "Return URL" to source.redirect?.returnUrl
        ).joinToString(separator = "\n") {
            "${it.first}: ${it.second}"
        }
    }

    private fun logException(throwable: Throwable) {
        enableUi()
        viewBinding.sourceResult.text = throwable.localizedMessage
    }

    private fun enableUi() {
        viewBinding.progressBar.visibility = View.INVISIBLE
        buttons.forEach { it.isEnabled = true }
    }

    private fun disableUi() {
        viewBinding.progressBar.visibility = View.VISIBLE
        buttons.forEach { it.isEnabled = false }
    }

    private fun createKlarnaSource(): LiveData<Result<Source>> {
        return viewModel.createSource(
            SourceParams.createKlarna(
                returnUrl = RETURN_URL,
                currency = "gbp",
                klarnaParams = KlarnaSourceParams(
                    purchaseCountry = "UK",
                    lineItems = LINE_ITEMS,
                    customPaymentMethods = setOf(
                        KlarnaSourceParams.CustomPaymentMethods.Installments,
                        KlarnaSourceParams.CustomPaymentMethods.PayIn4
                    ),
                    billingFirstName = "Arthur",
                    billingLastName = "Dent",
                    billingAddress = Address.Builder()
                        .setLine1("29 Arlington Avenue")
                        .setCity("London")
                        .setCountry("UK")
                        .setPostalCode("N1 7BE")
                        .build(),
                    billingEmail = "test@example.com",
                    billingPhone = "02012267709",
                    billingDob = DateOfBirth(1, 1, 1990)
                )
            )
        )
    }

    private companion object {
        private const val RETURN_URL = "https://example.com"

        private val LINE_ITEMS = listOf(
            KlarnaSourceParams.LineItem(
                itemType = KlarnaSourceParams.LineItem.Type.Sku,
                itemDescription = "towel",
                totalAmount = 10000,
                quantity = 1
            ),
            KlarnaSourceParams.LineItem(
                itemType = KlarnaSourceParams.LineItem.Type.Sku,
                itemDescription = "digital watch",
                totalAmount = 20000,
                quantity = 2
            ),
            KlarnaSourceParams.LineItem(
                itemType = KlarnaSourceParams.LineItem.Type.Tax,
                itemDescription = "taxes",
                totalAmount = 1500
            ),
            KlarnaSourceParams.LineItem(
                itemType = KlarnaSourceParams.LineItem.Type.Shipping,
                itemDescription = "ground shipping",
                totalAmount = 499
            )
        )
    }
}
