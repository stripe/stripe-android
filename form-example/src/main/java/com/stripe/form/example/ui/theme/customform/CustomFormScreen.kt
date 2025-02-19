package com.stripe.form.example.ui.theme.customform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.form.FormUI

@Composable
fun CustomFormScreen(
    navController: NavHostController,
    viewModel: CustomFormViewModel
) {
    val state by viewModel.state.collectAsState()
    val form = state.form

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Card Input")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null
                        )
                    }
                },
                windowInsets = WindowInsets.statusBars
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (form == null) {
                CircularProgressIndicator()
                return@Box
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                FormUI(
                    modifier = Modifier
                        .weight(1f, fill = false),
                    form = form
                )

                Button(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {},
                    enabled = state.valid
                ) {
                    Text("Save")
                }
            }
        }
    }
}