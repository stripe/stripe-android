package com.stripe.android.common.nfcscan.scanner

import com.stripe.android.common.nfcscan.scanner.apdu.pdol.PdolModule
import dagger.Binds
import dagger.Module

@Module(includes = [PdolModule::class])
internal interface NfcCardScannerModule {
    @Binds
    fun bindsNfcCardScanner(scanner: DefaultNfcCardScanner): NfcCardScanner

    @Binds
    fun bindsNfcTagTransceiverFactory(
        transceiver: IsoNfcTagTransceiver.Factory
    ): NfcTagTransceiver.Factory

    @Binds
    fun bindsNfcCardReader(reader: ApduCardReader): NfcCardReader

    @Binds
    fun bindsNfcCardDataParser(parser: DefaultNfcCardDataParser): NfcCardDataParser

    @Binds
    fun bindsNfcCardValidator(validator: DefaultNfcCardValidator): NfcCardValidator
}
