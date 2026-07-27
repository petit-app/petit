# Petit breed catalog tool

This Kotlin/JVM tool builds the offline cat and dog catalog shipped in the
Android app. Runtime code never downloads registry or VBO data.

## Pinned inputs

- `catalog-source/source-manifest.json` records the VBO release URL, SHA-256,
  selected-source checksum, and review date.
- `catalog-source/vbo-2026-04-15-selected.json` contains the reviewed VBO nodes
  and relationships relevant to FCI, FIFe, and TICA.
- `catalog-source/pt-br-overrides.json` contains reviewed Brazilian Portuguese
  display-name overrides. Names not listed there intentionally retain their
  canonical proper name.

The selected source is derived from VBO and remains licensed under CC BY 4.0.
Registry URLs are provenance only. Petit does not copy registry images or breed
standard prose.

## Commands

Use the JDK bundled with Android Studio when the shell does not already expose a
Java runtime:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :tools:breed-catalog:generateBreedCatalog
./gradlew :tools:breed-catalog:test :tools:breed-catalog:verifyBreedCatalog
```

`generateBreedCatalog` writes `app/src/main/assets/breed_catalog.json`.
`verifyBreedCatalog` regenerates into the build directory and compares the
result byte for byte with the committed asset.

## Updating the source

1. Download the new published VBO JSON release to a temporary directory.
2. Verify its SHA-256 and update `source-manifest.json`.
3. Extract only cat and dog nodes with full or partial recognition metadata and
   FCI, FIFe, or TICA provenance. Retain relationships between selected nodes.
4. Review the source diff, registry status, canonical names, and pt-BR
   overrides.
5. Update the selected-source checksum in the manifest.
6. Regenerate and run both verification commands.
7. Review the generated asset diff before committing.

Source updates are deliberate release work. Do not add a runtime refresh path.
