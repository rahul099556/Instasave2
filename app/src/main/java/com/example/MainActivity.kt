package com.example

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ui.theme.InstaOrange
import com.example.ui.theme.InstaPurple
import com.example.ui.theme.InstaRed
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern

data class MediaInfo(
    val url: String,
    val imageUrl: String?,
    val videoUrl: String?,
    val isVideo: Boolean
)

class InstaViewModel : ViewModel() {
    private val client = OkHttpClient()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val mediaInfo: MediaInfo) : UiState()
        data class Error(val message: String) : UiState()
    }

    fun fetchMedia(url: String) {
        if (url.isBlank()) {
            _uiState.value = UiState.Error("Please enter a valid Instagram link.")
            return
        }

        _uiState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Since this is a direct web request, Instagram might block it or redirect to login.
                // We're attempting to scrape the public OpenGraph tags.
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .build()

                val response = client.newCall(request).execute()
                val html = response.body?.string() ?: ""

                val videoPattern = Pattern.compile("<meta property=\"og:video\" content=\"(.*?)\"")
                val imagePattern = Pattern.compile("<meta property=\"og:image\" content=\"(.*?)\"")

                val videoMatcher = videoPattern.matcher(html)
                val imageMatcher = imagePattern.matcher(html)

                var videoUrl: String? = null
                var imageUrl: String? = null

                if (videoMatcher.find()) {
                    videoUrl = videoMatcher.group(1)?.replace("&amp;", "&")
                }
                if (imageMatcher.find()) {
                    imageUrl = imageMatcher.group(1)?.replace("&amp;", "&")
                }

                if (videoUrl != null || imageUrl != null) {
                    val isVideo = videoUrl != null
                    _uiState.value = UiState.Success(
                        MediaInfo(
                            url = url,
                            imageUrl = imageUrl,
                            videoUrl = videoUrl,
                            isVideo = isVideo
                        )
                    )
                } else {
                    _uiState.value = UiState.Error("Could not extract media. Instagram may have blocked the request or the profile is private. A backend API is required for reliable downloading.")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error: ${e.localizedMessage}")
            }
        }
    }

    fun downloadMedia(context: Context, mediaInfo: MediaInfo) {
        try {
            val downloadUrl = mediaInfo.videoUrl ?: mediaInfo.imageUrl ?: return
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("InstaSave Download")
                .setDescription("Downloading media from Instagram")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "InstaSave_${System.currentTimeMillis()}.${if (mediaInfo.isVideo) "mp4" else "jpg"}"
                )

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Download failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        var initialLinkText = ""
        if (Intent.ACTION_SEND == intent.action && "text/plain" == intent.type) {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                initialLinkText = it
            }
        }

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    InstaSaveScreen(
                        modifier = Modifier.padding(innerPadding),
                        initialLinkText = initialLinkText
                    )
                }
            }
        }
    }
}

@Composable
fun InstaSaveScreen(
    modifier: Modifier = Modifier,
    initialLinkText: String = "",
    viewModel: InstaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var linkText by remember { mutableStateOf(initialLinkText) }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(initialLinkText) {
        if (initialLinkText.isNotBlank()) {
            viewModel.fetchMedia(initialLinkText)
        }
    }

    val instaGradient = Brush.horizontalGradient(
        colors = listOf(InstaPurple, InstaRed, InstaOrange)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "InstaSave",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                brush = instaGradient
            )
        )
        
        Text(
            text = "Instagram Reels & Stories Downloader",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = linkText,
            onValueChange = { linkText = it },
            placeholder = { Text("Paste Instagram Reel/Story/Post link here") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Link, contentDescription = "Link")
            },
            trailingIcon = {
                TextButton(
                    onClick = {
                        val clip = clipboardManager.getText()
                        if (!clip.isNullOrEmpty()) {
                            linkText = clip.text
                        }
                    },
                    modifier = Modifier.padding(end = 4.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = InstaRed)
                ) {
                    Icon(
                        Icons.Default.ContentPaste, 
                        contentDescription = "Paste", 
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Paste", fontWeight = FontWeight.Bold)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = InstaRed,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.fetchMedia(linkText) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(instaGradient)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState is InstaViewModel.UiState.Loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp
                    )
                } else {
                    Text("Get Preview", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        when (val state = uiState) {
            is InstaViewModel.UiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = InstaRed)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Processing link...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
            is InstaViewModel.UiState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            is InstaViewModel.UiState.Success -> {
                PreviewSection(
                    mediaInfo = state.mediaInfo,
                    onDownloadClick = { viewModel.downloadMedia(context, state.mediaInfo) }
                )
            }
            else -> {}
        }

        Spacer(modifier = Modifier.weight(1f))
        
        FAQSection()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "This tool is for downloading your own content or content you have permission to use. Please respect copyright and Instagram's terms of service.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        )
    }
}

@Composable
fun PreviewSection(mediaInfo: MediaInfo, onDownloadClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 5f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                if (mediaInfo.imageUrl != null) {
                    AsyncImage(
                        model = mediaInfo.imageUrl,
                        contentDescription = "Media Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                if (mediaInfo.isVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info, // Placeholder for Play icon
                            contentDescription = "Video",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            FilledTonalButton(
                onClick = onDownloadClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = InstaPurple.copy(alpha = 0.1f),
                    contentColor = InstaPurple
                )
            ) {
                Icon(Icons.Default.Download, contentDescription = "Download")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save to Device", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FAQSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "How to copy a link?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        val steps = listOf(
            "1. Open the Instagram app.",
            "2. Find the Reel, Story, or Post you want to download.",
            "3. Tap the Share icon (paper airplane).",
            "4. Tap 'Copy link'.",
            "5. Return here and paste the link!"
        )
        
        steps.forEach { step ->
            Text(
                text = step,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

