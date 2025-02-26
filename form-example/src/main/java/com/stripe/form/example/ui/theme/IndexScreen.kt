package com.stripe.form.example.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
internal fun IndexScreen(
    navController: NavHostController,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Index")
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            item {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate("cardInput")
                        }
                        .padding(16.dp),
                    text = "Card Input"
                )
            }

            item {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate("customForm")
                        }
                        .padding(16.dp),
                    text = "Custom Form"
                )
            }
        }
    }
}