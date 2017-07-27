package com.stripe.android;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;

import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Customer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
//import static org.mockito.Mockito.when;

/**
 * Test class for {@link CustomerSession}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class CustomerSessionTest {

    private static final String FIRST_SAMPLE_KEY_RAW = "{\n" +
            "  \"id\": \"ephkey_123\",\n" +
            "  \"object\": \"ephemeral_key\",\n" +
            "  \"secret\": \"ek_test_123\",\n" +
            "  \"created\": 1501188006223,\n" +
            "  \"livemode\": false,\n" +
            "  \"expires\": 1501188016223,\n" +
            "  \"associated_objects\": [{\n" +
            "            \"type\": \"customer\",\n" +
            "            \"id\": \"cus_AQsHpvKfKwJDrF\"\n" +
            "            }]\n" +
            "}";

    private static final String SECOND_SAMPLE_KEY_RAW = "{\n" +
            "  \"id\": \"ephkey_ABC\",\n" +
            "  \"object\": \"ephemeral_key\",\n" +
            "  \"secret\": \"ek_test_456\",\n" +
            "  \"created\": 1483575790,\n" +
            "  \"livemode\": false,\n" +
            "  \"expires\": 1483579790,\n" +
            "  \"associated_objects\": [{\n" +
            "            \"type\": \"customer\",\n" +
            "            \"id\": \"cus_abc123\"\n" +
            "            }]\n" +
            "}";

    private static final String FIRST_TEST_CUSTOMER_OBJECT =
            "{\n" +
                    "  \"id\": \"cus_AQsHpvKfKwJDrF\",\n" +
                    "  \"object\": \"customer\",\n" +
                    "  \"default_source\": \"abc123\",\n" +
                    "  \"sources\": {\n" +
                    "    \"object\": \"list\",\n" +
                    "    \"data\": [\n" +
                    "\n" +
                    "    ],\n" +
                    "    \"has_more\": false,\n" +
                    "    \"total_count\": 0,\n" +
                    "    \"url\": \"/v1/customers/cus_AQsHpvKfKwJDrF/sources\"\n" +
                    "  }\n" +
                    "}";

    private static final String SECOND_TEST_CUSTOMER_OBJECT =
            "{\n" +
                    "  \"id\": \"cus_ABC123\",\n" +
                    "  \"object\": \"customer\",\n" +
                    "  \"default_source\": \"def456\",\n" +
                    "  \"sources\": {\n" +
                    "    \"object\": \"list\",\n" +
                    "    \"data\": [\n" +
                    "\n" +
                    "    ],\n" +
                    "    \"has_more\": false,\n" +
                    "    \"total_count\": 0,\n" +
                    "    \"url\": \"/v1/customers/cus_ABC123/sources\"\n" +
                    "  }\n" +
                    "}";

    @Mock CustomerSession.StripeApiProxy mStripeApiProxy;
    private CustomerSession mCustomerSession;
    private TestEphemeralKeyProvider mEphemeralKeyProvider;

    private Customer mFirstCustomer;
    private Customer mSecondCustomer;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        PaymentConfiguration.init("pk_test_abc123");

        mFirstCustomer = Customer.fromString(FIRST_TEST_CUSTOMER_OBJECT);
        assertNotNull(mFirstCustomer);
        mSecondCustomer = Customer.fromString(SECOND_TEST_CUSTOMER_OBJECT);
        assertNotNull(mSecondCustomer);

        mEphemeralKeyProvider = new TestEphemeralKeyProvider();

        try {
            when(mStripeApiProxy.retrieveCustomerWithKey(anyString(), anyString()))
                    .thenReturn(mFirstCustomer, mSecondCustomer);
        } catch (StripeException exception) {
            fail("Exception when accessing mock api proxy: " + exception.getMessage());
        }
    }

    @Test
    public void create_withoutInvokingFunctions_fetchesKeyAndCustomer() {
        EphemeralKey firstKey = EphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        // Note: the test ephemeral key expiration is set to July 27, 2017
        Calendar fixedCalendar = Calendar.getInstance();
        fixedCalendar.set(2017, 1, 1);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        CustomerSession session = new CustomerSession(
                mEphemeralKeyProvider,
                mStripeApiProxy,
                fixedCalendar);

        assertEquals(firstKey.getId(), session.getEphemeralKey().getId());

        try {
            verify(mStripeApiProxy, times(1)).retrieveCustomerWithKey(
                    firstKey.getCustomerId(), firstKey.getSecret());
            assertEquals(mFirstCustomer.getId(), session.getCustomer().getId());
        } catch (StripeException unexpected) {
            fail(unexpected.getMessage());
        }
    }

    @Test
    public void updateKeyIfNecessary_whenKeyIsValid_doesNotUpdateKey() {
        // This test will always be run after January 01, 2000.
        Calendar pastCalendar = Calendar.getInstance();
        pastCalendar.set(2000, 1, 1);
        Calendar now = Calendar.getInstance();

        EphemeralKey firstKey = EphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);
        EphemeralKey secondKey = EphemeralKey.fromString(SECOND_SAMPLE_KEY_RAW);
        assertNotNull(secondKey);
        assertNotEquals("Test keys should not have same ID", secondKey.getId(), firstKey.getId());

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        mEphemeralKeyProvider.setEphemeralKeyExpiration(now.getTimeInMillis());

        CustomerSession session = new CustomerSession(
                mEphemeralKeyProvider,
                mStripeApiProxy,
                pastCalendar);

        // We'll fetch a new key if the first one is expired.
        mEphemeralKeyProvider.setNextRawEphemeralKey(SECOND_SAMPLE_KEY_RAW);

        session.updateKeyIfNecessary(session.getEphemeralKey(), pastCalendar);
        assertEquals(firstKey.getId(), session.getEphemeralKey().getId());
    }

    @Test
    public void isTimeInPast_forTimeInPast_returnsTrue() {
        // This test will always be run after January 01, 2000.
        Calendar pastCalendar = Calendar.getInstance();
        pastCalendar.set(2000, 1, 1);

        assertTrue(CustomerSession.isTimeInPast(pastCalendar.getTimeInMillis(), null));
    }

    @Test
    public void isTimeInPast_forPresentOrFuture_returnsFalse() {
        // This test will always be run after January 01, 2000.
        Calendar pastCalendar = Calendar.getInstance();
        pastCalendar.set(2000, 1, 1);

        Calendar nowCalendar = Calendar.getInstance();
        assertFalse(CustomerSession.isTimeInPast(nowCalendar.getTimeInMillis(), pastCalendar));
    }

    @Test
    public void isTimeInPast_forZeroTime_returnsTrue() {
        assertTrue(CustomerSession.isTimeInPast(0L, null));
    }

    static class TestEphemeralKeyProvider implements EphemeralKeyProvider {

        private static final int INVALID_ERROR_CODE = -1;
        private @Nullable String mRawEphemeralKey;
        private int mErrorCode = INVALID_ERROR_CODE;
        private @Nullable String mErrorMessage;

        private EphemeralKey mEphemeralKey;

        private TestEphemeralKeyProvider(Parcel in) {}

        TestEphemeralKeyProvider() {}

        @Override
        public void fetchEphemeralKey(
                @NonNull @Size(min = 4) String apiVersion,
                @NonNull final EphemeralKeyUpdateListener keyUpdateListener) {
            if (mRawEphemeralKey != null) {
                keyUpdateListener.onKeyUpdate(mRawEphemeralKey);
            } else if (mErrorCode != INVALID_ERROR_CODE) {
                keyUpdateListener.onKeyUpdateFailure(mErrorCode, mErrorMessage);
            }
        }

        @NonNull
        @Override
        public ClassLoader getClassLoader() {
            return getClass().getClassLoader();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {

        }

        void setNextRawEphemeralKey(@NonNull String rawEphemeralKey) {
            mRawEphemeralKey = rawEphemeralKey;
            mEphemeralKey = EphemeralKey.fromString(mRawEphemeralKey);
            mErrorCode = INVALID_ERROR_CODE;
            mErrorMessage = null;
        }

        void setEphemeralKeyExpiration(long expiration) {
            if (mEphemeralKey != null) {
                mEphemeralKey.setExpires(expiration);
                mRawEphemeralKey = mEphemeralKey.toString();
            }
        }

        void setNextError(int errorCode, @NonNull String errorMessage) {
            mRawEphemeralKey = null;
            mErrorCode = errorCode;
            mErrorMessage = errorMessage;
        }

        static final Parcelable.Creator<TestEphemeralKeyProvider> CREATOR
                = new Parcelable.Creator<TestEphemeralKeyProvider>() {

            @Override
            public TestEphemeralKeyProvider createFromParcel(Parcel in) {
                return new TestEphemeralKeyProvider(in);
            }

            @Override
            public TestEphemeralKeyProvider[] newArray(int size) {
                return new TestEphemeralKeyProvider[size];
            }
        };
    }
}
