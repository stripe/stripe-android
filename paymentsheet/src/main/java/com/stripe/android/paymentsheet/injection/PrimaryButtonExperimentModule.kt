package com.stripe.android.paymentsheet.injection

import com.stripe.android.common.analytics.experiment.DefaultPrimaryButtonExperiment
import com.stripe.android.common.analytics.experiment.PrimaryButtonExperiment
import dagger.Binds
import dagger.Module

@Module
internal interface PrimaryButtonExperimentModule {
    @Binds
    fun bindsPrimaryButtonExperiment(impl: DefaultPrimaryButtonExperiment): PrimaryButtonExperiment
}
