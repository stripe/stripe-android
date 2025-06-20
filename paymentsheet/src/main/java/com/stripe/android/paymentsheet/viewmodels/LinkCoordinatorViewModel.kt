package com.stripe.android.paymentsheet.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.paymentsheet.LinkCoordinator
import com.stripe.android.paymentsheet.flowcontroller.DaggerLinkCoordinatorStateComponent
import com.stripe.android.paymentsheet.flowcontroller.LinkCoordinatorStateComponent
import com.stripe.android.paymentsheet.model.PaymentSelection

internal class LinkCoordinatorViewModel(
    application: Application,
    val handle: SavedStateHandle,
    linkElementCallbackIdentifier: String,
) : AndroidViewModel(application) {

    val linkCoordinatorStateComponent: LinkCoordinatorStateComponent =
        DaggerLinkCoordinatorStateComponent
            .builder()
            .application(application)
            .linkCoordinatorViewModel(this)
            .linkElementCallbackIdentifier(linkElementCallbackIdentifier)
            .build()

    var paymentSelection: PaymentSelection?
        get() = handle["paymentSelection"]
        set(value) = handle.set("paymentSelection", value)
        
    var configuration: LinkCoordinator.Configuration?
        get() = handle["configuration"]
        set(value) = handle.set("configuration", value)

    class Factory(
        private val linkElementCallbackIdentifier: String = "",
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return LinkCoordinatorViewModel(
                application = extras.requireApplication(),
                handle = extras.createSavedStateHandle(),
                linkElementCallbackIdentifier = linkElementCallbackIdentifier,
            ) as T
        }
    }
} 