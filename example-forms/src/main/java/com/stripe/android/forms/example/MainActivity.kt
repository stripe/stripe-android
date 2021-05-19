package com.stripe.android.forms.example

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.compose.StripeTheme
import com.stripe.android.compose.forms.SofortForm
import com.stripe.android.compose.forms.SofortFormViewModel


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            StripeTheme {
                Column(Modifier.fillMaxSize()) {
                    Text("Here is where we add some text.")
                }

            }
        }
    }
}