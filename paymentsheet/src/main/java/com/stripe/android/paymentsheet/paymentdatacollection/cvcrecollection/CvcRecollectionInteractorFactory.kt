package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

internal interface CvcRecollectionInteractorFactory {
    fun create(args: Any): CvcRecollectionInteractor
}