package com.stripe.android.paymentsheet.benchmarkapp

import android.os.Bundle
import android.os.Trace
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ConnectionFactory
import com.stripe.android.paymentsheet.PaymentSheet

/** A separate, fixture-backed release app; each process presents one real PaymentSheet. */
internal class BenchmarkActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ConnectionFactory.Default.connectionOpener = FixtureConnectionOpener(
            assets = assets,
            captchaEnabled = intent.getStringExtra("workload") == "captcha",
        )
        PaymentConfiguration.init(this, "pk_test_benchmark")

        Trace.beginSection("PaymentSheetBenchmark#present")
        val workload = intent.getStringExtra("workload") ?: "full"
        val contactMode = if (workload == "hidden") {
            PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never
        } else {
            PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
        }
        val paymentSheet = PaymentSheet.Builder { result ->
            setContentView(TextView(this).apply { text = "Unexpected sheet result: $result" })
        }.build(this)
        setContentView(TextView(this).apply { text = "Launching PaymentSheet" })

        if (savedInstanceState == null) {
            paymentSheet.presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = PaymentSheet.Configuration.Builder("Benchmark merchant")
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Horizontal)
                    .billingDetailsCollectionConfiguration(
                        PaymentSheet.BillingDetailsCollectionConfiguration(
                            name = contactMode,
                            email = contactMode,
                            phone = contactMode,
                            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                        )
                    )
                    .build(),
            )
        }
        Trace.endSection()
    }
}
