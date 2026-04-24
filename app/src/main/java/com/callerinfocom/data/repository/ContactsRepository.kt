package com.callerinfocom.data.repository

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import com.callerinfocom.data.model.Contact
import com.callerinfocom.data.model.EmailAddress
import com.callerinfocom.data.model.EmailType
import com.callerinfocom.data.model.PhoneNumber
import com.callerinfocom.data.model.PhoneType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsRepository(private val context: Context) {

    suspend fun fetchContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val contacts = mutableMapOf<Long, ContactBuilder>()
        val resolver: ContentResolver = context.contentResolver

        // Fetch base contact info
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Contacts.STARRED,
            ContactsContract.Contacts.HAS_PHONE_NUMBER
        )

        val cursor: Cursor? = resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC"
        )

        cursor?.use {
            val idIdx      = it.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameIdx    = it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val photoIdx   = it.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)
            val starredIdx = it.getColumnIndexOrThrow(ContactsContract.Contacts.STARRED)

            while (it.moveToNext()) {
                val id          = it.getLong(idIdx)
                val name        = it.getString(nameIdx) ?: ""
                val photoUriStr = it.getString(photoIdx)
                val starred     = it.getInt(starredIdx) == 1

                contacts[id] = ContactBuilder(
                    id         = id,
                    name       = name,
                    firstName  = "",
                    lastName   = "",
                    photoUri   = photoUriStr?.let { s -> Uri.parse(s) },
                    isFavorite = starred
                )
            }
        }

        // Fetch phone numbers
        val phoneCursor: Cursor? = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL
            ),
            null, null, null
        )

        phoneCursor?.use {
            val contactIdIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val numberIdx    = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val typeIdx      = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)
            val labelIdx     = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL)

            while (it.moveToNext()) {
                val contactId = it.getLong(contactIdIdx)
                val number    = it.getString(numberIdx) ?: continue
                val type = when (it.getInt(typeIdx)) {
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> PhoneType.MOBILE
                    ContactsContract.CommonDataKinds.Phone.TYPE_HOME   -> PhoneType.HOME
                    ContactsContract.CommonDataKinds.Phone.TYPE_WORK   -> PhoneType.WORK
                    else                                                -> PhoneType.OTHER
                }
                val label = it.getString(labelIdx)
                contacts[contactId]?.phones?.add(PhoneNumber(number, type, label))
            }
        }

        // Fetch emails
        val emailCursor: Cursor? = resolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.TYPE,
                ContactsContract.CommonDataKinds.Email.LABEL
            ),
            null, null, null
        )

        emailCursor?.use {
            val contactIdIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
            val addressIdx   = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
            val typeIdx      = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.TYPE)
            val labelIdx     = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.LABEL)

            while (it.moveToNext()) {
                val contactId = it.getLong(contactIdIdx)
                val address   = it.getString(addressIdx) ?: continue
                val type = when (it.getInt(typeIdx)) {
                    ContactsContract.CommonDataKinds.Email.TYPE_HOME -> EmailType.HOME
                    ContactsContract.CommonDataKinds.Email.TYPE_WORK -> EmailType.WORK
                    else                                             -> EmailType.OTHER
                }
                val label = it.getString(labelIdx)
                contacts[contactId]?.emails?.add(EmailAddress(address, type, label))
            }
        }

        // Fetch company / notes
        val dataCursor: Cursor? = resolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.Organization.COMPANY,
                ContactsContract.CommonDataKinds.Note.NOTE
            ),
            "${ContactsContract.Data.MIMETYPE} IN (?, ?)",
            arrayOf(
                ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE
            ),
            null
        )

        dataCursor?.use {
            val contactIdIdx = it.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
            val mimeIdx      = it.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)

            while (it.moveToNext()) {
                val contactId = it.getLong(contactIdIdx)
                val mime      = it.getString(mimeIdx)
                when (mime) {
                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
                        val companyIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY)
                        if (companyIdx >= 0) contacts[contactId]?.company = it.getString(companyIdx)
                    }
                    ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> {
                        val noteIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE)
                        if (noteIdx >= 0) contacts[contactId]?.notes = it.getString(noteIdx)
                    }
                }
            }
        }

        contacts.values.map { it.build() }
    }

    /**
     * Persists the starred/favourite flag to the Android Contacts provider.
     * Requires WRITE_CONTACTS permission.
     */
    suspend fun setFavorite(contactId: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(ContactsContract.Contacts.STARRED, if (isFavorite) 1 else 0)
        }
        val uri = Uri.withAppendedPath(
            ContactsContract.Contacts.CONTENT_URI,
            contactId.toString()
        )
        context.contentResolver.update(uri, values, null, null)
    }

    private data class ContactBuilder(
        val id        : Long,
        val name      : String,
        val firstName : String,
        val lastName  : String,
        val photoUri  : Uri?,
        val isFavorite: Boolean,
        val phones    : MutableList<PhoneNumber>  = mutableListOf(),
        val emails    : MutableList<EmailAddress> = mutableListOf(),
        var company   : String? = null,
        var notes     : String? = null
    ) {
        fun build() = Contact(
            id           = id,
            name         = name,
            firstName    = firstName,
            lastName     = lastName,
            phoneNumbers = phones.toList(),
            emails       = emails.toList(),
            photoUri     = photoUri,
            isFavorite   = isFavorite,
            company      = company,
            notes        = notes
        )
    }
}