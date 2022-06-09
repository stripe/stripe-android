package com.stripe.android.financialconnections.di

import com.stripe.android.financialconnections.presentation.ConsentState
import com.stripe.android.financialconnections.presentation.ConsentViewModel
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface ConsentSubcomponent {

    val viewModel: ConsentViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(state: ConsentState): Builder

        fun build(): ConsentSubcomponent
    }
}
