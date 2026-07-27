plugins {
  alias(libs.plugins.kotlin.jvm)
  application
}

kotlin { jvmToolchain(17) }

application { mainClass.set("com.woliveiras.petit.catalog.MainKt") }

dependencies {
  implementation(libs.gson)

  testImplementation(libs.junit)
  testImplementation(libs.truth)
}

val selectedSource = layout.projectDirectory.file("catalog-source/vbo-2026-04-15-selected.json")
val sourceManifest = layout.projectDirectory.file("catalog-source/source-manifest.json")
val ptBrOverrides = layout.projectDirectory.file("catalog-source/pt-br-overrides.json")
val appCatalog = rootProject.layout.projectDirectory.file("app/src/main/assets/breed_catalog.json")

fun JavaExec.configureCatalogGeneration(output: Provider<RegularFile>) {
  group = "breed catalog"
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("com.woliveiras.petit.catalog.MainKt")
  args(
    "--source",
    selectedSource.asFile.absolutePath,
    "--manifest",
    sourceManifest.asFile.absolutePath,
    "--pt-br",
    ptBrOverrides.asFile.absolutePath,
    "--output",
    output.get().asFile.absolutePath,
  )
  inputs.files(selectedSource, sourceManifest, ptBrOverrides)
  outputs.file(output)
}

val generateBreedCatalog =
  tasks.register<JavaExec>("generateBreedCatalog") {
    description = "Generates the committed offline breed catalog asset."
    configureCatalogGeneration(provider { appCatalog })
  }

val verificationCatalog = layout.buildDirectory.file("verification/breed_catalog.json")

tasks.register<JavaExec>("verifyBreedCatalog") {
  description = "Regenerates the catalog and checks the committed asset byte for byte."
  configureCatalogGeneration(verificationCatalog)
  args("--verify-against", appCatalog.asFile.absolutePath)
  inputs.file(appCatalog)
  mustRunAfter(generateBreedCatalog)
}
