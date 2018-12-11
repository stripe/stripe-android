package com.stripe.android.view;

import android.support.v7.widget.RecyclerView;

import com.stripe.android.BuildConfig;
import com.stripe.android.model.Card;
import com.stripe.android.model.Customer;
import com.stripe.android.model.CustomerSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test class for {@link MaskedCardAdapter}
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class MaskedCardAdapterTest {

    @Mock RecyclerView.AdapterDataObserver mAdapterDataObserver;
    private MaskedCardAdapter mMaskedCardAdapter;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mMaskedCardAdapter = new MaskedCardAdapter(new ArrayList<CustomerSource>());
        mMaskedCardAdapter.registerAdapterDataObserver(mAdapterDataObserver);
    }

    @Test
    public void addSources_onlyDisplaysCards() {
        CustomerSource cardSource =
                CustomerSource.fromString(CardInputTestActivity.EXAMPLE_JSON_CARD_SOURCE);
        CustomerSource bitcoinSource =
                CustomerSource.fromString(CardInputTestActivity.EXAMPLE_JSON_SOURCE_BITCOIN);

        mMaskedCardAdapter.addCustomerSourceIfSupported(cardSource, bitcoinSource);
        assertEquals(1, mMaskedCardAdapter.getItemCount());

        verify(mAdapterDataObserver).onChanged();
    }

    @Test
    public void setSelection_changesSelection() {
        CustomerSource cardSource =
                CustomerSource.fromString(CardInputTestActivity.EXAMPLE_JSON_CARD_SOURCE);
        CustomerSource secondCardSource =
                CustomerSource.fromString(CardInputTestActivity.EXAMPLE_JSON_CARD_SOURCE_SECOND);
        mMaskedCardAdapter.addCustomerSourceIfSupported(cardSource, secondCardSource);
        assertEquals(2, mMaskedCardAdapter.getItemCount());
        verify(mAdapterDataObserver, times(1)).onChanged();

        assertEquals(null, mMaskedCardAdapter.getSelectedSource());
        assertNotNull(secondCardSource);
        assertNotNull(secondCardSource.getId());
        mMaskedCardAdapter.setSelectedSource(secondCardSource.getId());
        verify(mAdapterDataObserver, times(2)).onChanged();

        assertNotNull(mMaskedCardAdapter.getSelectedSource());
        assertEquals(secondCardSource.getId(), mMaskedCardAdapter.getSelectedSource().getId());
    }

    @Test
    public void updateCustomer_removesExistingSourcesAndAddsAllCustomerSources() {
        CustomerSource cardSource =
                CustomerSource.fromString(CardInputTestActivity.EXAMPLE_JSON_CARD_SOURCE_SECOND);
        mMaskedCardAdapter.addCustomerSourceIfSupported(cardSource);
        assertEquals(1, mMaskedCardAdapter.getItemCount());
        assertEquals(null, mMaskedCardAdapter.getSelectedSource());

        Customer customer =
                Customer.fromString(PaymentMethodsActivityTest.TEST_CUSTOMER_OBJECT_WITH_SOURCES);
        assertNotNull(customer);
        mMaskedCardAdapter.updateCustomer(customer);
        assertEquals(2, mMaskedCardAdapter.getItemCount());
        assertNotNull(mMaskedCardAdapter.getSelectedSource());
        assertEquals(customer.getDefaultSource(), mMaskedCardAdapter.getSelectedSource().getId());
        verify(mAdapterDataObserver, times(4)).onChanged();
    }

    @Test
    public void updateCustomer_filtersOutNonCardSources() {
        List<CustomerSource> customerSourceList = new ArrayList<CustomerSource>();
        CustomerSource cardCustomerSource = Mockito.mock(CustomerSource.class);
        Card card = Mockito.mock(Card.class);
        Mockito.when(cardCustomerSource.asCard()).thenReturn(card);
        CustomerSource nonCardCustomerSource = Mockito.mock(CustomerSource.class);
        customerSourceList.add(cardCustomerSource);
        customerSourceList.add(nonCardCustomerSource);
        Customer customer = Mockito.mock(Customer.class);
        Mockito.when(customer.getSources()).thenReturn(customerSourceList);

        mMaskedCardAdapter.updateCustomer(customer);

        assertEquals(1, mMaskedCardAdapter.getItemCount());
    }

    @Test
    public void setCustomerSourceList_filtersOutNonCardSources() {
        List<CustomerSource> customerSourceList = new ArrayList<CustomerSource>();
        CustomerSource cardCustomerSource = Mockito.mock(CustomerSource.class);
        Card card = Mockito.mock(Card.class);
        Mockito.when(cardCustomerSource.asCard()).thenReturn(card);
        CustomerSource nonCardCustomerSource = Mockito.mock(CustomerSource.class);
        customerSourceList.add(cardCustomerSource);
        customerSourceList.add(nonCardCustomerSource);

        mMaskedCardAdapter.setCustomerSourceList(customerSourceList);

        assertEquals(1, mMaskedCardAdapter.getItemCount());
    }
}
