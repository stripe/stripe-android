package com.stripe.android.paymentelement.embedded.content

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class EmbeddedSheetLauncherHolder @Inject constructor() {
    var sheetLauncher: EmbeddedSheetLauncher? = null
}
