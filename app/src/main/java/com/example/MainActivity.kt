package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.Track
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BajaoViewModel
import kotlin.math.absoluteValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                BajaoPlayerApp()
            }
        }
    }
}

@Composable
fun BajaoPlayerApp() {
    val viewModel: BajaoViewModel = viewModel()
    val context = LocalContext.current

    val uiTracks by viewModel.uiTracks.collectAsStateWithLifecycle()
    val currentTrackIndex by viewModel.currentTrackIndex.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlayingFlow.collectAsStateWithLifecycle()
    val currentTimeSeconds by viewModel.currentTimeSeconds.collectAsStateWithLifecycle()
    val beatPulse by viewModel.beatPulse.collectAsStateWithLifecycle()
    val trackStates by viewModel.allTrackStates.collectAsStateWithLifecycle()

    val currentTrack = remember(uiTracks, currentTrackIndex) {
        val idx = currentTrackIndex.coerceIn(0, uiTracks.size - 1)
        if (uiTracks.isNotEmpty()) uiTracks[idx] else Track(1, "No Track", "Artist", "Album", 60, "#3B2D54")
    }
    
    val isDark = isSystemInDarkTheme()

    // Determine target permission by dynamic API Level
    val targetPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_AUDIO
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    // Permission launcher that auto-triggers memory scan upon success
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.scanDeviceMemoryForMusic()
            }
        }
    )

    // Pre-scan if permission is already granted when app launches
    LaunchedEffect(Unit) {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context, targetPermission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.scanDeviceMemoryForMusic()
        }
    }

    // Drag gesture tracking for track changes (Swipe)
    var swipeOffsetX by remember { mutableStateOf(0f) }
    
    // Setting up the bottom slide drawer
    var isDrawerOpen by remember { mutableStateOf(false) }

    val activeTrackState = trackStates.find { it.trackId == currentTrack.id }
    val isCurrentTrackFavorite = activeTrackState?.isFavorite ?: false

    // Compute dynamic track color
    val trackColor = remember(currentTrack.hexColor) {
        try {
            Color(android.graphics.Color.parseColor(currentTrack.hexColor))
        } catch (e: Exception) {
            Color(0xFF8B5CF6)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (swipeOffsetX > 150f) {
                            viewModel.prevTrack()
                        } else if (swipeOffsetX < -150f) {
                            viewModel.nextTrack()
                        }
                        swipeOffsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        swipeOffsetX += dragAmount
                    }
                )
            }
    ) {
        // Base: Beautiful Custom Ambient Background
        Image(
            painter = painterResource(id = R.drawable.img_app_background),
            contentDescription = "Abstract ambient backdrop",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Overlay: 12% Translucent Ambient Mask (Dark/Light Responsive)
        val overlayColor = if (isDark) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.15f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayColor)
        )

        // Main Player Screen Layout
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Main Content Block: Cover + Title (Zero Clutter)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .padding(horizontal = 24.dp)
                ) {
                    // Title Header "Bajao" (Fades slightly)
                    Text(
                        text = "বাজাও",
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.35f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 4.sp,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    // 1. Cover Art (70% Screen Width) with live pulse animations
                    AnimatedContent(
                        targetState = currentTrack,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                        },
                        label = "cover_crossfade"
                    ) { targetTrack ->
                        val targetTrackColor = remember(targetTrack.hexColor) {
                            try {
                                Color(android.graphics.Color.parseColor(targetTrack.hexColor))
                            } catch (e: Exception) {
                                Color(0xFF8B5CF6)
                            }
                        }

                        // Reactive bloom sizing variables
                        val pulseFactor by animateFloatAsState(
                            targetValue = if (isPlaying) beatPulse else 0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "pulse_spring"
                        )
                        val scale = 1.0f + (pulseFactor * 0.05f)

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(280.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .drawBehind {
                                    // Soft beating organic glow peak
                                    drawCircle(
                                        color = targetTrackColor.copy(alpha = 0.3f * (pulseFactor + 0.1f)),
                                        radius = size.minDimension * 0.55f + (pulseFactor * 40f)
                                    )
                                }
                                .clip(RoundedCornerShape(48.dp))
                                .pointerInput(targetTrack.id) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            viewModel.togglePlayPause()
                                        },
                                        onTap = {
                                            isDrawerOpen = true
                                        }
                                    )
                                }
                                .testTag("cover_art_card")
                        ) {
                            // Cover Image
                            Image(
                                painter = painterResource(id = R.drawable.img_default_cover),
                                contentDescription = "Cover of ${targetTrack.title}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Play overlay indicator if paused
                            if (!isPlaying) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.35f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "PAUSED",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    // 2. Songs title (Zero Clutter, centered, Big, and Lightweight)
                    AnimatedContent(
                        targetState = currentTrack,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                        },
                        label = "title_crossfade"
                    ) { targetTrack ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = targetTrack.title,
                                color = if (isDark) Color.White else Color.Black,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Light,
                                fontFamily = FontFamily.SansSerif,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("track_title_main")
                            )
                            
                            if (isCurrentTrackFavorite) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Favorited",
                                    tint = trackColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom Anchor Interaction Button (⚙️/⋮)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                ) {
                    IconButton(
                        onClick = { isDrawerOpen = true },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        (if (isDark) Color.White else Color.Black).copy(alpha = 0.08f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                            .testTag("settings_drawer_trigger")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Open Settings Panel",
                            tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Half-screen slide-up drawer backdrop (Tapping outside closes drawer)
        if (isDrawerOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { isDrawerOpen = false }
            )
        }

        // Drawer slider sheet representation
        val animDrawerOffset by animateDpAsState(
            targetValue = if (isDrawerOpen) 0.dp else 900.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "drawer_offset"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .offset(y = animDrawerOffset)
                .align(Alignment.BottomCenter)
                .background(
                    color = if (isDark) Color(0xF20B0A11) else Color(0xF2FDFCFF),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
                .pointerInput(Unit) {
                    detectTapGestures { }
                }
                .testTag("settings_drawer_sheet")
        ) {
            SettingsDrawerContent(
                viewModel = viewModel,
                uiTracks = uiTracks,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                currentTimeSeconds = currentTimeSeconds,
                isCurrentTrackFavorite = isCurrentTrackFavorite,
                trackColor = trackColor,
                trackStates = trackStates,
                targetPermission = targetPermission,
                onClose = { isDrawerOpen = false },
                onRequestPermission = { requestPermissionLauncher.launch(targetPermission) }
            )
        }
    }
}

@Composable
fun SettingsDrawerContent(
    viewModel: BajaoViewModel,
    uiTracks: List<Track>,
    currentTrack: Track,
    isPlaying: Boolean,
    currentTimeSeconds: Int,
    isCurrentTrackFavorite: Boolean,
    trackColor: Color,
    trackStates: List<com.example.data.BajaoTrackState>,
    targetPermission: String,
    onClose: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val defaultTextcolor = if (isDark) Color.White else Color.Black
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
    ) {
        // Drag Indicator Bar & Close Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        (if (isDark) Color.White else Color.Black).copy(alpha = 0.2f),
                        RoundedCornerShape(2.dp)
                    )
            )

            IconButton(
                onClick = onClose,
                modifier = Modifier.testTag("close_drawer_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close settings",
                    tint = defaultTextcolor.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tracks info
        Text(
            text = currentTrack.title,
            color = defaultTextcolor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${currentTrack.artist} • ${currentTrack.album}",
            color = defaultTextcolor.copy(alpha = 0.5f),
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Control Progress Sliders
        val totalTrackSeconds = (currentTrack.durationMs / 1000f).coerceAtLeast(1f)
        val currentProgressSecFactor = currentTimeSeconds.toFloat().coerceAtMost(totalTrackSeconds)

        Column {
            Slider(
                value = currentProgressSecFactor,
                onValueChange = { newVal -> 
                    viewModel.seekTo(newVal.toInt()) 
                },
                valueRange = 0f..totalTrackSeconds,
                colors = SliderDefaults.colors(
                    thumbColor = trackColor,
                    activeTrackColor = trackColor,
                    inactiveTrackColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.1f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currentFormatted = String.format("%02d:%02d", currentTimeSeconds / 60, currentTimeSeconds % 60)
                val totalMinutes = (totalTrackSeconds / 60).toInt()
                val totalSecs = (totalTrackSeconds % 60).toInt()
                val totalFormatted = String.format("%02d:%02d", totalMinutes, totalSecs)
                
                Text(
                    text = currentFormatted,
                    color = defaultTextcolor.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
                Text(
                    text = totalFormatted,
                    color = defaultTextcolor.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Floating Action Music Playback Cluster
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track Favorite
            IconButton(
                onClick = { viewModel.toggleFavorite(currentTrack.id) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("drawer_favorite_button")
            ) {
                Icon(
                    imageVector = if (isCurrentTrackFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Toggle Favorite",
                    tint = if (isCurrentTrackFavorite) trackColor else defaultTextcolor.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Sub Prev Button
            IconButton(
                onClick = { viewModel.prevTrack() },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        (if (isDark) Color.White else Color.Black).copy(alpha = 0.05f),
                        CircleShape
                    )
            ) {
                PrevIcon(color = defaultTextcolor)
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Central Play/Pause Button
            IconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier
                    .size(64.dp)
                    .background(trackColor, CircleShape)
                    .testTag("drawer_play_pause")
            ) {
                if (isPlaying) {
                    PauseIcon(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    PlayIcon(color = Color.White, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Next Button
            IconButton(
                onClick = { viewModel.nextTrack() },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        (if (isDark) Color.White else Color.Black).copy(alpha = 0.05f),
                        CircleShape
                    )
            ) {
                NextIcon(color = defaultTextcolor)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Spacer placeholder to balance the favorite button
            Box(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Custom Live BPM Pitch Modifier (Applicable to Synth, shows status on local)
        val activeBpmState = trackStates.find { it.trackId == currentTrack.id }
        val savedBpmValue = activeBpmState?.customBpm ?: 0
        val currentPlayBpm = if (savedBpmValue > 0) savedBpmValue else currentTrack.bpm

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    (if (isDark) Color.White else Color.Black).copy(alpha = 0.03f),
                    RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentTrack.isLocal) "Base Speed (মূল গতি)" else "BPM (সংশ্লেষণ গতি)",
                    color = defaultTextcolor.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$currentPlayBpm BPM",
                    color = trackColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (!currentTrack.isLocal) {
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = currentPlayBpm.toFloat(),
                    onValueChange = { newBpm ->
                        viewModel.updateBpm(currentTrack.id, newBpm.toInt())
                    },
                    valueRange = 50f..150f,
                    colors = SliderDefaults.colors(
                        thumbColor = trackColor,
                        activeTrackColor = trackColor,
                        inactiveTrackColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.05f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scan storage button & Queue list section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "প্লেলিস্ট কিউ (${uiTracks.size})",
                color = defaultTextcolor.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Button(
                onClick = {
                    val isGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, targetPermission
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (isGranted) {
                        viewModel.scanDeviceMemoryForMusic()
                    } else {
                        onRequestPermission()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = trackColor.copy(alpha = 0.12f),
                    contentColor = trackColor
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Scan Memory",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("স্ক্যান", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Queue Scrollable Block
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(uiTracks) { index, track ->
                val isSelected = index == viewModel.currentTrackIndex.value
                val hasSt = trackStates.find { it.trackId == track.id }
                val isFav = hasSt?.isFavorite ?: false
                val playCount = hasSt?.playCount ?: 0

                val bgTrackRowColor = if (isSelected) {
                    trackColor.copy(alpha = 0.08f)
                } else {
                    Color.Transparent
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgTrackRowColor)
                        .clickable { viewModel.selectTrack(index) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Index Indicator
                    Text(
                        text = String.format("%02d", index + 1),
                        color = if (isSelected) trackColor else defaultTextcolor.copy(alpha = 0.3f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(32.dp)
                    )

                    // Track Title & Artist
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            color = if (isSelected) trackColor else defaultTextcolor,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = (if (track.isLocal) "মেমোরি • " else "সংশ্লেষ • ") + "${track.artist}  •  ${track.bpm} BPM" + if (playCount > 0) "  •  $playCount plays" else "",
                            color = defaultTextcolor.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Fav indicator
                    IconButton(
                        onClick = { viewModel.toggleFavorite(track.id) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite track ${track.title}",
                            tint = if (isFav) trackColor else defaultTextcolor.copy(alpha = 0.25f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Credits Footer
        Text(
            text = "বাজাও  •  Zero Clutter • Storage Playback Edition",
            color = defaultTextcolor.copy(alpha = 0.25f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

// Custom Programmatic Drawn Icons
@Composable
fun NextIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier.size(14.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.15f, size.height * 0.2f)
            lineTo(size.width * 0.55f, size.height * 0.5f)
            lineTo(size.width * 0.15f, size.height * 0.8f)
            close()
            
            moveTo(size.width * 0.55f, size.height * 0.2f)
            lineTo(size.width * 0.95f, size.height * 0.5f)
            lineTo(size.width * 0.55f, size.height * 0.8f)
            close()
        }
        drawPath(path, color = color)
    }
}

@Composable
fun PrevIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier.size(14.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.85f, size.height * 0.2f)
            lineTo(size.width * 0.45f, size.height * 0.5f)
            lineTo(size.width * 0.85f, size.height * 0.8f)
            close()
            
            moveTo(size.width * 0.45f, size.height * 0.2f)
            lineTo(size.width * 0.05f, size.height * 0.5f)
            lineTo(size.width * 0.45f, size.height * 0.8f)
            close()
        }
        drawPath(path, color = color)
    }
}

@Composable
fun PauseIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier.size(24.dp)) {
        val barWidth = size.width * 0.20f
        val gap = size.width * 0.20f
        
        drawRect(
            color = color,
            topLeft = Offset(size.width * 0.20f, size.height * 0.2f),
            size = Size(barWidth, size.height * 0.6f)
        )
        
        drawRect(
            color = color,
            topLeft = Offset(size.width * 0.20f + barWidth + gap, size.height * 0.2f),
            size = Size(barWidth, size.height * 0.6f)
        )
    }
}

@Composable
fun PlayIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier.size(24.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.30f, size.height * 0.22f)
            lineTo(size.width * 0.82f, size.height * 0.50f)
            lineTo(size.width * 0.30f, size.height * 0.78f)
            close()
        }
        drawPath(path, color = color)
    }
}
