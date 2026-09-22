package com.propentatech.waka.ui.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.propentatech.waka.data.local.entity.Note
import com.propentatech.waka.data.repository.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class NotesViewModel(noteRepository: NoteRepository) : ViewModel() {
    val notes: StateFlow<List<Note>> = noteRepository.observeAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
