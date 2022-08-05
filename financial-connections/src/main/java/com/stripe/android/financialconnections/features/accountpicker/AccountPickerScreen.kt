package com.stripe.android.financialconnections.features.accountpicker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.institutionpicker.LoadingContent
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun AccountPickerScreen() {
    // activity view model
    val activityViewModel: FinancialConnectionsSheetNativeViewModel = mavericksActivityViewModel()
    val authSessionId = activityViewModel.collectAsState { it.authorizationSession?.id }

    val viewModel: AccountPickerViewModel = mavericksViewModel()
    val state: State<AccountPickerState> = viewModel.collectAsState()
    LaunchedEffect(authSessionId) {
        authSessionId.value?.let { viewModel.onAuthSessionReceived(it) }
    }
    AccountPickerContent(state.value)
}

@Composable
private fun AccountPickerContent(state: AccountPickerState) {
    FinancialConnectionsScaffold {
        when (val accounts = state.accounts) {
            Uninitialized, is Loading -> LoadingContent(
                R.string.stripe_account_picker_loading_title,
                R.string.stripe_account_picker_loading_desc
            )
            is Success -> AccountPickerLoaded(accounts())
            is Fail -> UnclassifiedErrorContent()
        }
    }
}

@Composable
private fun AccountPickerLoaded(accounts: PartnerAccountsList) {
    Column {
        Text(
            modifier = Modifier
                .fillMaxWidth(),
            text = stringResource(R.string.stripe_institutionpicker_pane_select_bank),
            style = FinancialConnectionsTheme.typography.subtitle
        )
        LazyColumn {
            items(accounts.data, key = { it.id }) { account ->
                Text(text = "${account.name} ****${account.displayableAccountNumbers}")
            }
        }
    }
}
