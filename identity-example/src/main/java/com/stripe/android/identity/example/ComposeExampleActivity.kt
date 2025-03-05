package com.stripe.android.identity.example

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.MaterialTheme
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.example.ui.ExampleScreen

abstract class ComposeExampleActivity : ComponentActivity() {
    protected abstract val getBrandLogoResId: Int

    private val viewModel: IdentityExampleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            MaterialTheme {
                ExampleScreen(viewModel = viewModel)
            }
        }
    }
}
