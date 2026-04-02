package com.memorytag.app.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build

/**
 * Gère toute la logique NFC :
 * - Activation/désactivation du foreground dispatch
 * - Extraction de l'ID depuis le tag
 */
class NfcHelper(private val activity: Activity) {

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    /** Vérifie si le NFC est disponible et activé sur l'appareil */
    val isNfcAvailable: Boolean get() = nfcAdapter != null
    val isNfcEnabled: Boolean get() = nfcAdapter?.isEnabled == true

    /**
     * Active le "foreground dispatch" :
     * l'app capte les tags NFC EN PRIORITÉ, même si une autre app est configurée.
     * À appeler dans onResume().
     */
    fun enableForegroundDispatch() {
        val intent = Intent(activity, activity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(activity, 0, intent, pendingIntentFlags)

        val filters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        )

        nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, filters, null)
    }

    /**
     * Désactive le foreground dispatch.
     * À appeler dans onPause().
     */
    fun disableForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    /**
     * Extrait l'ID du souvenir depuis l'intent NFC.
     *
     * Stratégie de lecture :
     * 1. Si le tag contient un message NDEF avec une URL → extrait l'ID de l'URL
     * 2. Sinon → utilise l'UID hardware du tag (converti en hex)
     */
    fun extractMemoryId(intent: Intent): String? {
    /*    return when (intent.action) {
            NfcAdapter.ACTION_NDEF_DISCOVERED -> extractFromNdef(intent)
            NfcAdapter.ACTION_TAG_DISCOVERED -> extractFromTagUid(intent)
            else -> null
        }*/

        extractFromNdef(intent)?.let { return it.trim() }
        return extractFromTagUid(intent)?.trim()
    }

    /**
     * Lit le payload NDEF (typiquement une URL comme https://memorytag.app/memory/PARIS_001)
     * et en extrait l'ID de souvenir.
     */
    private fun extractFromNdef(intent: Intent): String? {
        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            ?: return extractFromTagUid(intent)

        val ndefMessage = rawMessages[0] as? NdefMessage ?: return null
        val record = ndefMessage.records.firstOrNull() ?: return null

        val payloadBytes = record.payload

        // Décode le payload NDEF en UTF-8
        val payload = try {
            val statusByte = payloadBytes[0].toInt()
            val languageCodeLength = statusByte and 0x3F
            String(
                payloadBytes,
                1 + languageCodeLength,
                payloadBytes.size - 1 - languageCodeLength,
                Charsets.UTF_8
            )
        } catch (e: Exception) {
            String(payloadBytes, Charsets.UTF_8)
        }
        // Extrait l'ID depuis une URL du type : https://memorytag.app/memory/PARIS_001
        return when {
            payload.contains("/memory/") -> payload.substringAfterLast("/memory/").trim()
            else -> payload.trim()
        }
    }

    /**
     * Fallback : utilise l'UID hardware du tag comme identifiant.
     * Convertit les bytes en string hexadécimale ex: "A1B2C3D4"
     */
    private fun extractFromTagUid(intent: Intent): String? {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return null
        return tag.id.joinToString("") { "%02X".format(it) }
    }
}
