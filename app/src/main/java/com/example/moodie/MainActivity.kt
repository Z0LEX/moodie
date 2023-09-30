package com.example.moodie

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MoodieTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Moodie()
                }
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun Moodie(modifier: Modifier = Modifier) {
    var userInput by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }

    val retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.0.144:5000/").addConverterFactory(GsonConverterFactory.create())
        .build()
    val sentimentService = retrofit.create(SentimentService::class.java)
    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = {
            // Make the web request here
            val request = SentimentRequest(userInput)
            println("Get prediction")
            Log.d("ServerResponse", "Get predictíon")
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val response = sentimentService.predictSentiment(request)
                    val label = response.result.label
                    result = label
                    val score = response.result.score

                    // Log the response
                    Log.d("ServerResponse", "Label: $label, Score: $score")
                } catch (e: Exception) {
                    Log.e("ServerRequest", "Request failed: ${e.message}", e)
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
            Image(
                painter = painterResource(id = mapLabelToEmotion(result)),
                contentDescription = null,
                modifier = Modifier.size(200.dp)
            )
            TextField(
                value = userInput,
                onValueChange = { userInput = it },
                label = { Text("Enter text") }
            )
            Text(text = result)
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
        Moodie()
    }
}
