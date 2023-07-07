package com.stripe.android.customersheet.injection

import android.content.Context
import com.stripe.android.uicore.image.StripeImageLoader
import dagger.Module
import dagger.Provides

@Module
internal class CustomerSheetModule {
    @Provides
    fun provideStripeImageLoader(context: Context): StripeImageLoader {
        return StripeImageLoader(context)
    }
}