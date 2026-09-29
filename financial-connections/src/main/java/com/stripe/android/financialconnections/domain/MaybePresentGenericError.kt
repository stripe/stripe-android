package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.genericErrorPane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.repository.GenericErrorContentRepository
import com.stripe.android.uicore.navigation.NavigationManager
import com.stripe.android.uicore.navigation.PopUpToBehavior
import javax.inject.Inject

/**
 * Shows the server-driven error pane when an API error asks for one.
 *
 * Any endpoint can opt in, so this is the single seam every error path goes through before falling
 * back to the SDK's own error handling.
 */
internal class MaybePresentGenericError @Inject constructor(
    private val contentRepository: GenericErrorContentRepository,
    private val navigationManager: NavigationManager,
) {

    /**
     * @return whether [error] carried a server-driven pane and we navigated to it. Callers should
     * leave the error alone when this returns `true`.
     */
    operator fun invoke(error: Throwable, referrer: Pane): Boolean {
        val pane = error.genericErrorPane() ?: return false

        contentRepository.set(pane)
        navigationManager.tryNavigateTo(
            route = Destination.GenericError(referrer = referrer),
            // The pane we came from is what failed, so replace it rather than stacking on top.
            popUpTo = PopUpToBehavior.Current(inclusive = true),
        )
        return true
    }
}
