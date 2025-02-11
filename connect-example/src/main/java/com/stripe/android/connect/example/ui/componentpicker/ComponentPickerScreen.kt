package com.stripe.android.connect.example.ui.componentpicker

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.core.Success
import com.stripe.android.connect.example.core.then
import com.stripe.android.connect.example.ui.appearance.AppearanceView
import com.stripe.android.connect.example.ui.appearance.AppearanceViewModel
import com.stripe.android.connect.example.ui.common.BetaBadge
import com.stripe.android.connect.example.ui.common.ConnectExampleScaffold
import com.stripe.android.connect.example.ui.common.CustomizeAppearanceIconButton
import com.stripe.android.connect.example.ui.embeddedcomponentmanagerloader.EmbeddedComponentLoaderViewModel
import com.stripe.android.connect.example.ui.embeddedcomponentmanagerloader.EmbeddedComponentManagerLoader
import com.stripe.android.connect.example.ui.features.accountonboarding.AccountOnboardingExampleActivity
import com.stripe.android.connect.example.ui.features.payouts.PayoutsExampleActivity
import kotlinx.coroutines.launch

@OptIn(PrivateBetaConnectSDK::class, ExperimentalMaterialApi::class)
@Composable
fun ComponentPickerContent(
    viewModel: EmbeddedComponentLoaderViewModel,
    openSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val embeddedComponentAsync = state.embeddedComponentManagerAsync

    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
    )
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheetLayout(
        modifier = Modifier.fillMaxSize(),
        sheetState = sheetState,
        sheetContent = {
            val appearanceViewModel = hiltViewModel<AppearanceViewModel>()
            AppearanceView(
                viewModel = appearanceViewModel,
                onDismiss = { coroutineScope.launch { sheetState.hide() } },
            )
        },
    ) {
        ConnectExampleScaffold(
            title = stringResource(R.string.connect_sdk_example),
            actions = (embeddedComponentAsync is Success).then {
                {
                    IconButton(onClick = openSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                    CustomizeAppearanceIconButton(onClick = { coroutineScope.launch { sheetState.show() } })
                }
            } ?: { },
        ) {
            EmbeddedComponentManagerLoader(
                embeddedComponentAsync = embeddedComponentAsync,
                reload = viewModel::reload,
                openSettings = openSettings,
            ) {
                ComponentPickerList()
            }
        }
    }
}

@Composable
fun ComponentPickerList() {
    LazyColumn {
        items(menuItems) { menuItem ->
            MenuRowItem(menuItem)
        }
    }
}

@Composable
private fun LazyItemScope.MenuRowItem(menuItem: MenuItem) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillParentMaxWidth()
            .clickable { context.startActivity(Intent(context, menuItem.activity)) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(menuItem.title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (menuItem.isBeta) {
                        BetaBadge()
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(menuItem.subtitle),
                    fontSize = 16.sp,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
        }

        Icon(
            modifier = Modifier
                .size(36.dp)
                .padding(start = 8.dp),
            contentDescription = null,
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        )
    }
}

private data class MenuItem(
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    val activity: Class<out ComponentActivity>,
    val isBeta: Boolean = false,
)

private val menuItems = listOf(
    MenuItem(
        title = R.string.account_onboarding,
        subtitle = R.string.account_onboarding_menu_subtitle,
        activity = AccountOnboardingExampleActivity::class.java,
        isBeta = true,
    ),
    MenuItem(
        title = R.string.payouts,
        subtitle = R.string.payouts_menu_subtitle,
        activity = PayoutsExampleActivity::class.java,
        isBeta = true,
    ),
)

// Previews

@Composable
@Preview(showBackground = true)
private fun ComponentListPreview() {
    ComponentPickerList()
}
