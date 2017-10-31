package com.stripe.android.view;

import com.stripe.android.BuildConfig;
import com.stripe.android.model.ShippingMethod;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

/**
 * Test for {@link SelectShippingMethodWidget}
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class SelectShippingMethodWidgetTest {

    private SelectShippingMethodWidget mSelectShippingMethodWidget;
    private ShippingMethodAdapter mShippingMethodAdapter;
    private List<ShippingMethod> mShippingMethods;

    @Before
    public void setup() {
        Locale.setDefault(Locale.US);
        ActivityController<SelectShippingMethodTestActivity> activityController =
                Robolectric.buildActivity(SelectShippingMethodTestActivity.class).create().start();
        mSelectShippingMethodWidget = activityController.get().getSelectShippingMethodWidget();
        mShippingMethods = new ArrayList<>();
        ShippingMethod ups = new ShippingMethod("UPS Ground", "ups-ground", "Arrives in 3-5 days", 0, "USD");
        ShippingMethod fedEx = new ShippingMethod("FedEx", "fedex", "Arrives tomorrow", 599, "USD");
        mShippingMethods.add(ups);
        mShippingMethods.add(fedEx);
        mSelectShippingMethodWidget.setShippingMethods(mShippingMethods, ups);
        mShippingMethodAdapter = mSelectShippingMethodWidget.mShippingMethodAdapter;
    }

    @Test
    public void selectShippingMethodWidget_whenSelected_selectionChanges() {
        assertEquals(mShippingMethodAdapter.getSelectedShippingMethod(), mShippingMethods.get(0));
        mShippingMethodAdapter.setSelectedIndex(1);
        assertEquals(mShippingMethodAdapter.getSelectedShippingMethod(), mShippingMethods.get(1));
    }

}
