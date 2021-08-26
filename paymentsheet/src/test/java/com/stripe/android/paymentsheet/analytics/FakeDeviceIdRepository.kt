package com.stripe.android.paymentsheet.analytics

internal class FakeDeviceIdRepository : DeviceIdRepository {
    override suspend fun get(): DeviceId = DeviceId()
}
