package com.stripe.android.view;

import android.support.v7.widget.RecyclerView;

import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test class for {@link PaymentMethodsAdapter}
 */
@RunWith(RobolectricTestRunner.class)
public class PaymentMethodsAdapterTest {

    static final String PAYMENT_METHOD_JSON = "{\n" +
            "\t\"id\": \"pm_987654321\",\n" +
            "\t\"created\": 1550757934256,\n" +
            "\t\"customer\": \"cus_AQsHpvKfKwJDrF\",\n" +
                   "\t\"livemode\": true,\n" +
                   "\t\"metadata\": {\n" +
                   "\t\t\"order_id\": \"123456789\"\n" +
                   "\t}," +
                   "\t\"type\": \"card\",\n" +
                   "\t\"billing_details\": {\n" +
                   "\t\t\"address\": {\n" +
                   "\t\t\t\"city\": \"San Francisco\",\n" +
                   "\t\t\t\"country\": \"USA\",\n" +
                   "\t\t\t\"line1\": \"510 Townsend St\",\n" +
                   "\t\t\t\"postal_code\": \"94103\",\n" +
                   "\t\t\t\"state\": \"CA\"\n" +
                   "\t\t},\n" +
                   "\t\t\"email\": \"patrick@example.com\",\n" +
                   "\t\t\"name\": \"Patrick\",\n" +
                   "\t\t\"phone\": \"123-456-7890\"\n" +
                   "\t},\n" +
                   "\t\"card\": {\n" +
                   "\t\t\"brand\": \"visa\",\n" +
                   "\t\t\"checks\": {\n" +
                   "\t\t\t\"address_line1_check\": \"unchecked\",\n" +
                   "\t\t\t\"cvc_check\": \"unchecked\"\n" +
                   "\t\t},\n" +
                   "\t\t\"country\": \"US\",\n" +
                   "\t\t\"exp_month\": 8,\n" +
                   "\t\t\"exp_year\": 2022,\n" +
                   "\t\t\"funding\": \"credit\",\n" +
                   "\t\t\"last4\": \"4242\",\n" +
                   "\t\t\"three_d_secure_usage\": {\n" +
                   "\t\t\t\"supported\": true\n" +
                   "\t\t}\n" +
                   "\t}\n" +
                   "}";


    @Mock RecyclerView.AdapterDataObserver mAdapterDataObserver;
    private PaymentMethodsAdapter mPaymentMethodsAdapter;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mPaymentMethodsAdapter = new PaymentMethodsAdapter();
        mPaymentMethodsAdapter.registerAdapterDataObserver(mAdapterDataObserver);
    }

    @Test
    public void setSelection_changesSelection() {
        final PaymentMethod paymentMethod1 =
                PaymentMethod.fromString(PaymentMethodTest.PM_CARD_JSON);
        final PaymentMethod paymentMethod2 = PaymentMethod.fromString(PAYMENT_METHOD_JSON);
        assertNotNull(paymentMethod1);
        assertNotNull(paymentMethod2);
        assertNotNull(paymentMethod1.id);
        assertNotNull(paymentMethod2.id);
        final List<PaymentMethod> paymentMethods = Arrays.asList(paymentMethod1, paymentMethod2);
        mPaymentMethodsAdapter.setPaymentMethods(paymentMethods);
        assertEquals(2, mPaymentMethodsAdapter.getItemCount());
        verify(mAdapterDataObserver, times(1)).onChanged();

        assertNotNull(mPaymentMethodsAdapter.getSelectedPaymentMethod());
        assertEquals(paymentMethod2.id, mPaymentMethodsAdapter.getSelectedPaymentMethod().id);

        mPaymentMethodsAdapter.setSelectedPaymentMethod(paymentMethod1.id);
        verify(mAdapterDataObserver, times(1)).onChanged();

        assertNotNull(mPaymentMethodsAdapter.getSelectedPaymentMethod());
        assertEquals(paymentMethod1.id, mPaymentMethodsAdapter.getSelectedPaymentMethod().id);

        mPaymentMethodsAdapter.setSelectedIndex(1);
        assertNotNull(mPaymentMethodsAdapter.getSelectedPaymentMethod());
        assertEquals(paymentMethod2.id, mPaymentMethodsAdapter.getSelectedPaymentMethod().id);
    }

    @Test
    public void updatePaymentMethods_removesExistingPaymentMethodsAndAddsAllPaymentMethods() {
        final PaymentMethod paymentMethod1 =
                PaymentMethod.fromString(PaymentMethodTest.PM_CARD_JSON);
        final PaymentMethod paymentMethod2 = PaymentMethod.fromString(PAYMENT_METHOD_JSON);
        final List<PaymentMethod> singlePaymentMethod = Collections.singletonList(paymentMethod1);
        final List<PaymentMethod> paymentMethods = Arrays.asList(paymentMethod1, paymentMethod2);

        assertNotNull(paymentMethod1);
        assertNotNull(paymentMethod1.id);

        mPaymentMethodsAdapter.setPaymentMethods(singlePaymentMethod);
        assertEquals(1, mPaymentMethodsAdapter.getItemCount());
        assertNotNull(mPaymentMethodsAdapter.getSelectedPaymentMethod());

        assertEquals(paymentMethod1.id, mPaymentMethodsAdapter.getSelectedPaymentMethod().id);

        mPaymentMethodsAdapter.setPaymentMethods(paymentMethods);
        assertEquals(2, mPaymentMethodsAdapter.getItemCount());
        assertNotNull(mPaymentMethodsAdapter.getSelectedPaymentMethod());
        assertEquals(paymentMethod1.id, mPaymentMethodsAdapter.getSelectedPaymentMethod().id);
        verify(mAdapterDataObserver, times(2)).onChanged();
    }

    @Test
    public void updatePaymentMethods_withSelection_updatesPaymentMethodsAndSelectionMaintained() {
        final PaymentMethod paymentMethod1 =
                PaymentMethod.fromString(PaymentMethodTest.PM_CARD_JSON);
        final PaymentMethod paymentMethod2 = PaymentMethod.fromString(PAYMENT_METHOD_JSON);
        assertNotNull(paymentMethod1);
        assertNotNull(paymentMethod2);
        final List<PaymentMethod> singlePaymentMethod = Collections.singletonList(paymentMethod2);
        final List<PaymentMethod> paymentMethods = Arrays.asList(paymentMethod1, paymentMethod2);

        mPaymentMethodsAdapter.setPaymentMethods(singlePaymentMethod);
        assertEquals(1, mPaymentMethodsAdapter.getItemCount());
        mPaymentMethodsAdapter.setSelectedIndex(0);
        assertNotNull(mPaymentMethodsAdapter.getSelectedPaymentMethod());

        mPaymentMethodsAdapter.setPaymentMethods(paymentMethods);
        assertEquals(2, mPaymentMethodsAdapter.getItemCount());
        assertNotNull(mPaymentMethodsAdapter.getSelectedPaymentMethod());
        assertEquals(paymentMethod2.id, mPaymentMethodsAdapter.getSelectedPaymentMethod().id);
    }
}
