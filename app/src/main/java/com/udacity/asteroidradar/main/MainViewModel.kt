package com.udacity.asteroidradar.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.udacity.asteroidradar.Asteroid
import com.udacity.asteroidradar.AsteroidRepository
import com.udacity.asteroidradar.Constants.API_KEY
import com.udacity.asteroidradar.PictureOfDay
import com.udacity.asteroidradar.api.AsteroidApiService
import com.udacity.asteroidradar.database.getDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = getDatabase(application)
    private val asteroidRepository = AsteroidRepository(database)

    init {
        viewModelScope.launch {
            asteroidRepository.refreshAsteroids()
            refreshPicture()
        }
    }

    private val _navigateToDetailFragment = MutableLiveData<Asteroid>()
    val navigateToDetailFragment: LiveData<Asteroid>
        get() = _navigateToDetailFragment

    private val _pictureOfTheDay = MutableLiveData<PictureOfDay>()
    val pictureOfTheDay: LiveData<PictureOfDay>
        get() = _pictureOfTheDay

    private var asteroidFilter = MutableLiveData(Filter.WEEK)

    val listOfAsteroids = Transformations.switchMap(asteroidFilter) {
        when (it) {
            Filter.TODAY -> AsteroidRepository(database).asteroidsToday
            else ->  AsteroidRepository(database).asteroids
        }
    }

    fun onClickAsteroid(asteroid: Asteroid) {
        _navigateToDetailFragment.value = asteroid
    }

    fun onNavigateAsteroid() {
        _navigateToDetailFragment.value = null
    }

    fun updateFilter(filter: Filter) {
        asteroidFilter.postValue(filter)
    }

    private suspend fun refreshPicture() {
        withContext(Dispatchers.IO) {
            try {
                _pictureOfTheDay.postValue(
                    AsteroidApiService.AsteroidApi.retrofitService.getPictureOfTheDay(API_KEY)
                )
            } catch (e: Exception) {
                Log.e("Failed", e.message.toString())
            }
        }
    }
}

class DetailViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

enum class Filter {
    TODAY,
    WEEK
}