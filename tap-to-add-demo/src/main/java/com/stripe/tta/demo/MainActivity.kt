package com.stripe.tta.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.stripe.tta.demo.ui.TapToAddApp

class MainActivity : ComponentActivity() {

    private val checkoutViewModel: CheckoutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            TapToAddApp(
                checkoutViewModel = checkoutViewModel,
            )
        }
    }
}
