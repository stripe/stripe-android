package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet

@Module
internal interface PdolModule {
    @Binds
    fun bindsPdolBuilder(parser: DefaultPdolBuilder): PdolBuilder

    companion object {
        @Provides
        @IntoSet
        fun providesTransactionQualifiersTagValueProducer(): TagValueProducer = TerminalTransactionQualifiersProducer

        @Provides
        @IntoSet
        fun providesUnpredictableNumberProducer(): TagValueProducer = UnpredictableNumberProducer
    }
}
