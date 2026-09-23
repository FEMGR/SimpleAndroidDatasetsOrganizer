package com.femgr.datasetscreatorandorganizer

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DatasetViewerActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvPath: TextView

    // Ensure "private" is separated and spelled correctly here:
    private lateinit var btnDeleteSelected: com.google.android.material.floatingactionbutton.FloatingActionButton

    private var baseDirectory: DocumentFile? = null
    private var currentDirectory: DocumentFile? = null

    // State trackers for multi-selection mode
    private var isSelectionMode = false
    private val selectedFiles = HashSet<DocumentFile>()
    private var fileAdapter: FileAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dataset_viewer)

        recyclerView = findViewById(R.id.recycler_view_files)
        tvPath = findViewById(R.id.tv_current_path)
        btnDeleteSelected = findViewById(R.id.btn_delete_selected)

        btnDeleteSelected.setOnClickListener {
            showBulkDeleteConfirmationDialog()
        }
        val savedUriStr = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("base_folder_uri", null)

        if (savedUriStr != null) {
            val baseUri = Uri.parse(savedUriStr)
            baseDirectory = DocumentFile.fromTreeUri(this, baseUri)

            if (baseDirectory != null) {
                openDirectory(baseDirectory!!)
            } else {
                Toast.makeText(this, "Unable to access directory", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "Select a folder first.", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Handle delete selected button click
        btnDeleteSelected.setOnClickListener {
            showBulkDeleteConfirmationDialog()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // If selection mode is active, back press cancels it instead of navigating away
                if (isSelectionMode) {
                    exitSelectionMode()
                } else if (currentDirectory != null && baseDirectory != null && currentDirectory!!.uri != baseDirectory!!.uri) {
                    currentDirectory!!.parentFile?.let { openDirectory(it) }
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun openDirectory(directory: DocumentFile) {
        currentDirectory = directory
        tvPath.text = directory.name ?: "ML_Datasets"
        exitSelectionMode() // Reset selections when moving into another directory

        val filesAndFolders = directory.listFiles()
            .sortedWith(compareBy({ !it.isDirectory }, { it.name }))

        recyclerView.layoutManager = GridLayoutManager(this, 2)

        fileAdapter = FileAdapter(filesAndFolders)
        recyclerView.adapter = fileAdapter
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        selectedFiles.clear()
        btnDeleteSelected.visibility = View.GONE
        fileAdapter?.notifyDataSetChanged()
    }

    private fun showBulkDeleteConfirmationDialog() {
        if (selectedFiles.isEmpty()) return

        AlertDialog.Builder(this)
            .setTitle("Delete Multiple Items")
            .setMessage("Are you sure you want to delete the ${selectedFiles.size} selected items?")
            .setPositiveButton("Delete") { _, _ ->
                var successCount = 0
                for (file in selectedFiles) {
                    if (file.delete()) {
                        successCount++
                    }
                }
                Toast.makeText(this, "Successfully deleted $successCount items", Toast.LENGTH_SHORT).show()
                currentDirectory?.let { openDirectory(it) } // Refresh view
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Handles single item long-press options (Rename/Delete single)
    // 1. Shows a menu when an individual file/folder is long-pressed
    // 1. Shows a menu when an individual file/folder is long-pressed
    private fun showSingleOptionsDialog(file: DocumentFile, position: Int) {
        val options = arrayOf("Rename", "Delete Item", "Select Multiple Items")

        AlertDialog.Builder(this)
            .setTitle(file.name ?: "Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameDialog(file) // User chose Rename
                    1 -> { // User chose Delete Single Item
                        if (file.delete()) {
                            Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show()
                            currentDirectory?.let { openDirectory(it) } // Refresh layout grid
                        } else {
                            Toast.makeText(this, "Failed to delete item", Toast.LENGTH_SHORT).show()
                        }
                    }
                    2 -> { // Fixed from 3 to 2! User chose Select Multiple Items
                        isSelectionMode = true
                        btnDeleteSelected.visibility = View.VISIBLE
                        fileAdapter?.toggleSelection(file, position)
                        fileAdapter?.notifyDataSetChanged() // Show checkboxes across items
                    }
                }
            }
            .show()
    }

    // 2. Displays the input text field and executes the rename action safely
    private fun showRenameDialog(file: DocumentFile) {
        val input = android.widget.EditText(this)
        val originalName = file.name ?: ""

        input.setText(originalName)
        input.setSelection(input.text.length) // Moves cursor focus cleanly to the end

        AlertDialog.Builder(this)
            .setTitle("Rename Item")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()

                if (newName.isEmpty()) {
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newName == originalName) return@setPositiveButton // No changes made

                // Check if extension was accidentally wiped on file objects
                if (file.isFile && !newName.contains(".")) {
                    val originalExtension = originalName.substringAfterLast('.', "")
                    if (originalExtension.isNotEmpty()) {
                        Toast.makeText(this, "Warning: Keep the file extension (.${originalExtension})", Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }
                }

                // DocumentFile natively changes names safely under Scoped Storage limits
                val success = file.renameTo(newName)
                if (success) {
                    Toast.makeText(this, "Renamed successfully", Toast.LENGTH_SHORT).show()
                    currentDirectory?.let { openDirectory(it) } // Refresh layout grid
                } else {
                    Toast.makeText(this, "Rename failed. File might already exist.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    // --- RECYCLERVIEW ADAPTER ---
    inner class FileAdapter(private val files: List<DocumentFile>) :
        RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

        inner class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgThumbnail: ImageView = view.findViewById(R.id.img_thumbnail)
            val tvFileName: TextView = view.findViewById(R.id.tv_file_name)
            val tvSubtext: TextView = view.findViewById(R.id.tv_subtext)

            // To make this checkable, make sure you add a CheckBox element
            // inside your layout resource item file (R.layout.item_file) with this ID.
            val checkBox: CheckBox? = view.findViewById(R.id.checkbox_select)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
            return FileViewHolder(view)
        }

        override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
            val file = files[position]
            holder.tvFileName.text = file.name

            // Manage Checkbox Visibility States based on selection mode
            if (isSelectionMode) {
                holder.checkBox?.visibility = View.VISIBLE
                holder.checkBox?.isChecked = selectedFiles.contains(file)
            } else {
                holder.checkBox?.visibility = View.GONE
            }

            if (file.isDirectory) {
                val itemCount = file.listFiles().size
                holder.tvSubtext.text = "$itemCount items"
                holder.imgThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
            } else {
                holder.tvSubtext.text = "${file.length() / 1024} KB"
                try {
                    contentResolver.openInputStream(file.uri).use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        if (bitmap != null) holder.imgThumbnail.setImageBitmap(bitmap)
                        else holder.imgThumbnail.setImageResource(android.R.drawable.ic_menu_report_image)
                    }
                } catch (e: Exception) {
                    holder.imgThumbnail.setImageResource(android.R.drawable.ic_menu_report_image)
                }
            }

            // Click action logic
            holder.itemView.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(file, position)
                } else {
                    if (file.isDirectory) openDirectory(file)
                }
            }

            // Long click action logic
            holder.itemView.setOnLongClickListener {
                if (!isSelectionMode) {
                    // Pass BOTH file and position to fix the unresolved reference
                    showSingleOptionsDialog(file, position)
                } else {
                    // If already in selection mode, a long click toggles selection normally
                    toggleSelection(file, position)
                }
                true
            }

            holder.checkBox?.setOnClickListener {
                toggleSelection(file, position)
            }

        }


        fun toggleSelection(file: DocumentFile, position: Int) {
            if (selectedFiles.contains(file)) {
                selectedFiles.remove(file)
            } else {
                selectedFiles.add(file)
            }
            notifyItemChanged(position)

            // Auto-exit mode if the user unchecks everything manually
            if (selectedFiles.isEmpty()) {
                exitSelectionMode()
            }
        }

        override fun getItemCount(): Int = files.size
    }

}
