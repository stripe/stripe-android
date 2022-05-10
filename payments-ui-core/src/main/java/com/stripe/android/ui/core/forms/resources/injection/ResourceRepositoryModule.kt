package com.stripe.android.ui.core.forms.resources.injection

import android.content.Context
import android.content.res.Resources
import com.stripe.android.ui.core.forms.resources.AsyncResourceRepository
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
abstract class ResourceRepositoryModule {
    @Binds
    abstract fun bindsResourceRepository(asyncResourceRepository: AsyncResourceRepository):
        ResourceRepository

    companion object {

        @Provides
        @Singleton
        fun provideResources(context: Context): Resources {
            return context.resources
        }
    }
}
