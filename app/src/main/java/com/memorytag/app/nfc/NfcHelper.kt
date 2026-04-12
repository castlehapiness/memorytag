package com.memorytag.app.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.util.Log

class NfcHelper(private val activity: Activity) {

    companion object { private const val TAG = "NfcHelper" }

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    val isNfcAvailable: Boolean get() = nfcAdapter != null
    val isNfcEnabled: Boolean get() = nfcAdapter?.isEnabled == true

    fun enableForegroundDispatch() {
        val intent = Intent(activity, activity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(activity, 0, intent, flags)
        val filters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        )
        nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, filters, null)
    }

    fun disableForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    /**
     * Essaie TOUJOURS le NDEF en premier, quelle que soit l'action NFC.
     * Fallback sur l'UID hardware seulement si aucun NDEF lisible.
     */
    fun extractMemoryId(intent: Intent): String? {
        Log.d(TAG, "extractMemoryId action=" + intent.action)
        val fromNdef = tryExtractFromNdef(intent)
        if (fromNdef != null) {
            Log.d(TAG, "NDEF -> " + fromNdef)
            return fromNdef
        }
        if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED) {
            val uid = extractTagUid(intent)
            Log.w(TAG, "Pas de NDEF, UID: " + uid)
            return uid
        }
        return null
    }

    private fun tryExtractFromNdef(intent: Intent): String? {
        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            ?: return null
        val msg = rawMessages[0] as? NdefMessage ?: return null
        val rec = msg.records.firstOrNull() ?: return null
        Log.d(TAG, "NDEF tnf=" + rec.tnf + " type=" + String(rec.type))
        return when (rec.tnf) {
            NdefRecord.TNF_WELL_KNOWN -> when {
                rec.type.contentEquals(NdefRecord.RTD_URI)  -> extractFromUriRecord(rec)
                rec.type.contentEquals(NdefRecord.RTD_TEXT) -> extractFromTextRecord(rec)
                else -> null
            }
            NdefRecord.TNF_ABSOLUTE_URI ->
                extractIdFromUrl(String(rec.payload, Charsets.UTF_8))
            else -> null
        }
    }

    /**
     * URI Record NDEF — le 1er byte est un préfixe de schéma :
     * 0x04 = "https://"  (utilisé par NFC Tools)
     * 0x03 = "http://"
     * 0x00 = pas de préfixe
     */
    private fun extractFromUriRecord(record: NdefRecord): String? {
        val payload = record.payload
        if (payload.isEmpty()) return null
        val prefixByte = payload[0].toInt() and 0xFF
        val prefixes = mapOf(
            0x00 to "", 0x01 to "http://www.", 0x02 to "https://www.",
            0x03 to "http://", 0x04 to "https://",
            0x05 to "tel:", 0x06 to "mailto:"
        )
        val fullUrl = (prefixes[prefixByte] ?: "") +
                String(payload, 1, payload.size - 1, Charsets.UTF_8)
        Log.d(TAG, "URI Record complet : " + fullUrl)
        return extractIdFromUrl(fullUrl)
    }

    /**
     * Text Record NDEF — le 1er byte indique la longueur du code langue.
     */
    private fun extractFromTextRecord(record: NdefRecord): String? {
        val payload = record.payload
        if (payload.isEmpty()) return null
        val langLen = payload[0].toInt() and 0x3F
        val text = String(payload, 1 + langLen, payload.size - 1 - langLen, Charsets.UTF_8)
        Log.d(TAG, "Text Record : " + text)
        return extractIdFromUrl(text)
    }

    /**
     * Extrait le memoryId depuis une URL ou un texte.
     * Supporte :
     *   https://memorytag.app/memory/UUID  -> "UUID"
     *   UUID direct (avec tirets, > 30 chars)  -> UUID
     */
    private fun extractIdFromUrl(url: String): String? {
        val t = url.trim()
        return when {
            t.contains("/memory/") -> t.substringAfterLast("/memory/").trim()
            t.contains("-") && t.length > 30 -> t
            else -> null
        }
    }

    private fun extractTagUid(intent: Intent): String? {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return null
        return tag.id.joinToString("") { "%02X".format(it) }
    }
}