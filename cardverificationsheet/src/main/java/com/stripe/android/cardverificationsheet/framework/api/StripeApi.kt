@file:JvmName("StripeApi")
package com.stripe.android.cardverificationsheet.framework.api

import android.content.Context
import com.stripe.android.cardverificationsheet.framework.api.dto.AppInfo
import com.stripe.android.cardverificationsheet.framework.api.dto.ClientDevice
import com.stripe.android.cardverificationsheet.framework.api.dto.ModelVersion
import com.stripe.android.cardverificationsheet.framework.api.dto.ScanStatistics
import com.stripe.android.cardverificationsheet.framework.api.dto.StatsPayload
import com.stripe.android.cardverificationsheet.framework.ml.getLoadedModelVersions
import com.stripe.android.cardverificationsheet.framework.util.AppDetails
import com.stripe.android.cardverificationsheet.framework.util.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private const val STATS_PATH = "/scan_stats"

/**
 * Upload stats data to bouncer servers.
 */
internal fun uploadScanStats(
    context: Context,
    instanceId: String,
    scanId: String?,
    device: Device,
    appDetails: AppDetails,
    scanStatistics: ScanStatistics,
) = GlobalScope.launch(Dispatchers.IO) {
    postData(
        context = context.applicationContext,
        path = STATS_PATH,
        data = StatsPayload(
            instanceId = instanceId,
            scanId = scanId,
            device = ClientDevice.fromDevice(device),
            app = AppInfo.fromAppDetails(appDetails),
            scanStats = scanStatistics,
            modelVersions = getLoadedModelVersions().map { ModelVersion.fromModelLoadDetails(it) },
        ),
        requestSerializer = StatsPayload.serializer(),
    )
}
