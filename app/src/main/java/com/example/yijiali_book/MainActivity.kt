package com.example.yijiali_book

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class BookSearchResponse(
    val items: List<BookItem>?
)

data class BookItem(
    val id: String,
    val volumeInfo: VolumeInfo
)

data class VolumeInfo(
    val title: String,
    val authors: List<String>?,
    val description: String?,
    val imageLinks: ImageLinks?
)

data class ImageLinks(
    val thumbnail: String?
)

interface GoogleBooksApi {
    @GET("volumes")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("key") apiKey: String
    ): BookSearchResponse
}

class BookViewModel : ViewModel() {
    private val apiKey = "AIzaSyAAdsa3rGsRm5Ho4f3Ml22iKeR-8AXC-C8"

    private val _books = MutableStateFlow<List<BookItem>>(emptyList())
    val books: StateFlow<List<BookItem>> get() = _books

    private val _selectedBook = MutableStateFlow<BookItem?>(null)
    val selectedBook: StateFlow<BookItem?> get() = _selectedBook

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/books/v1/")
        .addConverterFactory(
            MoshiConverterFactory.create(
                Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            )
        )
        .build()

    private val api = retrofit.create(GoogleBooksApi::class.java)

    fun searchBooks(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = api.searchBooks(query = query, apiKey = apiKey)
                _books.value = response.items ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectBook(book: BookItem) {
        _selectedBook.value = book
    }

    fun clearSelectedBook() {
        _selectedBook.value = null
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val bookViewModel: BookViewModel = viewModel()
                BookFinderApp(bookViewModel)
            }
        }
    }
}

@Composable
fun BookFinderApp(viewModel: BookViewModel) {
    val books by viewModel.books.collectAsState()
    val selectedBook by viewModel.selectedBook.collectAsState()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (selectedBook != null) {
                BookList(
                    books = books,
                    onBookClick = { book ->
                        viewModel.selectBook(book)
                    },
                    modifier = Modifier.weight(1f)
                )
                BookDetailsScreen(
                    book = selectedBook!!,
                    onBack = { viewModel.clearSelectedBook() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                BookSearchScreen(
                    books = books,
                    onSearch = { query ->
                        viewModel.searchBooks(query)
                    },
                    onBookClick = { book ->
                        viewModel.selectBook(book)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    } else {
        if (selectedBook != null) {
            BookDetailsScreen(
                book = selectedBook!!,
                onBack = { viewModel.clearSelectedBook() },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            BookSearchScreen(
                books = books,
                onSearch = { query ->
                    viewModel.searchBooks(query)
                },
                onBookClick = { book ->
                    viewModel.selectBook(book)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun BookSearchScreen(
    books: List<BookItem>,
    onSearch: (String) -> Unit,
    onBookClick: (BookItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }

    Column(modifier = modifier.padding(16.dp)) {
        Text("Book Finder", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search by Title or Author") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (query.isNotBlank()) {
                    onSearch(query)
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Search")
        }

        Spacer(modifier = Modifier.height(8.dp))

        BookList(books = books, onBookClick = onBookClick)
    }
}

@Composable
fun BookList(
    books: List<BookItem>,
    onBookClick: (BookItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(books) { book ->
            BookListItem(book = book, onClick = { onBookClick(book) })
        }
    }
}
@Composable
fun BookListItem(book: BookItem, onClick: () -> Unit) {
    val volumeInfo = book.volumeInfo
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            val imageUrl = volumeInfo.imageLinks?.thumbnail?.replace("http://", "https://")
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = volumeInfo.title,
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Book,
                        contentDescription = "No image",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = volumeInfo.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = volumeInfo.authors?.joinToString(", ") ?: "Unknown Author",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
@Composable
fun BookDetailsScreen(
    book: BookItem,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val volumeInfo = book.volumeInfo
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }
        Text(
            volumeInfo.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "By ${volumeInfo.authors?.joinToString(", ") ?: "Unknown Author"}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        val imageUrl = volumeInfo.imageLinks?.thumbnail?.replace("http://", "https://")
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = volumeInfo.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Book,
                    contentDescription = "No image",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(80.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = volumeInfo.description ?: "No description available.",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}


/*
Tested on:
- Pixel 8a emulator (Android 13) in both portrait and landscape modes.
- Pixel Tablet emulator (Android 13) in both portrait and landscape modes.
*/

