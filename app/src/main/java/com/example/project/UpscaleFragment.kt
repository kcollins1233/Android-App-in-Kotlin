package com.example.project

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import crocodile8.image_picker_plus.ImagePickerPlus
import crocodile8.image_picker_plus.PickRequest
import crocodile8.image_picker_plus.PickSource
import kotlinx.coroutines.MainScope
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URI


class UpscaleFragment : Fragment(), View.OnClickListener {

    private lateinit var upscaleButton: Button
    private lateinit var imgView: ImageView
    private val scope = MainScope()
    private var resolver = activity?.contentResolver




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val myView = inflater.inflate(R.layout.upscalefragment, container, false)
        upscaleButton = myView.findViewById(R.id.imgPickButton) as Button
        upscaleButton.setOnClickListener(this)
        imgView = myView.findViewById(R.id.imageView) as ImageView

        return myView
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onClick(v: View?) {
        upscaleButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "image/*"
            intent.action = Intent.ACTION_OPEN_DOCUMENT
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivityForResult(intent, 1)
        }

    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK){

            val uri: Uri = data?.data!!
            val context: Context = requireContext()

            val path: String = crocodile8.image_picker_plus.utils.FilePath.getPath(context, uri)

//            writeFile(requireContext(), "test.jpg", uri.toString())
            Log.e("Path", uri.toString())

//            scope.launch {
//                ClientSocket().sendFiles(data.toString())
//            }




        }
    }

}