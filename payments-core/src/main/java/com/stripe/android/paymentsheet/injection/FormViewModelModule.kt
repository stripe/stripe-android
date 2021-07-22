package com.stripe.android.paymentsheet.injection

import android.app.Application
import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
internal abstract class FormViewModelModule {

    @Binds
    abstract fun bindsApplicationForContext(application: Application): Context

    companion object {

    }
}
