package com.stripe.example.activity

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import com.stripe.android.view.TestComposeView

class TestComposeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dialog = Dialog(this)
        dialog.setContentView(TestComposeView(this))
        dialog.show()
        //this.setContentView(TestComposeView(this))
    }
}