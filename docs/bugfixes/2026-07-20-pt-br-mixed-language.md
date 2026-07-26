# Bugfix: Brazilian Portuguese shows mixed English copy

**Status:** implemented; physical-device validation pending
**Date opened:** 2026-07-20
**Source:** user report and local repository audit
**Outage:** no
**Proposal approved:** 2026-07-20

## Summary

When Brazilian Portuguese is selected, backup settings, saved backups, restore,
and a backup notification fall back to English. Additional app-owned copy can
bypass the selected locale through hardcoded accessibility text and persisted
domain display labels. Every app-owned visible or announced string should use
the active supported locale while caregiver-entered data and proper names remain
unchanged.

## Impact

- Affects caregivers using the officially supported `pt-BR` locale.
- Produces mixed Portuguese/English navigation in Settings and backup/recovery.
- Includes destructive restore/delete confirmations and authorization/error
  states, where language consistency is especially important.
- Can announce Portuguese chart semantics under English and English vaccine/task
  labels under pt-BR.
- Does not corrupt local data or constitute a production outage.

## Reproduction

**Environment:** current Android source tree; app locale set to Brazilian
Portuguese. Android 13+ applies the locale through `LocaleManager`; earlier
versions require the already documented restart.

**Steps:**

1. Select **Português (Brasil)** in Settings and restart when required.
2. Open the dedicated **Backup settings** route.
3. Observe English service state, toggles, errors, or disconnect confirmation.
4. Open **Saved backups** and exercise loading, empty/authorization, selection,
   details, restore, and delete states.
5. Open one restore flow and its confirmation/progress/result states.
6. Trigger a successful automatic-backup notification.
7. Navigate through the Portuguese backup history route to observe Portuguese
   history copy next to English saved-backup/restore copy.
8. With TalkBack or semantics inspection, open the weight chart; its chart
   description is hardcoded in Portuguese even when English is active.

**Expected:** Every app-owned visible and accessibility string resolves in the
active supported locale. User-entered names, brands, acronyms, and proper nouns
are preserved.

**Actual:** Missing pt-BR resources fall back to default English, and some
visible/announced strings bypass Android resources entirely.

**Deterministic resource audit:**

```bash
ruby -r rexml/document -r set -e '
def ids(path)
  document = REXML::Document.new(File.read(path))
  result = Set.new
  document.root.elements.each do |element|
    next unless %w[string plurals string-array].include?(element.name)
    result << [element.name, element.attributes["name"]]
  end
  result
end
base = ids("app/src/main/res/values/strings.xml")
pt_br = ids("app/src/main/res/values-pt-rBR/strings.xml")
puts "missing=#{(base - pt_br).size} extra=#{(pt_br - base).size}"
(base - pt_br).sort.each { |kind, name| puts "#{kind} #{name}" }
'
```

Before the fix, the command reported `missing=73 extra=0` and listed the keys
below.

After the fix, the canonical and pt-BR sets each contain 804 resources: 800
strings and 4 plurals. The post-fix audit reports `missing=0 extra=0`, with no
duplicate names.

## Resource audit evidence

| Qualifier | Total | Strings | Plurals | Arrays | `translatable="false"` |
| --- | ---: | ---: | ---: | ---: | ---: |
| `values` | 796 | 793 | 3 | 0 | 0 |
| `values-pt-rBR` | 723 | 720 | 3 | 0 | 0 |
| `values-es` | 692 | 689 | 3 | 0 | 0 |

- pt-BR is missing exactly 73 canonical strings and has no extra keys.
- No duplicate names or resource-kind mismatches were found.
- The three plurals exist in both target locales with `one` and `other`.
- All shared English/pt-BR printf signatures currently match in index, order,
  and conversion type; escaped `%%` is also preserved.
- No XLIFF markup or `translatable="false"` resources currently exist.
- Spanish is incomplete by 104 canonical keys and is not listed in
  `locales_config.xml`; it remains outside this bugfix.

### Missing pt-BR keys

```text
backup_compatibility_archive_new
backup_compatibility_compatible
backup_compatibility_invalid
backup_compatibility_schema_new
backup_notification_success
backup_preferences_authorization_required
backup_preferences_authorizing
backup_preferences_automatic
backup_preferences_automatic_description
backup_preferences_connect
backup_preferences_connected
backup_preferences_disconnect
backup_preferences_disconnect_confirm
backup_preferences_disconnect_message
backup_preferences_disconnect_title
backup_preferences_disconnected
backup_preferences_error_authorization
backup_preferences_error_disconnect
backup_preferences_error_update
backup_preferences_inexact
backup_preferences_notify
backup_preferences_notify_description
backup_preferences_service_state
backup_preferences_title
backup_preferences_unavailable
backup_preferences_unmetered
backup_preferences_unmetered_description
backup_saved_authorization_required
backup_saved_close
backup_saved_collection_total
backup_saved_confirm_all
backup_saved_confirm_delete
backup_saved_confirm_selected
backup_saved_confirm_title
backup_saved_create
backup_saved_delete
backup_saved_delete_all
backup_saved_delete_selected
backup_saved_description
backup_saved_details
backup_saved_disconnect
backup_saved_empty
backup_saved_error_authorization
backup_saved_error_permanent
backup_saved_error_quota
backup_saved_error_retryable
backup_saved_item_summary
backup_saved_item_title
backup_saved_loading
backup_saved_partial_deletion
backup_saved_reconnect
backup_saved_restore
backup_saved_retry
backup_saved_retry_deletion
backup_saved_select_item
backup_saved_title
backup_saved_unavailable
restore_backup_action
restore_backup_apply_preferences
restore_backup_authorization_required
restore_backup_confirm_action
restore_backup_confirm_message
restore_backup_confirm_title
restore_backup_explanation
restore_backup_failed
restore_backup_invalid
restore_backup_merge
restore_backup_merge_description
restore_backup_replace
restore_backup_replace_description
restore_backup_restoring
restore_backup_success
restore_backup_title
```

## Affected code paths and hardcodes

- `app/src/main/res/values/strings.xml`: canonical restore, saved-backup,
  compatibility, backup-settings, and notification strings.
- `app/src/main/res/values-pt-rBR/strings.xml`: the 73 translations are absent.
- `presentation/feature/backup/BackupRoutes.kt` and
  `RestoreBackupRoute.kt`: correctly request resources, which then fall back.
- `presentation/components/WeightChart.kt`: hardcoded Portuguese accessibility
  description bypasses the selected locale.
- `domain/model/Enums.kt` contains mixed-language `displayName` values.
  `worker/AutoTaskServiceImpl.kt` uses those labels to generate persisted task
  titles, and `TaskNotificationWorker.kt` later announces the stored title.
- Fixed date patterns and direct number/string formatting exist in multiple
  screens. They require a bounded locale-format audit under spec 0009, but are
  not the root cause of the 73-key English fallback.

Technical IDs, routes, MIME types, log tags, emojis, version identifiers, and
caregiver-entered names/brands are deliberately not translated.

## Hypotheses

| Rank | Hypothesis | Prediction | Result |
| --- | --- | --- | --- |
| 1 | New backup features added only canonical English resources. | Default keys exist but the same keys are absent from pt-BR. | confirmed: 73 missing, concentrated in backup/restore |
| 2 | Locale application is broken globally. | Existing older pt-BR strings would also render in English. | falsified: translated Settings and backup-history keys resolve normally |
| 3 | Placeholder/plural incompatibility causes resource lookup or formatting failure. | Shared keys would differ by kind, quantity, or printf signature. | falsified: no current mismatch found |
| 4 | Some mixed output bypasses resources. | Visible semantics/task templates reference literals or domain labels. | confirmed: chart semantics and generated task titles bypass locale resources |

## Investigation log

- 2026-07-20: confirmed only `en` and `pt-BR` are advertised in
  `res/xml/locales_config.xml` and spec 0009.
- 2026-07-20: XML parsing counted 796 canonical resources and 723 pt-BR
  resources; exact difference is 73 strings.
- 2026-07-20: verified zero shared placeholder, plural-quantity, resource-kind,
  duplicate-key, or translatability mismatches.
- 2026-07-20: traced the missing groups to Backup Settings, Saved Backups,
  Restore, compatibility, and success-notification UI.
- 2026-07-20: found locale-bypassing chart semantics and system-generated task
  titles; classified technical identifiers and caregiver data as deliberate
  non-translated values.

## Regression test

- Proposed test file:
  `app/src/test/java/com/woliveiras/petit/localization/PtBrResourceContractTest.kt`
- Proposed test names:
  - `ptBrMatchesCanonicalTranslatableResourceContract`
  - `ptBrPrintfAndPluralSignaturesMatchCanonicalResources`
  - `supportedLocaleConfigExcludesIncompleteSpanishResources`
- Failure observed before fix: yes; the aggregate contract reported the 73
  missing pt-BR resources before translations were added.

The primary JVM test will parse source XML and fail deterministically on missing
or extra localized keys, duplicate names, kind changes, plural-quantity changes,
placeholder index/type changes, or invalid `translatable="false"` duplication.
The expected RED state lists the current 73 missing keys in sorted order.

Targeted locale-configured Compose/instrumented tests will cover representative
Backup Settings, Backup History, Saved Backups, Restore, chart accessibility,
and backup-notification output. A bounded static check will flag literals in
visible Compose, Snackbar, notification, and accessibility APIs, with explicit
allowances for technical identifiers and reviewed suppressions.

## Proposed fix

- Add reviewed Brazilian Portuguese translations for all 73 missing resources.
- Add the resource contract test before translations and capture its expected
  RED result.
- Move the chart accessibility description to a formatted resource.
- Stop using mixed-language enum `displayName` values for app-owned automatic
  task and notification templates; resolve system-generated copy in the active
  locale while preserving caregiver-authored titles and proper names.
- Audit visible date/number/unit formatting at the targeted flows and replace
  locale-bypassing presentation where necessary.
- Keep Spanish unchanged and unsupported.
- Do not modify tests merely to accept missing translations or exempt app-owned
  visible copy.

## Why this fixes the root cause

The translations eliminate Android's canonical-English fallback for the known
resource gap. The resource contract prevents future supported-locale drift, and
the hardcode/template checks close paths that resource parity alone cannot see.

## Verification

- [x] Regression test fails against the pre-fix resource tree with 73 missing keys.
- [x] Regression test passes after the translations and production fixes.
- [ ] Representative pt-BR Settings, Backup Settings, History, Saved Backups,
  Restore, notification, care, Snackbar, and accessibility paths show no
  app-owned English fallback.
- [x] English does not receive hardcoded Portuguese semantics.
- [x] Related JVM and focused instrumented tests pass.
- [x] `./gradlew test` passes.
- [x] `./gradlew spotlessCheck` passes.
- [ ] `./gradlew lintDebug` passes.
- [x] `git diff --check` passes.
- [x] Temporary instrumentation is removed.

If `assembleDebug` is run, `installDebug` must run immediately afterward.

The final `lintDebug` run completed analysis but failed on existing repository
debt with 103 errors and 57 warnings. Its first failure is the pre-existing
`LocalDate.ofInstant` minimum-SDK/desugaring issue in
`worker/AutoTaskServiceImpl.kt`; the eight canonical resources added here are
explicitly exempted from incomplete, unsupported Spanish localization.

## Follow-ups and non-automated validation

- [x] Review pt-BR translation meaning and terminology manually; structural
  parity cannot prove semantic quality.
- [ ] Validate representative flows on a physical device under pt-BR.
- [ ] Keep existing real Google Drive provider validation separate; translation
  coverage does not prove provider behavior.
- [ ] Decide separately whether Spanish will be completed and officially added.

## Approval gate

The user approved this proposal and the material spec 0009 update on
2026-07-20. Implementation proceeds through the documented regression-first
workflow.
