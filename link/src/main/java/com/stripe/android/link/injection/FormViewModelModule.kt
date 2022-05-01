package com.stripe.android.link.injection

import android.content.Context
import com.stripe.android.ui.core.forms.TransformSpecToElements
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import dagger.Module
import dagger.Provides

@Module
internal abstract class FormViewModelModule {

    companion object {

        @Provides
        fun provideTransformSpecToElements(
            resourceRepository: ResourceRepository,
            context: Context
        ) = TransformSpecToElements(
            resourceRepository = resourceRepository,
            initialValues = emptyMap(),
            amount = null,
            saveForFutureUseInitialValue = false,
            merchantName = "Merchant",
            context = context
        )
    }
}
