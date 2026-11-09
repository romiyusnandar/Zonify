package com.zonify.feature.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zonify.core.model.Track
import com.zonify.feature.library.scanner.AudioFileScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = AudioFileScanner(application)

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun scanMusic() {
        viewModelScope.launch {
            _isScanning.value = true
            scanner.scanAllAudio().collect { scannedList ->
                _tracks.value = scannedList
            }
            _isScanning.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
