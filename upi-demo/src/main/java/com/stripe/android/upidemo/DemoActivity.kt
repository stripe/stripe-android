package com.stripe.android.upidemo

import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult

class DemoActivity : AppCompatActivity() {
    private lateinit var paymentSheet: PaymentSheet
    private lateinit var statusView: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val refreshStatus = object : Runnable {
        override fun run() {
            statusView.text = DemoStore.summary()
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        paymentSheet = PaymentSheet.Builder { result ->
            DemoStore.recordResult(
                when (result) {
                    PaymentSheetResult.Completed -> "Completed"
                    PaymentSheetResult.Canceled -> "Canceled"
                    is PaymentSheetResult.Failed -> "Failed: ${result.error.message}"
                }
            )
        }.build(this)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp, 24.dp, 24.dp, 24.dp)
        }
        content.addView(TextView(this).apply {
            text = "UPI Demo Checkout"
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = "Fake payment · ₹100.00\n\n" +
                "Install the demo bank apps, open PaymentSheet, and pay with UPI Demo. " +
                "Approval updates a local fake backend; the SDK verifies it after you return. No money moves."
            textSize = 16f
            setPadding(0, 16.dp, 0, 16.dp)
        })
        content.addView(Button(this).apply {
            text = "Start fake UPI payment"
            setOnClickListener {
                val secret = DemoStore.newPayment()
                paymentSheet.presentWithPaymentIntent(
                    paymentIntentClientSecret = secret,
                    configuration = PaymentSheet.Configuration(
                        merchantDisplayName = "UPI Demo Merchant",
                        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                            name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                        ),
                    ),
                )
            }
        })
        statusView = TextView(this).apply {
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            setPadding(0, 24.dp, 0, 0)
        }
        content.addView(statusView)
        val scroll = ScrollView(this).apply {
            addView(content)
            setOnApplyWindowInsetsListener { view, insets ->
                @Suppress("DEPRECATION")
                view.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
                insets
            }
        }
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshStatus)
    }

    override fun onPause() {
        handler.removeCallbacks(refreshStatus)
        super.onPause()
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
