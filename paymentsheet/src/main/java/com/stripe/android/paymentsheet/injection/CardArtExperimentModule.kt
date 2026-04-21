package com.stripe.android.paymentsheet.injection

import com.stripe.android.common.analytics.experiment.DefaultLogCardArtExperiment
import com.stripe.android.common.analytics.experiment.LogCardArtExperiment
import dagger.Binds
import dagger.Module

@Module
internal abstract class CardArtExperimentModule {

    @Binds
    abstract fun bindLogCardArtExperiment(
        default: DefaultLogCardArtExperiment
    ): LogCardArtExperiment
}
