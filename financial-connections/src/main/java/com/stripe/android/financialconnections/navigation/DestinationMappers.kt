package com.stripe.android.financialconnections.navigation

import androidx.navigation.NavDestination
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane

private val paneToDestination = mapOf(
    Pane.INSTITUTION_PICKER to Destination.InstitutionPicker,
    Pane.CONSENT to Destination.Consent,
    Pane.PARTNER_AUTH to Destination.PartnerAuth,
    Pane.PARTNER_AUTH_DRAWER to Destination.PartnerAuthDrawer,
    Pane.ACCOUNT_PICKER to Destination.AccountPicker,
    Pane.SUCCESS to Destination.Success,
    Pane.MANUAL_ENTRY to Destination.ManualEntry,
    Pane.ATTACH_LINKED_PAYMENT_ACCOUNT to Destination.AttachLinkedPaymentAccount,
    Pane.NETWORKING_LINK_SIGNUP_PANE to Destination.NetworkingLinkSignup,
    Pane.LINK_LOGIN to Destination.NetworkingLinkSignup,
    Pane.NETWORKING_LINK_LOGIN_WARMUP to Destination.NetworkingLinkLoginWarmup,
    Pane.NETWORKING_LINK_VERIFICATION to Destination.NetworkingLinkVerification,
    Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION to Destination.NetworkingSaveToLinkVerification,
    Pane.LINK_ACCOUNT_PICKER to Destination.LinkAccountPicker,
    Pane.LINK_STEP_UP_VERIFICATION to Destination.LinkStepUpVerification,
    Pane.RESET to Destination.Reset,
    Pane.UNEXPECTED_ERROR to Destination.Error,
    Pane.EXIT to Destination.Exit,
    Pane.BANK_AUTH_REPAIR to Destination.BankAuthRepair,
    Pane.MANUAL_ENTRY_SUCCESS to Destination.ManualEntrySuccess,
    Pane.NOTICE to Destination.Notice,
    Pane.ACCOUNT_UPDATE_REQUIRED to Destination.AccountUpdateRequired,
    Pane.STREAMLINED_CONSENT to Destination.StreamlinedConsent,
)

internal val Pane.destination: Destination
    get() = paneToDestination[this]
        ?: throw IllegalArgumentException("No corresponding destination for $this")

internal val NavDestination.pane: Pane
    get() = paneToDestination.entries
        .firstOrNull { (_, destination) -> destination.fullRoute == route }
        ?.key
        ?: throw IllegalArgumentException("No corresponding destination for $this")
