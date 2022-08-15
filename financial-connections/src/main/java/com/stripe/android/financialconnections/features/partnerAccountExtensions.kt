package com.stripe.android.financialconnections.features

import com.stripe.android.financialconnections.model.PartnerAccount

internal val PartnerAccount.encryptedNumbers get() = displayableAccountNumbers?.let { "••••$it" } ?: ""

internal val PartnerAccount.fullName get() = "${this.name} $encryptedNumbers"
