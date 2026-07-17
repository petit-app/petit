package com.woliveiras.petit.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.MainActivity
import com.woliveiras.petit.R
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.VaccineType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class VaccinationTaskJourneyTest {
  private val composeRule = createAndroidComposeRule<MainActivity>()

  @get:Rule val rules: RuleChain = RuleChain.outerRule(ClearAppStateRule()).around(composeRule)

  @Test
  fun addVaccination_persistsLinkedAutomaticTask() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val next = context.getString(R.string.onboarding_next)
    val getStarted = context.getString(R.string.onboarding_get_started)
    val registerPet = context.getString(R.string.home_register_pet)
    val namePlaceholder = context.getString(R.string.pet_form_name_placeholder)
    val save = context.getString(R.string.action_save)
    val edit = context.getString(R.string.action_edit)
    val vaccinations = context.getString(R.string.pet_detail_section_vaccinations)
    val addVaccination = context.getString(R.string.vaccination_add)
    val addVaccinationTitle = context.getString(R.string.vaccination_add_title)
    val initialVaccine = context.getString(R.string.vaccine_v3)
    val rabies = context.getString(R.string.vaccine_rabies)
    val petName = "Luna"
    val expectedTaskTitle = "$petName - ${VaccineType.RABIES.displayName}"

    composeRule.onNodeWithText(next).performClick()
    composeRule.onNodeWithText(next).performClick()
    composeRule.onNodeWithText(getStarted).performClick()
    composeRule.onNodeWithText(registerPet).performClick()
    composeRule.onNodeWithText(namePlaceholder).performTextInput(petName)
    closeSoftKeyboard()
    composeRule.onNodeWithText(save).performScrollTo().performClick()

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithContentDescription(edit).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithText(vaccinations).performScrollTo().performClick()
    composeRule.onNodeWithContentDescription(addVaccination).performClick()
    composeRule.onNodeWithText(addVaccinationTitle).assertIsDisplayed()

    composeRule.onNodeWithText(initialVaccine).performClick()
    composeRule.onNodeWithText(rabies).performClick()
    composeRule.onNodeWithText(save).performScrollTo().performClick()

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule
        .onAllNodes(hasContentDescription(rabies, substring = true))
        .fetchSemanticsNodes()
        .isNotEmpty()
    }

    val database =
      Room.databaseBuilder(context, PetitDatabase::class.java, PetitDatabase.DATABASE_NAME).build()
    try {
      val tasks = runBlocking { database.taskDao().getPendingTasks().first() }
      assertThat(tasks).hasSize(1)
      with(tasks.single()) {
        assertThat(kind).isEqualTo(TaskKind.VACCINATION.name)
        assertThat(title).isEqualTo(expectedTaskTitle)
        assertThat(referenceEntityId).isNotNull()
        assertThat(id).isEqualTo("auto_vacc_$referenceEntityId")
      }
    } finally {
      database.close()
    }
  }
}
