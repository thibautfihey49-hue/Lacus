package com.megasync

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.*

class MainActivity : Activity() {

    private lateinit var folderBtn: Button
    private lateinit var folderPath: TextView
    private lateinit var uploadBtn: Button
    private lateinit var statusText: TextView

    private var selectedFolderUri: Uri? = null

    companion object {
        private const val PICK_FOLDER = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        folderBtn = findViewById(R.id.folderBtn)
        folderPath = findViewById(R.id.folderPath)
        uploadBtn = findViewById(R.id.uploadBtn)
        statusText = findViewById(R.id.statusText)

        folderBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivityForResult(intent, PICK_FOLDER)
        }

        uploadBtn.setOnClickListener {
            if (selectedFolderUri == null) {
                statusText.text = "⚠️ Choisis un dossier d'abord"
                return@setOnClickListener
            }
            uploadFolderToMEGA()
        }
    }

    private fun uploadFolderToMEGA() {
        val uri = selectedFolderUri!!
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            uri,
            DocumentsContract.getTreeDocumentId(uri)
        )
        
        val cursor = contentResolver.query(childrenUri, null, null, null)
        val filesToShare = mutableListOf<Uri>()

        cursor?.use {
            statusText.text = "📂 ${it.count} fichiers trouvés"
            while (it.moveToNext()) {
                val docId = it.getString(it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
                val docUri = DocumentsContract.buildDocumentUriUsingTree(uri, docId)
                contentResolver.takePersistableUriPermission(docUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                filesToShare.add(docUri)
            }
        }

        if (filesToShare.isEmpty()) {
            statusText.text = "⚠️ Aucun fichier dans ce dossier"
            return
        }

        statusText.text = "📤 Ouverture de MEGA pour l'envoi..."

        if (filesToShare.size == 1) {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "*/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, filesToShare[0])
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(shareIntent, "Envoyer vers MEGA"))
        } else {
            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
            shareIntent.type = "*/*"
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(filesToShare))
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(shareIntent, "Envoyer vers MEGA"))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FOLDER && resultCode == RESULT_OK && data != null) {
            selectedFolderUri = data.data
            contentResolver.takePersistableUriPermission(data.data!!, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            folderPath.text = "✅ Dossier sélectionné"
            statusText.text = "✅ Prêt ! Clique sur Envoyer"
        }
    }
}
