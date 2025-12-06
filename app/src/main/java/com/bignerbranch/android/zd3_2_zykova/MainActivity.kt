package com.bignerbranch.android.zd3_2_zykova

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var gridView: GridView
    private lateinit var movieList: MutableList<Movie>
    private lateinit var adapter: MovieAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var titleText: TextView

    private val genres = listOf(
        Genre("Все", ""),
        Genre("Боевик", "action"),
        Genre("Комедия", "comedy"),
        Genre("Драма", "drama"),
        Genre("Ужасы", "horror"),
        Genre("Фэнтези", "fantasy"),
        Genre("Научная фантастика", "sci-fi"),
        Genre("Анимация", "animation")
    )

    private var currentGenre: Genre = genres[0]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gridView = findViewById(R.id.gridView)
        progressBar = findViewById(R.id.progressBar)
        searchEditText = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)
        titleText = findViewById(R.id.titleText)

        movieList = mutableListOf()
        adapter = MovieAdapter(movieList)
        gridView.adapter = adapter

        setupGenreButtons()

        loadMoviesByGenre(currentGenre)

        searchButton.setOnClickListener {
            performSearch()
        }

        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        searchEditText.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                performSearch()
                true
            } else {
                false
            }
        }

        gridView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val movie = movieList[position]
            val intent = Intent(this, DetailActivity::class.java).apply {
                putExtra("title", movie.title)
                putExtra("poster", movie.poster)
                putExtra("year", movie.year)
                putExtra("imdbID", movie.imdbID)
            }
            startActivity(intent)
        }
    }

    private fun setupGenreButtons() {
        val genreButtons = listOf(
            findViewById<Button>(R.id.genre_all),
            findViewById<Button>(R.id.genre_action),
            findViewById<Button>(R.id.genre_comedy),
            findViewById<Button>(R.id.genre_drama),
            findViewById<Button>(R.id.genre_horror),
            findViewById<Button>(R.id.genre_fantasy),
            findViewById<Button>(R.id.genre_scifi),
            findViewById<Button>(R.id.genre_animation)
        )

        updateGenreButtons(genreButtons)

        genreButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                selectGenre(genres[index], genreButtons)
            }

            button.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    view.isSelected = true
                } else {
                    view.isSelected = genres[index] == currentGenre
                }
            }
        }
    }

    private fun selectGenre(genre: Genre, genreButtons: List<Button>) {
        currentGenre = genre
        titleText.text = if (genre.searchQuery.isEmpty()) "Все фильмы" else "Жанр: ${genre.name}"

        updateGenreButtons(genreButtons)

        loadMoviesByGenre(genre)
    }

    private fun updateGenreButtons(genreButtons: List<Button>) {
        genreButtons.forEachIndexed { index, button ->
            button.isSelected = genres[index] == currentGenre
        }
    }

    private fun loadMoviesByGenre(genre: Genre) {
        progressBar.visibility = View.VISIBLE

        val searchQuery = if (genre.searchQuery.isEmpty()) {
            "movie"
        } else {
            genre.searchQuery
        }

        lifecycleScope.launch {
            try {
                val response = ApiService.api.searchMovies(searchQuery)
                if (response.response == "True") {
                    movieList.clear()
                    response.search?.forEach { movieResult ->
                        val movie = Movie(
                            title = movieResult.title,
                            poster = if (movieResult.poster != "N/A") movieResult.poster else "https://via.placeholder.com/300x450/333333/FFFFFF?text=No+Image",
                            description = "Year: ${movieResult.year}",
                            year = movieResult.year,
                            imdbID = movieResult.imdbID
                        )
                        movieList.add(movie)
                    }
                    adapter.notifyDataSetChanged()

                    if (movieList.isEmpty()) {
                        Toast.makeText(this@MainActivity, "Фильмы не найдены", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Фильмы не найдены: ${response.error}", Toast.LENGTH_SHORT).show()
                    loadSampleMovies()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
                loadSampleMovies()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun performSearch() {
        val query = searchEditText.text.toString().trim()
        if (query.isNotEmpty()) {
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(searchEditText.windowToken, 0)

            loadMovies(query)
            titleText.text = "Результаты поиска: $query"
        } else {
            loadMoviesByGenre(currentGenre)
            titleText.text = if (currentGenre.searchQuery.isEmpty()) "Все фильмы" else "Жанр: ${currentGenre.name}"
        }
    }

    private fun loadMovies(searchQuery: String) {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = ApiService.api.searchMovies(searchQuery)
                if (response.response == "True") {
                    movieList.clear()
                    response.search?.forEach { movieResult ->
                        val movie = Movie(
                            title = movieResult.title,
                            poster = if (movieResult.poster != "N/A") movieResult.poster else "https://via.placeholder.com/300x450/333333/FFFFFF?text=No+Image",
                            description = "Year: ${movieResult.year}",
                            year = movieResult.year,
                            imdbID = movieResult.imdbID
                        )
                        movieList.add(movie)
                    }
                    adapter.notifyDataSetChanged()

                    if (movieList.isEmpty()) {
                        Toast.makeText(this@MainActivity, "Фильмы не найдены", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Фильмы не найдены: ${response.error}", Toast.LENGTH_SHORT).show()
                    loadSampleMovies()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
                loadSampleMovies()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun loadSampleMovies() {
        movieList.clear()
        movieList.addAll(
            listOf(
                Movie("Фильм 1", "https://via.placeholder.com/300x450/FF6B6B/FFFFFF?text=Film+1", "Описание фильма 1", "2020", "tt1234567"),
                Movie("Фильм 2", "https://via.placeholder.com/300x450/4ECDC4/FFFFFF?text=Film+2", "Описание фильма 2", "2021", "tt2345678"),
                Movie("Фильм 3", "https://via.placeholder.com/300x450/45B7D1/FFFFFF?text=Film+3", "Описание фильма 3", "2022", "tt3456789")
            )
        )
        adapter.notifyDataSetChanged()
    }

    private inner class MovieAdapter(private val movies: List<Movie>) : BaseAdapter() {

        override fun getCount(): Int = movies.size

        override fun getItem(position: Int): Any = movies[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_movie, parent, false)

            val imageView = view.findViewById<ImageView>(R.id.movie_image)
            val textView = view.findViewById<TextView>(R.id.movie_title)
            val container = view.findViewById<LinearLayout>(R.id.movie_container)

            val movie = movies[position]
            textView.text = movie.title

            Glide.with(this@MainActivity)
                .load(movie.poster)
                .into(imageView)

            return view
        }
    }

    data class Genre(val name: String, val searchQuery: String)
}