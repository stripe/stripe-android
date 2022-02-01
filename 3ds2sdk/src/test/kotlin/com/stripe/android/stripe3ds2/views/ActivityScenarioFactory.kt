package com.stripe.android.stripe3ds2.views

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import com.stripe.android.stripe3ds2.ChallengeMessageFixtures
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.transaction.ChallengeRequestExecutorFixtures
import com.stripe.android.stripe3ds2.transaction.IntentDataFixtures
import com.stripe.android.stripe3ds2.transactions.ChallengeResponseData
import org.mockito.kotlin.mock

internal class ActivityScenarioFactory(
    private val context: Context,
    cres: ChallengeResponseData = ChallengeMessageFixtures.CRES,
    uiCustomization: StripeUiCustomization = UiCustomizationFixtures.DEFAULT
) {
    private val viewArgs = ChallengeViewArgs(
        cres,
        ChallengeMessageFixtures.CREQ,
        uiCustomization,
        ChallengeRequestExecutorFixtures.CONFIG,
        { _, _ -> mock() },
        5,
        IntentDataFixtures.DEFAULT
    )

    fun create(): ActivityScenario<ChallengeActivity> {
        return ActivityScenario.launch(
            Intent(context, ChallengeActivity::class.java)
                .putExtras(viewArgs.toBundle())
        )
    }
}
