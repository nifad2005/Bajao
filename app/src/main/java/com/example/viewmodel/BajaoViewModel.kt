package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.BajaoAudioEngine
import com.example.audio.Track
import com.example.data.BajaoDatabase
import com.example.data.BajaoRepository
import com.example.data.BajaoTrackState
import com.example.utils.MediaStoreScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BajaoViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        val audioEngine = BajaoAudioEngine()
    }

    private val database = BajaoDatabase.getDatabase(application)
    private val repository = BajaoRepository(database.trackDao())

    val allTrackStates: StateFlow<List<BajaoTrackState>> = repository.allTrackStates
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentTrackIndex = audioEngine.currentTrackIndex
    val isPlayingFlow = audioEngine.isPlayingFlow
    val currentTimeSeconds = audioEngine.currentTimeSeconds
    val beatPulse = audioEngine.beatPulse

    // Expose dynamic track list (preset synthesizers + scanned mobile songs)
    private val _uiTracks = MutableStateFlow<List<Track>>(audioEngine.tracks)
    val uiTracks: StateFlow<List<Track>> = _uiTracks

    fun scanDeviceMemoryForMusic() {
        viewModelScope.launch {
            val scannedSongs = withContext(Dispatchers.IO) {
                MediaStoreScanner.scanLocalAudioFiles(getApplication())
            }
            audioEngine.updateScannedLocalTracks(scannedSongs)
            // Trigger Compose recomposition of the shared tracks list
            _uiTracks.value = ArrayList(audioEngine.tracks)
        }
    }

    fun getCurrentTrack(): Track {
        val currentList = _uiTracks.value
        val index = currentTrackIndex.value.coerceIn(0, currentList.size - 1)
        return currentList[index]
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            audioEngine.togglePlayPause(getApplication())
            if (audioEngine.isPlayingFlow.value) {
                val currentTrackId = getCurrentTrack().id
                repository.registerPlay(currentTrackId)
            }
        }
    }

    fun nextTrack() {
        viewModelScope.launch {
            audioEngine.nextTrack(getApplication())
            if (audioEngine.isPlayingFlow.value) {
                val currentTrackId = getCurrentTrack().id
                repository.registerPlay(currentTrackId)
            }
        }
    }

    fun prevTrack() {
        viewModelScope.launch {
            audioEngine.prevTrack(getApplication())
            if (audioEngine.isPlayingFlow.value) {
                val currentTrackId = getCurrentTrack().id
                repository.registerPlay(currentTrackId)
            }
        }
    }

    fun selectTrack(index: Int) {
        viewModelScope.launch {
            audioEngine.selectTrack(getApplication(), index)
            if (audioEngine.isPlayingFlow.value) {
                val currentTrackId = getCurrentTrack().id
                repository.registerPlay(currentTrackId)
            }
        }
    }

    fun seekTo(seconds: Int) {
        audioEngine.seekTo(getApplication(), seconds)
    }

    fun toggleFavorite(trackId: Int) {
        viewModelScope.launch {
            repository.toggleFavorite(trackId)
        }
    }

    fun updateBpm(trackId: Int, bpm: Int) {
        viewModelScope.launch {
            repository.updateBpm(trackId, bpm)
        }
    }
}
