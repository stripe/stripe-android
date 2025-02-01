package com.stripe.android.paymentelement.confirmation.cvc

import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentsheet.cvcrecollection.CvcRecollectionHandler
import com.stripe.android.paymentsheet.cvcrecollection.CvcRecollectionHandlerImpl
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.DefaultCvcRecollectionLauncherFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet

@Module
internal interface CvcRecollectionConfirmationModule {
    @JvmSuppressWildcards
    @Binds
    @IntoSet
    fun providesCvcConfirmationDefinition(
        cvcReConfirmationDefinition: CvcRecollectionConfirmationDefinition,
    ): ConfirmationDefinition<*, *, *, *>

    companion object {
        @Provides
        fun provideCvcRecollectionLauncherFactory(): CvcRecollectionLauncherFactory {
            return DefaultCvcRecollectionLauncherFactory
        }

        @Provides
        fun provideCvcRecollectionHandler(): CvcRecollectionHandler {
            return CvcRecollectionHandlerImpl()
        }
    }
}
