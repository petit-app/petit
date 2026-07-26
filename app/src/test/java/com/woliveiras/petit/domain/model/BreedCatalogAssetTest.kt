package com.woliveiras.petit.domain.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BreedCatalogAssetTest {

  @Test
  fun packagedCatalogCarriesPinnedAttributionAndExpectedSpeciesCoverage() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog =
      context.assets.open("breed_catalog.json").bufferedReader().use {
        BreedCatalog.fromJson(it.readText())
      }

    assertThat(catalog.metadata?.vboRelease).isEqualTo("2026-04-15")
    assertThat(catalog.metadata?.license).isEqualTo("CC BY 4.0")
    assertThat(catalog.metadata?.sources?.map { it.authority })
      .containsExactly("FCI", "FIFe", "TICA")
      .inOrder()
    assertThat(catalog.search(PetType.DOG, "en", "")).hasSize(356)
    assertThat(catalog.search(PetType.CAT, "en", "")).hasSize(96)
  }
}
