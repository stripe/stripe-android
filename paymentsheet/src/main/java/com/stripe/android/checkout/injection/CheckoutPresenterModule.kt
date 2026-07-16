package com.stripe.android.checkout.injection

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.checkout.CheckoutEmbeddedResultCallbackHelper
import com.stripe.android.core.utils.StatusBarCompat
import com.stripe.android.paymentelement.embedded.EmbeddedResultCallbackHelper
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedSheetLauncher
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named

/**
 * Supplies the presenter-scoped bindings the reused [DefaultEmbeddedSheetLauncher] needs — the
 * activity (as [ActivityResultCaller]/[LifecycleOwner]) it registers against and the status bar
 * color it forwards to launched sheets — plus a checkout [EmbeddedResultCallbackHelper]. Everything
 * else the launcher depends on is provided by the parent component.
 */
@Module
internal interface CheckoutPresenterModule {
    @Binds
    fun bindsEmbeddedSheetLauncher(launcher: DefaultEmbeddedSheetLauncher): EmbeddedSheetLauncher

    @Binds
    fun bindsEmbeddedResultCallbackHelper(
        helper: CheckoutEmbeddedResultCallbackHelper
    ): EmbeddedResultCallbackHelper

    companion object {
        @Provides
        fun provideActivityResultCaller(activity: ComponentActivity): ActivityResultCaller = activity

        @Provides
        fun provideLifecycleOwner(activity: ComponentActivity): LifecycleOwner = activity

        @Provides
        @Named(STATUS_BAR_COLOR)
        fun provideStatusBarColor(activity: ComponentActivity): Int? = StatusBarCompat.color(activity)
    }
}
