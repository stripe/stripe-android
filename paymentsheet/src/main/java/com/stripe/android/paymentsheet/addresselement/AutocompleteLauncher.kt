package com.stripe.android.paymentsheet.addresselement

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.elements.AddressDetails
import com.stripe.android.elements.Appearance
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.ui.LinkAppBar
import com.stripe.android.link.ui.LinkAppBarState
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.paymentsheet.ui.AddressOptionsAppBar
import com.stripe.android.uicore.StripeTheme
import kotlinx.parcelize.Parcelize
import java.lang.ref.WeakReference
import java.util.UUID
import javax.inject.Inject

internal interface AutocompleteLauncher {
    fun launch(
        country: String,
        googlePlacesApiKey: String,
        resultHandler: AutocompleteLauncherResultHandler
    )

    sealed interface Result : Parcelable {
        val addressDetails: AddressDetails?

        @Parcelize
        data class EnterManually(override val addressDetails: AddressDetails?) : Result

        @Parcelize
        data class OnBack(override val addressDetails: AddressDetails?) : Result
    }
}

internal interface AutocompleteActivityLauncher : AutocompleteLauncher {
    fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner)

    fun interface Factory {
        fun create(appearanceContext: AutocompleteAppearanceContext): AutocompleteActivityLauncher
    }
}

internal fun interface AutocompleteLauncherResultHandler {
    fun onAutocompleteLauncherResult(result: AutocompleteLauncher.Result)
}

internal sealed interface AutocompleteAppearanceContext : Parcelable {
    fun applyAppearance()

    @Composable
    fun Theme(content: @Composable () -> Unit)

    @Composable
    fun AppBar(
        isRootScreen: Boolean,
        onBack: () -> Unit,
    )

    @get:Composable
    val backgroundColor: Color

    @Parcelize
    data object Link : AutocompleteAppearanceContext {
        override val backgroundColor: Color
            @Composable
            get() = LinkTheme.colors.surfacePrimary

        override fun applyAppearance() {
            // No-op, Link supplies its own values and does adhere to merchant supplied appearance values
        }

        @Composable
        override fun Theme(content: @Composable () -> Unit) {
            DefaultLinkTheme {
                StripeThemeForLink {
                    content()
                }
            }
        }

        @Composable
        override fun AppBar(
            isRootScreen: Boolean,
            onBack: () -> Unit,
        ) {
            LinkAppBar(
                state = LinkAppBarState(
                    showHeader = false,
                    canNavigateBack = !isRootScreen,
                    title = null,
                    isElevated = false
                )
            ) {
                onBack()
            }
        }
    }

    @Parcelize
    data class PaymentElement(
        val appearance: Appearance
    ) : AutocompleteAppearanceContext {
        override val backgroundColor: Color
            @Composable
            get() = MaterialTheme.colors.surface

        override fun applyAppearance() {
            appearance.parseAppearance()
        }

        @Composable
        override fun Theme(content: @Composable () -> Unit) {
            StripeTheme {
                content()
            }
        }

        @Composable
        override fun AppBar(
            isRootScreen: Boolean,
            onBack: () -> Unit,
        ) {
            AddressOptionsAppBar(isRootScreen = isRootScreen) {
                onBack()
            }
        }
    }
}

internal class DefaultAutocompleteLauncher(
    private val appearanceContext: AutocompleteAppearanceContext,
) : AutocompleteActivityLauncher {
    private var activityLauncher: ActivityResultLauncher<AutocompleteContract.Args>? = null

    private val registeredAutocompleteListeners =
        mutableMapOf<String, WeakReference<AutocompleteLauncherResultHandler>>()

    override fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner) {
        activityLauncher = activityResultCaller.registerForActivityResult(
            AutocompleteContract
        ) { result ->
            registeredAutocompleteListeners[result.id]?.get()?.onAutocompleteLauncherResult(
                when (result) {
                    is AutocompleteContract.Result.EnterManually -> AutocompleteLauncher.Result.EnterManually(
                        result.addressDetails,
                    )
                    is AutocompleteContract.Result.Address -> AutocompleteLauncher.Result.OnBack(
                        result.addressDetails
                    )
                }
            )
            registeredAutocompleteListeners.remove(result.id)
        }

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    activityLauncher?.unregister()
                    activityLauncher = null
                }
            }
        )
    }

    override fun launch(
        country: String,
        googlePlacesApiKey: String,
        resultHandler: AutocompleteLauncherResultHandler
    ) {
        val id = UUID.randomUUID().toString()

        registeredAutocompleteListeners[id] = WeakReference(resultHandler)

        activityLauncher?.launch(
            AutocompleteContract.Args(
                id = id,
                country = country,
                googlePlacesApiKey = googlePlacesApiKey,
                appearanceContext = appearanceContext,
            )
        )
    }

    class Factory @Inject constructor() : AutocompleteActivityLauncher.Factory {
        override fun create(appearanceContext: AutocompleteAppearanceContext): AutocompleteActivityLauncher {
            return DefaultAutocompleteLauncher(appearanceContext)
        }
    }
}
