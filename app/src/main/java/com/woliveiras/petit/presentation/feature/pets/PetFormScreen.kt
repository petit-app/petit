package com.woliveiras.petit.presentation.feature.pets

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.PetType
import com.woliveiras.petit.domain.model.Sex
import com.woliveiras.petit.presentation.components.PetitTopAppBar
import com.woliveiras.petit.presentation.util.localizedBreed
import com.woliveiras.petit.presentation.util.localizedColor
import com.woliveiras.petit.presentation.util.localizedName
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Common option key
private const val OTHER_OPTION = "OTHER"

private fun breedsForPetType(petType: PetType): List<String> =
  when (petType) {
    PetType.CAT ->
      listOf(
        "MIXED_BREED",
        "PERSIAN",
        "SIAMESE",
        "MAINE_COON",
        "RAGDOLL",
        "BRITISH_SHORTHAIR",
        "BENGAL",
        "ABYSSINIAN",
        "SPHYNX",
        "SCOTTISH_FOLD",
        "BURMESE",
        "RUSSIAN_BLUE",
        "NORWEGIAN_FOREST",
        "TURKISH_ANGORA",
        OTHER_OPTION,
      )
    PetType.DOG ->
      listOf(
        "MIXED_BREED",
        "LABRADOR",
        "GOLDEN_RETRIEVER",
        "GERMAN_SHEPHERD",
        "POODLE",
        "BULLDOG",
        "BEAGLE",
        "SHIH_TZU",
        "YORKSHIRE",
        OTHER_OPTION,
      )
    else -> listOf("MIXED_BREED", OTHER_OPTION)
  }

private val commonColors =
  listOf(
    "BLACK",
    "WHITE",
    "ORANGE",
    "GRAY",
    "TABBY",
    "CALICO",
    "TUXEDO",
    "TORTOISESHELL",
    "CREAM",
    "BROWN",
    "BLUE",
    "SILVER",
    "GOLDEN",
    "BRINDLE",
    OTHER_OPTION,
  )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetFormScreen(
  petId: String?,
  onNavigateBack: () -> Unit,
  onPetSaved: (String) -> Unit,
  viewModel: PetFormViewModel = hiltViewModel(),
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var showDatePicker by remember { mutableStateOf(false) }
  var showPetTypeDropdown by remember { mutableStateOf(false) }
  var showSexDropdown by remember { mutableStateOf(false) }
  var showBreedDropdown by remember { mutableStateOf(false) }
  var showColorDropdown by remember { mutableStateOf(false) }
  var showPhotoSourceDialog by remember { mutableStateOf(false) }

  // Track if user explicitly selected "Other"
  var isBreedOther by remember { mutableStateOf(false) }
  var isColorOther by remember { mutableStateOf(false) }

  // Reset breed "Other" state when petType changes
  LaunchedEffect(uiState.petType) { isBreedOther = false }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) {
      uri: Uri? ->
      uri?.let(viewModel::importPhoto)
    }

  val cameraLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { success ->
      viewModel.completeCameraCapture(success)
    }

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is PetFormEvent.PetSaved -> onPetSaved(event.petId)
        is PetFormEvent.Error -> {
          snackbarHostState.showSnackbar(event.message)
        }
        is PetFormEvent.LaunchCamera -> cameraLauncher.launch(event.uri)
      }
    }
  }

  if (showPhotoSourceDialog) {
    AlertDialog(
      onDismissRequest = { showPhotoSourceDialog = false },
      title = { Text(stringResource(R.string.pet_photo_choose_title)) },
      text = {
        Column {
          TextButton(
            onClick = {
              showPhotoSourceDialog = false
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            }
          ) {
            Text(stringResource(R.string.pet_photo_gallery))
          }
          TextButton(
            onClick = {
              showPhotoSourceDialog = false
              viewModel.startCameraCapture()
            }
          ) {
            Text(stringResource(R.string.pet_photo_camera))
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showPhotoSourceDialog = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }

  // Date Picker Dialog
  if (showDatePicker) {
    val datePickerState =
      rememberDatePickerState(
        initialSelectedDateMillis =
          uiState.birthDate?.let {
            it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
          }
      )

    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
              if (!date.isAfter(LocalDate.now())) {
                viewModel.updateBirthDate(date)
              }
            }
            showDatePicker = false
          }
        ) {
          Text(stringResource(R.string.action_ok))
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    ) {
      DatePicker(state = datePickerState, showModeToggle = false)
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      PetitTopAppBar(
        title = {
          Text(
            if (uiState.isEditMode) stringResource(R.string.pet_form_title_edit)
            else stringResource(R.string.pet_form_title_new)
          )
        },
        onNavigateBack = onNavigateBack,
      )
    },
  ) { padding ->
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
      if (uiState.isLoading) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
      } else {
        Column(
          modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          // Photo Picker
          Surface(
            modifier =
              Modifier.size(120.dp).clip(CircleShape).clickable { showPhotoSourceDialog = true },
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Box(contentAlignment = Alignment.Center) {
              if (uiState.photoUri != null) {
                AsyncImage(
                  model =
                    ImageRequest.Builder(context).data(uiState.photoUri).crossfade(true).build(),
                  contentDescription = stringResource(R.string.pet_form_photo),
                  modifier = Modifier.fillMaxSize().clip(CircleShape),
                  contentScale = ContentScale.Crop,
                )
              } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(
                    Icons.Default.AddAPhoto,
                    contentDescription = stringResource(R.string.pet_form_add_photo),
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                  Text(
                    text = stringResource(R.string.pet_form_photo),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Name Field
          FormField(label = stringResource(R.string.pet_form_name)) {
            OutlinedTextField(
              value = uiState.name,
              onValueChange = { viewModel.updateName(it) },
              placeholder = { Text(stringResource(R.string.pet_form_name_placeholder)) },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              isError = uiState.nameError != null,
              supportingText = uiState.nameError?.let { { Text(it) } },
              keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
              shape = RoundedCornerShape(12.dp),
              colors =
                OutlinedTextFieldDefaults.colors(
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
            )
          }

          // Birth Date Field
          val birthDateLabel = stringResource(R.string.pet_form_birth_date)
          val formattedBirthDate =
            uiState.birthDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
          FormField(label = birthDateLabel) {
            Box(modifier = Modifier.fillMaxWidth()) {
              OutlinedTextField(
                value =
                  formattedBirthDate ?: stringResource(R.string.pet_form_birth_date_placeholder),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
                trailingIcon = {
                  Icon(imageVector = Icons.Default.DateRange, contentDescription = null)
                },
                colors =
                  OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    unfocusedTextColor =
                      if (uiState.birthDate != null) MaterialTheme.colorScheme.onSurface
                      else MaterialTheme.colorScheme.onSurfaceVariant,
                  ),
                shape = RoundedCornerShape(12.dp),
              )
              Box(
                modifier =
                  Modifier.matchParentSize()
                    .semantics {
                      contentDescription =
                        if (formattedBirthDate != null) "$birthDateLabel: $formattedBirthDate"
                        else birthDateLabel
                      role = Role.Button
                    }
                    .clickable { showDatePicker = true }
              )
            }
          }

          // Pet Type Dropdown
          FormField(label = stringResource(R.string.pet_form_type_label)) {
            ExposedDropdownMenuBox(
              expanded = showPetTypeDropdown,
              onExpandedChange = { showPetTypeDropdown = it },
              modifier = Modifier.fillMaxWidth(),
            ) {
              OutlinedTextField(
                value = uiState.petType.localizedName(),
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                readOnly = true,
                trailingIcon = {
                  ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPetTypeDropdown)
                },
                shape = RoundedCornerShape(12.dp),
                colors =
                  OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                  ),
              )

              ExposedDropdownMenu(
                expanded = showPetTypeDropdown,
                onDismissRequest = { showPetTypeDropdown = false },
              ) {
                PetType.entries.forEach { petType ->
                  DropdownMenuItem(
                    text = { Text(petType.localizedName()) },
                    onClick = {
                      viewModel.updatePetType(petType)
                      showPetTypeDropdown = false
                    },
                  )
                }
              }
            }
          }

          // Sex Dropdown
          FormField(label = stringResource(R.string.pet_form_sex)) {
            ExposedDropdownMenuBox(
              expanded = showSexDropdown,
              onExpandedChange = { showSexDropdown = it },
              modifier = Modifier.fillMaxWidth(),
            ) {
              val sexText = uiState.sex.localizedName()
              OutlinedTextField(
                value = sexText,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                readOnly = true,
                trailingIcon = {
                  ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSexDropdown)
                },
                shape = RoundedCornerShape(12.dp),
                colors =
                  OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                  ),
              )

              ExposedDropdownMenu(
                expanded = showSexDropdown,
                onDismissRequest = { showSexDropdown = false },
              ) {
                Sex.entries.forEach { sex ->
                  DropdownMenuItem(
                    text = { Text(sex.localizedName()) },
                    onClick = {
                      viewModel.updateSex(sex)
                      showSexDropdown = false
                    },
                  )
                }
              }
            }
          }

          // Breed Dropdown
          FormField(label = stringResource(R.string.pet_form_breed)) {
            // Build localized breeds map
            val localizedBreeds =
              breedsForPetType(uiState.petType).associateWith { localizedBreed(it) }
            val otherLabel = stringResource(R.string.option_other)

            // Determine if current breed is a known option or custom
            val isKnownBreed =
              uiState.breed.isNotEmpty() &&
                breedsForPetType(uiState.petType).any { it == uiState.breed }
            val showCustomBreedInput = isBreedOther || (uiState.breed.isNotEmpty() && !isKnownBreed)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              ExposedDropdownMenuBox(
                expanded = showBreedDropdown,
                onExpandedChange = { showBreedDropdown = it },
                modifier = Modifier.fillMaxWidth(),
              ) {
                val displayText =
                  when {
                    uiState.breed.isEmpty() -> stringResource(R.string.pet_form_breed_select)
                    showCustomBreedInput -> otherLabel
                    else -> localizedBreed(uiState.breed)
                  }
                OutlinedTextField(
                  value = displayText,
                  onValueChange = {},
                  modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                  readOnly = true,
                  placeholder = { Text(stringResource(R.string.pet_form_breed_select)) },
                  trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = showBreedDropdown)
                  },
                  shape = RoundedCornerShape(12.dp),
                  colors =
                    OutlinedTextFieldDefaults.colors(
                      unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                )

                ExposedDropdownMenu(
                  expanded = showBreedDropdown,
                  onDismissRequest = { showBreedDropdown = false },
                ) {
                  localizedBreeds.forEach { (key, displayName) ->
                    DropdownMenuItem(
                      text = { Text(displayName) },
                      onClick = {
                        if (key == OTHER_OPTION) {
                          isBreedOther = true
                          viewModel.updateBreed("")
                        } else {
                          isBreedOther = false
                          viewModel.updateBreed(key)
                        }
                        showBreedDropdown = false
                      },
                    )
                  }
                }
              }

              // Custom breed input when "Other" is selected
              if (showCustomBreedInput) {
                OutlinedTextField(
                  value = uiState.breed,
                  onValueChange = { viewModel.updateBreed(it) },
                  placeholder = { Text(stringResource(R.string.pet_form_breed_custom)) },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                  shape = RoundedCornerShape(12.dp),
                  colors =
                    OutlinedTextFieldDefaults.colors(
                      unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                )
              }
            }
          }

          // Color Dropdown
          FormField(label = stringResource(R.string.pet_form_color)) {
            // Build localized colors map
            val localizedColors = commonColors.associateWith { localizedColor(it) }
            val otherLabel = stringResource(R.string.option_other)

            // Determine if current color is a known option or custom
            val isKnownColor =
              uiState.color.isNotEmpty() && commonColors.any { it == uiState.color }
            val showCustomColorInput = isColorOther || (uiState.color.isNotEmpty() && !isKnownColor)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              ExposedDropdownMenuBox(
                expanded = showColorDropdown,
                onExpandedChange = { showColorDropdown = it },
                modifier = Modifier.fillMaxWidth(),
              ) {
                val displayText =
                  when {
                    uiState.color.isEmpty() -> stringResource(R.string.pet_form_color_select)
                    showCustomColorInput -> otherLabel
                    else -> localizedColor(uiState.color)
                  }
                OutlinedTextField(
                  value = displayText,
                  onValueChange = {},
                  modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                  readOnly = true,
                  placeholder = { Text(stringResource(R.string.pet_form_color_select)) },
                  trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = showColorDropdown)
                  },
                  shape = RoundedCornerShape(12.dp),
                  colors =
                    OutlinedTextFieldDefaults.colors(
                      unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                )

                ExposedDropdownMenu(
                  expanded = showColorDropdown,
                  onDismissRequest = { showColorDropdown = false },
                ) {
                  localizedColors.forEach { (key, displayName) ->
                    DropdownMenuItem(
                      text = { Text(displayName) },
                      onClick = {
                        if (key == OTHER_OPTION) {
                          isColorOther = true
                          viewModel.updateColor("")
                        } else {
                          isColorOther = false
                          viewModel.updateColor(key)
                        }
                        showColorDropdown = false
                      },
                    )
                  }
                }
              }

              // Custom color input when "Other" is selected
              if (showCustomColorInput) {
                OutlinedTextField(
                  value = uiState.color,
                  onValueChange = { viewModel.updateColor(it) },
                  placeholder = { Text(stringResource(R.string.pet_form_color_custom)) },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                  shape = RoundedCornerShape(12.dp),
                  colors =
                    OutlinedTextFieldDefaults.colors(
                      unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                )
              }
            }
          }

          // Microchip Number
          FormField(label = stringResource(R.string.pet_form_microchip)) {
            OutlinedTextField(
              value = uiState.microchipNumber,
              onValueChange = { viewModel.updateMicrochipNumber(it) },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              colors =
                OutlinedTextFieldDefaults.colors(
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
            )
          }

          // Passport Number
          FormField(label = stringResource(R.string.pet_form_passport)) {
            OutlinedTextField(
              value = uiState.passportNumber,
              onValueChange = { viewModel.updatePassportNumber(it) },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              colors =
                OutlinedTextFieldDefaults.colors(
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
            )
          }

          // Notes
          FormField(label = stringResource(R.string.pet_form_notes)) {
            OutlinedTextField(
              value = uiState.notes,
              onValueChange = { viewModel.updateNotes(it) },
              modifier = Modifier.fillMaxWidth(),
              minLines = 3,
              maxLines = 5,
              shape = RoundedCornerShape(12.dp),
              colors =
                OutlinedTextFieldDefaults.colors(
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Save Button
          Button(
            onClick = { viewModel.savePet() },
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
          ) {
            if (uiState.isSaving) {
              CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
              )
              Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
              text = stringResource(R.string.action_save),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }

          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    }
  }
}

@Composable
private fun FormField(label: String, content: @Composable () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    content()
  }
}
