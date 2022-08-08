package com.stripe.android.paymentsheet.addresselement.analytics

import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
internal class DefaultAddressLauncherEventReporter @Inject internal constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    @IOContext private val workContext: CoroutineContext
) : AddressLauncherEventReporter {

    override fun onShow(country: String) {
        fireEvent(
            AddressLauncherEvent.Show(
                country = country
            )
        )
    }

    override fun onCompleted(
        country: String,
        autocompleteResultSelected: Boolean,
        editDistance: Int?
    ) {
        fireEvent(
            AddressLauncherEvent.Completed(
                country = country,
                autocompleteResultSelected = autocompleteResultSelected,
                editDistance = editDistance
            )
        )
    }

    private fun fireEvent(event: AddressLauncherEvent) {
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.createRequest(
                    event,
                    event.additionalParams
                )
            )
        }
    }
}
