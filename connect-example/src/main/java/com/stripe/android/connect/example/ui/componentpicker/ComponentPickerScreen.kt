package com.stripe.android.connect.example.ui.componentpicker

import android.util.Log
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.stripe.android.connect.AccountOnboardingListener
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.StripeComponentController
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
import com.stripe.android.connect.example.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

@Suppress("LongMethod")
@OptIn(PrivateBetaConnectSDK::class, ExperimentalMaterialApi::class)
@Composable
fun ComponentPickerContent(
    viewModel: EmbeddedComponentLoaderViewModel,
    settingsViewModel: SettingsViewModel,
    openSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val settingsState by settingsViewModel.state.collectAsState()
    val embeddedComponentAsync = state.embeddedComponentManagerAsync
    val context = LocalContext.current as FragmentActivity

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
            ) { embeddedComponentManager ->
                val onboardingSettings = settingsState.onboardingSettings
                val onboardingController = remember(embeddedComponentManager, onboardingSettings) {
                    val controller = embeddedComponentManager.createAccountOnboardingController(
                        activity = context,
                        title = "Account Onboarding",
                        props = onboardingSettings.toProps(),
                    )
                    val logTag = "AccountOnboarding"
                    val listener = object : AccountOnboardingListener {
                        override fun onLoaderStart() {
                            Log.d(logTag, "onLoaderStart")
                        }

                        override fun onLoadError(error: Throwable) {
                            Log.d(logTag, "onLoadError", error)
                        }

                        override fun onExit() {
                            Log.d(logTag, "onExit")
                            controller.dismiss()
                        }
                    }
                    controller.apply {
                        this.listener = listener
                        this.onDismissListener = StripeComponentController.OnDismissListener {
                            Log.d(logTag, "onDismiss")
                        }
                    }
                }

                ComponentPickerList(
                    onMenuItemClick = { menuItem ->
                        when (menuItem) {
                            MenuItem.AccountOnboarding -> {
                                onboardingController.show()
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ComponentPickerList(onMenuItemClick: (MenuItem) -> Unit) {
    val items = remember { listOf(MenuItem.AccountOnboarding) }
    LazyColumn {
        items(items) { menuItem ->
            MenuRowItem(menuItem, onMenuItemClick)
        }
    }
}

@Composable
private fun LazyItemScope.MenuRowItem(
    menuItem: MenuItem,
    onMenuItemClick: (MenuItem) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillParentMaxWidth()
            .clickable(onClick = { onMenuItemClick(menuItem) }),
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

private enum class MenuItem(
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    val isBeta: Boolean = false,
) {
    AccountOnboarding(
        title = R.string.account_onboarding,
        subtitle = R.string.account_onboarding_menu_subtitle,
        isBeta = true,
    ),
}

// Previews

@Composable
@Preview(showBackground = true)
private fun ComponentListPreview() {
    ComponentPickerList(onMenuItemClick = {})
}
