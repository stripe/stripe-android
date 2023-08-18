package com.stripe.android.customersheet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.stripe.android.customersheet.injection.CustomerSessionComponent
import com.stripe.android.customersheet.injection.DaggerCustomerSessionComponent

/**
 * Class that represents a customers session for the [CustomerSheet]. The customer session is based
 * on the dependencies supplied in [createCustomerSessionComponent]. If the merchant supplies any
 * new dependencies, then the customer session component is recreated. The lifecycle of the
 * customer session lives longer than the [CustomerSheetActivity].
 */
@OptIn(ExperimentalCustomerSheetApi::class)
internal class CustomerSessionViewModel(
    application: Application,
) : AndroidViewModel(application) {

    internal fun createCustomerSessionComponent(
        configuration: CustomerSheet.Configuration,
        customerAdapter: CustomerAdapter,
        callback: CustomerSheetResultCallback,
        statusBarColor: () -> Int?,
    ): CustomerSessionComponent {
        val shouldCreateNewComponent = configuration != backingComponent?.configuration ||
            customerAdapter != backingComponent?.customerAdapter ||
            callback != backingComponent?.callback
        if (shouldCreateNewComponent) {
            backingComponent = DaggerCustomerSessionComponent
                .builder()
                .application(getApplication())
                .configuration(configuration)
                .customerAdapter(customerAdapter)
                .callback(callback)
                .statusBarColor(statusBarColor)
                .customerSessionViewModel(this)
                .build()
        }

        return component
    }

    override fun onCleared() {
        super.onCleared()
        clear()
    }

    internal companion object {
        internal fun clear() {
            backingComponent = null
        }

        private var backingComponent: CustomerSessionComponent? = null
        val component: CustomerSessionComponent
            get() = backingComponent ?: error("Component could not be retrieved")
    }
}
