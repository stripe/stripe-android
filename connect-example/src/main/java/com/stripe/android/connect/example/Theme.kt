package com.stripe.android.connect.example

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.darkColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.lightColors
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.stripe.android.connect.example.ui.appearance.AppearanceView
import kotlinx.coroutines.launch

@Composable
fun ConnectSdkExampleTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colors = if (isSystemInDarkTheme()) darkColors() else lightColors(),
        content = content,
    )
}

@Composable
fun MainContent(
    title: String,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = navigationIcon,
                actions = actions,
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BasicComponentContent(
    title: String,
    navigationIcon: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
    )
    val coroutineScope = rememberCoroutineScope()
    fun toggleAppearanceSheet() {
        coroutineScope.launch {
            if (!sheetState.isVisible) {
                sheetState.show()
            } else {
                sheetState.hide()
            }
        }
    }

    MainContent(
        title = title,
        navigationIcon = navigationIcon,
        actions = {
            IconButton(onClick = { toggleAppearanceSheet() }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.customize_appearance),
                )
            }
        },
    ) {
        ModalBottomSheetLayout(
            modifier = Modifier.fillMaxSize(),
            sheetState = sheetState,
            sheetContent = {
                AppearanceView(
                    onDismiss = { coroutineScope.launch { sheetState.hide() } },
                )
            },
            content = content,
        )
    }
}
