package com.udacity.asteroidradar

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.udacity.asteroidradar.Constants.API_KEY
import com.udacity.asteroidradar.api.AsteroidApiService
import com.udacity.asteroidradar.api.parseAsteroidsJsonResult
import com.udacity.asteroidradar.database.AsteroidDatabase
import com.udacity.asteroidradar.database.asDatabaseModel
import com.udacity.asteroidradar.database.asDomainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AsteroidRepository(private val database: AsteroidDatabase) {

    @RequiresApi(Build.VERSION_CODES.O)
    val today: LocalDateTime = LocalDateTime.now()

    @RequiresApi(Build.VERSION_CODES.O)
    val dateFormat: DateTimeFormatter = DateTimeFormatter.ISO_DATE

    @RequiresApi(Build.VERSION_CODES.O)
    val asteroids: LiveData<List<Asteroid>> =
        Transformations.map(
            database.asteroidDao.getAsteroids(
                today.format(dateFormat), today.plusDays(7).format(dateFormat)
            )
        ) {
            it.asDomainModel()
        }

    @RequiresApi(Build.VERSION_CODES.O)
    val asteroidsToday: LiveData<List<Asteroid>> =
        Transformations.map(
            database.asteroidDao.getAsteroids(
                today.format(dateFormat), today.format(dateFormat)
            )
        ) {
            it.asDomainModel()
        }

    @RequiresApi(Build.VERSION_CODES.N)
    suspend fun refreshAsteroids() {
        withContext(Dispatchers.IO) {
            try {
                val listOfAsteroids =
                    AsteroidApiService.AsteroidApi.retrofitService.getAsteroids(API_KEY)
                val result = parseAsteroidsJsonResult(JSONObject(listOfAsteroids))
                database.asteroidDao.insertAll(*result.asDatabaseModel())
            } catch (e: Exception) {
                Log.e("Failed", e.message.toString())
            }
        }
    }
}
