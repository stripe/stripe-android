package com.stripe.example.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.model.Address
import com.stripe.android.model.ShippingInformation
import com.stripe.example.databinding.ActivityShippingInfoWidgetBinding

class ShippingInfoWidgetActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewBinding = ActivityShippingInfoWidgetBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.shippingInfoWidget.populateShippingInfo(
            shippingInformation = ShippingInformation(
                address = Address.Builder()
                    .setCountry("CA")
                    .build(),
            ),
        )
    }
}
