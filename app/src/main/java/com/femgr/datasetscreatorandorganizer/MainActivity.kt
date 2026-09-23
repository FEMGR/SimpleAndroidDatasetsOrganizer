package com.femgr.datasetscreatorandorganizer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.femgr.datasetscreatorandorganizer.utils.DatasetManager
import java.io.File

class MainActivity : AppCompatActivity() {

    private var baseDirectoryUri: Uri? = null
    private var tempImageUri: Uri? = null
    private var tempImageFile: File? = null
    private var currentLabel: String = "unlabeled"

    // 1. Camera Intent Contract
    private val takePictureContract = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            processCapturedImage()
        } else {
            tempImageFile?.delete()
            Toast.makeText(this, R.string.toast_capture_cancelled, Toast.LENGTH_SHORT).show()
        }
    }

    // 2. Folder Picker Intent Contract
    private val openDocumentTreeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // Persist read/write folder permissions across device reboots
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            // Save URI string to local storage
            getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("base_folder_uri", uri.toString())
                .apply()

            baseDirectoryUri = uri
            DatasetManager.initializeBaseFolders(this, uri)
            Toast.makeText(this, "Dataset folder selected successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Folder selection is required.", Toast.LENGTH_SHORT).show()
        }
    }

    // 3. Permission Launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) launchCamera()
        else Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Load existing saved folder URI preference if it exists
        val savedUriStr = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("base_folder_uri", null)

        if (savedUriStr != null) {
            baseDirectoryUri = Uri.parse(savedUriStr)
        } else {
            // Automatically launch folder picker on first run
            openDocumentTreeLauncher.launch(null)
        }

        findViewById<Button>(R.id.btn_take_picture).setOnClickListener {
            if (baseDirectoryUri == null) {
                openDocumentTreeLauncher.launch(null)
            } else {
                promptForLabelAndCapture()
            }
        }

        findViewById<Button>(R.id.btn_split_data).setOnClickListener {
            val baseUri = baseDirectoryUri
            if (baseUri != null) {
                DatasetManager.divideDataset(this, baseUri)
                Toast.makeText(this, R.string.toast_split_complete, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please select a dataset folder first.", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btn_view_dataset).setOnClickListener {
            val intent = Intent(this, DatasetViewerActivity::class.java)
            startActivity(intent)
        }
    }

    private fun promptForLabelAndCapture() {
        val input = EditText(this)
        input.hint = getString(R.string.hint_enter_label)

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_title_label)
            .setMessage(R.string.dialog_message_label)
            .setView(input)
            .setPositiveButton(R.string.dialog_btn_capture) { _, _ ->
                val label = input.text.toString().trim()
                if (label.isNotEmpty()) {
                    currentLabel = label
                    checkPermissionAndLaunch()
                } else {
                    Toast.makeText(this, R.string.toast_label_empty, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.dialog_btn_cancel, null)
            .show()
    }

    private fun checkPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        try {
            // Temp capture file remains safe inside context's internal app cache folder
            tempImageFile = File.createTempFile("temp_capture", ".jpg", cacheDir)
            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", tempImageFile!!)
            tempImageUri = uri
            takePictureContract.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error opening camera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun processCapturedImage() {
        val tempFile = tempImageFile
        val baseUri = baseDirectoryUri

        if (tempFile == null || !tempFile.exists() || tempFile.length() == 0L) {
            Toast.makeText(this, "Camera did not write image data.", Toast.LENGTH_LONG).show()
            return
        }
        if (baseUri == null) {
            Toast.makeText(this, "No save directory targeted.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val labelDirDocument = DatasetManager.getLabelDirectory(this, baseUri, "test", currentLabel)
            val extension = tempFile.extension.ifEmpty { "jpg" }
            val mimeType = if (extension == "png") "image/png" else "image/jpeg"

            val finalFileDocument = DatasetManager.getNextFile(this, labelDirDocument, extension, mimeType)

            // Stream bytes safely from cache File to the SAF Document Storage Uri
            tempFile.inputStream().use { input ->
                contentResolver.openOutputStream(finalFileDocument.uri)?.use { output ->
                    input.copyTo(output)
                }
            }
            tempFile.delete()

            val successMessage = getString(R.string.toast_saved_image, finalFileDocument.name, currentLabel)
            Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Save error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
