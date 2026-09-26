package com.femgr.datasetscreatorandorganizer.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object DatasetManager {

    const val FOLDER_TRAIN = "train"
    const val FOLDER_TEST = "test"
    const val FOLDER_VALIDATION = "validation"

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

    /**
     * Scans for labeled folders in train, test, and validation directories.
     */
    fun getExistingLabels(context: Context, baseDirUri: Uri): List<String> {
        val rootDir = DocumentFile.fromTreeUri(context, baseDirUri) ?: return emptyList()
        val labels = mutableSetOf<String>()
        
        listOf(FOLDER_TRAIN, FOLDER_TEST, FOLDER_VALIDATION).forEach { folderName ->
            val parentDir = rootDir.findFile(folderName)
            parentDir?.listFiles()?.forEach { file ->
                if (file.isDirectory && !file.name.isNullOrEmpty()) {
                    labels.add(file.name!!)
                }
            }
        }
        
        // Also check if the root itself has folders that aren't the base ones
        rootDir.listFiles().forEach { file ->
            if (file.isDirectory && 
                file.name != FOLDER_TRAIN && 
                file.name != FOLDER_TEST && 
                file.name != FOLDER_VALIDATION) {
                if (!file.name.isNullOrEmpty()) {
                    labels.add(file.name!!)
                }
            }
        }

        return labels.toList().sorted()
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

    /**
     * Splits data from the landing zone (now train folder) into test and validation.
     * Accounts for existing files in all folders to maintain requested proportions.
     */
    fun divideDataset(
        context: Context,
        baseDirUri: Uri,
        labelsToSplit: Set<String>,
        trainPercent: Int,
        valPercent: Int,
        testPercent: Int
    ) {
        val rootDir = DocumentFile.fromTreeUri(context, baseDirUri) ?: return
        val trainDir = rootDir.findFile(FOLDER_TRAIN) ?: return

        for (labelName in labelsToSplit) {
            val labelDirInLanding = trainDir.findFile(labelName) ?: continue
            if (!labelDirInLanding.isDirectory) continue

            val valLabelDir = getLabelDirectory(context, baseDirUri, FOLDER_VALIDATION, labelName)
            val testLabelDir = getLabelDirectory(context, baseDirUri, FOLDER_TEST, labelName)

            // Count existing files in each destination folder to account for them in the ratio
            val currentLandingFiles = labelDirInLanding.listFiles().filter { it.isFile }.sortedBy { it.name }
            val currentValCount = valLabelDir.listFiles().count { it.isFile }
            val currentTestCount = testLabelDir.listFiles().count { it.isFile }
            val currentTrainCount = currentLandingFiles.size

            val totalImages = currentTrainCount + currentValCount + currentTestCount
            if (totalImages == 0) continue

            // Determine target counts based on the user-specified percentages
            val targetVal = (totalImages * (valPercent / 100f)).toInt()
            val targetTest = (totalImages * (testPercent / 100f)).toInt()
            // Train count is essentially the remainder

            // 1. Move from Train (Landing Zone) to Validation if needed
            if (currentValCount < targetVal) {
                val moveCount = (targetVal - currentValCount).coerceAtMost(currentLandingFiles.size)
                currentLandingFiles.take(moveCount).forEach { file ->
                    moveDocumentFile(context, file, valLabelDir)
                }
            }

            // 2. Move from Train (Landing Zone) to Test if needed
            // Re-fetch remaining files in the landing folder after moves to validation
            val remainingLandingFiles = labelDirInLanding.listFiles().filter { it.isFile }.sortedBy { it.name }
            if (currentTestCount < targetTest) {
                val moveCount = (targetTest - currentTestCount).coerceAtMost(remainingLandingFiles.size)
                remainingLandingFiles.take(moveCount).forEach { file ->
                    moveDocumentFile(context, file, testLabelDir)
                }
            }
        }
    }

    private fun moveDocumentFile(context: Context, source: DocumentFile, destDir: DocumentFile) {
        try {
            val extension = source.name?.substringAfterLast('.', "jpg") ?: "jpg"
            val mimeType = source.type ?: "image/jpeg"
            val destFile = getNextFile(context, destDir, extension, mimeType)

            context.contentResolver.openInputStream(source.uri).use { input ->
                context.contentResolver.openOutputStream(destFile.uri).use { output ->
                    if (input != null && output != null) {
                        input.copyTo(output)
                        source.delete()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
