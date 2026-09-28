package com.stripe.android.payments.core.injection

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.payments.core.authentication.threeds2.Stripe3ds2TransactionContract
import com.stripe.android.payments.core.authentication.threeds2.Stripe3ds2TransactionViewModel
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent(
    modules = [Stripe3dsTransactionViewModelModule::class]
)
internal interface Stripe3ds2TransactionViewModelSubcomponent {
    val viewModel: Stripe3ds2TransactionViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance
            args: Stripe3ds2TransactionContract.Args,
            @BindsInstance
            handle: SavedStateHandle,
            @BindsInstance
            application: Application,
        ): Stripe3ds2TransactionViewModelSubcomponent
    }
}
