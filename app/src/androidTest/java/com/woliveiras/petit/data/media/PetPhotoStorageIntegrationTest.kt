package com.woliveiras.petit.data.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.BuildConfig
import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PetPhotoStorageIntegrationTest {

  private lateinit var context: Context
  private lateinit var directory: File
  private lateinit var storage: PetPhotoStorage

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    directory = context.filesDir.resolve("pet_photos").also { it.mkdirs() }
    directory.deleteRecursively()
    directory.mkdirs()
    storage = PetPhotoStorage(context)
  }

  @After
  fun tearDown() {
    directory.deleteRecursively()
  }

  @Test
  fun pickerCopiesValidJpegAndPngIntoDurablePrivateStorage() {
    val jpeg = source("picker.jpg", JPEG_BYTES)
    val png = source("picker.png", PNG_BYTES)

    val jpegResult = storage.importFromPicker(jpeg).getOrThrow()
    val pngResult = storage.importFromPicker(png).getOrThrow()

    assertThat(context.contentResolver.openInputStream(Uri.parse(jpegResult))?.read())
      .isEqualTo(0xFF)
    assertThat(context.contentResolver.openInputStream(Uri.parse(pngResult))?.read())
      .isEqualTo(0x89)
  }

  @Test
  fun pickerRejectsUnsupportedOversizedMisleadingAndMissingContentWithoutLeakingCopy() {
    val unsupported = source("picker.gif", byteArrayOf(0x47, 0x49, 0x46))
    val misleading = source("fake.jpg", "not a jpeg".encodeToByteArray())
    val oversized =
      source("large.jpg", JPEG_BYTES + ByteArray(PetPhotoStorage.MAX_IMAGE_BYTES.toInt()))
    val sources = directory.listFiles()?.toSet().orEmpty()

    assertThat(storage.importFromPicker(unsupported).isFailure).isTrue()
    assertThat(storage.importFromPicker(misleading).isFailure).isTrue()
    assertThat(storage.importFromPicker(oversized).isFailure).isTrue()
    assertThat(storage.importFromPicker(Uri.parse("content://missing/photo.jpg")).isFailure)
      .isTrue()

    assertThat(directory.listFiles()?.toSet()).containsExactlyElementsIn(sources)
  }

  @Test
  fun cameraUsesGrantablePrivateUriAndRejectsInvalidBytes() {
    val pending = storage.createCameraPhoto().getOrThrow()
    val intent = ActivityResultContracts.TakePicture().createIntent(context, pending.uri)

    assertThat(intent.action).isEqualTo("android.media.action.IMAGE_CAPTURE")
    assertThat(intent.getParcelableExtra<Uri>("output")).isEqualTo(pending.uri)
    assertThat(intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION).isNotEqualTo(0)

    context.contentResolver.openOutputStream(pending.uri)?.use {
      it.write("invalid".encodeToByteArray())
    }
    assertThat(storage.completeCameraPhoto(pending, success = true).isFailure).isTrue()
    assertThat(pending.file.exists()).isFalse()
  }

  @Test
  fun cameraAcceptsValidJpegAndCancellationCleansPendingFile() {
    val completed = storage.createCameraPhoto().getOrThrow()
    context.contentResolver.openOutputStream(completed.uri)?.use { it.write(JPEG_BYTES) }
    assertThat(storage.completeCameraPhoto(completed, success = true).getOrThrow())
      .isEqualTo(completed.uri.toString())

    val cancelled = storage.createCameraPhoto().getOrThrow()
    assertThat(storage.completeCameraPhoto(cancelled, success = false).isFailure).isTrue()
    assertThat(cancelled.file.exists()).isFalse()
  }

  private fun source(name: String, bytes: ByteArray): Uri {
    val file = directory.resolve(name).apply { writeBytes(bytes) }
    return FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
  }

  private companion object {
    val JPEG_BYTES = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
    val PNG_BYTES = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0)
  }
}
