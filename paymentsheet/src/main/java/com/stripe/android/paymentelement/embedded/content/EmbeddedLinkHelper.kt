package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.paymentsheet.LinkHandler
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

internal interface EmbeddedLinkHelper {
    val linkEmail: StateFlow<String?>
}

internal class DefaultEmbeddedLinkHelper @Inject constructor(
    linkHandler: LinkHandler,
) : EmbeddedLinkHelper {
    override val linkEmail: StateFlow<String?> = linkHandler.linkConfigurationCoordinator.emailFlow
}
