package com.femgr.datasetscreatorandorganizer.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object DatasetManager {

    private const val FOLDER_TRAIN = "train"
    private const val FOLDER_TEST = "test"
    private const val FOLDER_VALIDATION = "validation"

    fun initializeBaseFolders(context: Context, baseDirUri: Uri) {
        val rootDir = DocumentFile.fromTreeUri(context, baseDirUri) ?: return
        val folders = listOf(FOLDER_TRAIN, FOLDER_TEST, FOLDER_VALIDATION)
        folders.forEach { folderName ->
            val dir = rootDir.findFile(folderName)
            if (dir == null || !dir.isDirectory) {
                rootDir.createDirectory(folderName)
            }
        }
    }

    fun getLabelDirectory(context: Context, baseDirUri: Uri, parentFolder: String, label: String): DocumentFile {
        val rootDir = DocumentFile.fromTreeUri(context, baseDirUri)!!

        // Find or create parent ("train", "test", etc.)
        var parentDir = rootDir.findFile(parentFolder)
        if (parentDir == null || !parentDir.isDirectory) {
            parentDir = rootDir.createDirectory(parentFolder)!!
        }

        // Find or create sub-folder label name
        var labelDir = parentDir.findFile(label)
        if (labelDir == null || !labelDir.isDirectory) {
            labelDir = parentDir.createDirectory(label)!!
        }
        return labelDir
    }

    fun getNextFile(context: Context, labelDir: DocumentFile, extension: String, mimeType: String): DocumentFile {
        val files = labelDir.listFiles()
        var maxIndex = 0
        val regex = Regex("^(\\d+)\\..+$")

        for (file in files) {
            val name = file.name ?: continue
            val match = regex.find(name)
            if (match != null) {
                val index = match.groupValues[1].toIntOrNull() ?: 0
                if (index > maxIndex) maxIndex = index
            }
        }

        val nextIndex = maxIndex + 1
        val nextFileName = String.format("%04d.%s", nextIndex, extension)
        return labelDir.createFile(mimeType, nextFileName)!!
    }

    fun divideDataset(context: Context, baseDirUri: Uri) {
        val rootDir = DocumentFile.fromTreeUri(context, baseDirUri) ?: return
        val testDir = rootDir.findFile(FOLDER_TEST) ?: return

        val labelDirs = testDir.listFiles().filter { it.isDirectory }

        for (labelDir in labelDirs) {
            val images = labelDir.listFiles().filter { it.isFile && !it.name.isNullOrEmpty() }.sortedBy { it.name }

            if (images.size > 200) {
                val label = labelDir.name ?: continue

                val trainCount = (images.size * 0.70).toInt()
                val valCount = (images.size * 0.15).toInt()

                val trainLabelDir = getLabelDirectory(context, baseDirUri, FOLDER_TRAIN, label)
                val valLabelDir = getLabelDirectory(context, baseDirUri, FOLDER_VALIDATION, label)

                // Move first chunk to Train
                for (i in 0 until trainCount) {
                    val fileToMove = images[i]
                    val extension = fileToMove.name?.substringAfterLast('.', "jpg") ?: "jpg"
                    val mimeType = fileToMove.type ?: "image/jpeg"

                    val destFile = getNextFile(context, trainLabelDir, extension, mimeType)
                    moveDocumentFile(context, fileToMove, destFile)
                }

                // Move second chunk to Validation
                for (i in trainCount until (trainCount + valCount)) {
                    val fileToMove = images[i]
                    val extension = fileToMove.name?.substringAfterLast('.', "jpg") ?: "jpg"
                    val mimeType = fileToMove.type ?: "image/jpeg"

                    val destFile = getNextFile(context, valLabelDir, extension, mimeType)
                    moveDocumentFile(context, fileToMove, destFile)
                }
            }
        }
    }

    // Helper method to simulate file moving under Scoped Storage streams
    private fun moveDocumentFile(context: Context, source: DocumentFile, dest: DocumentFile) {
        try {
            context.contentResolver.openInputStream(source.uri).use { input ->
                context.contentResolver.openOutputStream(dest.uri).use { output ->
                    if (input != null && output != null) {
                        input.copyTo(output)
                        source.delete() // Delete original test directory item after clean copying
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
