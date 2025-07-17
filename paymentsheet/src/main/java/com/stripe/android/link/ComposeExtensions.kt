package com.stripe.android.link

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.uicore.utils.extractActivity

/**
 * Retrieves or builds a ViewModel instance, providing the [NativeLinkComponent] (activity
 * retained component) to the factory to facilitate its creation via dependency injection.
 */
@Composable
internal inline fun <reified T : ViewModel> linkViewModel(
    factory: (NativeLinkComponent) -> ViewModelProvider.Factory
): T {
    val component = parentActivity().viewModel?.activityRetainedComponent
        ?: throw IllegalStateException("no viewmodel in parent activity")
    return viewModel<T>(factory = factory(component))
}

/**
 * Retrieves the parent [LinkActivity] from the current Compose context.
 */
@Composable
internal fun parentActivity(): LinkActivity {
    return LocalContext.current.extractActivity() as LinkActivity
}
