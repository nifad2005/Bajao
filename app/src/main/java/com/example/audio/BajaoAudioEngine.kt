package com.example.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.exp
import java.util.Random

data class Track(
    val id: Int,
    val title: String,
    val artist: String,
    val album: String,
    val bpm: Int,
    val hexColor: String,
    val scale: List<Double> = emptyList(), // Pentatonic notes for synthesis
    val contentUri: String? = null, // URI of stored local MP3 file
    val durationMs: Long = 180000L,
    val isLocal: Boolean = false
)

class BajaoAudioEngine {
    
    // Default preset synth tracks
    private val presetTracks = listOf(
        Track(
            id = 1,
            title = "Twilight Sleep",
            artist = "Synthesized Amber Drone",
            album = "Cosmic Slate Vol. 1",
            bpm = 60,
            hexColor = "#3B2D54", // Twilight purple
            scale = listOf(220.0, 261.63, 329.63, 392.00, 440.0, 523.25) // Amin7 scale (A3, C4, E4, G4, A4, C5)
        ),
        Track(
            id = 2,
            title = "Rain Forest Echo",
            artist = "Wooden Harp & Drops",
            album = "Nature Chords",
            bpm = 70,
            hexColor = "#1D544C", // Forest teal
            scale = listOf(261.63, 293.66, 329.63, 392.00, 440.0, 523.25) // C Maj pentatonic
        ),
        Track(
            id = 3,
            title = "Midnight Oasis",
            artist = "Pulsing Grid wave",
            album = "Neon Drift",
            bpm = 85,
            hexColor = "#1E3B5C", // Deep Neon Blue
            scale = listOf(164.81, 196.00, 220.0, 246.94, 293.66, 329.63) // Emin7 scale
        ),
        Track(
            id = 4,
            title = "Morning Light",
            artist = "Solfeggio Shimmer Piano",
            album = "Solfeggio Resonances",
            bpm = 92,
            hexColor = "#543C1D", // Ochre Sun
            scale = listOf(261.63, 329.63, 392.00, 523.25, 587.33, 659.25) // C Maj scale
        )
    )

    // Master memory queue (contains presets + scanned songs)
    private val _tracksList = ArrayList<Track>(presetTracks)
    val tracks: List<Track> get() = _tracksList

    private val sampleRate = 22050
    
    // Synth engine assets
    private var audioTrack: AudioTrack? = null
    private var playbackThread: Thread? = null
    @Volatile private var isPlayingSynth = false

    // Local media player engine assets
    private var mediaPlayer: MediaPlayer? = null
    private var localProgressJob: Job? = null
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _currentTrackIndex = MutableStateFlow(0)
    val currentTrackIndex: StateFlow<Int> = _currentTrackIndex

    private val _isPlayingFlow = MutableStateFlow(false)
    val isPlayingFlow: StateFlow<Boolean> = _isPlayingFlow

    private val _currentTimeSeconds = MutableStateFlow(0)
    val currentTimeSeconds: StateFlow<Int> = _currentTimeSeconds

    private val _beatPulse = MutableStateFlow(0f)
    val beatPulse: StateFlow<Float> = _beatPulse

    fun updateScannedLocalTracks(scanned: List<Track>) {
        _tracksList.clear()
        _tracksList.addAll(presetTracks)
        _tracksList.addAll(scanned)
    }

    fun getTrack(): Track {
        val index = _currentTrackIndex.value.coerceIn(0, _tracksList.size - 1)
        return _tracksList[index]
    }

    fun play(context: Context) {
        val track = getTrack()
        
        // Stop any currently playing audio stream first
        stopAllPlayback()

        _isPlayingFlow.value = true

        if (track.isLocal) {
            playLocal(context, track)
        } else {
            playSynth(track)
        }
    }

    private fun playLocal(context: Context, track: Track) {
        try {
            val fileUri = Uri.parse(track.contentUri)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, fileUri)
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                setOnPreparedListener { mp ->
                    mp.start()
                    // Seek to stored elapsed time if needed
                    val currentSec = _currentTimeSeconds.value
                    if (currentSec > 0 && currentSec * 1000 < track.durationMs) {
                        mp.seekTo(currentSec * 1000)
                    }
                }
                setOnCompletionListener {
                    nextTrack(context)
                }
                prepareAsync()
            }

            // Start coroutine to track local file progress & dispatch pulse triggers
            localProgressJob = engineScope.launch {
                var localPulsePhase = 0f
                val bpmFactor = track.bpm
                val sleepInterval = 50L
                while (isActive) {
                    val mp = mediaPlayer
                    if (mp != null && mp.isPlaying) {
                        val currentMs = mp.currentPosition
                        _currentTimeSeconds.value = currentMs / 1000

                        // Generate a beautiful, hypnotic virtual pulse synced to local BPM for visualization
                        localPulsePhase += (bpmFactor / 60f) * (sleepInterval / 1000f)
                        val virtualBpmSin = sin(localPulsePhase * 2f * PI.toFloat()).coerceAtLeast(0f)
                        _beatPulse.value = exp(-2.0 * (1.0 - virtualBpmSin)).toFloat()
                    }
                    delay(sleepInterval)
                }
            }
        } catch (e: Exception) {
            Log.e("BajaoAudioEngine", "Error playing local media file", e)
            _isPlayingFlow.value = false
        }
    }

    private fun playSynth(track: Track) {
        isPlayingSynth = true
        
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate, 
            AudioFormat.CHANNEL_OUT_MONO, 
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = (minBufferSize * 2).coerceAtLeast(1024)

        try {
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e("BajaoAudioEngine", "Error initializing AudioTrack", e)
            isPlayingSynth = false
            _isPlayingFlow.value = false
            return
        }

        playbackThread = Thread {
            renderSynthLoop(track, bufferSize)
        }.apply {
            name = "BajaoRenderThread"
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun pause() {
        _isPlayingFlow.value = false
        stopAllPlayback()
    }

    private fun stopAllPlayback() {
        // 1. Terminate local progress tracker
        localProgressJob?.cancel()
        localProgressJob = null

        // 2. Tear down MediaPlayer
        try {
            mediaPlayer?.run {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("BajaoAudioEngine", "Error destroying local player", e)
        }

        // 3. Terminate Synth Stream
        isPlayingSynth = false
        try {
            audioTrack?.run {
                stop()
                release()
            }
            audioTrack = null
        } catch (e: Exception) {
            Log.e("BajaoAudioEngine", "Error destroying synthesis engine", e)
        }

        playbackThread?.join(500)
        playbackThread = null
    }

    fun togglePlayPause(context: Context) {
        if (_isPlayingFlow.value) {
            pause()
        } else {
            play(context)
        }
    }

    fun nextTrack(context: Context) {
        val wasPlaying = _isPlayingFlow.value
        pause()
        
        val size = _tracksList.size
        val nextIndex = (_currentTrackIndex.value + 1) % size
        _currentTrackIndex.value = nextIndex
        _currentTimeSeconds.value = 0
        _beatPulse.value = 0f
        
        if (wasPlaying) {
            play(context)
        }
    }

    fun prevTrack(context: Context) {
        val wasPlaying = _isPlayingFlow.value
        pause()
        
        val size = _tracksList.size
        val prevIndex = (_currentTrackIndex.value - 1 + size) % size
        _currentTrackIndex.value = prevIndex
        _currentTimeSeconds.value = 0
        _beatPulse.value = 0f
        
        if (wasPlaying) {
            play(context)
        }
    }

    fun selectTrack(context: Context, index: Int) {
        if (index in _tracksList.indices) {
            val wasPlaying = _isPlayingFlow.value
            pause()
            _currentTrackIndex.value = index
            _currentTimeSeconds.value = 0
            _beatPulse.value = 0f
            if (wasPlaying) {
                play(context)
            }
        }
    }

    fun seekTo(context: Context, seconds: Int) {
        _currentTimeSeconds.value = seconds
        val mp = mediaPlayer
        if (mp != null && getTrack().isLocal) {
            try {
                mp.seekTo(seconds * 1000)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun renderSynthLoop(track: Track, bufferSize: Int) {
        val buffer = ShortArray(512)
        var sampleIndex = (_currentTimeSeconds.value * sampleRate).toLong()
        val random = Random()

        // Synth sound parameters
        var currentNoteFreq = 0.0
        var noteTriggerSample = -99999L
        var lfoPhase = 0.0

        while (isPlayingSynth) {
            val bpm = track.bpm
            val samplesPerBeat = (sampleRate * 60) / bpm
            val scale = track.scale

            for (i in buffer.indices) {
                val currentSampleIndex = sampleIndex + i

                // 1. Synchronized visual rhythmic variables
                val beatProgress = (currentSampleIndex % samplesPerBeat).toDouble() / samplesPerBeat
                val pulse = exp(-5.0 * beatProgress).toFloat()

                // Trigger fresh notes on the scale array periodically
                if (currentSampleIndex % samplesPerBeat == 0L || currentSampleIndex - noteTriggerSample > samplesPerBeat * 2) {
                    noteTriggerSample = currentSampleIndex
                    if (scale.isNotEmpty()) {
                        currentNoteFreq = scale[random.nextInt(scale.size)]
                    }
                }

                if (currentSampleIndex % 512 == 0L) {
                    _beatPulse.value = pulse
                    _currentTimeSeconds.value = (currentSampleIndex / sampleRate).toInt()
                }

                // Low LFO panning sweep
                lfoPhase += 2.0 * PI / (sampleRate * 8.0)

                // 2. Procedural Soft Pad Synthesis (warm overlay frequencies)
                val chordIndex = ((currentSampleIndex / (samplesPerBeat * 8)) % 4).toInt()
                val chordRoot = if (scale.isNotEmpty()) {
                    when (chordIndex) {
                        0 -> scale[0]
                        1 -> scale[2].coerceAtLeast(20.0)
                        2 -> scale[1].coerceAtLeast(20.0)
                        else -> scale[3].coerceAtLeast(20.0)
                    }
                } else 220.0
                
                val padWave1 = sin(currentSampleIndex * 2.0 * PI * (chordRoot * 0.5) / sampleRate) 
                val padWave2 = sin(currentSampleIndex * 2.0 * PI * (chordRoot * 0.75) / sampleRate)
                val padWave3 = sin(currentSampleIndex * 2.0 * PI * (chordRoot * 1.5) / sampleRate)
                val padMix = (padWave1 + padWave2 * 0.5 + padWave3 * 0.25) * 0.25

                // 3. Harp & plucked bell notes with exponential timing decay envelopes
                val timeSincePluck = currentSampleIndex - noteTriggerSample
                val pluckDuration = samplesPerBeat * 2.0
                val pluckDecay = exp(-4.0 * (timeSincePluck.toDouble() / pluckDuration))
                
                val pluckFreq = currentNoteFreq
                val pluckWave = sin(timeSincePluck * 2.0 * PI * pluckFreq / sampleRate) * 
                                (1.0 + 0.1 * sin(timeSincePluck * 2.0 * PI * 12.0 / sampleRate))
                val pluckMix = if (scale.isNotEmpty()) pluckWave * pluckDecay * 0.35 else 0.0

                // 4. Ambient environment whisper (soft filtered noise floor)
                val noise = (random.nextFloat() * 2f - 1f) * 0.04f

                // 5. Analog clock heartbeat tick
                var beatTick = 0.0
                val tickDuration = 120
                val sampleSinceBeat = currentSampleIndex % samplesPerBeat
                if (sampleSinceBeat < tickDuration) {
                    val tickPhase = sampleSinceBeat * 2.0 * PI * 600.0 / sampleRate
                    beatTick = sin(tickPhase) * 0.1 * (1.0 - (sampleSinceBeat.toDouble() / tickDuration))
                }

                // 6. Output PCM mixing buffer
                val mixed = (padMix + pluckMix + noise + beatTick) * 32767.0
                buffer[i] = mixed.toInt().coerceIn(-32768, 32767).toShort()
            }

            // Write generated audio frame directly to AudioTrack OpenSL audio pipeline
            val written = audioTrack?.write(buffer, 0, buffer.size) ?: 0
            if (written <= 0) {
                break
            }
            sampleIndex += buffer.size
        }
    }
}
