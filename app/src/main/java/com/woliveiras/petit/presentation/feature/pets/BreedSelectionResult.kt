package com.woliveiras.petit.presentation.feature.pets

import android.os.Bundle

fun BreedSelectionValue.toNavigationResult(): Bundle =
  Bundle().apply {
    putString("breedId", breedId)
    putString("breed", breed)
    putString("displayName", displayName)
  }

fun Bundle.toBreedSelectionValue(): BreedSelectionValue =
  BreedSelectionValue(
    breedId = getString("breedId"),
    breed = getString("breed").orEmpty(),
    displayName = getString("displayName"),
  )
