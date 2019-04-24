package com.stripe.android.view;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.model.ShippingMethod;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

/**
 * Test for {@link SelectShippingMethodWidget}
 */
@RunWith(RobolectricTestRunner.class)
public class SelectShippingMethodWidgetTest {

    private ShippingMethodAdapter mShippingMethodAdapter;
    private List<ShippingMethod> mShippingMethods;

    @Before
    public void setup() {
        Locale.setDefault(Locale.US);
        mShippingMethods = new ArrayList<>();
        ShippingMethod ups = new ShippingMethod("UPS Ground", "ups-ground", "Arrives in 3-5 days", 0, "USD");
        ShippingMethod fedEx = new ShippingMethod("FedEx", "fedex", "Arrives tomorrow", 599, "USD");
        mShippingMethods.add(ups);
        mShippingMethods.add(fedEx);

        final SelectShippingMethodWidget selectShippingMethodWidget =
                new SelectShippingMethodWidget(ApplicationProvider.getApplicationContext());
        selectShippingMethodWidget.setShippingMethods(mShippingMethods, ups);
        mShippingMethodAdapter = selectShippingMethodWidget.mShippingMethodAdapter;
    }

    @Test
    public void selectShippingMethodWidget_whenSelected_selectionChanges() {
        assertEquals(mShippingMethodAdapter.getSelectedShippingMethod(), mShippingMethods.get(0));
        mShippingMethodAdapter.setSelectedIndex(1);
        assertEquals(mShippingMethodAdapter.getSelectedShippingMethod(), mShippingMethods.get(1));
    }

}
