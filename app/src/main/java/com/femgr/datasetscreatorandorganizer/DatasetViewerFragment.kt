package com.femgr.datasetscreatorandorganizer

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.femgr.datasetscreatorandorganizer.ui.theme.ForestTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DatasetViewerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ForestTheme {
                    DatasetViewerScreen()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DatasetViewerScreen() {
        val context = LocalContext.current
        val baseDirectory = remember {
            File(context.getExternalFilesDir(null), "ML_Datasets").also {
                if (!it.exists()) it.mkdirs()
            }
        }
        var currentDirectory by remember { mutableStateOf(baseDirectory) }
        
        val files = remember(currentDirectory) {
            currentDirectory.listFiles()
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
        }

        val pathText = remember(currentDirectory) {
            val basePath = context.getExternalFilesDir(null)?.path ?: ""
            currentDirectory.path.replace(basePath, "ML_Datasets")
        }

        // Handle physical back button for folder navigation
        BackHandler(enabled = currentDirectory != baseDirectory) {
            currentDirectory.parentFile?.let { currentDirectory = it }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = pathText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
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
                    FileItem(file = file) {
                        if (file.isDirectory) {
                            currentDirectory = file
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun FileItem(file: File, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .padding(6.dp)
                .fillMaxWidth()
                .height(180.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
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
                            contentDescription = "Folder Icon",
                            modifier = Modifier.size(48.dp).align(Alignment.Center),
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                        )
                    } else {
                        val bitmap = remember(file.absolutePath) {
                            BitmapFactory.decodeFile(file.absolutePath)
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
                                contentScale = ContentScale.Fit,
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title - matches android:textStyle="bold"
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Details Text (e.g., "12 items • Modified 2 days ago")
                // Matches textSize="12sp", alpha="0.7"
                val subtext = if (file.isDirectory) {
                    val itemCount = file.listFiles()?.size ?: 0
                    val modified = getModifiedText(file.lastModified())
                    "$itemCount items • $modified"
                } else {
                    val modified = getModifiedText(file.lastModified())
                    "${file.length() / 1024} KB • $modified"
                }

                Text(
                    text = subtext,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    private fun getModifiedText(time: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - time
        val days = diff / (24 * 60 * 60 * 1000)
        return when {
            days < 1L -> "Modified today"
            days == 1L -> "Modified 1 day ago"
            days < 7L -> "Modified $days days ago"
            else -> {
                val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                "Modified ${sdf.format(Date(time))}"
            }
        }
    }
}
