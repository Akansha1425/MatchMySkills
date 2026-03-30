package com.example.matchmyskills.util

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/**
 * Extension functions for Firestore DocumentSnapshot to safely retrieve dates.
 * Handles both the native Firebase [Timestamp] and legacy [Long] millisecond values.
 */
fun DocumentSnapshot.getDateSafe(field: String): Date? {
    return when (val value = get(field)) {
        is Timestamp -> value.toDate()
        is Long -> Date(value)
        is Number -> Date(value.toLong())
        else -> null
    }
}

/**
 * Safely maps a DocumentSnapshot to a data class, with better handling for Date type conversions.
 * Use this as a more resilient alternative to toObject() for refactored models.
 */
inline fun <reified T : Any> DocumentSnapshot.toObjectSafe(
    dateFields: List<String> = emptyList(),
    onDateMapped: (T, Map<String, Date?>) -> T
): T? {
    val obj = toObject(T::class.java) ?: return null
    val mappedDates = mutableMapOf<String, Date?>()
    
    dateFields.forEach { field ->
        mappedDates[field] = getDateSafe(field)
    }
    
    return onDateMapped(obj, mappedDates)
}
