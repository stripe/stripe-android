package com.stripe.android;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class TelemetryClientUtilTest {

    @Mock
    private PackageManager mPackageManager;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void initWithAppContext_shouldSucceed() {
        assertFalse(new TelemetryClientUtil(ApplicationProvider.getApplicationContext())
                .createTelemetryMap()
                .isEmpty());
    }

    @Test
    public void createTelemetryMap_returnsHasExpectedEntries() {
        final Context context = ApplicationProvider.getApplicationContext();
        final Map<String, Object> telemetryMap = new TelemetryClientUtil(
                new FakeUidSupplier(),
                context.getResources().getDisplayMetrics(),
                "package_name",
                context.getPackageManager(),
                "-5"
        )
                .createTelemetryMap();
        assertEquals(5, telemetryMap.size());

        final Map firstMap = Objects.requireNonNull((Map) telemetryMap.get("a"));
        assertEquals(4, firstMap.size());

        assertEquals("Android 9 REL 28", getSingleValue(firstMap, "d"));
        assertEquals("-5", getSingleValue(firstMap, "g"));
        assertEquals(8, Objects.requireNonNull((Map) telemetryMap.get("b")).size());
    }

    @Test
    public void createTelemetryMap_withVersionName_includesVersionName()
            throws PackageManager.NameNotFoundException {
        final PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionName = "version_name";

        when(mPackageManager.getPackageInfo("package_name", 0))
                .thenReturn(packageInfo);

        final Context context = ApplicationProvider.getApplicationContext();
        final Map<String, Object> telemetryMap = new TelemetryClientUtil(
                new FakeUidSupplier(),
                context.getResources().getDisplayMetrics(),
                "package_name",
                mPackageManager,
                "-5"
        )
                .createTelemetryMap();

        final Map secondMap = Objects.requireNonNull((Map) telemetryMap.get("b"));
        assertEquals(9, secondMap.size());
        assertEquals("version_name", secondMap.get("l"));
    }

    @NonNull
    private Object getSingleValue(@NonNull Map map, @NonNull String key) {
        return ((Map) map.get(key)).get("v");
    }
}
