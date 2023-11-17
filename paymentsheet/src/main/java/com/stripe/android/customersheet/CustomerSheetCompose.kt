package com.stripe.android.customersheet

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.stripe.android.utils.rememberActivityOrNull

/**
 * Creates a [CustomerSheet] that is remembered across compositions.
 *
 * This *must* be called unconditionally, as part of the initialization path.
 *
 * @param configuration An optional [CustomerSheet.Configuration]
 * @param customerAdapter The [CustomerAdapter] to fetch customer-related information
 * @param callback Called with the result of the operation after [CustomerSheet] is dismissed
 */
@ExperimentalCustomerSheetApi
@Composable
fun rememberCustomerSheet(
    configuration: CustomerSheet.Configuration,
    customerAdapter: CustomerAdapter,
    callback: CustomerSheetResultCallback,
): CustomerSheet {
    val viewModelStoreOwner = requireNotNull(LocalViewModelStoreOwner.current) {
        "CustomerSheet must be created with access to a ViewModelStoreOwner"
    }

    val activityResultRegistryOwner = requireNotNull(LocalActivityResultRegistryOwner.current) {
        "CustomerSheet must be created with access to an ActivityResultRegistryOwner"
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = rememberActivityOrNull()

    return remember(configuration) {
        CustomerSheet.getInstance(
            lifecycleOwner = lifecycleOwner,
            viewModelStoreOwner = viewModelStoreOwner,
            activityResultRegistryOwner = activityResultRegistryOwner,
            configuration = configuration,
            customerAdapter = customerAdapter,
            callback = callback,
            statusBarColor = { activity?.window?.statusBarColor },
        )
    }
}
