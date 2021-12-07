package com.stripe.android.link.ui.signup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.link.R

@Preview
@Composable
internal fun SignUpBody() {
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.sign_up_header),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h1
        )
        Text(
            text = stringResource(R.string.sign_up_message),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.body1
        )
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            value = email,
            onValueChange = {
                email = it
            },
            label = {
                Text(text = "email")
            }
        )
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            value = phone,
            onValueChange = {
                phone = it
            },
            label = {
                Text(text = "phone")
            }
        )
        Text(
            text = stringResource(R.string.sign_up_terms),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption
        )
        Button(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.sign_up),
                style = MaterialTheme.typography.button
            )
        }
    }
}
