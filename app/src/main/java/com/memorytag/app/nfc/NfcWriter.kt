package com.memorytag.app.nfc

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef

object NfcWriter {

    fun writeTextToTag(tag: Tag, memoryId: String): Boolean {
        return try {
            val ndef = Ndef.get(tag) ?: return false
            ndef.connect()

            val message = NdefMessage(
                arrayOf(NdefRecord.createUri(Uri.parse("https://memorytag.app/memory/$memoryId")))
            )


            if (!ndef.isWritable) {
                ndef.close()
                return false
            }

            if (ndef.maxSize < message.toByteArray().size) {
                ndef.close()
                return false
            }

            ndef.writeNdefMessage(message)
            ndef.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}