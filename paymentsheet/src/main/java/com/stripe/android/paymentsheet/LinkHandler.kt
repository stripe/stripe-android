package com.stripe.android.paymentsheet

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.paymentsheet.state.LinkState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LinkHandler @Inject constructor(
    val linkConfigurationCoordinator: LinkConfigurationCoordinator,
) {
    private val _isLinkEnabled = MutableStateFlow<Boolean?>(null)
    val isLinkEnabled: StateFlow<Boolean?> = _isLinkEnabled

    private val _linkConfiguration = MutableStateFlow<LinkConfiguration?>(null)
    val linkConfiguration: StateFlow<LinkConfiguration?> = _linkConfiguration.asStateFlow()

    fun setupLink(state: LinkState?) {
        _isLinkEnabled.value = state != null

        if (state == null) return

        _linkConfiguration.value = state.configuration
    }

    fun setupLink(
        state: LinkState?,
        scope: CoroutineScope,
        launchLinkEagerly: suspend (LinkConfiguration) -> Unit
    ) {
        _isLinkEnabled.value = state != null

        if (state == null) return

        _linkConfiguration.value = state.configuration

        scope.launch {
            val accountStatus = linkConfigurationCoordinator
                .getAccountStatusFlow(state.configuration)
                .first()

            when (accountStatus) {
                AccountStatus.Verified,
                AccountStatus.NeedsVerification,
                AccountStatus.VerificationStarted -> {
                    launchLinkEagerly(state.configuration)
                }
                AccountStatus.SignedOut, AccountStatus.Error -> Unit
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun logOut() {
        val configuration = linkConfiguration.value ?: return

        GlobalScope.launch {
            // This usage is intentional. We want the request to be sent without regard for the UI lifecycle.
            linkConfigurationCoordinator.logOut(configuration = configuration)
        }
    }
}
