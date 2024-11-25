package com.example.image_loader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.rememberAsyncImagePainter
import com.example.image_loader.ui.theme.Image_loaderTheme
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query


class MainActivity : ComponentActivity() {
    private val gifViewModel by viewModels<GifViewModel>() // ViewModel для состояния

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Image_loaderTheme {
                AppContent(viewModel = gifViewModel)
            }
        }
    }
}


interface GiphyApi {
    @GET("v1/gifs/trending")
    suspend fun getTrendingGifs(
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): GiphyResponse
}


data class GiphyResponse(val data: List<Gif>)
data class Gif(val images: Images)
data class Images(val original: ImageDetails)
data class ImageDetails(val url: String)


class GifViewModel : ViewModel() {
    var gifs by mutableStateOf<List<Gif>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorOccurred by mutableStateOf(false)
        private set
    var offset by mutableStateOf(0)
        private set

    fun loadData() {
        if (isLoading) return
        isLoading = true
        viewModelScope.launch {
            try {
                val response = getRetrofit().create(GiphyApi::class.java)
                    .getTrendingGifs("ROGxA0z2Hf2ofMKfMrBzoIu0IA9BQqGP", 10, offset)
                gifs = gifs + response.data
                offset += response.data.size
                errorOccurred = false
            } catch (e: Exception) {
                errorOccurred = true
            } finally {
                isLoading = false
            }
        }
    }
}


fun getRetrofit(): Retrofit {
    return Retrofit.Builder()
        .baseUrl("https://api.giphy.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}


@Composable
fun AppContent(viewModel: GifViewModel) {
    val gifs by remember { derivedStateOf { viewModel.gifs } }
    val isLoading by remember { derivedStateOf { viewModel.isLoading } }
    val errorOccurred by remember { derivedStateOf { viewModel.errorOccurred } }

    Scaffold(
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (gifs.isEmpty() && isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    GifList(
                        gifs = gifs,
                        isLoading = isLoading,
                        errorOccurred = errorOccurred,
                        onLoadMore = { viewModel.loadData() }
                    )
                }
            }
        }
    )
}


@Composable
fun GifList(
    gifs: List<Gif>,
    isLoading: Boolean,
    errorOccurred: Boolean,
    onLoadMore: () -> Unit
) {
    LazyColumn {
        items(gifs) { gif ->
            GifItem(gif)
        }
        item {
            LoadMoreSection(
                isLoading = isLoading,
                errorOccurred = errorOccurred,
                onLoadMore = onLoadMore
            )
        }
    }
}

@Composable
fun LoadMoreSection(isLoading: Boolean, errorOccurred: Boolean, onLoadMore: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator()
            }

            errorOccurred -> {
                Button(onClick = onLoadMore) {
                    Text("Retry")
                }
            }

            else -> {
                Button(onClick = onLoadMore) {
                    Text("Load More")
                }
            }
        }
    }
}

@Composable
fun GifItem(gif: Gif) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .height(200.dp)
    ) {
        Image(
            painter = rememberAsyncImagePainter(gif.images.original.url),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showSystemUi = true)
@Composable
fun PreviewAppContent() {
    val previewViewModel = GifViewModel()
    Image_loaderTheme {
        AppContent(viewModel = previewViewModel)
    }
}
