package com.femgr.datasetscreatorandorganizer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.femgr.datasetscreatorandorganizer.utils.DatasetManager
import java.io.File

class MainFragment : Fragment(R.layout.fragment_main) {

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
            Toast.makeText(requireContext(), R.string.toast_capture_cancelled, Toast.LENGTH_SHORT).show()
        }
    }

    // 2. Folder Picker Intent Contract
    private val openDocumentTreeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val context = requireContext()
            // Persist read/write folder permissions across device reboots
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            // Save URI string to local preferences
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("base_folder_uri", uri.toString())
                .apply()

            baseDirectoryUri = uri
            DatasetManager.initializeBaseFolders(context, uri)
            Toast.makeText(context, "Dataset folder selected successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Folder selection is required.", Toast.LENGTH_SHORT).show()
        }
    }

    // 3. Permission Launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        // Load existing saved folder URI preference if it exists
        val savedUriStr = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("base_folder_uri", null)

        if (savedUriStr != null) {
            baseDirectoryUri = Uri.parse(savedUriStr)
        } else {
            // Automatically launch folder picker on first run
            openDocumentTreeLauncher.launch(null)
        }

        view.findViewById<Button>(R.id.btn_take_picture).setOnClickListener {
            if (baseDirectoryUri == null) {
                openDocumentTreeLauncher.launch(null)
            } else {
                promptForLabelAndCapture()
            }
        }

        view.findViewById<Button>(R.id.btn_view_dataset).setOnClickListener {
            // Preserved your exact Jetpack Navigation routing logic
            findNavController().navigate(R.id.action_mainFragment_to_datasetViewerFragment)
        }

        view.findViewById<Button>(R.id.btn_split_data).setOnClickListener {
            val baseUri = baseDirectoryUri
            if (baseUri != null) {
                DatasetManager.divideDataset(context, baseUri)
                Toast.makeText(context, R.string.toast_split_complete, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Please select a dataset folder first.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun promptForLabelAndCapture() {
        val input = EditText(requireContext())
        input.hint = getString(R.string.hint_enter_label)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_title_label)
            .setMessage(R.string.dialog_message_label)
            .setView(input)
            .setPositiveButton(R.string.dialog_btn_capture) { _, _ ->
                val label = input.text.toString().trim()
                if (label.isNotEmpty()) {
                    currentLabel = label
                    checkPermissionAndLaunch()
                } else {
                    Toast.makeText(requireContext(), R.string.toast_label_empty, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.dialog_btn_cancel, null)
            .show()
    }

    private fun checkPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        try {
            val context = requireContext()
            tempImageFile = File.createTempFile("temp_capture", ".jpg", context.cacheDir)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                tempImageFile!!
            )

            tempImageUri = uri
            takePictureContract.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error opening camera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun processCapturedImage() {
        val context = requireContext()
        val tempFile = tempImageFile
        val baseUri = baseDirectoryUri

        if (tempFile == null || !tempFile.exists() || tempFile.length() == 0L) {
            Toast.makeText(context, "Camera did not write image data.", Toast.LENGTH_LONG).show()
            return
        }
        if (baseUri == null) {
            Toast.makeText(context, "No save directory targeted.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val labelDirDocument = DatasetManager.getLabelDirectory(context, baseUri, "test", currentLabel)
            val extension = tempFile.extension.ifEmpty { "jpg" }
            val mimeType = if (extension == "png") "image/png" else "image/jpeg"

            val finalFileDocument = DatasetManager.getNextFile(context, labelDirDocument, extension, mimeType)

            // Safe contentResolver system data stream
            tempFile.inputStream().use { input ->
                context.contentResolver.openOutputStream(finalFileDocument.uri)?.use { output ->
                    input.copyTo(output)
                }
            }
            tempFile.delete()

            val successMessage = getString(R.string.toast_saved_image, finalFileDocument.name, currentLabel)
            Toast.makeText(context, successMessage, Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Save error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
