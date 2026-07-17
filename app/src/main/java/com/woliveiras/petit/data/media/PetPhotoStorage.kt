package com.woliveiras.petit.data.media

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.woliveiras.petit.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class PendingCameraPhoto(val uri: Uri, internal val file: File)

interface PetPhotoStore {
  fun importFromPicker(source: Uri): Result<String>

  fun createCameraPhoto(): Result<PendingCameraPhoto>

  fun completeCameraPhoto(pending: PendingCameraPhoto, success: Boolean): Result<String>
}

@Singleton
class PetPhotoStorage @Inject constructor(@ApplicationContext private val context: Context) :
  PetPhotoStore {

  override fun importFromPicker(source: Uri): Result<String> = runCatching {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(source)
    require(mimeType == JPEG_MIME || mimeType == PNG_MIME) { "Unsupported image format" }
    val extension = if (mimeType == PNG_MIME) "png" else "jpg"
    val destination = photoDirectory().resolve("${UUID.randomUUID()}.$extension")
    try {
      copyValidated(source, destination)
      require(hasExpectedSignature(destination, mimeType)) { "Image content does not match format" }
      uriFor(destination).toString()
    } catch (error: Exception) {
      destination.delete()
      throw error
    }
  }

  override fun createCameraPhoto(): Result<PendingCameraPhoto> = runCatching {
    val file = photoDirectory().resolve("${UUID.randomUUID()}.jpg")
    try {
      check(file.createNewFile()) { "Could not create camera image" }
      PendingCameraPhoto(uriFor(file), file)
    } catch (error: Exception) {
      file.delete()
      throw error
    }
  }

  override fun completeCameraPhoto(pending: PendingCameraPhoto, success: Boolean): Result<String> =
    runCatching {
        if (!success) {
          pending.file.delete()
          throw IOException("Camera capture cancelled")
        }
        require(pending.file.exists() && pending.file.length() in 1..MAX_IMAGE_BYTES) {
          "Invalid camera image"
        }
        require(hasExpectedSignature(pending.file, JPEG_MIME)) { "Invalid camera image" }
        pending.uri.toString()
      }
      .onFailure { pending.file.delete() }

  private fun copyValidated(source: Uri, destination: File) {
    try {
      context.contentResolver.openInputStream(source).use { input ->
        requireNotNull(input) { "Image is unavailable" }
        destination.outputStream().use { output ->
          val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
          var total = 0L
          while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= MAX_IMAGE_BYTES) { "Image exceeds 5 MB" }
            output.write(buffer, 0, read)
          }
          require(total > 0) { "Image is empty" }
        }
      }
    } catch (error: Exception) {
      destination.delete()
      throw error
    }
  }

  private fun photoDirectory(): File =
    context.filesDir.resolve(PHOTO_DIRECTORY).also { check(it.mkdirs() || it.isDirectory) }

  private fun uriFor(file: File): Uri =
    FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)

  private fun hasExpectedSignature(file: File, mimeType: String): Boolean {
    val header = ByteArray(PNG_SIGNATURE.size)
    val read = file.inputStream().use { it.read(header) }
    return when (mimeType) {
      JPEG_MIME -> read >= JPEG_SIGNATURE.size && header.take(3) == JPEG_SIGNATURE
      PNG_MIME -> read >= PNG_SIGNATURE.size && header.toList() == PNG_SIGNATURE
      else -> false
    }
  }

  companion object {
    const val MAX_IMAGE_BYTES = 5L * 1024L * 1024L
    private const val PHOTO_DIRECTORY = "pet_photos"
    private const val JPEG_MIME = "image/jpeg"
    private const val PNG_MIME = "image/png"
    private val JPEG_SIGNATURE = listOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val PNG_SIGNATURE =
      listOf(
        0x89.toByte(),
        0x50.toByte(),
        0x4E.toByte(),
        0x47.toByte(),
        0x0D.toByte(),
        0x0A.toByte(),
        0x1A.toByte(),
        0x0A.toByte(),
      )
  }
}
