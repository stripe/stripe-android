package com.stripe.android.link.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.rememberNavController
import com.stripe.android.link.LinkScreen
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
                        modifier = Modifier,
                        navController = navController,
                        appBarState = LinkAppBarState(
                            canNavigateBack = false,
                            showHeader = false,
                        ),
                        bottomSheetContent = null,
                        initialDestination = LinkScreen.Loading,
                        onUpdateSheetContent = {},
                        handleViewAction = {},
                        navigate = { _, _ -> },
                        dismissWithResult = {},
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
