package com.stripe.android.financialconnections

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.PersistState
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForDataContract
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest

/**
 *  Class containing all of the data needed to represent the screen.
 */
internal data class FinancialConnectionsSheetState(
    val initialArgs: FinancialConnectionsSheetActivityArgs,
    val activityRecreated: Boolean = false,
    @PersistState val manifest: FinancialConnectionsSessionManifest? = null,
    @PersistState val authFlowActive: Boolean = false,
    val viewEffect: FinancialConnectionsSheetViewEffect? = null
) : MavericksState {

    val sessionSecret: String
        get() = initialArgs.configuration.financialConnectionsSessionClientSecret

    /**
     * Constructor used by Mavericks to build the initial state.
     */
    constructor(args: FinancialConnectionsSheetActivityArgs) : this(
        initialArgs = args
    )
}

/**
 *  Class containing all side effects intended to be run by the view.
 *
 *  Mostly one-off actions to be executed by the view will be instances of ViewEffect.
 */
internal sealed class FinancialConnectionsSheetViewEffect {

    /**
     * Open the AuthFlow native activity.
     */
    data class OpenNativeAuthFlow(
        val configuration: FinancialConnectionsSheet.Configuration,
        val manifest: FinancialConnectionsSessionManifest
    ) : FinancialConnectionsSheetViewEffect()

    /**
     * Open the AuthFlow.
     */
    data class OpenAuthFlowWithUrl(
        val url: String
    ) : FinancialConnectionsSheetViewEffect()

    /**
     * Finish [FinancialConnectionsSheetActivity] with a given [FinancialConnectionsSheetForDataContract.Result]
     */
    data class FinishWithResult(
        val result: FinancialConnectionsSheetActivityResult
    ) : FinancialConnectionsSheetViewEffect()
}
