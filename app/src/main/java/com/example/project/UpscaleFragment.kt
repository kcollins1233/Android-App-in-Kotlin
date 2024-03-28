package com.example.project

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import kotlin.coroutines.CoroutineContext


class UpscaleFragment : Fragment(), View.OnClickListener {

    private lateinit var imgPickButton: Button
    private lateinit var upscaleButton: Button

    private lateinit var imgView: ImageView
//    private var connected by Delegates.notNull<Boolean>()
    private val scope = MainScope()
    private var context: Context? = null
    private var bitmap: Bitmap? = null






    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val myView = inflater.inflate(R.layout.upscalefragment, container, false)
        imgPickButton = myView.findViewById(R.id.imgPickButton) as Button
        upscaleButton = myView.findViewById(R.id.upscaleButton) as Button
        imgPickButton.setOnClickListener(this)
        upscaleButton.setOnClickListener(this)
        imgView = myView.findViewById(R.id.imageView) as ImageView
        context = container?.context
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

        upscaleButton.setOnClickListener {
            if (bitmap != null) {
                scope.launch {
                    SocketManager().start(Dispatchers.IO, bitmap!!)
//                    SocketManager().getFile(Dispatchers.IO, imgView)

                }
                scope.async(Dispatchers.IO) {
                    SocketManager().getFile(Dispatchers.IO, imgView)
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
        private lateinit var socket1: Socket
        private lateinit var serverSocket: ServerSocketChannel
        private lateinit var out: DataOutputStream
        private lateinit var dIS: DataInputStream
        private var boolean = false

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        suspend fun start(coroutineContext: CoroutineContext, bitmap: Bitmap) {
            withContext(coroutineContext) {
                try {
                    socket = Socket("192.168.0.197", 8080)
                    out = DataOutputStream(socket.getOutputStream())
                    out.write(sendFiles(bitmap))
                    out.write(sendFiles(bitmap))
//                    socket.close()
                } catch (e: Exception) {
                    Log.e("Socket", "Error: ${e.message}")
                }
            }

        }

        suspend fun getFile(coroutineContext: CoroutineContext , imgView: ImageView) {
            withContext(coroutineContext) {
                try {
                    serverSocket = ServerSocketChannel.open()
                    serverSocket.socket().reuseAddress = true
                    dIS = DataInputStream(socket.getInputStream())
                    val size = dIS.readInt()
                    val byteArray = ByteArray(size)
                    dIS.readFully(byteArray)
                    val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                    imgView.setImageBitmap(bitmap)
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
}
