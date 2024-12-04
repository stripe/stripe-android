package com.stripe.android.connect.example.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ConnectExampleScaffold(
    title: String,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modalContent: (@Composable ColumnScope.() -> Unit)? = null,
    modalSheetState: ModalBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
    ),
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
            if (modalContent != null) {
                ModalBottomSheetLayout(
                    modifier = Modifier.fillMaxSize(),
                    sheetState = modalSheetState,
                    sheetContent = modalContent,
                    content = content,
                )
            } else {
                content()
            }
        }
    }
}

// Previews

@OptIn(ExperimentalMaterialApi::class)
@Preview(showBackground = true)
@Composable
fun ConnectExampleScaffoldPreview() {
    ConnectExampleScaffold(
        title = "Title",
        content = {
            Text("Content")
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Preview(showBackground = true)
@Composable
private fun ConnectExampleScaffoldWithNavigationIconPreview() {
    ConnectExampleScaffold(
        title = "Title",
        navigationIcon = { BackIconButton(onClick = { }) },
        content = {
            Text("Content")
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Preview(showBackground = true)
@Composable
private fun ConnectExampleScaffoldWithActionsPreview() {
    ConnectExampleScaffold(
        title = "Title",
        actions = { MoreIconButton(onClick = { }) },
        content = {
            Text("Content")
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Preview(showBackground = true)
@Composable
private fun ConnectExampleScaffoldWithModalPreview() {
    ConnectExampleScaffold(
        title = "Title",
        modalContent = {
            Text("Modal Content")
        },
        modalSheetState = rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Expanded,
            skipHalfExpanded = true,
        ),
        content = {
            Text("Content")
        }
    )
}
