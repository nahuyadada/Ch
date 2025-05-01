package com.example.chowcheck.viewmodel // Create this package or use your preferred one

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * ViewModel to hold and manage the selected date shared between fragments.
 * It's scoped to the Activity hosting the fragments.
 */
class DateViewModel : ViewModel() {

    // Format for internal consistency if needed, though Calendar object is primary
    private val keyDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Private MutableLiveData that holds the currently selected Calendar instance.
    // Only the ViewModel can change this.
    private val _selectedDate = MutableLiveData<Calendar>()

    // Public LiveData that Fragments can observe. They get notified when the date changes.
    val selectedDate: LiveData<Calendar>
        get() = _selectedDate // Expose read-only LiveData

    init {
        // Initialize with today's date when the ViewModel is first created.
        _selectedDate.value = Calendar.getInstance()
    }

    /**
     * Updates the selected date in the ViewModel.
     * This will notify any active observers.
     */
    fun updateSelectedDate(year: Int, month: Int, dayOfMonth: Int) {
        // Get the current time components to preserve them if needed
        val currentCalendar = _selectedDate.value ?: Calendar.getInstance()
        val currentHour = currentCalendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentCalendar.get(Calendar.MINUTE)
        val currentSecond = currentCalendar.get(Calendar.SECOND)
        val currentMillis = currentCalendar.get(Calendar.MILLISECOND)

        // Create a new Calendar instance for the selected date
        val newCalendar = Calendar.getInstance().apply {
            set(year, month, dayOfMonth, currentHour, currentMinute) // Set date, keep time
            set(Calendar.SECOND, currentSecond)
            set(Calendar.MILLISECOND, currentMillis)
        }

        // Only update if the date actually changed to avoid unnecessary notifications
        if (!isSameDay(newCalendar, _selectedDate.value)) {
            _selectedDate.value = newCalendar // Set the new value, triggering observers
        }
    }

    /**
     * Sets the selected date directly from a Calendar object.
     */
    fun setSelectedDate(calendar: Calendar) {
        // Create a new instance to ensure LiveData triggers an update even if millis are the same
        val newCalendar = Calendar.getInstance()
        newCalendar.timeInMillis = calendar.timeInMillis
        // Only update if the date actually changed
        if (!isSameDay(newCalendar, _selectedDate.value)) {
            _selectedDate.value = newCalendar
        }
    }


    /**
     * Resets the selected date to the current day.
     */
    fun resetToToday() {
        val today = Calendar.getInstance()
        // Only update if the current selection isn't already today
        if (!isSameDay(today, _selectedDate.value)) {
            _selectedDate.value = today
        }
    }

    /**
     * Helper function to check if two Calendar instances represent the same day.
     * (Ignores time components). Returns false if either calendar is null.
     */
    private fun isSameDay(cal1: Calendar?, cal2: Calendar?): Boolean {
        if (cal1 == null || cal2 == null) {
            return false
        }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH) && // Check Day first (more likely to differ)
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }

    /**
     * Gets the currently selected date as a Calendar object.
     * Provides a default (today) if not yet set.
     */
    fun getCurrentSelectedDate(): Calendar {
        return _selectedDate.value ?: Calendar.getInstance()
    }

    /**
     * Gets the selected date formatted as "yyyy-MM-dd" for use in SharedPreferences keys.
     */
    fun getSelectedDateKeyString(): String {
        return keyDateFormat.format(getCurrentSelectedDate().time)
    }
}
