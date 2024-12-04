package com.stripe.android.connect.example.ui.common

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import com.stripe.android.connect.BuildConfig
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.ConnectExampleScaffold
import com.stripe.android.connect.example.ConnectSdkExampleTheme
import com.stripe.android.connect.example.data.EmbeddedComponentManagerProvider
import com.stripe.android.connect.example.ui.accountloader.EmbeddedComponentLoader
import com.stripe.android.connect.example.ui.accountloader.EmbeddedComponentLoaderViewModel
import com.stripe.android.core.Logger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@OptIn(PrivateBetaConnectSDK::class)
@AndroidEntryPoint
abstract class BasicExampleComponentActivity : FragmentActivity() {

    @Inject
    lateinit var embeddedComponentManagerProvider: EmbeddedComponentManagerProvider

    private val viewModel: EmbeddedComponentLoaderViewModel by viewModels()

    protected val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG)

    @get:StringRes
    abstract val titleRes: Int

    abstract fun createComponentView(context: Context, embeddedComponentManager: EmbeddedComponentManager): View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ConnectSdkExampleTheme {
                ExampleComponentContent()
            }
        }
    }

    @Composable
    private fun ExampleComponentContent() {
        val state by viewModel.state.collectAsState()
        val embeddedComponentAsync = state.embeddedComponentAsync
        ConnectExampleScaffold(
            title = stringResource(titleRes),
        ) {
            EmbeddedComponentLoader(
                embeddedComponentAsync = embeddedComponentAsync,
                reload = viewModel::reload,
            ) { embeddedComponentManager ->
                BasicExampleComponent(
                    title = stringResource(titleRes),
                    finish = ::finish,
                    createComponentView = { context -> createComponentView(context, embeddedComponentManager) },
                )
            }
        }
    }
}
