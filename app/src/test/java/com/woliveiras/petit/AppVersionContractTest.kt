package com.woliveiras.petit

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class AppVersionContractTest {

  @Test
  fun settingsSupportingTextUsesTheGeneratedReleaseVersion() {
    val settingsSource =
      File("src/main/java/com/woliveiras/petit/presentation/feature/settings/SettingsScreen.kt")
        .readText()

    assertThat(
        Regex("""supportingContent\s*=\s*\{\s*Text\s*\(\s*BuildConfig\.VERSION_NAME\s*\)\s*}""")
          .containsMatchIn(settingsSource)
      )
      .isTrue()
    assertThat(Regex("""Text\s*\(\s*"1\.0\.0"\s*\)""").containsMatchIn(settingsSource)).isFalse()
  }
}
