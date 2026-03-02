package com.example.projekat.ui.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projekat.data.model.Note
import com.example.projekat.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _currentFilter = MutableStateFlow(NoteFilter.ALL)
    val currentFilter: StateFlow<NoteFilter> = _currentFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val notes: StateFlow<List<Note>> = combine(
        _currentFilter,
        _searchQuery
    ) { filter, query ->
        Pair(filter, query)
    }.flatMapLatest { (filter, query) ->
        when (filter) {
            NoteFilter.ALL -> {
                if (query.isBlank()) noteRepository.getAllNotes()
                else noteRepository.searchNotes(query)
            }
            NoteFilter.BOOKMARKED -> noteRepository.getBookmarkedNotes()
            NoteFilter.DELETED -> noteRepository.getDeletedNotes()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Clean up notes deleted more than 7 days ago
        viewModelScope.launch {
            noteRepository.cleanupOldDeletedNotes()
        }
    }

    fun setFilter(filter: NoteFilter) {
        _currentFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleBookmark(note: Note) {
        viewModelScope.launch {
            noteRepository.toggleBookmark(note)
        }
    }

    fun softDeleteNote(note: Note) {
        viewModelScope.launch {
            noteRepository.softDeleteNote(note)
        }
    }

    fun restoreNote(note: Note) {
        viewModelScope.launch {
            noteRepository.restoreNote(note)
        }
    }

    fun permanentlyDeleteNote(note: Note) {
        viewModelScope.launch {
            noteRepository.permanentlyDeleteNote(note)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            noteRepository.emptyTrash()
        }
    }
}
