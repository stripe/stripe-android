package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp
import dev.drewhamilton.poko.Poko

/**
 * A registered crypto consumer wallet.
 *
 * @property id The wallet record identifier.
 * @property isLiveMode Whether the wallet record exists in live mode.
 * @property network The crypto network for the wallet address.
 * @property walletAddress The registered wallet address.
 * @property verifiedOwnership Whether this wallet has completed ownership verification.
 */
@ExperimentalCryptoOnramp
@Poko
class CryptoConsumerWallet internal constructor(
    val id: String,
    val isLiveMode: Boolean,
    val network: CryptoNetwork,
    val walletAddress: String,
    val verifiedOwnership: Boolean,
)
