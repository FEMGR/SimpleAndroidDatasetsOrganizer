package com.femgr.datasetscreatorandorganizer

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.femgr.datasetscreatorandorganizer.ui.theme.ForestTheme

class DatasetViewerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val savedUriStr = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("base_folder_uri", null)

        if (savedUriStr == null) {
            Toast.makeText(this, "Select a folder first.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val baseUri = Uri.parse(savedUriStr)
        val rootDoc = DocumentFile.fromTreeUri(this, baseUri)

        if (rootDoc == null) {
            Toast.makeText(this, "Unable to access directory", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            ForestTheme {
                DatasetViewerScreen(rootDoc)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DatasetViewerScreen(rootDirectory: DocumentFile) {
        val context = LocalContext.current
        var currentDirectory by remember { mutableStateOf(rootDirectory) }
        var refreshTrigger by remember { mutableStateOf(0) }
        
        val files = remember(currentDirectory, refreshTrigger) {
            currentDirectory.listFiles()
                .sortedWith(compareBy({ !it.isDirectory }, { it.name }))
        }

        var isSelectionMode by remember { mutableStateOf(false) }
        val selectedFiles = remember { mutableStateListOf<DocumentFile>() }

        // Back navigation
        BackHandler(enabled = isSelectionMode || currentDirectory.uri != rootDirectory.uri) {
            if (isSelectionMode) {
                isSelectionMode = false
                selectedFiles.clear()
            } else {
                currentDirectory.parentFile?.let { currentDirectory = it }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = currentDirectory.name ?: "ML_Datasets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        if (isSelectionMode) {
                            IconButton(onClick = { 
                                showBulkDeleteConfirmation(selectedFiles) {
                                    isSelectionMode = false
                                    selectedFiles.clear()
                                    refreshTrigger++
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(10.dp),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                items(files) { file ->
                    val isSelected = selectedFiles.contains(file)
                    FileItem(
                        file = file,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelectionMode) {
                                if (isSelected) selectedFiles.remove(file) else selectedFiles.add(file)
                                if (selectedFiles.isEmpty()) isSelectionMode = false
                            } else {
                                if (file.isDirectory) {
                                    currentDirectory = file
                                }
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                showSingleOptionsDialog(file, 
                                    onRename = { refreshTrigger++ },
                                    onDelete = { refreshTrigger++ },
                                    onEnterSelection = {
                                        isSelectionMode = true
                                        selectedFiles.add(file)
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun FileItem(
        file: DocumentFile, 
        isSelectionMode: Boolean, 
        isSelected: Boolean,
        onClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        val context = LocalContext.current
        Card(
            modifier = Modifier
                .padding(6.dp)
                .fillMaxWidth()
                .height(180.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        if (file.isDirectory) {
                            Image(
                                painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                                contentDescription = "Folder",
                                modifier = Modifier.size(48.dp).align(Alignment.Center),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                            )
                        } else {
                            val bitmap = remember(file.uri) {
                                try {
                                    context.contentResolver.openInputStream(file.uri)?.use { 
                                        BitmapFactory.decodeStream(it)
                                    }
                                } catch (e: Exception) { null }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Thumbnail",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = android.R.drawable.ic_menu_report_image),
                                    contentDescription = "Error",
                                    modifier = Modifier.size(48.dp).align(Alignment.Center),
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = file.name ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val subtext = if (file.isDirectory) {
                        val count = file.listFiles().size
                        "$count items"
                    } else {
                        "${file.length() / 1024} KB"
                    }

                    Text(
                        text = subtext,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    )
                }

                if (isSelectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )
                }
            }
        }
    }

    private fun showSingleOptionsDialog(
        file: DocumentFile, 
        onRename: () -> Unit, 
        onDelete: () -> Unit,
        onEnterSelection: () -> Unit
    ) {
        val options = arrayOf("Rename", "Delete Item", "Select Multiple Items")
        AlertDialog.Builder(this)
            .setTitle(file.name ?: "Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameDialog(file, onRename)
                    1 -> {
                        if (file.delete()) {
                            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                            onDelete()
                        }
                    }
                    2 -> onEnterSelection()
                }
            }
            .show()
    }

    private fun showRenameDialog(file: DocumentFile, onRename: () -> Unit) {
        val input = android.widget.EditText(this)
        val originalName = file.name ?: ""
        input.setText(originalName)
        input.setSelection(input.text.length)

        AlertDialog.Builder(this)
            .setTitle("Rename Item")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty() && newName != originalName) {
                    if (file.renameTo(newName)) {
                        onRename()
                    } else {
                        Toast.makeText(this, "Rename failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBulkDeleteConfirmation(selected: List<DocumentFile>, onDeleted: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Delete Items")
            .setMessage("Delete ${selected.size} items?")
            .setPositiveButton("Delete") { _, _ ->
                selected.forEach { it.delete() }
                onDeleted()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
