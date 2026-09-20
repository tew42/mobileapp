package coredevices.util

enum class Permission {
    Location,
    PreciseLocation,
    BackgroundLocation,
    PostNotifications,
    Bluetooth,
    ReadCallLog,
    Calendar,
    Contacts,
    ReadPhoneState,
    ReadNotifications,
    RecordAudio,
    SpeechRecognizer,
    ExternalStorage,
    SetAlarms,
    BatteryOptimization,
    Beeper,
    Reminders
}

/** Holding any one of the mapped platform permissions is enough - approximate location is fine. */
val Permission.partialGrantSuffices: Boolean
    get() = this == Permission.Location

/**
 * Whether [results], the grant state of this permission's platform permissions, satisfies it.
 * An empty list means the platform needs nothing on this API level, which is always satisfied -
 * callers holding a runtime *result* must reject an empty one themselves, since there it means
 * the request was cancelled.
 */
fun Permission.isGrantedBy(results: List<Boolean>): Boolean = when {
    results.isEmpty() -> true
    partialGrantSuffices -> results.any { it }
    else -> results.all { it }
}
