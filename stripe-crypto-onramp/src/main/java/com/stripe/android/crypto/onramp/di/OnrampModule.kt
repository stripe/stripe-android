package com.stripe.android.crypto.onramp.di

import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import dagger.Binds
import dagger.Module

@Module
internal abstract class OnrampModule {

    @Binds
    abstract fun bindCryptoRepository(
        cryptoApiRepository: CryptoApiRepository
    ): CryptoApiRepository
}
