package com.stripe.android.financialconnections.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.example.ui.theme.StripeandroidTheme

class FinancialConnectionsHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StripeandroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val context = LocalContext.current
                    Column {
                        ExampleButton("Regular flow") {
                            context.startActivity(
                                Intent(
                                    context,
                                    FinancialConnectionsExampleActivity::class.java
                                )
                            )
                        }
                        Divider()
                        ExampleButton(text = "Compose example") {
                            context.startActivity(
                                Intent(
                                    context,
                                    FinancialConnectionsComposeExampleActivity::class.java
                                )
                            )
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun Divider() {
    Divider(
        color = Color.LightGray,
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    )
}

@Composable
fun ExampleButton(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable { onClick() }
            .padding(vertical = 25.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.h5
        )
    }
}
