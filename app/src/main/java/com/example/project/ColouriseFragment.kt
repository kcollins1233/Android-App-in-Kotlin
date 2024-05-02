package com.example.project

import android.animation.ObjectAnimator
import android.app.Activity
import android.app.AlertDialog
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


class ColouriseFragment : Fragment(), View.OnClickListener {

    private lateinit var imgPickButton: Button
    private lateinit var colouriseButton: Button
    private lateinit var saveButton: Button

    private lateinit var imgViewColourise: ImageView
    private val scope = MainScope()
//    private var context: Context? = null
    private var bitmap: Bitmap? = null
    private var myView: View? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (myView == null) {
            myView = inflater.inflate(R.layout.colourisefragment, container, false)
            imgPickButton = myView!!.findViewById(R.id.cfChooseImage)!!
            colouriseButton = myView!!.findViewById(R.id.cfProcessButton)!!
            saveButton = myView!!.findViewById(R.id.cfSaveButton)!!
            imgPickButton.setOnClickListener(this)
            colouriseButton.setOnClickListener(this)
            imgViewColourise = myView!!.findViewById(R.id.cfImageView)!!
//            context = container?.context
        }
//        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())

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

        colouriseButton.setOnClickListener {
            if (bitmap != null) {
                scope.launch {
                    SocketManager().start(Dispatchers.IO, bitmap!!)
                    SocketManager().getFile(Dispatchers.IO, imgViewColourise, this@ColouriseFragment, colouriseButton, saveButton)

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
//            imgView.setImageBitmap(bitmap)







        }
    }

    class SocketManager {
        //        private var out = DataOutputStream(socket.getOutputStream())
//        protected val scope = CoroutineScope(TODO())
        private lateinit var socket: Socket
        private lateinit var serverSocket: ServerSocket
        private lateinit var out: DataOutputStream
        private var boolean = false

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        suspend fun start(coroutineContext: CoroutineContext, bitmap: Bitmap) {
            withContext(coroutineContext) {
                try {
                    socket = Socket("192.168.0.194", 8080)
                    out = DataOutputStream(socket.getOutputStream())
                    out.writeUTF("colourise")
                    out.flush()
                    out.write(sendFiles(bitmap))
                    out.write(sendFiles(bitmap))
                    out.flush()


                } catch (e: Exception) {
                    Log.e("Socket", "Error: ${e.message}")
                }
            }

        }



        suspend fun getFile(coroutineContext: CoroutineContext , imgViewColourise: ImageView, fragment: Fragment, processBtn: Button, saveBtn: Button) {
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
                            imgViewColourise.setImageBitmap(bitmap)
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
                                ColouriseFragment().saveImageToGallery(bitmap, fragment.requireContext())
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
                    val picturesDirectory =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val file = File(picturesDirectory, "$name.png")
                    val fos = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
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
