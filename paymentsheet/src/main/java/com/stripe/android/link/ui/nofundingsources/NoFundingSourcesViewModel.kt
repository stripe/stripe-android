package com.stripe.android.link.ui.nofundingsources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.linkAccountUpdate
import com.stripe.android.link.effectiveLinkBrand
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.wallet.secondaryButtonLabel
import com.stripe.android.model.LinkBrand

internal data class NoFundingSourcesViewState(
    val linkBrand: LinkBrand,
    /**
     * The label of the button that dismisses Link so the user can pay another way, or null when the user
     * can't continue without Link.
     */
    val payAnotherWayLabel: ResolvableString?,
)

/**
 * Shown instead of the wallet when none of the consumer's payment details types are supported for this session.
 */
internal class NoFundingSourcesViewModel(
    linkAccount: LinkAccount,
    configuration: LinkConfiguration,
    linkLaunchMode: LinkLaunchMode,
    private val linkAccountManager: LinkAccountManager,
    private val dismissWithResult: (LinkActivityResult) -> Unit,
    private val logInToAnotherAccount: () -> Unit,
) : ViewModel() {

    val viewState = NoFundingSourcesViewState(
        linkBrand = configuration.effectiveLinkBrand(linkAccount),
        payAnotherWayLabel = configuration.stripeIntent.secondaryButtonLabel(linkLaunchMode),
    )

    fun onPayAnotherWayClicked() {
        dismissWithResult(
            LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.PayAnotherWay,
                linkAccountUpdate = linkAccountManager.linkAccountUpdate
            )
        )
    }

    fun onLogInToAnotherAccountClicked() {
        logInToAnotherAccount()
    }

    companion object {
        fun factory(
            parentComponent: NativeLinkComponent,
            linkAccount: LinkAccount,
            dismissWithResult: (LinkActivityResult) -> Unit,
            logInToAnotherAccount: () -> Unit,
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    NoFundingSourcesViewModel(
                        linkAccount = linkAccount,
                        configuration = parentComponent.configuration,
                        linkLaunchMode = parentComponent.linkLaunchMode,
                        linkAccountManager = parentComponent.linkAccountManager,
                        dismissWithResult = dismissWithResult,
                        logInToAnotherAccount = logInToAnotherAccount,
                    )
                }
            }
        }
    }
}
