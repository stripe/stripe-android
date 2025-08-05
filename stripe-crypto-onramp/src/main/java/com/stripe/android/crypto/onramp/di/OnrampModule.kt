package com.stripe.android.crypto.onramp.di

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.LinkController
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(
    subcomponents = [OnrampPresenterComponent::class]
)
internal class OnrampModule {

    @Provides
    @Singleton
    fun provideLinkController(
        application: Application,
        savedStateHandle: SavedStateHandle
    ): LinkController {
        return LinkController.create(
            application = application,
            savedStateHandle = savedStateHandle
        )
    }
}
