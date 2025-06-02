package com.stripe.android.iap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

internal class InAppPurchaseViewModel : ViewModel() {
    fun lookup(
        lookupType: InAppPurchasePluginLookupType,
        plugins: List<InAppPurchasePlugin>,
        priceId: String,
    ) {
        viewModelScope.launch {
            val plugin = lookupType.lookup(plugins)
            plugin.purchase(priceId)
        }
    }
}
