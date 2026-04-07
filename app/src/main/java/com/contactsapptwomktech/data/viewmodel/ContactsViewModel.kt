package com.contactsapptwomktech.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.contactsapptwomktech.data.model.CallLogEntry
import com.contactsapptwomktech.data.model.Contact
import com.contactsapptwomktech.data.repository.CallLogRepository
import com.contactsapptwomktech.data.repository.ContactsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val contactsRepo = ContactsRepository(application)
    private val callLogRepo = CallLogRepository(application)

    // --- Contacts ---
    private val _contactsState = MutableStateFlow<UiState<List<Contact>>>(UiState.Loading)
    val contactsState: StateFlow<UiState<List<Contact>>> = _contactsState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredContacts: StateFlow<List<Contact>> = combine(
        _contactsState, _searchQuery
    ) { state, query ->
        if (state !is UiState.Success) return@combine emptyList()
        if (query.isBlank()) state.data
        else state.data.filter { contact ->
            contact.name.contains(query, ignoreCase = true) ||
                    contact.phoneNumbers.any { it.number.contains(query) } ||
                    contact.emails.any { it.address.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<Contact>> = _contactsState.combine(_searchQuery) { state, _ ->
        if (state !is UiState.Success) emptyList()
        else state.data.filter { it.isFavorite }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Call Log ---
    private val _callLogState = MutableStateFlow<UiState<List<CallLogEntry>>>(UiState.Loading)
    val callLogState: StateFlow<UiState<List<CallLogEntry>>> = _callLogState.asStateFlow()

    // --- Selected Contact ---
    private val _selectedContact = MutableStateFlow<Contact?>(null)
    val selectedContact: StateFlow<Contact?> = _selectedContact.asStateFlow()

    init {
        loadContacts()
        loadCallLog()
    }

    fun loadContacts() {
        viewModelScope.launch {
            _contactsState.value = UiState.Loading
            try {
                val contacts = contactsRepo.fetchContacts()
                _contactsState.value = UiState.Success(contacts)
            } catch (e: Exception) {
                _contactsState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadCallLog() {
        viewModelScope.launch {
            _callLogState.value = UiState.Loading
            try {
                val logs = callLogRepo.fetchCallLogs()
                _callLogState.value = UiState.Success(logs)
            } catch (e: Exception) {
                _callLogState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectContact(contactId: Long) {
        val state = _contactsState.value
        if (state is UiState.Success) {
            _selectedContact.value = state.data.find { it.id == contactId }
        }
    }

    fun clearSelectedContact() {
        _selectedContact.value = null
    }

    fun getContactById(id: Long): Contact? {
        val state = _contactsState.value
        return if (state is UiState.Success) state.data.find { it.id == id } else null
    }
}
