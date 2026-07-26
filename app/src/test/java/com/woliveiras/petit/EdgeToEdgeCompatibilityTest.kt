package com.woliveiras.petit

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class EdgeToEdgeCompatibilityTest {

  @Test
  fun releaseSourceEnablesEdgeToEdgeWithoutAppOwnedDeprecatedSystemBarConfiguration() {
    val mainActivity =
      appProjectRoot().resolve("src/main/java/com/woliveiras/petit/MainActivity.kt").readText()
    val buildConfiguration = appProjectRoot().resolve("build.gradle.kts").readText()
    val violations = mutableListOf<String>()

    val onCreateBody = functionBody(mainActivity, "override fun onCreate")
    if (
      onCreateBody == null || !Regex("""\benableEdgeToEdge\s*\(""").containsMatchIn(onCreateBody)
    ) {
      violations += "MainActivity.onCreate must call enableEdgeToEdge()"
    }
    val versionCode =
      Regex("""\bversionCode\s*=\s*(\d+)""").find(buildConfiguration)?.groupValues?.get(1)?.toInt()
    if (versionCode == null || versionCode <= WARNED_RELEASE_VERSION_CODE) {
      violations += "The corrected Play release must use a version code greater than 1"
    }
    val versionName =
      Regex("""\bversionName\s*=\s*"([^"]+)"""").find(buildConfiguration)?.groupValues?.get(1)
    if (versionName == null || versionName == WARNED_RELEASE_VERSION_NAME) {
      violations += "The corrected Play release must not reuse version name 1.0.0"
    }

    appProjectRoot()
      .resolve("src/main")
      .walkTopDown()
      .filter { it.isFile && (it.extension == "kt" || it.extension == "xml") }
      .forEach { file ->
        val source = file.readText()
        if (file.extension == "kt") {
          val code = stripKotlinCommentsAndLiterals(source)
          DEPRECATED_KOTLIN_SYSTEM_BAR_IDENTIFIERS.forEach { identifier ->
            if (Regex("""\b${Regex.escape(identifier)}\b""").containsMatchIn(code)) {
              violations +=
                "${file.relativeTo(appProjectRoot())}: direct deprecated API $identifier"
            }
          }
        }
        if (file.extension == "xml") {
          val xmlWithoutComments =
            XML_COMMENT.replace(source) { comment -> comment.value.replaceNonNewlinesWithSpaces() }
          THEME_ATTRIBUTE.findAll(xmlWithoutComments).forEach { match ->
            val attribute = match.groupValues[1]
            if (attribute in DEPRECATED_THEME_ATTRIBUTE_NAMES) {
              violations +=
                "${file.relativeTo(appProjectRoot())}: deprecated theme attribute $attribute"
            }
          }
        }
      }

    assertThat(violations).isEmpty()
  }

  private fun appProjectRoot(): File {
    val configuredRoot =
      System.getenv("PETIT_EDGE_TO_EDGE_SOURCE_ROOT")?.takeIf { it.isNotBlank() }?.let(::File)
        ?: File(checkNotNull(System.getProperty("user.dir")))
    return configuredRoot.takeIf { it.resolve("src/main").isDirectory }
      ?: configuredRoot.resolve("app")
  }

  private fun functionBody(source: String, signature: String): String? {
    val code = stripKotlinCommentsAndLiterals(source)
    val signatureStart = code.indexOf(signature).takeIf { it >= 0 } ?: return null
    val openingBrace = code.indexOf('{', signatureStart).takeIf { it >= 0 } ?: return null
    var depth = 0
    for (index in openingBrace until code.length) {
      when (code[index]) {
        '{' -> depth++
        '}' -> {
          depth--
          if (depth == 0) return code.substring(openingBrace + 1, index)
        }
      }
    }
    return null
  }

  private fun stripKotlinCommentsAndLiterals(source: String): String {
    val result = StringBuilder(source.length)
    var index = 0
    while (index < source.length) {
      when {
        source.startsWith("//", index) -> {
          val end = source.indexOf('\n', index).let { if (it < 0) source.length else it }
          result.append(source.substring(index, end).replaceNonNewlinesWithSpaces())
          index = end
        }
        source.startsWith("/*", index) -> {
          var depth = 1
          var end = index + 2
          while (end < source.length && depth > 0) {
            when {
              source.startsWith("/*", end) -> {
                depth++
                end += 2
              }
              source.startsWith("*/", end) -> {
                depth--
                end += 2
              }
              else -> end++
            }
          }
          result.append(source.substring(index, end).replaceNonNewlinesWithSpaces())
          index = end
        }
        source.startsWith("\"\"\"", index) -> {
          val closing = source.indexOf("\"\"\"", index + 3)
          val end = if (closing < 0) source.length else closing + 3
          result.append(source.substring(index, end).replaceNonNewlinesWithSpaces())
          index = end
        }
        source[index] == '"' || source[index] == '\'' -> {
          val quote = source[index]
          var end = index + 1
          while (end < source.length) {
            if (source[end] == '\\') {
              end += 2
            } else {
              val isClosingQuote = source[end] == quote
              end++
              if (isClosingQuote) break
            }
          }
          val safeEnd = end.coerceAtMost(source.length)
          result.append(source.substring(index, safeEnd).replaceNonNewlinesWithSpaces())
          index = safeEnd
        }
        else -> {
          result.append(source[index])
          index++
        }
      }
    }
    return result.toString()
  }

  private fun String.replaceNonNewlinesWithSpaces(): String =
    map { character -> if (character == '\n' || character == '\r') character else ' ' }
      .joinToString("")

  private companion object {
    const val WARNED_RELEASE_VERSION_CODE = 1
    const val WARNED_RELEASE_VERSION_NAME = "1.0.0"
    val DEPRECATED_KOTLIN_SYSTEM_BAR_IDENTIFIERS =
      setOf(
        "statusBarColor",
        "navigationBarColor",
        "navigationBarDividerColor",
        "setStatusBarColor",
        "setNavigationBarColor",
        "setNavigationBarDividerColor",
        "getStatusBarColor",
        "getNavigationBarColor",
        "getNavigationBarDividerColor",
        "setDecorFitsSystemWindows",
        "isStatusBarContrastEnforced",
        "setStatusBarContrastEnforced",
      )
    val DEPRECATED_THEME_ATTRIBUTE_NAMES =
      setOf(
        "statusBarColor",
        "navigationBarColor",
        "navigationBarDividerColor",
        "windowLightStatusBar",
        "windowLightNavigationBar",
        "enforceStatusBarContrast",
      )
    val XML_COMMENT = Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL)
    val THEME_ATTRIBUTE = Regex("""<item\s+name\s*=\s*"android:([^"]+)"""")
  }
}
