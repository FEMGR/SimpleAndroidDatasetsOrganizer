package com.femgr.datasetscreatorandorganizer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.femgr.datasetscreatorandorganizer.ui.theme.ForestTheme
import com.femgr.datasetscreatorandorganizer.utils.DatasetManager
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ForestTheme {
                MainScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        val context = LocalContext.current
        val sharedPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
        
        var baseDirectoryUri by remember { 
            mutableStateOf(sharedPrefs.getString("base_folder_uri", null)?.let { Uri.parse(it) })
        }
        var showLabelDialog by remember { mutableStateOf(false) }
        var showSplitDialog by remember { mutableStateOf(false) }
        var existingLabels by remember { mutableStateOf(emptyList<String>()) }
        var currentLabel by remember { mutableStateOf("") }
        var tempImageFile by remember { mutableStateOf<File?>(null) }

        // Automatically scan for labels when base directory is loaded or changed
        LaunchedEffect(baseDirectoryUri) {
            baseDirectoryUri?.let {
                existingLabels = DatasetManager.getExistingLabels(context, it)
            }
        }

        // --- Contracts ---
        
        val openDocumentTreeLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                sharedPrefs.edit().putString("base_folder_uri", uri.toString()).apply()
                baseDirectoryUri = uri
                DatasetManager.initializeBaseFolders(context, uri)
                // Refresh labels after selection
                existingLabels = DatasetManager.getExistingLabels(context, uri)
                Toast.makeText(context, "Dataset folder selected!", Toast.LENGTH_SHORT).show()
            }
        }

        val takePictureLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && tempImageFile != null && baseDirectoryUri != null) {
                processCapturedImage(context, tempImageFile!!, baseDirectoryUri!!, currentLabel)
                // Refresh labels in case a new one was created
                existingLabels = DatasetManager.getExistingLabels(context, baseDirectoryUri!!)
            } else {
                tempImageFile?.delete()
                Toast.makeText(context, R.string.toast_capture_cancelled, Toast.LENGTH_SHORT).show()
            }
        }

        val requestPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Toast.makeText(context, "Permission granted! Tap Capture again.", Toast.LENGTH_SHORT).show()
            }
        }

        val labelFolderPickerLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            uri?.let { pickedUri ->
                val documentFile = DocumentFile.fromTreeUri(context, pickedUri)
                val folderName = documentFile?.name
                if (!folderName.isNullOrEmpty()) {
                    currentLabel = folderName
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        tempImageFile = launchCamera(context, takePictureLauncher)
                    } else {
                        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            }
        }

        // --- UI ---

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.title_main),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Take Picture Button
                MainButton(
                    text = stringResource(R.string.btn_take_picture),
                    containerColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        if (baseDirectoryUri == null) {
                            openDocumentTreeLauncher.launch(null)
                        } else {
                            showLabelDialog = true
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // View Datasets Button
                MainButton(
                    text = "View Saved Datasets",
                    containerColor = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        val intent = Intent(context, DatasetViewerActivity::class.java)
                        context.startActivity(intent)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Split Dataset Button
                MainButton(
                    text = stringResource(R.string.btn_split_data),
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    onClick = {
                        if (baseDirectoryUri == null) {
                            openDocumentTreeLauncher.launch(null)
                        } else {
                            showSplitDialog = true
                        }
                    }
                )

                if (baseDirectoryUri == null) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "No folder selected. Tap a button to select one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Label Selection/Creation Dialog
        if (showLabelDialog) {
            LabelSelectionDialog(
                existingLabels = existingLabels,
                onDismiss = { showLabelDialog = false },
                onConfirm = { label ->
                    currentLabel = label
                    showLabelDialog = false
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        tempImageFile = launchCamera(context, takePictureLauncher)
                    } else {
                        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onBrowseFolder = {
                    showLabelDialog = false
                    labelFolderPickerLauncher.launch(null)
                }
            )
        }

        // Split Dataset Dialog
        if (showSplitDialog) {
            SplitDatasetDialog(
                labels = existingLabels,
                onDismiss = { showSplitDialog = false },
                onSplit = { selectedLabels, train, valPct, test ->
                    DatasetManager.divideDataset(context, baseDirectoryUri!!, selectedLabels, train, valPct, test)
                    showSplitDialog = false
                    Toast.makeText(context, R.string.toast_split_complete, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    @Composable
    fun LabelSelectionDialog(
        existingLabels: List<String>,
        onDismiss: () -> Unit,
        onConfirm: (String) -> Unit,
        onBrowseFolder: () -> Unit
    ) {
        var labelInput by remember { mutableStateOf("") }
        val filteredLabels = existingLabels.filter { it.contains(labelInput, ignoreCase = true) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select or Create Label") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = labelInput,
                        onValueChange = { labelInput = it },
                        label = { Text("Search or Enter New Label") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Found Labels:", style = MaterialTheme.typography.labelMedium)
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        if (filteredLabels.isNotEmpty()) {
                            LazyColumn {
                                items(filteredLabels) { label ->
                                    Text(
                                        text = label,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { labelInput = label }
                                            .padding(12.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), thickness = 0.5.dp)
                                }
                            }
                        } else {
                            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("No labels found", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = onBrowseFolder,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Folder, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Pick Existing Folder from Device")
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = labelInput.isNotBlank(),
                    onClick = { onConfirm(labelInput.trim()) }
                ) {
                    Text("Capture")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    @Composable
    fun SplitDatasetDialog(
        labels: List<String>,
        onDismiss: () -> Unit,
        onSplit: (Set<String>, Int, Int, Int) -> Unit
    ) {
        val context = LocalContext.current
        var selectedLabels by remember { mutableStateOf(labels.toSet()) }
        var useDefaultProportions by remember { mutableStateOf(true) }
        var trainPct by remember { mutableStateOf("70") }
        var valPct by remember { mutableStateOf("15") }
        var testPct by remember { mutableStateOf("15") }
        
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Split Dataset Settings") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Select folders to split (from /train):", style = MaterialTheme.typography.titleSmall)
                    labels.forEach { label ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedLabels = if (selectedLabels.contains(label)) {
                                        selectedLabels - label
                                    } else {
                                        selectedLabels + label
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = selectedLabels.contains(label),
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Text("Proportions:", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = useDefaultProportions,
                            onClick = { useDefaultProportions = true }
                        )
                        Text("Default (70/15/15)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !useDefaultProportions,
                            onClick = { useDefaultProportions = false }
                        )
                        Text("Custom Proportions (%)")
                    }

                    if (!useDefaultProportions) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            TextField(
                                value = trainPct,
                                onValueChange = { trainPct = it.filter { c -> c.isDigit() } },
                                label = { Text("Train") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = valPct,
                                onValueChange = { valPct = it.filter { c -> c.isDigit() } },
                                label = { Text("Val") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = testPct,
                                onValueChange = { testPct = it.filter { c -> c.isDigit() } },
                                label = { Text("Test") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        
                        val total = (trainPct.toIntOrNull() ?: 0) + (valPct.toIntOrNull() ?: 0) + (testPct.toIntOrNull() ?: 0)
                        if (total != 100) {
                            Text(
                                text = "Total must be 100% (Current: $total%)",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val t = if (useDefaultProportions) 70 else trainPct.toIntOrNull() ?: 70
                        val v = if (useDefaultProportions) 15 else valPct.toIntOrNull() ?: 15
                        val te = if (useDefaultProportions) 15 else testPct.toIntOrNull() ?: 15
                        
                        if (selectedLabels.isNotEmpty() && (t + v + te == 100)) {
                            onSplit(selectedLabels, t, v, te)
                        } else {
                            Toast.makeText(context, "Invalid settings", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Split")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    @Composable
    fun MainButton(text: String, containerColor: Color, onClick: () -> Unit) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = Color.White
            )
        ) {
            Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }

    private fun launchCamera(
        context: Context,
        launcher: androidx.activity.result.ActivityResultLauncher<Uri>
    ): File? {
        return try {
            val file = File.createTempFile("temp_capture", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            launcher.launch(uri)
            file
        } catch (e: Exception) {
            Toast.makeText(context, "Camera error: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    private fun processCapturedImage(context: Context, tempFile: File, baseUri: Uri, label: String) {
        try {
            // Landing zone is now /train
            val labelDir = DatasetManager.getLabelDirectory(context, baseUri, DatasetManager.FOLDER_TRAIN, label)
            val finalFile = DatasetManager.getNextFile(context, labelDir, "jpg", "image/jpeg")

            context.contentResolver.openOutputStream(finalFile.uri)?.use { output ->
                tempFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            tempFile.delete()
            Toast.makeText(context, "Saved to train/$label", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Save error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
