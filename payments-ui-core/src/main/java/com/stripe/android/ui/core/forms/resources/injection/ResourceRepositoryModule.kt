package com.stripe.android.ui.core.forms.resources.injection

import android.content.Context
import android.content.res.Resources
import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.forms.resources.LpmRepository
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ResourceRepositoryModule {
    @Provides
    @Singleton
    fun provideResources(context: Context): Resources {
        return context.resources
    }

    @Provides
    @Singleton
    fun providesLpmRepository(resources: Resources): LpmRepository {
        return LpmRepository.getInstance(LpmRepository.LpmRepositoryArguments(resources))
    }
}
