package com.woliveiras.petit.catalog

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.security.MessageDigest

fun main(args: Array<String>) {
  val options = parseOptions(args)
  val sourceFile = options.requiredFile("--source")
  val manifestFile = options.requiredFile("--manifest")
  val outputFile = options.requiredOutput("--output")
  val overrides =
    options["--pt-br"]?.let { path ->
      val type = object : TypeToken<Map<String, String>>() {}.type
      Gson().fromJson<Map<String, String>>(File(path).readText(), type)
    } ?: emptyMap()
  val manifest = Gson().fromJson(manifestFile.readText(), SourceManifest::class.java)
  manifest.selectedSourceSha256?.let { expected ->
    val actual = sourceFile.sha256()
    require(actual == expected) {
      "Selected VBO source checksum mismatch: expected $expected, got $actual"
    }
  }
  val generated = BreedCatalogGenerator.generate(sourceFile.readText(), manifest, overrides)
  outputFile.parentFile?.mkdirs()
  outputFile.writeText(generated)
  options["--verify-against"]?.let { expectedPath ->
    val expected = File(expectedPath)
    require(expected.isFile) { "--verify-against is not a file: $expectedPath" }
    require(expected.readBytes().contentEquals(outputFile.readBytes())) {
      "Committed breed catalog is stale. Run :tools:breed-catalog:generateBreedCatalog."
    }
  }
}

private fun parseOptions(args: Array<String>): Map<String, String> {
  require(args.size % 2 == 0) {
    "Usage: --source FILE --manifest FILE [--pt-br FILE] --output FILE [--verify-against FILE]"
  }
  return args.toList().chunked(2).associate { (key, value) -> key to value }
}

private fun Map<String, String>.requiredFile(option: String): File {
  val path = get(option) ?: throw IllegalArgumentException("Missing $option")
  return File(path).also { require(it.isFile) { "$option is not a file: $path" } }
}

private fun Map<String, String>.requiredOutput(option: String): File {
  val path = get(option) ?: throw IllegalArgumentException("Missing $option")
  return File(path)
}

private fun File.sha256(): String {
  val digest = MessageDigest.getInstance("SHA-256")
  inputStream().use { input ->
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
      val read = input.read(buffer)
      if (read < 0) break
      digest.update(buffer, 0, read)
    }
  }
  return digest.digest().joinToString("") { "%02x".format(it) }
}
