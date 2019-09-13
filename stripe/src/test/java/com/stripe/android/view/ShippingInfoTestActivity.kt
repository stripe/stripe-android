package com.stripe.android.view

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.R

/**
 * Activity used to test UI components
 */
class ShippingInfoTestActivity : AppCompatActivity() {

    lateinit var shippingInfoWidget: ShippingInfoWidget
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.StripeDefaultTheme)
        shippingInfoWidget = ShippingInfoWidget(this)

        val linearLayout = LinearLayout(this)
        linearLayout.addView(shippingInfoWidget)
        setContentView(linearLayout)
    }
}
