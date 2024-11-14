package com.stripe.android.customersheet

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi
import com.stripe.android.utils.rememberActivity

/**
 * Creates a [CustomerSheet] that is remembered across compositions.
 *
 * This *must* be called unconditionally, as part of the initialization path.
 *
 * @param customerAdapter The [CustomerAdapter] to fetch customer-related information
 * @param callback Called with the result of the operation after [CustomerSheet] is dismissed
 */
@Composable
fun rememberCustomerSheet(
    customerAdapter: CustomerAdapter,
    callback: CustomerSheetResultCallback,
): CustomerSheet {
    return rememberCustomerSheet(
        integration = remember(customerAdapter) {
            CustomerSheetIntegration.Adapter(customerAdapter)
        },
        callback = callback,
    )
}

/**
* Creates a [CustomerSheet] with `CustomerSession` support that is remembered across compositions.
*
* This *must* be called unconditionally, as part of the initialization path.
*
* @param customerSessionProvider provider for providing customer session elements
* @param callback Called with the result of the operation after [CustomerSheet] is dismissed
*/
@ExperimentalCustomerSessionApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun rememberCustomerSheet(
    customerSessionProvider: CustomerSheet.CustomerSessionProvider,
    callback: CustomerSheetResultCallback,
): CustomerSheet {
    return rememberCustomerSheet(
        integration = remember(customerSessionProvider) {
            CustomerSheetIntegration.CustomerSession(customerSessionProvider)
        },
        callback = callback,
    )
}

@Composable
private fun rememberCustomerSheet(
    integration: CustomerSheetIntegration,
    callback: CustomerSheetResultCallback,
): CustomerSheet {
    val activityResultRegistryOwner = requireNotNull(LocalActivityResultRegistryOwner.current) {
        "CustomerSheet must be created with access to an ActivityResultRegistryOwner"
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = rememberActivity {
        "CustomerSheet must be created in the context of an Activity"
    }

    val viewModelStoreOwner = requireNotNull(LocalViewModelStoreOwner.current) {
        "CustomerSheet must be created with access to a ViewModelStoreOwner"
    }

    return remember(
        integration,
        callback,
    ) {
        CustomerSheet.getInstance(
            application = activity.application,
            lifecycleOwner = lifecycleOwner,
            activityResultRegistryOwner = activityResultRegistryOwner,
            viewModelStoreOwner = viewModelStoreOwner,
            integration = integration,
            callback = callback,
            statusBarColor = { activity.window?.statusBarColor },
        )
    }
}
