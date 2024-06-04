package com.stripe.android.identity.states

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.toMap
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.EVENT_MB_STATUS
import com.stripe.android.identity.networking.IdentityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MBDetectorTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val mockIdentityRepository = mock<IdentityRepository>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val analyticsRequestFactory = IdentityAnalyticsRequestFactory(
        context = context,
        args = mock(),
        mockIdentityRepository,
        CoroutineScope(UnconfinedTestDispatcher())
    )

    @Test
    fun `Returns null and send analysis when MBSettings is null`() = runBlocking {
        val createdResult = MBDetector.maybeCreateMBInstance(
            context = context,
            settings = null,
            identityAnalyticsRequestFactory = analyticsRequestFactory
        )
        assertThat(createdResult).isNull()
        verify(mockIdentityRepository).sendAnalyticsRequest(
            argWhere {
                it.eventName == EVENT_MB_STATUS &&
                    (
                    it.params.toMap()[IdentityAnalyticsRequestFactory.PARAM_EVENT_META_DATA]
                        as Map<*, *>
                    )[IdentityAnalyticsRequestFactory.PARAM_REQUIRED] == "false"
            }
        )
    }
}
