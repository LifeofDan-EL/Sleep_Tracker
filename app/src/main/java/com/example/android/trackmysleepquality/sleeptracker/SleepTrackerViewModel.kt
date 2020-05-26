/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import android.provider.SyncStateContract.Helpers.insert
import android.provider.SyncStateContract.Helpers.update
import androidx.lifecycle.*
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.*

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {

    private var viewModelJob = Job()

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private var tonight = MutableLiveData<SleepNight>()

    val nights = database.getAllNights()

    // Converted nights to Spanned for displaying
    val nightsString = Transformations.map(nights) { nights ->
        formatNights(nights, application.resources)
    }

    val startButtonVisible = Transformations.map(tonight){
        null == it
    }

    val stopButtonVisible = Transformations.map(tonight){
        null !== it
    }

    val clearButtonVisible = Transformations.map(nights){
        it?.isNotEmpty()
    }

    private var _showSnackbarEvent = MutableLiveData<Boolean>()

    val showSnackBarEvent: LiveData<Boolean>
    get() = _showSnackbarEvent

    fun doneShowingSnackbar(){
        _showSnackbarEvent.value = false
    }


    private val _navigateToSleepQuality = MutableLiveData<SleepNight>()

    val navigateToSleepQuality: LiveData<SleepNight>
        get() = _navigateToSleepQuality

    fun doneNavigating() {
        _navigateToSleepQuality.value = null
    }

    init {
        intializeTonight()
    }

    private fun intializeTonight(){
        uiScope.launch {
            tonight.value = getTonightFromDatabase()
        }
    }

    private suspend fun getTonightFromDatabase(): SleepNight? {
        return withContext(Dispatchers.IO){
            var night = database.getTonight()
        if (night?.endTimeMilli != night?.startTimeMilli) {
            night = null
        }
         night
        }
    }

    fun onStartTracking() {
        uiScope.launch {
            val newNight = SleepNight()
            insert(newNight)
            tonight.value = getTonightFromDatabase()
        }
    }

    private suspend fun insert(night:SleepNight){
        withContext(Dispatchers.IO){
            database.insert(night)
        }
    }

    fun onStopTracking() {
        uiScope.launch {
            val oldNight = tonight.value ?: return@launch

            // Update the night in the Database to add the end time
            oldNight.endTimeMilli = System.currentTimeMillis()
            update(oldNight)
            _navigateToSleepQuality.value = oldNight
        }
    }

    private suspend fun update(night: SleepNight){
        withContext(Dispatchers.IO){
            database.update(night)
        }
    }


    // Called when the ViewModel is cleared
    fun onClear() {
        uiScope.launch {
            // Clear the Database Table
            clear()

            //And Clear tonight since it's no longer in the Database
            tonight.value = null

            _showSnackbarEvent.value = true
        }
    }

    suspend fun clear(){
        withContext(Dispatchers.IO){
            database.clear()
        }
    }

    private val _navigateToSleepDataQuality = MutableLiveData<Long>()
    val navigateToSleepDataQuality
    get() = _navigateToSleepDataQuality

    fun onSleepNightClicked(id: Long){
        _navigateToSleepDataQuality.value = id
    }

    fun onSleepQualityNavigated() {
        _navigateToSleepDataQuality.value = null
    }
}

