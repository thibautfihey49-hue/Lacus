package com.megasync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.DocumentsContract
import android.util.Log
import java.io.File

class SyncService : Service() {

    private val CHANNEL = "MEGA_SYNC"
    private var handler: Handler? = null
    private var runnable: Runnable? = null
    private lateinit var megaManager: MegaManager
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("MEGA_SYNC", Context.MODE_PRIVATE)
        megaManager = MegaManager(this, prefs.getString("api_key", "") ?: "")
        createChannel()
        startForeground(1, createNotif())
        handler = Handler(Looper.getMainLooper())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val email = prefs.getString("email", "") ?: ""
        val pass = prefs.getString("pass", "") ?: ""
        val folderUri = prefs.getString("folder_uri", null)
        val interval = prefs.getLong("interval_min", 60) * 60 * 1000

        megaManager.login(email, pass) { ok, _ ->
            if (ok) Log.d("MEGA", "Service connecté")
        }

        runnable = object : Runnable {
            override fun run() {
                syncFolder(folderUri)
                handler?.postDelayed(this, interval)
            }
        }
        handler?.post(runnable!!)
        return START_STICKY
    }

    private fun syncFolder(folderUri: String?) {
        Log.d("MEGA", "=== Synchronisation ===")
        if (!megaManager.isAuthenticated() || folderUri == null) return

        val uri = Uri.parse(folderUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        val cursor = contentResolver.query(childrenUri, null, null, null)

        cursor?.use {
            Log.d("MEGA", "Fichiers : ${it.count}")
            while (it.moveToNext()) {
                val name = it.getString(it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
                val docId = it.getString(it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
                val docUri = DocumentsContract.buildDocumentUriUsingTree(uri, docId)
                val path = docUri.path ?: continue
                val file = File(path)
                if (file.exists() && file.isFile) {
                    megaManager.uploadFile(file.absolutePath, name) { ok, msg ->
                        Log.d("MEGA", msg)
                    }
                }
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, "MEGA Sync", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun createNotif(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL)
                .setContentTitle("☁️ MEGA Sync Auto")
                .setContentText("Synchronisation en cours...")
                .setSmallIcon(android.R.drawable.ic_menu_upload)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("☁️ MEGA Sync Auto")
                .setContentText("Synchronisation en cours...")
                .setSmallIcon(android.R.drawable.ic_menu_upload)
                .build()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runnable?.let { handler?.removeCallbacks(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
