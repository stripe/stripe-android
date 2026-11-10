package com.stripe.android.paymentsheet.injection

import com.stripe.android.common.analytics.experiment.DefaultPreselectLinkExperiment
import com.stripe.android.common.analytics.experiment.PreselectLinkExperiment
import dagger.Binds
import dagger.Module

@Module
internal interface PreselectLinkExperimentModule {
    @Binds
    fun bindsPreselectLinkExperiment(impl: DefaultPreselectLinkExperiment): PreselectLinkExperiment
}
