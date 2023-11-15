package com.stripe.android.customersheet.injection

import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.customersheet.CustomerSheet
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent(
    modules = [
        CustomerSheetModule::class
    ]
)
internal interface CustomerSheetComponent {
    val customerSheet: CustomerSheet
    val sessionComponent: CustomerSessionComponent

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun lifecycleOwner(
            lifecycleOwner: LifecycleOwner,
        ): Builder

        @BindsInstance
        fun activityResultRegistryOwner(
            activityResultRegistryOwner: ActivityResultRegistryOwner,
        ): Builder

        fun build(): CustomerSheetComponent
    }
}
