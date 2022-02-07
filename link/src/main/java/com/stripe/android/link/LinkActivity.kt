package com.stripe.android.link

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stripe.android.link.ui.signup.SignUpBody
import com.stripe.android.link.ui.theme.DefaultLinkTheme

@Suppress("EXPERIMENTAL_ANNOTATION_ON_OVERRIDE_WARNING")
internal class LinkActivity : ComponentActivity() {
    internal lateinit var navController: NavHostController

    private val starterArgs: LinkActivityContract.Args? by lazy {
        LinkActivityContract.Args.fromIntent(intent)
    }

    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            navController = rememberNavController()
            DefaultLinkTheme {
                Surface {
                    Column(Modifier.fillMaxWidth()) {
                        LinkAppBar()
                        NavHost(navController, startDestination = LinkScreen.SignUp.name) {
                            composable(LinkScreen.SignUp.name) {
                                SignUpBody(
                                    application,
                                    { requireNotNull(starterArgs) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
internal fun LinkAppBar() {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.background
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_link_logo),
                contentDescription = stringResource(R.string.link),
                tint = LocalContentColor.current.copy(alpha = 0.2f)
            )
        }
    }
}
