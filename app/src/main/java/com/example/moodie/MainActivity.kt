package com.example.moodie

import android.Manifest
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.moodie.ui.theme.MoodieTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class SentimentRequest(val text: String)

data class SentimentResponse(val result: Result)

data class Result(val label: String, val score: Double)

interface SentimentService {
    @POST("predict")
    suspend fun predictSentiment(@Body request: SentimentRequest): SentimentResponse
}

class MainActivity : ComponentActivity() {
    val voiceToTextParser by lazy { VoiceToTextParser(application) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MoodieTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var canRecord by remember {
                        mutableStateOf(false)
                    }
                    val recordAudioLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(),
                        onResult = { isGranted -> canRecord = isGranted }
                    )

                    LaunchedEffect(key1 = recordAudioLauncher) {
                        recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                    Moodie(voiceToTextParser)
                }
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class, ExperimentalAnimationApi::class)
@Composable
fun Moodie(
    voiceToTextParser: VoiceToTextParser,
    modifier: Modifier = Modifier
) {
    var userInput by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.0.144:5000/").addConverterFactory(GsonConverterFactory.create())
        .build()
    val sentimentService = retrofit.create(SentimentService::class.java)
    val state by voiceToTextParser.state.collectAsState()
    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = {
            isLoading = true

            val request = SentimentRequest(state.spokenText)
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val response = sentimentService.predictSentiment(request)
                    result = response.result.label
                    val score = response.result.score

                    // Log the response
                    Log.d("ServerResponse", "Label: $result, Score: $score")
                } catch (e: Exception) {
                    Log.e("ServerRequest", "Request failed: ${e.message}", e)
                } finally {
                    isLoading = false
                }
            }
        }) {
            Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null)
        }
    }, floatingActionButtonPosition = FabPosition.Center) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(200.dp),
            ) {
                Image(
                    painter = painterResource(id = mapLabelToEmotion(result)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                if (state.isSpeaking) {
                    voiceToTextParser.stopListening()
                } else {
                    voiceToTextParser.startListening("en")
                }
            }) {
                Text(text = "click to speak")
            }
            AnimatedContent(targetState = state.isSpeaking, label = "") { isSpeaking ->
                if (isSpeaking) {
                    Text(
                        text = "Speak...",
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Text(
                        text = state.spokenText.ifEmpty { "Click on record" },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            TextField(
                value = userInput,
                onValueChange = { userInput = it },
                label = { Text("Enter text") }
            )
            Box(modifier = Modifier.size(200.dp)) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
                } else {
                    Text(text = result, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

fun mapLabelToEmotion(label: String): Int {
    return when (label) {
        "anger" -> R.drawable.angry
        "excitement" -> R.drawable.excited
        "joy" -> R.drawable.happy
        "love" -> R.drawable.love
        else -> R.drawable.neutral
    }
}

@Preview(showBackground = true)
@Composable
fun MoodiePreview() {
    MoodieTheme {
        val voiceToTextParser by lazy { VoiceToTextParser(Application()) }
        Moodie(voiceToTextParser)
    }
}
