package com.example.project

import android.animation.ObjectAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import kotlin.coroutines.CoroutineContext


class UpscaleFragment : Fragment(), View.OnClickListener {

    private lateinit var imgPickButton: Button
    private lateinit var upscaleButton: Button
    private lateinit var saveButton: Button
    private lateinit var imgViewUpscale: ImageView
    private val scope = MainScope()
    private var context: Context? = null
    private var bitmap: Bitmap? = null
    private var myView: View? = null

//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//        outState.putString("currentFragmentTag", "UpscaleFragment")
//
//        CoroutineScope(Dispatchers.Default).launch {
//            val stream = ByteArrayOutputStream()
//            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
//            val byteArray = stream.toByteArray()
//
//            withContext(Dispatchers.Main) {
//                outState.putByteArray("bitmap", byteArray)
//            }
//        }
//    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (myView == null) {
            myView = inflater.inflate(R.layout.upscalefragment, container, false)
            imgPickButton = myView!!.findViewById(R.id.ufChooseImage)!!
            upscaleButton = myView!!.findViewById(R.id.ufProcessButton)!!
            saveButton = myView!!.findViewById(R.id.ufSaveButton)!!
            imgViewUpscale = myView!!.findViewById(R.id.ufImageView)!!
            imgPickButton.setOnClickListener(this)
            upscaleButton.setOnClickListener(this)
            saveButton.setOnClickListener(this)
        }

//        val byteArray = savedInstanceState?.getByteArray("bitmap")
//        if (byteArray != null) {
//            bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
//            imgView.setImageBitmap(bitmap)
//        }

        return myView
    }

    @RequiresApi(33)
    override fun onClick(v: View?) {
        imgPickButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "image/*"
            intent.action = Intent.ACTION_OPEN_DOCUMENT
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivityForResult(intent, 1)
        }

        upscaleButton.setOnClickListener {
            if (bitmap != null) {
                scope.launch {
                    SocketManager().start(Dispatchers.IO, bitmap!!)
                    SocketManager().getFile(Dispatchers.IO, imgViewUpscale, this@UpscaleFragment, upscaleButton, saveButton)
                }
            }
        }
    }



    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {

            val uri: Uri = data?.data!!
            val context: Context = requireContext()

            Log.e("Path", uri.toString())

            bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }

    class SocketManager() {
        private lateinit var socket: Socket
        private lateinit var serverSocket: ServerSocket
        private lateinit var out: DataOutputStream
        private var boolean = false

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        suspend fun start(coroutineContext: CoroutineContext, bitmap: Bitmap) {
            withContext(coroutineContext) {
                try {
                    socket = Socket("192.168.0.194", 8080) // Change to the IP of the server
                    out = DataOutputStream(socket.getOutputStream())
                    out.writeUTF("upscale")
                    out.flush()
                    out.write(sendFiles(bitmap))
                    out.write(sendFiles(bitmap))
                    out.flush()
                    out.close()


                } catch (e: Exception) {
                    Log.e("Socket", "Error: ${e.message}")
                }
            }

        }



        suspend fun getFile(coroutineContext: CoroutineContext , imgViewUpscale: ImageView, fragment: Fragment, processBtn: Button, saveBtn: Button) {
            withContext(coroutineContext) {
                try {
                    serverSocket = ServerSocket(8181)

                    while (true) {
                        socket = serverSocket.accept()
                        val size = DataInputStream(socket.getInputStream()).readInt()
                        val byteArray = ByteArray(size)
                        DataInputStream(socket.getInputStream()).readFully(byteArray)
                        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                        fragment.activity?.runOnUiThread {
                            imgViewUpscale.setImageBitmap(bitmap)
                            val scale = fragment.context?.resources?.displayMetrics?.density
                            val params = processBtn.layoutParams
                            ObjectAnimator.ofFloat(processBtn, "translationX", (-62.5f * scale!!)).apply {
                                duration = 1000
                                start()
                            }
                            params.width = (125 * scale).toInt()
                            processBtn.layoutParams = params

                            saveBtn.visibility = View.VISIBLE
                            saveBtn.isEnabled = true
                            saveBtn.setOnClickListener {
                                UpscaleFragment().saveImageToGallery(bitmap, fragment.requireContext())
                            }
                        }

                    }

                } catch (e: Exception) {
                    Log.e("Socket", "Error: ${e.message}")
                }
            }
        }

        private fun sendFiles(bitmap: Bitmap): ByteArray {

            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            return if (!boolean) {
                boolean = true
                ByteBuffer.allocate(4).putInt(byteArray.size).array()
            } else {
                boolean = false
                byteArray
            }
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap, context: Context) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_layout, null)
        val editText = dialogView.findViewById<EditText>(R.id.ufAlert)

        AlertDialog.Builder(context)
            .setTitle("Save Image")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                try {
                    val name = editText.text.toString()
                    val picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val file = File(picturesDirectory, "$name.png")
                    val fos = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    fos.flush()
                    fos.close()
                    Toast.makeText(context, "Image saved", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {

                }

            }
            .setNegativeButton("Cancel") { _, _ -> }
            .show()
    }
}
