package com.megasync

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.*

class MainActivity : Activity() {

    private lateinit var megaEmail: EditText
    private lateinit var megaPass: EditText
    private lateinit var loginBtn: Button
    private lateinit var folderBtn: Button
    private lateinit var folderPath: TextView
    private lateinit var intervalSpinner: Spinner
    private lateinit var startBtn: Button
    private lateinit var statusText: TextView
    private lateinit var prefs: SharedPreferences
    private lateinit var megaManager: MegaManager

    private var selectedFolderUri: Uri? = null
    private var isLoggedIn = false

    companion object {
        private const val PICK_FOLDER = 200
        private val INTERVALS = arrayOf("15 minutes", "30 minutes", "1 heure", "6 heures", "24 heures")
        private val INTERVAL_MIN = longArrayOf(15, 30, 60, 360, 1440)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("MEGA_SYNC", 0)
        megaManager = MegaManager(this, prefs.getString("api_key", "") ?: "")

        megaEmail = findViewById(R.id.megaEmail)
        megaPass = findViewById(R.id.megaPass)
        loginBtn = findViewById(R.id.loginBtn)
        folderBtn = findViewById(R.id.folderBtn)
        folderPath = findViewById(R.id.folderPath)
        intervalSpinner = findViewById(R.id.intervalSpinner)
        startBtn = findViewById(R.id.startBtn)
        statusText = findViewById(R.id.statusText)

        intervalSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, INTERVALS)

        loginBtn.setOnClickListener {
            val email = megaEmail.text.toString().trim()
            val pass = megaPass.text.toString().trim()
            if (email.isEmpty() || pass.isEmpty()) {
                statusText.text = "⚠️ Saisis email et mot de passe"
                return@setOnClickListener
            }
            megaManager.login(email, pass) { ok, msg ->
                statusText.text = msg
                if (ok) {
                    isLoggedIn = true
                    prefs.edit().putString("email", email).putString("pass", pass).apply()
                    loginBtn.text = "✅ Connecté"
                    loginBtn.isEnabled = false
                }
            }
        }

        folderBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivityForResult(intent, PICK_FOLDER)
        }

        startBtn.setOnClickListener {
            if (!isLoggedIn) { statusText.text = "⚠️ Connecte-toi d'abord"; return@setOnClickListener }
            if (selectedFolderUri == null) { statusText.text = "⚠️ Choisis un dossier"; return@setOnClickListener }
            prefs.edit()
                .putString("folder_uri", selectedFolderUri.toString())
                .putLong("interval_min", INTERVAL_MIN[intervalSpinner.selectedItemPosition])
                .apply()
            startService(Intent(this, SyncService::class.java))
            statusText.text = "✅ Synchronisation DÉMARRÉE !"
            startBtn.text = "🔄 Sync en cours..."
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FOLDER && resultCode == RESULT_OK && data != null) {
            selectedFolderUri = data.data
            contentResolver.takePersistableUriPermission(data.data!!, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            folderPath.text = "✅ Dossier sélectionné"
            statusText.text = "✅ Dossier prêt !"
        }
    }
}
