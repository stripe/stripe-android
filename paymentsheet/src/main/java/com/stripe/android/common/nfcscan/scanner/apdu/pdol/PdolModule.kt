package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import dagger.Binds
import dagger.Module
import dagger.Provides

@Module
internal interface PdolModule {
    @Binds
    fun bindsPdolBuilder(builder: DefaultPdolBuilder): PdolBuilder

    companion object {
        @Provides
        fun providesProducers(): Set<TagValueProducer> = setOf(
            AmountAuthorizedProducer,
            TransactionCurrencyCodeProducer,
            TerminalCountryCodeProducer,
            TransactionDateProducer,
            TerminalTransactionQualifiersProducer,
            UnpredictableNumberProducer,
            TerminalTypeProducer,
            EnhancedContactlessReaderCapabilitiesProducer
        )
    }
}
