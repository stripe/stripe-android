package com.stripe.android.customersheet

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.stripe.android.utils.rememberActivity

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
    val activityResultRegistryOwner = requireNotNull(LocalActivityResultRegistryOwner.current) {
        "CustomerSheet must be created with access to an ActivityResultRegistryOwner"
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = rememberActivity {
        "CustomerSheet must be created in the context of an Activity"
    }

    return remember(
        configuration,
        customerAdapter,
        callback,
    ) {
        CustomerSheet.getInstance(
            application = activity.application,
            lifecycleOwner = lifecycleOwner,
            activityResultRegistryOwner = activityResultRegistryOwner,
            configuration = configuration,
            customerAdapter = customerAdapter,
            callback = callback,
            statusBarColor = { activity.window?.statusBarColor },
        )
    }
}
