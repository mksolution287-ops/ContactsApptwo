package com.contactsapp.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast

object IntentUtils {

    fun dialNumber(context: Context, phoneNumber: String, errorMsg: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${Uri.encode(phoneNumber)}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    fun sendSms(context: Context, phoneNumber: String, errorMsg: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(phoneNumber)}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    fun sendEmail(context: Context, email: String, errorMsg: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${Uri.encode(email)}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    fun addContact(context: Context) {
        val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
        }
        context.startActivity(intent)
    }

    fun editContact(context: Context, contactId: Long) {
        val contactUri = Uri.withAppendedPath(
            ContactsContract.Contacts.CONTENT_URI,
            contactId.toString()
        )
        val intent = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(contactUri, ContactsContract.Contacts.CONTENT_ITEM_TYPE)
            putExtra("finishActivityOnSaveCompleted", true)
        }
        context.startActivity(intent)
    }

    fun viewContact(context: Context, contactId: Long) {
        val contactUri = Uri.withAppendedPath(
            ContactsContract.Contacts.CONTENT_URI,
            contactId.toString()
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = contactUri
        }
        context.startActivity(intent)
    }

    fun shareContact(context: Context, contactId: Long) {
        val contactUri = Uri.withAppendedPath(
            ContactsContract.Contacts.CONTENT_URI,
            contactId.toString()
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = ContactsContract.Contacts.CONTENT_VCARD_TYPE
            data = contactUri
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    fun videoCall(context: Context, phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${Uri.encode(phoneNumber)}")
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun makeCall(context: Context, phoneNumber: String, errorMsg: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:${Uri.encode(phoneNumber)}")
            }
            context.startActivity(intent)
        } catch (e: SecurityException) {
            Toast.makeText(context, "Permission not granted", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    fun openDialer(context: Context) {
        val intent = Intent(Intent.ACTION_DIAL)
        context.startActivity(intent)
    }
}
