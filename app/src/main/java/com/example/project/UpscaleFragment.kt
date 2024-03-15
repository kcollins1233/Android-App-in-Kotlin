package com.example.project

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.Socket
import kotlin.coroutines.CoroutineContext


class UpscaleFragment : Fragment(), View.OnClickListener {

    private lateinit var upscaleButton: Button
    private lateinit var imgView: ImageView
//    private var connected by Delegates.notNull<Boolean>()
    private val scope = MainScope()
    private var context: Context? = null






    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val myView = inflater.inflate(R.layout.upscalefragment, container, false)
        upscaleButton = myView.findViewById(R.id.imgPickButton) as Button
        upscaleButton.setOnClickListener(this)
        imgView = myView.findViewById(R.id.imageView) as ImageView
        context = container?.context
//        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())

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
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {


            val uri: Uri = data?.data!!
            val context: Context = requireContext()

//            val path: String = crocodile8.image_picker_plus.utils.FilePath.getPath(context, uri)

//            writeFile(requireContext(), "test.jpg", uri.toString())
            Log.e("Path", uri.toString())

//            scope.launch {
//                ClientSocket().sendFiles(data.toString())
//            }

            val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            imgView.setImageBitmap(bitmap)

            scope.launch {
                            SocketManager().start(Dispatchers.IO, bitmap)
                        }





        }
    }

    class SocketManager {
        private val out = DataOutputStream(socket.getOutputStream())
        protected val scope = CoroutineScope(TODO())
        private lateinit var socket: Socket
        private lateinit var out: DataOutputStream

        suspend fun start(coroutineContext: CoroutineContext, bitmap: Bitmap) = coroutineScope {
            withContext(coroutineContext) {
            socket = Socket("192.168.0.198", 8080)
            out = DataOutputStream(socket.getOutputStream())
            out.writeBytes(encodeImage(bitmap))
        }
        }

        private fun encodeImage(bitmap: Bitmap): String {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val b: ByteArray = baos.toByteArray()
            return android.util.Base64.encodeToString(b, android.util.Base64.DEFAULT)
        }
    }

    interface MyInterface {
        fun sendData(bitmap: Bitmap)
    }



}
