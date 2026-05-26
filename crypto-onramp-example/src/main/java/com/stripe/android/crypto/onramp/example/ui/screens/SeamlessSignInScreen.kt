package com.stripe.android.crypto.onramp.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.R as AndroidR

@Composable
internal fun SeamlessSignInScreen(
    email: String,
    onContinue: () -> Unit,
    onNotMe: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = AndroidR.drawable.ic_menu_myplaces),
            contentDescription = "User Icon",
            modifier = Modifier.size(128.dp)
        )

        Text(
            text = buildAnnotatedString {
                append("Continue as ")
                val start = length
                append("$email?")
                addStyle(
                    style = SpanStyle(fontWeight = FontWeight.Bold),
                    start = start,
                    end = length
                )
            },
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Continue")
        }

        Button(
            onClick = onNotMe,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Not me")
        }
    }
}
