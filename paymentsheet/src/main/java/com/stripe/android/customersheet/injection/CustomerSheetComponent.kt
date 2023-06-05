package com.stripe.android.customersheet.injection

import androidx.activity.result.ActivityResultCaller
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import dagger.BindsInstance
import dagger.Subcomponent

@OptIn(ExperimentalCustomerSheetApi::class)
@Subcomponent
internal interface CustomerSheetComponent {
    val customerSheet: CustomerSheet
    val sessionComponent: CustomerSessionComponent

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun activityResultCaller(activityResultCaller: ActivityResultCaller): Builder

        fun build(): CustomerSheetComponent
    }
}
