package com.woliveiras.petit.presentation.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.TaskSubjectControl
import com.woliveiras.petit.domain.model.TaskSubjectOptions
import com.woliveiras.petit.domain.model.VaccineType

/**
 * Text for what a task is about, resolved from the stored catalog code.
 *
 * A code written by a newer version of the app is unknown here, so the stored free text is used
 * instead of dropping the subject.
 */
fun taskSubjectLabel(context: Context, task: Task): String? =
  taskSubjectLabel(context, task.kind, task.subjectCode, task.subjectName)

fun taskSubjectLabel(
  context: Context,
  kind: TaskKind,
  subjectCode: String?,
  subjectName: String?,
): String? {
  val name = subjectName?.trim()?.takeIf { it.isNotEmpty() }
  return when (TaskSubjectOptions.controlFor(kind)) {
    TaskSubjectControl.NONE -> null
    TaskSubjectControl.MEDICATION -> name
    TaskSubjectControl.VACCINE -> {
      val vaccine = subjectCode?.let { code -> VaccineType.entries.firstOrNull { it.name == code } }
      when {
        vaccine == null -> name
        vaccine == VaccineType.OTHER -> name ?: context.getString(vaccine.labelResId())
        else -> context.getString(vaccine.labelResId())
      }
    }
    TaskSubjectControl.ANTIPARASITIC -> {
      val type = subjectCode?.let { code -> DewormingType.entries.firstOrNull { it.name == code } }
      val typeLabel = type?.let { context.getString(it.labelResId()) }
      when {
        typeLabel == null -> name
        name == null -> typeLabel
        else -> "$typeLabel: $name"
      }
    }
  }
}

@Composable fun Task.subjectLabel(): String? = taskSubjectLabel(LocalContext.current, this)
