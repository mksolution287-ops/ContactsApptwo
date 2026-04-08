package com.contactsapptwomktech.utils

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

//    fun videoCall(context: Context, phoneNumber: String) {
//        try {
//            val intent = Intent(Intent.ACTION_DIAL).apply {
//                data = Uri.parse("tel:${Uri.encode(phoneNumber)}")
//            }
//            context.startActivity(intent)
//        } catch (_: Exception) {}
//    }

    fun videoCall(context: Context, phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:${Uri.encode(phoneNumber)}")
                putExtra("android.telecom.extra.START_CALL_WITH_VIDEO_STATE", 3)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // fallback
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${Uri.encode(phoneNumber)}")
            }
            context.startActivity(dialIntent)
        }
    }

    /**
     * Opens the system call log filtered to a specific phone number.
     */
    fun openCallHistory(context: Context, phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data  = Uri.parse("content://call_log/calls")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No call history app found", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens the system contact editor pre-filled to delete the given contact.
     * Android doesn't expose a direct delete intent, so we open the edit screen
     * and the user can delete from there — or use ContentResolver directly if
     * you have WRITE_CONTACTS permission (handled via confirmation dialog in UI).
     */
    fun deleteContact(context: Context, contactId: Long) {
        try {
            val uri    = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId.toString())
            val intent = Intent(Intent.ACTION_DELETE, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to delete contact", Toast.LENGTH_SHORT).show()
        }
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
    /**
     * Deletes a contact DIRECTLY using ContentResolver.
     * This removes the contact completely (no system edit screen).
     *
     * Requires: android.permission.WRITE_CONTACTS
     * Important: Delete at RawContacts level to properly clean up everything.
     */
    fun deleteContactDirect(context: Context, contactId: Long): Boolean {
        val contentResolver = context.contentResolver

        try {
            // Delete all RawContacts linked to this Contact ID
            // This is the recommended & cleanest way
            val selection = "${ContactsContract.RawContacts.CONTACT_ID} = ?"
            val selectionArgs = arrayOf(contactId.toString())

            val rowsDeleted = contentResolver.delete(
                ContactsContract.RawContacts.CONTENT_URI,
                selection,
                selectionArgs
            )

            return if (rowsDeleted > 0) {
                Toast.makeText(context, "Contact deleted successfully", Toast.LENGTH_SHORT).show()
                true
            } else {
                Toast.makeText(context, "Contact not found or already deleted", Toast.LENGTH_SHORT).show()
                false
            }

        } catch (e: SecurityException) {
            Toast.makeText(context, "Permission denied. Cannot delete contact.", Toast.LENGTH_LONG).show()
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to delete contact", Toast.LENGTH_SHORT).show()
            return false
        }
    }
}
