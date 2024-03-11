package com.example.project

import android.os.AsyncTask
import android.provider.ContactsContract.Directory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.Socket

class ClientSocket{
    private var s: Socket? = null

    suspend fun sendFiles(fileLoc: String) = withContext(Dispatchers.IO) {
        try {
            s = Socket("192.168.0.198", 8000)
            val input = FileInputStream(fileLoc)



        }  finally {
            s?.getOutputStream()?.flush()
            s?.close()
        }
    }
}