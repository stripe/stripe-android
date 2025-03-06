package com.stripe.android.link.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.rememberNavController
import com.stripe.android.link.LinkScreen
import com.stripe.android.paymentsheet.R
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test

internal class LinkContentScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    @OptIn(ExperimentalMaterialApi::class)
    @Test
    fun testLinkContentScreenHasOpaqueBackground() {
        paparazziRule.snapshot {
            val viewModelStoreOwner = object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                val navController = rememberNavController()
                Box {
                    Text(
                        modifier = Modifier
                            .align(Alignment.TopStart),
                        text = "Hello!"
                    )
                    LinkContent(
                        navController = navController,
                        appBarState = LinkAppBarState(
                            navigationIcon = R.drawable.stripe_link_close,
                            showHeader = false,
                            showOverflowMenu = false,
                            email = null
                        ),
                        sheetState = ModalBottomSheetState(
                            initialValue = ModalBottomSheetValue.Hidden,
                            density = Density(1f)
                        ),
                        bottomSheetContent = null,
                        initialDestination = LinkScreen.Wallet,
                        onUpdateSheetContent = {},
                        handleViewAction = {},
                        navigate = { _, _ -> },
                        dismissWithResult = null,
                        getLinkAccount = { null },
                        onBackPressed = {},
                        moveToWeb = {},
                        goBack = {},
                        changeEmail = {},
                    )
                }
            }
        }
    }
}
