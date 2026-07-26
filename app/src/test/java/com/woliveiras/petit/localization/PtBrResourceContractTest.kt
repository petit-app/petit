package com.woliveiras.petit.localization

import com.google.common.truth.Truth.assertThat
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Test
import org.w3c.dom.Element

class PtBrResourceContractTest {

  @Test
  fun brazilianPortugueseMatchesTheCanonicalTranslatableResourceContract() {
    val canonical = readResources(resourceFile("values/strings.xml"))
    val portuguese = readResources(resourceFile("values-pt-rBR/strings.xml"))

    val violations = buildList {
      addAll(canonical.duplicates.map { "canonical duplicate resource: $it" })
      addAll(portuguese.duplicates.map { "pt-BR duplicate resource: $it" })
      addAll(canonical.duplicatePluralQuantities.map { "canonical duplicate plural quantity: $it" })
      addAll(portuguese.duplicatePluralQuantities.map { "pt-BR duplicate plural quantity: $it" })

      val canonicalByName = canonical.resources
      val portugueseByName = portuguese.resources
      val requiredCanonical = canonicalByName.filterValues { it.translatable }.keys
      val missing = (requiredCanonical - portugueseByName.keys).sorted()
      val extras = (portugueseByName.keys - canonicalByName.keys).sorted()
      val localizedNonTranslatable =
        portugueseByName.keys.filter { canonicalByName[it]?.translatable == false }.sorted()

      addAll(missing.map { "missing pt-BR resource: ${canonicalByName.getValue(it).kind} $it" })
      addAll(extras.map { "unexpected pt-BR resource: ${portugueseByName.getValue(it).kind} $it" })
      addAll(localizedNonTranslatable.map { "pt-BR overrides translatable=false resource: $it" })

      (canonicalByName.keys intersect portugueseByName.keys).sorted().forEach { name ->
        val canonicalResource = canonicalByName.getValue(name)
        val portugueseResource = portugueseByName.getValue(name)

        if (canonicalResource.kind != portugueseResource.kind) {
          add(
            "resource kind mismatch for $name: " +
              "canonical=${canonicalResource.kind}, pt-BR=${portugueseResource.kind}"
          )
          return@forEach
        }

        if (
          canonicalResource.kind == ResourceKind.PLURALS &&
            canonicalResource.quantities != portugueseResource.quantities
        ) {
          add(
            "plural quantities mismatch for $name: " +
              "canonical=${canonicalResource.quantities}, pt-BR=${portugueseResource.quantities}"
          )
        }

        if (canonicalResource.printfSignatures != portugueseResource.printfSignatures) {
          add(
            "printf signatures mismatch for $name: " +
              "canonical=${canonicalResource.printfSignatures}, " +
              "pt-BR=${portugueseResource.printfSignatures}"
          )
        }
      }
    }

    assertThat(violations).isEmpty()
  }

  @Test
  fun officialLocaleConfigListsOnlyEnglishAndBrazilianPortuguese() {
    val document = parseXml(resourceFile("xml/locales_config.xml"))
    val localeNames =
      document.documentElement.childElements("locale").map { element ->
        element.getAttributeNS(ANDROID_NAMESPACE, "name")
      }

    assertThat(localeNames).containsExactly("en", "pt-BR").inOrder()
  }

  private fun readResources(file: File): ParsedResources {
    val resources = linkedMapOf<String, ResourceDefinition>()
    val duplicates = mutableListOf<String>()
    val duplicatePluralQuantities = mutableListOf<String>()
    val root = parseXml(file).documentElement

    root
      .childElements()
      .filter { it.tagName in RESOURCE_TAGS }
      .forEach { element ->
        val name = element.getAttribute("name")
        val definition = element.toResourceDefinition()
        definition.duplicateQuantities.forEach { quantity ->
          duplicatePluralQuantities += "$name/$quantity"
        }
        if (resources.put(name, definition) != null) {
          duplicates += name
        }
      }

    return ParsedResources(
      resources = resources,
      duplicates = duplicates.sorted(),
      duplicatePluralQuantities = duplicatePluralQuantities.sorted(),
    )
  }

  private fun Element.toResourceDefinition(): ResourceDefinition {
    val kind = ResourceKind.fromTag(tagName)
    val values = if (kind == ResourceKind.PLURALS) childElements("item") else listOf(this)
    val quantityList =
      if (kind == ResourceKind.PLURALS) values.map { it.getAttribute("quantity") } else emptyList()

    return ResourceDefinition(
      kind = kind,
      translatable = getAttribute("translatable") != "false",
      quantities = quantityList.toSortedSet(),
      duplicateQuantities =
        quantityList.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.sorted(),
      printfSignatures = values.map { it.textContent.printfSignature() },
    )
  }

  private fun String.printfSignature(): List<PrintfToken> {
    val tokens = mutableListOf<PrintfToken>()
    var cursor = 0
    var nextImplicitIndex = 1

    while (cursor < length) {
      if (this[cursor] != '%') {
        cursor++
        continue
      }
      if (cursor + 1 < length && this[cursor + 1] == '%') {
        tokens += PrintfToken(index = null, conversion = "%", positional = false)
        cursor += 2
        continue
      }

      val match = PRINTF_TOKEN.find(this, cursor)
      if (match == null || match.range.first != cursor) {
        cursor++
        continue
      }

      val explicitIndex = match.groups[1]?.value?.toInt()
      tokens +=
        PrintfToken(
          index = explicitIndex ?: nextImplicitIndex,
          conversion = match.groups[2]!!.value,
          positional = explicitIndex != null,
        )
      if (explicitIndex == null) nextImplicitIndex++
      cursor = match.range.last + 1
    }

    return tokens
  }

  private fun parseXml(file: File) =
    DocumentBuilderFactory.newInstance()
      .apply { isNamespaceAware = true }
      .newDocumentBuilder()
      .parse(file)
      .also { it.documentElement.normalize() }

  private fun resourceFile(relativePath: String) = File("src/main/res", relativePath)

  private fun Element.childElements(tagName: String? = null): List<Element> {
    val result = mutableListOf<Element>()
    val children = childNodes
    for (index in 0 until children.length) {
      val child = children.item(index)
      if (child is Element && (tagName == null || child.tagName == tagName)) result += child
    }
    return result
  }

  private data class ParsedResources(
    val resources: Map<String, ResourceDefinition>,
    val duplicates: List<String>,
    val duplicatePluralQuantities: List<String>,
  )

  private data class ResourceDefinition(
    val kind: ResourceKind,
    val translatable: Boolean,
    val quantities: Set<String>,
    val duplicateQuantities: List<String>,
    val printfSignatures: List<List<PrintfToken>>,
  )

  private data class PrintfToken(val index: Int?, val conversion: String, val positional: Boolean)

  private enum class ResourceKind {
    STRING,
    PLURALS,
    STRING_ARRAY;

    companion object {
      fun fromTag(tagName: String) =
        when (tagName) {
          "string" -> STRING
          "plurals" -> PLURALS
          "string-array" -> STRING_ARRAY
          else -> error("Unsupported resource tag: $tagName")
        }
    }
  }

  private companion object {
    const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    val RESOURCE_TAGS = setOf("string", "plurals", "string-array")
    val PRINTF_TOKEN =
      Regex("%(?:([1-9][0-9]*)\\$)?[-#+ 0,(<]*[0-9]*(?:\\.[0-9]+)?(?:[tT])?([a-zA-Z])")
  }
}
