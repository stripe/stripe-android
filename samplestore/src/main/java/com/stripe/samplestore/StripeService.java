package com.stripe.samplestore;

import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.POST;
import rx.Observable;

public interface StripeService {

    @POST("create_charge")
    Observable<String> createCharge(@Body ChargeParams params);

    @POST("charge")
    Observable<String> createSimpleCharge(@Body ChargeParams params);
}
