package com.bignerbranch.android.zd3_2_zykova

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var titleView: TextView
    private lateinit var yearView: TextView
    private lateinit var descriptionView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var backButton: Button
    private lateinit var ratingView: TextView
    private lateinit var runtimeView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        imageView = findViewById(R.id.detail_image)
        titleView = findViewById(R.id.detail_title)
        yearView = findViewById(R.id.detail_year)
        descriptionView = findViewById(R.id.detail_description)
        progressBar = findViewById(R.id.progressBar)
        backButton = findViewById(R.id.backButton)
        ratingView = findViewById(R.id.detail_rating)
        runtimeView = findViewById(R.id.detail_runtime)

        val title = intent.getStringExtra("title") ?: ""
        val poster = intent.getStringExtra("poster") ?: ""
        val year = intent.getStringExtra("year") ?: ""
        val imdbID = intent.getStringExtra("imdbID") ?: ""

        titleView.text = title
        yearView.text = "Год: $year"

        descriptionView.text = "Загрузка описания..."
        ratingView.text = "IMDb: Загрузка..."
        runtimeView.text = "Длительность: Загрузка..."

        progressBar.visibility = View.VISIBLE

        Glide.with(this)
            .load(poster)
            .into(imageView)

        setupBackButton()

        if (imdbID.isNotEmpty()) {
            loadMovieDetails(imdbID)
        } else {
            showError("Нет данных о фильме")
        }
    }

    private fun loadMovieDetails(imdbID: String) {
        lifecycleScope.launch {
            try {
                val response = ApiService.api.getMovieDetails(imdbID)
                if (response.response == "True") {
                    updateMovieDetails(response)
                } else {
                    showError("Фильм не найден")
                }
            } catch (e: Exception) {
                showError("Ошибка сети: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun updateMovieDetails(response: MovieDetailResponse) {
        val plot = if (response.plot != "N/A" && response.plot.isNotEmpty()) {
            response.plot
        } else {
            "Описание отсутствует"
        }
        descriptionView.text = plot

        val rating = if (response.imdbRating != "N/A" && response.imdbRating.isNotEmpty()) {
            response.imdbRating
        } else {
            "N/A"
        }
        ratingView.text = "IMDb: $rating"

        val runtime = if (response.runtime != "N/A" && response.runtime.isNotEmpty()) {
            response.runtime
        } else {
            "N/A"
        }
        runtimeView.text = "Длительность: $runtime"
    }

    private fun showError(errorMessage: String = "Произошла ошибка") {
        descriptionView.text = if (errorMessage.isNotEmpty()) {
            "Ошибка: $errorMessage\n\nПопробуйте обновить данные позже."
        } else {
            "Информация о фильме временно недоступна."
        }

        ratingView.text = "IMDb: N/A"
        runtimeView.text = "Длительность: N/A"

        progressBar.visibility = View.GONE
    }

    private fun setupBackButton() {
        backButton.setOnClickListener {
            finish()
        }

        backButton.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.setBackgroundColor(android.graphics.Color.parseColor("#00cc00"))
            } else {
                view.setBackgroundResource(R.drawable.button)
            }
        }
    }
}