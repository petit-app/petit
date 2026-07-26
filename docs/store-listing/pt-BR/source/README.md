# Brazilian Portuguese Store Image Sources

This directory contains editable inputs used to render the Brazilian Portuguese
Google Play graphics. It is not a publishable metadata directory.

Open `render.html` with an `asset` query parameter to select one layout, for
example:

```text
render.html?asset=feature-graphic
render.html?asset=phone-01-all-pets
render.html?asset=tablet-7-01-all-pets
render.html?asset=tablet-10-01-all-pets
```

After review, save the rendered output directly to the matching canonical
location under:

```text
fastlane/metadata/android/pt-BR/images/
```

Use these mappings:

| Source asset | Canonical output |
| --- | --- |
| `feature-graphic` | `featureGraphic.png` |
| `phone-*` | `phoneScreenshots/*.png` |
| `tablet-7-*` | `sevenInchScreenshots/*.png` |
| `tablet-10-*` | `tenInchScreenshots/*.png` |

The Play icon is `fastlane/metadata/android/pt-BR/images/icon.png`.

Do not copy publishable PNGs or ZIP archives back into
`docs/store-listing/`. The release metadata test rejects duplicate publishable
assets outside the Fastlane hierarchy.
