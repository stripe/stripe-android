package com.stripe.android.financialconnections.webview

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatActivity.RESULT_CANCELED
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.stripe.android.financialconnections.FinancialConnectionsSheetActivity.Companion.EXTRA_ARGS
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Completed
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccountList
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.webview.FinancialConnectionsWebviewViewModel.ViewEffect.OpenAuthFlowWithUrl
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class FinancialConnectionsWebviewViewModel(
    application: Application,
    val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val httpClient = HttpClient()
    private val args: FinancialConnectionsSheetActivityArgs
        get() = savedStateHandle.get<FinancialConnectionsSheetActivityArgs>(EXTRA_ARGS)!!

    internal val viewEffects = MutableSharedFlow<ViewEffect>()

    init {
        viewModelScope.launch {
            val response = GetHostedAuthUrl(httpClient).invoke(
                clientSecret = args.configuration.financialConnectionsSessionClientSecret,
                applicationId = application.packageName,
                publishableKey = args.configuration.publishableKey,
            )

            // Load the URL in the WebView
            val manifest = response?.getJSONObject("manifest")
            if (manifest != null) {
                viewEffects.emit(
                    OpenAuthFlowWithUrl(manifest.getString("hosted_auth_url"))
                )
            } else {
                Log.e("FinancialConnections", "Failed to load hosted URL")
            }
        }
    }

    internal fun onAuthFlowFinished(url: String) {
        val configuration = args.configuration
        viewModelScope.launch {
            val session = GetFinancialConnectionsSession(httpClient).invoke(
                clientSecret = configuration.financialConnectionsSessionClientSecret,
                publishableKey = configuration.publishableKey,
            )

            if (session != null) {
                val accounts = session.getJSONObject("accounts")
                val accountsData = accounts.getJSONArray("data")
                ViewEffect.FinishWithResult(
                    Completed(
                        financialConnectionsSession = FinancialConnectionsSession(
                            id = session.getString("id"),
                            clientSecret = session.getString("client_secret"),
                            livemode = session.getBoolean("livemode"),
                            accountsNew = FinancialConnectionsAccountList(
                                url = accounts.getString("url"),
                                hasMore = false,
                                totalCount = accounts.getJSONArray("data").length(),
                                data = (0 until accountsData.length()).map { index ->
                                    val accountJson = accountsData.getJSONObject(index)
                                    FinancialConnectionsAccount(
                                        id = accountJson.getString("id"),
                                        created = accountJson.getInt("created"),
                                        institutionName = accountJson.getString("institution_name"),
                                        livemode = accountJson.getBoolean("livemode"),
                                        supportedPaymentMethodTypes = emptyList()
                                    )
                                }
                            )
                        )
                    )
                ).let { viewEffects.emit(it) }
            } else {
                Log.e("FinancialConnections", "Failed to load session")
                viewEffects.emit(
                    ViewEffect.FinishWithResult(
                        FinancialConnectionsSheetActivityResult.Canceled
                    )
                )
            }
        }
    }

    internal sealed class ViewEffect {
        data class OpenAuthFlowWithUrl(val url: String) : ViewEffect()
        data class FinishWithResult(val result: FinancialConnectionsSheetActivityResult) : ViewEffect()
    }
}