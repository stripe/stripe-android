package com.stripe.android.connect.example.ui.common

import android.content.Context
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.stripe.android.connect.example.ConnectExampleScaffold
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.ui.appearance.AppearanceView
import kotlinx.coroutines.launch

@Composable
fun BasicExampleComponent(
    title: String,
    finish: () -> Unit,
    createComponentView: (context: Context) -> View,
) {
    BackHandler(onBack = finish)
    BasicExampleComponentContent(
        title = title,
        navigationIcon = {
            BackIconButton(onClick = finish)
        }
    ) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { context ->
            createComponentView(context)
        })
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BasicExampleComponentContent(
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

    ConnectExampleScaffold(
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
