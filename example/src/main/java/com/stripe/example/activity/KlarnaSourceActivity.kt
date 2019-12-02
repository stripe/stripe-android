package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.stripe.android.model.KlarnaSourceParams
import com.stripe.android.model.SourceParams
import com.stripe.example.R
import kotlinx.android.synthetic.main.activity_klarna_source.*

class KlarnaSourceActivity : AppCompatActivity() {
    private val viewModel: SourceViewModel by lazy {
        ViewModelProviders.of(this)[SourceViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_klarna_source)

        viewModel.createdSource.observe(this, Observer {
            progress_bar.visibility = View.INVISIBLE
            source_result.text = it.toString()
        })

        btn_create_klarna_source.setOnClickListener {
            progress_bar.visibility = View.VISIBLE
            createKlarnaSource()
        }
    }

    private fun createKlarnaSource() {
        viewModel.createSource(SourceParams.createKlarna(
            returnUrl = RETURN_URL,
            currency = "eur",
            klarnaParams = KlarnaSourceParams(
                purchaseCountry = "DE",
                lineItems = LINE_ITEMS
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
