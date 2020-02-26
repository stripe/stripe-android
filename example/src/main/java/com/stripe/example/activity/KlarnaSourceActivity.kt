package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.ApiResultCallback
import com.stripe.android.Stripe
import com.stripe.android.model.Address
import com.stripe.android.model.KlarnaSourceParams
import com.stripe.android.model.Source
import com.stripe.android.model.SourceParams
import com.stripe.example.StripeFactory
import com.stripe.example.databinding.KlarnaSourceActivityBinding

class KlarnaSourceActivity : AppCompatActivity() {
    private val viewBinding: KlarnaSourceActivityBinding by lazy {
        KlarnaSourceActivityBinding.inflate(layoutInflater)
    }

    private val viewModel: SourceViewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(application)
        )[SourceViewModel::class.java]
    }

    private val stripe: Stripe by lazy {
        StripeFactory(this).create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(viewBinding.root)

        viewBinding.createButton.setOnClickListener {
            viewBinding.sourceResult.text = ""
            viewBinding.progressBar.visibility = View.VISIBLE
            createKlarnaSource().observe(this, Observer { result ->
                viewBinding.progressBar.visibility = View.INVISIBLE
                when (result) {
                    is SourceViewModel.SourceResult.Success -> {
                        val source = result.source
                        logSource(source)
                        stripe.authenticateSource(this, source)
                    }
                    is SourceViewModel.SourceResult.Error -> {
                        viewBinding.sourceResult.text = result.e.localizedMessage
                    }
                }
            })
        }

        viewBinding.fetchButton.setOnClickListener {
            viewBinding.progressBar.visibility = View.VISIBLE
            viewModel.fetchSource(viewModel.source).observe(this, Observer { result ->
                viewBinding.progressBar.visibility = View.INVISIBLE
                when (result) {
                    is SourceViewModel.SourceResult.Success -> {
                        logSource(result.source)
                    }
                    is SourceViewModel.SourceResult.Error -> {
                        logException(result.e)
                    }
                }
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null && stripe.isAuthenticateSourceResult(requestCode, data)) {
            stripe.onAuthenticateSourceResult(data, object : ApiResultCallback<Source> {
                override fun onSuccess(result: Source) {
                    viewModel.source = result
                    logSource(result)
                }

                override fun onError(e: Exception) {
                    logException(e)
                }
            })
        }
    }

    private fun logSource(source: Source) {
        viewBinding.sourceResult.text = """
            Source ID
            ${source.id}
            
            Flow
            ${source.flow}
            
            Status
            ${source.status}

            Redirect Status
            ${source.redirect?.status}
                
            Authenticate URL
            ${source.redirect?.url}
                
            Return URL
            ${source.redirect?.returnUrl}
        """.trimIndent()
    }

    private fun logException(ex: Exception) {
        viewBinding.sourceResult.text = ex.localizedMessage
    }

    private fun createKlarnaSource(): LiveData<SourceViewModel.SourceResult> {
        return viewModel.createSource(SourceParams.createKlarna(
            returnUrl = RETURN_URL,
            currency = "gbp",
            klarnaParams = KlarnaSourceParams(
                purchaseCountry = "UK",
                lineItems = LINE_ITEMS,
                billingFirstName = "Arthur",
                billingLastName = "Dent",
                billingAddress = Address.Builder()
                    .setLine1("29 Arlington Avenue")
                    .setCity("London")
                    .setCountry("UK")
                    .setPostalCode("N1 7BE")
                    .build(),
                billingEmail = "test@example.com",
                billingPhone = "02012267709"
            )
        ))
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
