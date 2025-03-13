package com.stripe.android.financialconnections

import android.os.Bundle
import androidx.annotation.StringRes
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.ui.theme.Theme
import com.stripe.android.financialconnections.ui.theme.Theme.DefaultLight
import com.stripe.android.financialconnections.ui.theme.Theme.LinkLight

/**
 *  Class containing all of the data needed to represent the screen.
 */
internal data class FinancialConnectionsSheetState(
    val initialArgs: FinancialConnectionsSheetActivityArgs,
    val activityRecreated: Boolean,
    val manifest: FinancialConnectionsSessionManifest?,
    val webAuthFlowStatus: AuthFlowStatus,
    val viewEffect: FinancialConnectionsSheetViewEffect?
) {
    val isInstantDebits: Boolean
        get() = initialArgs is FinancialConnectionsSheetActivityArgs.ForInstantDebits

    val theme: Theme
        // We can't rely on the value coming from the `manifest` here, because its initial value will be null
        get() = if (isInstantDebits) LinkLight else DefaultLight

    val sessionSecret: String
        get() = initialArgs.configuration.financialConnectionsSessionClientSecret

    /**
     * Constructor used to build the initial state.
     */
    constructor(args: FinancialConnectionsSheetActivityArgs, savedState: Bundle?) : this(
        initialArgs = args,
        activityRecreated = false,
        manifest = savedState?.getParcelable(KEY_MANIFEST),
        webAuthFlowStatus = savedState?.getSerializable(KEY_WEB_AUTH_FLOW_STATUS)
            as? AuthFlowStatus
            ?: AuthFlowStatus.NONE,
        viewEffect = null
    )

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

    companion object {
        const val KEY_SAVED_STATE = "financial_connections_sheet_state"
        const val KEY_MANIFEST = "financial_connections_sheet_manifest"
        const val KEY_WEB_AUTH_FLOW_STATUS = "financial_connections_sheet_web_auth_flow_status"
    }
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
        val configuration: FinancialConnectionsSheetConfiguration,
        val initialSyncResponse: SynchronizeSessionResponse,
        val elementsSessionContext: ElementsSessionContext?,
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
