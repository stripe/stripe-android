package com.stripe.android.financialconnections.features.attachpayment

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.domain.PollAttachPaymentAccount
import com.stripe.android.financialconnections.domain.GetAuthorizationSessionAccounts
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.NextPane
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject

internal class AttachPaymentViewModel @Inject constructor(
    initialState: AttachPaymentState,
    private val pollAttachPaymentAccount: PollAttachPaymentAccount,
    private val getAuthorizationSessionAccounts: GetAuthorizationSessionAccounts,
    private val getManifest: GetManifest,
    private val goNext: GoNext,
    private val logger: Logger
) : MavericksViewModel<AttachPaymentState>(initialState) {

    init {
        logErrors()
        suspend {
            val manifest = getManifest()
            val authSessionId = manifest.activeAuthSession!!.id
            AttachPaymentState.Payload(
                businessName = manifest.businessName,
                accountsCount = getAuthorizationSessionAccounts(authSessionId).data.size
            )
        }.execute { copy(payload = it) }
        suspend {
            val manifest = getManifest()
            val authSessionId = manifest.activeAuthSession!!.id
            val accounts = getAuthorizationSessionAccounts(authSessionId).data
            require(accounts.size == 1)
            val id = accounts.first().linkedAccountId
            pollAttachPaymentAccount(PaymentAccountParams.LinkedAccount(requireNotNull(id)))
                .also { goNext(it.nextPane ?: NextPane.SUCCESS) }
        }.execute { copy(linkPaymentAccount = it) }
    }

    private fun logErrors() {
        onAsync(AttachPaymentState::payload, onFail = {
            logger.error("Error retrieving accounts to attach payment", it)
        })
        onAsync(AttachPaymentState::linkPaymentAccount, onFail = {
            logger.error("Error Attaching payment account", it)
        })
    }

    companion object : MavericksViewModelFactory<AttachPaymentViewModel, AttachPaymentState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: AttachPaymentState
        ): AttachPaymentViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .attachPaymentSubcomponent
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

internal data class AttachPaymentState(
    val payload: Async<Payload> = Uninitialized,
    val linkPaymentAccount: Async<LinkAccountSessionPaymentAccount> = Uninitialized
) : MavericksState {
    data class Payload(
        val accountsCount: Int,
        val businessName: String?
    )
}
