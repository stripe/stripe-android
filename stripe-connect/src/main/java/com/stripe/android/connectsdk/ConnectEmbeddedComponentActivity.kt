package com.stripe.android.connectsdk

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material.Text

internal class ConnectEmbeddedComponentActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        androidx.activity.compose.setContent {
            Text("Not yet built...")
        }
    }
}
