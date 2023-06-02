package com.stripe.android.financialconnections

import androidx.annotation.StringRes
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.PersistState
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse

/**
 *  Class containing all of the data needed to represent the screen.
 */
internal data class FinancialConnectionsSheetState(
    val initialArgs: FinancialConnectionsSheetActivityArgs,
    val activityRecreated: Boolean = false,
    @PersistState val manifest: FinancialConnectionsSessionManifest? = null,
    @PersistState val webAuthFlowStatus: AuthFlowStatus = AuthFlowStatus.NONE,
    val viewEffect: FinancialConnectionsSheetViewEffect? = null
) : MavericksState {

    val sessionSecret: String
        get() = initialArgs.configuration.financialConnectionsSessionClientSecret

    enum class AuthFlowStatus {
        /**
         * AuthFlow is happening outside of the SDK (app2app, web browser, etc).
         *
         * If no deeplink is received an we're on this state, external activity was cancelled.
         */
        ON_EXTERNAL_ACTIVITY,

        /**
         * We came back from an external activity but the flow is not yet completed.
         * - coming from browser and opening app2ap
         * - coming from app2app and opening browser
         *
         * It'll be a short status until the next external activity is opened, moving again to
         * [ON_EXTERNAL_ACTIVITY].
         *
         */
        INTERMEDIATE_DEEPLINK,

        /**
         * We're in an SDK activity and lifecycle should be handled normally.
         */
        NONE
    }

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
        val initialSyncResponse: SynchronizeSessionResponse
    ) : FinancialConnectionsSheetViewEffect()

    /**
     * Open the AuthFlow.
     */
    data class OpenAuthFlowWithUrl(
        val url: String
    ) : FinancialConnectionsSheetViewEffect()

    /**
     * Finish [FinancialConnectionsSheetActivity] with a given [FinancialConnectionsSheetActivityResult]
     */
    data class FinishWithResult(
        val result: FinancialConnectionsSheetActivityResult,
        @StringRes val finishToast: Int? = null
    ) : FinancialConnectionsSheetViewEffect()
}
