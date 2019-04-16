package com.stripe.samplestore.service;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import rx.Observable;

/**
 * The {@link retrofit2.Retrofit} interface that creates our API service.
 */
public interface StripeService {

    @POST("capture_payment")
    Observable<Void> capturePayment(@Body Map<String, Object> params);

    @FormUrlEncoded
    @POST("ephemeral_keys")
    Observable<ResponseBody> createEphemeralKey(@FieldMap Map<String, String> apiVersionMap);
}
