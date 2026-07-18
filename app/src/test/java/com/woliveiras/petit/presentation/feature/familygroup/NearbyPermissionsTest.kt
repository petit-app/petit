package com.woliveiras.petit.presentation.feature.familygroup

import android.Manifest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NearbyPermissionsTest {
  @Test
  fun android13UsesNearbyWifiAndBluetoothRuntimePermissions() {
    assertThat(requiredNearbyPermissions(apiLevel = 33))
      .containsExactly(
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.NEARBY_WIFI_DEVICES,
      )
  }

  @Test
  fun android12UsesBluetoothAndFineLocationPermissions() {
    assertThat(requiredNearbyPermissions(apiLevel = 31))
      .containsExactly(
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION,
      )
  }

  @Test
  fun olderAndroidUsesFineLocationPermission() {
    assertThat(requiredNearbyPermissions(apiLevel = 30))
      .containsExactly(Manifest.permission.ACCESS_FINE_LOCATION)
  }
}
