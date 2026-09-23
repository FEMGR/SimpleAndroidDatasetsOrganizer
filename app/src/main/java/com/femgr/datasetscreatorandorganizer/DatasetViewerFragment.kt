package com.femgr.datasetscreatorandorganizer

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class DatasetViewerFragment : Fragment(R.layout.activity_dataset_viewer) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvPath: TextView
    private lateinit var baseDirectory: File
    private var currentDirectory: File? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_view_files)
        tvPath = view.findViewById(R.id.tv_current_path)

        baseDirectory = File(requireContext().getExternalFilesDir(null), "ML_Datasets")
        openDirectory(baseDirectory)

        // Custom back button navigation for nested folders
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentDirectory != null && currentDirectory != baseDirectory) {
                    currentDirectory?.parentFile?.let { openDirectory(it) }
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun openDirectory(directory: File) {
        currentDirectory = directory
        tvPath.text = directory.path.replace(requireContext().getExternalFilesDir(null)?.path ?: "", "ML_Datasets")

        val filesAndFolders = directory.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = FileAdapter(filesAndFolders) { selectedFile ->
            if (selectedFile.isDirectory) {
                openDirectory(selectedFile)
            }
        }
    }

    inner class FileAdapter(
        private val files: List<File>,
        private val onItemClick: (File) -> Unit
    ) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

        inner class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgThumbnail: ImageView = view.findViewById(R.id.img_thumbnail)
            val tvFileName: TextView = view.findViewById(R.id.tv_file_name)
            val tvSubtext: TextView = view.findViewById(R.id.tv_subtext)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
            return FileViewHolder(view)
        }

        override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
            val file = files[position]
            holder.tvFileName.text = file.name

            if (file.isDirectory) {
                val itemCount = file.listFiles()?.size ?: 0
                holder.tvSubtext.text = "$itemCount items"
                holder.imgThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
            } else {
                holder.tvSubtext.text = "${file.length() / 1024} KB"
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    holder.imgThumbnail.setImageBitmap(bitmap)
                } else {
                    holder.imgThumbnail.setImageResource(android.R.drawable.ic_menu_report_image)
                }
            }

            holder.itemView.setOnClickListener { onItemClick(file) }
        }

        override fun getItemCount(): Int = files.size
    }
}