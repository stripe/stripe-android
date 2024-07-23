package com.stripe.example.activity

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.widget.FrameLayout
import com.stripe.android.PaymentConfiguration
import com.stripe.android.view.CardFormView
import com.stripe.android.view.CardInputWidget
import com.stripe.example.Settings

class TestDialogActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PaymentConfiguration.init(this, Settings(this).publishableKey)

        //val dialog = Dialog(this)
        val frame = FrameLayout(this)

        val cardFormView = CardFormView(this)
        frame.addView(cardFormView)
        setContentView(frame)
//        dialog.setContentView(cardFormView)
//        dialog.show()
    }
}