package com.stripe.android.paymentsheet.example.playground.embedded

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.rememberEmbeddedPaymentElement
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.activity.FawryActivity
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundConfigurationData
import com.stripe.android.paymentsheet.example.samples.ui.shared.BuyButton
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentMethodSelector
import com.stripe.android.uicore.utils.collectAsState

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal class EmbeddedPlaygroundActivity : AppCompatActivity(), ExternalPaymentMethodConfirmHandler {
    companion object {
        fun create(context: Context, playgroundState: PlaygroundState.Payment): Intent {
            return Intent(context, EmbeddedPlaygroundActivity::class.java).apply {
                putExtra("playgroundState", playgroundState)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        val playgroundState = intent.getParcelableExtra<PlaygroundState.Payment?>("playgroundState")
        if (playgroundState == null ||
            playgroundState.integrationType != PlaygroundConfigurationData.IntegrationType.Embedded
        ) {
            finish()
            return
        }

        setContent {
            val embeddedPaymentElement = rememberEmbeddedPaymentElement(
                createIntentCallback = { _, _ ->
                    CreateIntentResult.Success(playgroundState.clientSecret)
                },
                externalPaymentMethodConfirmHandler = this,
                resultCallback = ::handleEmbeddedResult,
            )

            LaunchedEffect(embeddedPaymentElement) {
                embeddedPaymentElement.configure(
                    intentConfiguration = playgroundState.intentConfiguration(),
                    configuration = playgroundState.embeddedConfiguration(),
                )
            }

            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                embeddedPaymentElement.Content()

                val selectedPaymentOption by embeddedPaymentElement.paymentOption.collectAsState()

                selectedPaymentOption?.let { selectedPaymentOption ->
                    PaymentMethodSelector(
                        isEnabled = true,
                        paymentMethodLabel = selectedPaymentOption.label,
                        paymentMethodPainter = selectedPaymentOption.iconPainter,
                        clickable = false,
                        onClick = { },
                    )

                    BuyButton(buyButtonEnabled = true) {
                        embeddedPaymentElement.confirm()
                    }
                }
            }
        }
    }

    override fun confirmExternalPaymentMethod(
        externalPaymentMethodType: String,
        billingDetails: PaymentMethod.BillingDetails
    ) {
        startActivity(
            Intent().setClass(
                this,
                FawryActivity::class.java
            ).putExtra(FawryActivity.EXTRA_EXTERNAL_PAYMENT_METHOD_TYPE, externalPaymentMethodType)
                .putExtra(FawryActivity.EXTRA_BILLING_DETAILS, billingDetails)
        )
    }

    private fun handleEmbeddedResult(result: EmbeddedPaymentElement.Result) {
        when (result) {
            is EmbeddedPaymentElement.Result.Canceled -> {
                Log.d("EmbeddedPlayground", "Canceled")
            }
            is EmbeddedPaymentElement.Result.Completed -> {
                Log.d("EmbeddedPlayground", "Complete")
                setResult(RESULT_OK)
                finish()
            }
            is EmbeddedPaymentElement.Result.Failed -> {
                Log.e("EmbeddedPlayground", "Failed", result.error)
                Toast.makeText(this, "Payment Failed.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
