package com.megasync

import android.content.Context
import android.util.Log
import com.mega.sdk.MegaApi
import com.mega.sdk.MegaApiAndroid
import com.mega.sdk.MegaError
import com.mega.sdk.MegaNode
import com.mega.sdk.MegaRequest
import com.mega.sdk.MegaRequestListener
import java.io.File

class MegaManager(context: Context, private val apiKey: String = "") {

    private val megaApi: MegaApi = MegaApiAndroid(
        apiKey,
        null,
        "MEGA Sync Auto",
        context.applicationContext
    )

    private var isLoggedIn = false
    private var rootNode: MegaNode? = null

    fun login(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        megaApi.login(email, password, object : MegaRequestListener {
            override fun onRequestStart(api: MegaApi?, request: MegaRequest?) {
                Log.d("MEGA", "Connexion en cours...")
            }

            override fun onRequestFinish(api: MegaApi?, request: MegaRequest?, e: MegaError?) {
                if (e?.errorCode == MegaError.API_OK) {
                    isLoggedIn = true
                    rootNode = megaApi.rootNode
                    onResult(true, "✅ Connecté à MEGA !")
                } else {
                    onResult(false, "❌ ${e?.errorString}")
                }
            }

            override fun onRequestUpdate(api: MegaApi?, request: MegaRequest?) {}
            override fun onRequestTemporaryError(api: MegaApi?, request: MegaRequest?, e: MegaError?) {
                onResult(false, "⚠️ ${e?.errorString}")
            }
        })
    }

    fun uploadFile(localPath: String, remoteName: String, onResult: (Boolean, String) -> Unit) {
        if (!isLoggedIn) {
            onResult(false, "❌ Non connecté")
            return
        }
        val file = File(localPath)
        if (!file.exists()) {
            onResult(false, "❌ Fichier introuvable")
            return
        }
        val parent = rootNode ?: run {
            onResult(false, "❌ Dossier racine introuvable")
            return
        }
        megaApi.uploadFile(file.absolutePath, parent, remoteName, null, object : MegaRequestListener {
            override fun onRequestStart(api: MegaApi?, request: MegaRequest?) {
                Log.d("MEGA", "Envoi : $remoteName")
            }
            override fun onRequestFinish(api: MegaApi?, request: MegaRequest?, e: MegaError?) {
                if (e?.errorCode == MegaError.API_OK) {
                    onResult(true, "✅ $remoteName envoyé !")
                } else {
                    onResult(false, "❌ ${e?.errorString}")
                }
            }
            override fun onRequestUpdate(api: MegaApi?, request: MegaRequest?) {
                val pct = if (request.totalBytes > 0) (request.transferredBytes * 100 / request.totalBytes).toInt() else 0
                Log.d("MEGA", "Progression: $pct%")
            }
            override fun onRequestTemporaryError(api: MegaApi?, request: MegaRequest?, e: MegaError?) {
                onResult(false, "⚠️ ${e?.errorString}")
            }
        })
    }

    fun isAuthenticated() = isLoggedIn
}
