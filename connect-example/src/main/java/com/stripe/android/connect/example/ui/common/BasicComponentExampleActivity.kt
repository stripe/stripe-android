package com.stripe.android.connect.example.ui.common

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import com.stripe.android.connect.BuildConfig
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.data.EmbeddedComponentManagerProvider
import com.stripe.android.core.Logger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@OptIn(PrivateBetaConnectSDK::class)
@AndroidEntryPoint
abstract class BasicComponentExampleActivity : FragmentActivity() {

    @Inject
    lateinit var embeddedComponentManagerProvider: EmbeddedComponentManagerProvider

    protected lateinit var embeddedComponentManager: EmbeddedComponentManager

    protected val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG)

    @get:StringRes
    abstract val titleRes: Int

    abstract fun createComponentView(context: Context): View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        embeddedComponentManager = try {
            embeddedComponentManagerProvider.provideEmbeddedComponentManager()
        } catch (e: IllegalStateException) {
            // TODO - handle app restoration more gracefully
            logger.error("Error retrieving EmbeddedComponentManager: $e")
            finish() // we don't have an embedded component manager, so go back to MainActivity to get one
            return
        }

        setContent {
            BasicComponentExample(
                title = stringResource(titleRes),
                finish = ::finish,
                createComponentView = ::createComponentView,
            )
        }
    }
}
