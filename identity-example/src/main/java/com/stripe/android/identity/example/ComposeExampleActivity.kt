package com.stripe.android.identity.example

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.google.accompanist.themeadapter.material.MdcTheme
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.example.ui.ExampleScreen

abstract class ComposeExampleActivity : ComponentActivity() {
    protected abstract val getBrandLogoResId: Int

    private val viewModel: IdentityExampleViewModel by viewModels()
    private val configuration by lazy {
        IdentityVerificationSheet.Configuration(
            // Or use webImage by
            // brandLogo = Uri.parse("https://path/to/a/logo.jpg")
            brandLogo = Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(resources.getResourcePackageName(getBrandLogoResId))
                .appendPath(resources.getResourceTypeName(getBrandLogoResId))
                .appendPath(resources.getResourceEntryName(getBrandLogoResId))
                .build()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MdcTheme {
                ExampleScreen(
                    configuration = configuration,
                    viewModel = viewModel
                )
            }
        }
    }
}
