package com.stripe.android.paymentelement.embedded.content

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the imperatively-set [EmbeddedSheetLauncher] so the (singleton) content pieces can share it.
 * The launcher is bound to an activity's [androidx.activity.result.ActivityResultCaller], so it can't
 * be a constructor dependency of the singletons that outlive the activity: [DefaultEmbeddedContentHelper]
 * sets/clears it across the activity lifecycle, while [DefaultEmbeddedContentHelperDataSource] reads it
 * lazily from the payment-method-list interactor's navigation callbacks. This holder decouples the two
 * so neither has to depend on the other.
 */
@Singleton
internal class EmbeddedSheetLauncherHolder @Inject constructor() {
    var sheetLauncher: EmbeddedSheetLauncher? = null
}
