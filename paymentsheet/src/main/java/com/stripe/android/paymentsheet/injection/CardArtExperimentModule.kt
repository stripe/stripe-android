package com.stripe.android.paymentsheet.injection

import com.stripe.android.common.analytics.experiment.CardArtExperimentHandler
import com.stripe.android.common.analytics.experiment.DefaultCardArtExperimentHandler
import dagger.Binds
import dagger.Module

@Module
internal abstract class CardArtExperimentModule {

    @Binds
    abstract fun bindCardArtExperimentHandler(
        default: DefaultCardArtExperimentHandler
    ): CardArtExperimentHandler
}
