package com.stripe.android.link.injection

import com.stripe.android.link.LinkActivityContract
import com.stripe.android.ui.core.injection.FormControllerSubcomponent
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

/**
 * Module that extracts variables needed for injection from [LinkActivityContract.Args].
 */
@Module(
    subcomponents = [
        SignedInViewModelSubcomponent::class,
        SignUpViewModelSubcomponent::class,
        FormControllerSubcomponent::class
    ]
)
internal interface LinkActivityContractArgsModule {
    companion object {
        @Provides
        @Singleton
        @Named(LINK_INTENT)
        fun provideStripeIntent(args: LinkActivityContract.Args) = args.stripeIntent

        @Provides
        @Singleton
        @Named(MERCHANT_NAME)
        fun provideMerchantName(args: LinkActivityContract.Args) = args.merchantName

        @Provides
        @Singleton
        @Named(CUSTOMER_EMAIL)
        fun provideCustomerEmail(args: LinkActivityContract.Args) = args.customerEmail

        @Provides
        @Singleton
        @Named(CUSTOMER_PHONE)
        fun provideCustomerPhone(args: LinkActivityContract.Args) = args.customerPhone
    }
}
