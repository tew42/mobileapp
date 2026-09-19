package coredevices.util

import PlatformUiContext
import android.Manifest
import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidPermissionRequester(
    private val context: Context,
    requiredPermissions: RequiredPermissions,
    private val companionDevice: AndroidCompanionDevice,
    appResumed: AppResumed,
) : PermissionRequester(requiredPermissions, appResumed) {

    override suspend fun requestPlatformPermission(
        permission: Permission,
        uiContext: PlatformUiContext
    ): PermissionResult =
        when (permission) {
            Permission.ReadNotifications -> requestNotificationAccess(uiContext.activity)
            Permission.SetAlarms -> requestAlarmPermission(uiContext.activity)
            Permission.BatteryOptimization -> requestBatteryOptimizationDisable(uiContext.activity)
            else -> requestAndroidRuntimePermissions(
                permission,
                uiContext.activity,
            )
        }

    override suspend fun hasPermission(permission: Permission): Boolean =
        when (permission) {
            Permission.ReadNotifications -> companionDevice.hasNotificationAccess(context)
            Permission.SetAlarms -> hasAlarmPermission()
            Permission.BatteryOptimization -> batteryOptimizationsAreDisabled()
            else -> permissionGranted(permission)
        }

    private suspend fun requestAndroidRuntimePermissions(
        permission: Permission,
        activity: Activity
    ): PermissionResult {
        val permissions = permission.asAndroidPermissions()
        logger.v { "requestAndroidRuntimePermissions: $permissions" }
        val firstPermission = permissions.firstOrNull()
        if (firstPermission == null) {
            logger.e { "requestAndroidRuntimePermissions: no permissions" }
            return PermissionResult.Error
        }
        val registry = activity as? ActivityResultRegistryOwner
        if (registry == null) {
            logger.e { "requestAndroidRuntimePermissions: registry is null" }
            return PermissionResult.Error
        }
        return suspendCancellableCoroutine { continuation ->
            val launcher = registry.activityResultRegistry.register(
                key = "permissions-$firstPermission",
                contract = ActivityResultContracts.RequestMultiplePermissions(),
            ) { permissions ->
                permissions.forEach {
                    logger.d { "Permission ${it.key} granted: ${it.value}" }
                }
                // An empty result map means the request was cancelled, never that the
                // permission was granted.
                val granted = permissions.isNotEmpty() &&
                        permission.isGrantedBy(permissions.values.toList())
                val result = if (granted) {
                    PermissionResult.Granted
                } else {
                    // Classify on something actually denied: with an approximate grant the
                    // coarse entry is granted, and rationale is always false for those.
                    val denied = permissions.entries.firstOrNull { !it.value }?.key
                        ?: firstPermission
                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity, denied)) {
                        PermissionResult.Rejected
                    } else {
                        PermissionResult.RejectedForever
                    }
                }
                logger.v { "result = $result" }
                if (continuation.isActive) {
                    continuation.resume(result)
                } else {
                    logger.w { "continuation is not active" }
                }
            }
            launcher.launch(permissions.toTypedArray())
        }
    }

    override fun openPermissionsScreen(uiContext: PlatformUiContext) {
        logger.d { "openPermissionsScreen()" }
        val intent = Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        uiContext.activity.startActivity(intent)
    }

    private fun permissionGranted(permission: Permission): Boolean {
        val granted = permission.asAndroidPermissions().map {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
        return permission.isGrantedBy(granted)
    }

    private fun hasAlarmPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService<AlarmManager>()?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    private suspend fun requestAlarmPermission(activity: Activity): PermissionResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return PermissionResult.Granted
        }
        val registry = activity as? ActivityResultRegistryOwner
        if (registry == null) {
            logger.e { "requestAlarmPermission: registry is null" }
            return PermissionResult.Error
        }
        return suspendCancellableCoroutine { continuation ->
            val launcher = registry.activityResultRegistry.register(
                "requestScheduleExactAlarm",
                ActivityResultContracts.StartActivityForResult(),
            ) { _ ->
                val result = if (hasAlarmPermission()) {
                    PermissionResult.Granted
                } else {
                    PermissionResult.Rejected
                }
                continuation.resume(result)
            }
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            launcher.launch(intent)
        }
    }

    private fun batteryOptimizationsAreDisabled(): Boolean {
        val powerManager = context.getSystemService<PowerManager>()
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }

    private suspend fun requestBatteryOptimizationDisable(activity: Activity): PermissionResult {
        val registry = activity as? ActivityResultRegistryOwner
        if (registry == null) {
            logger.e { "requestBatteryOptimizationDisable: registry is null" }
            return PermissionResult.Error
        }
        return suspendCancellableCoroutine { continuation ->
            val launcher = registry.activityResultRegistry.register(
                "requestScheduleExactAlarm",
                ActivityResultContracts.StartActivityForResult(),
            ) { _ ->
                val result = if (batteryOptimizationsAreDisabled()) {
                    PermissionResult.Granted
                } else {
                    PermissionResult.Rejected
                }
                continuation.resume(result)
            }
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = "package:${context.packageName}".toUri()
            }
            launcher.launch(intent)
        }
    }

    private suspend fun requestNotificationAccess(activity: Activity): PermissionResult {
        return companionDevice.requestNotificationAccess(activity)
    }
}

private fun Permission.asAndroidPermissions(): List<String> = when (this) {
    // Requesting fine alone is ignored on Android 12; both must go in one request.
    Permission.Location -> listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    Permission.PreciseLocation -> listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    Permission.BackgroundLocation -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(ACCESS_BACKGROUND_LOCATION)
    } else emptyList()

    Permission.PostNotifications -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.POST_NOTIFICATIONS)
    } else emptyList()

    Permission.Bluetooth -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else emptyList()
    }

    Permission.ReadCallLog -> listOf(Manifest.permission.READ_CALL_LOG)
    Permission.Calendar -> listOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR,
    )

    Permission.Contacts -> listOf(Manifest.permission.READ_CONTACTS)
    Permission.ReadPhoneState -> listOf(Manifest.permission.READ_PHONE_STATE)
    Permission.ReadNotifications -> throw IllegalArgumentException("Shouldn't be calling this")
    Permission.RecordAudio -> listOf(Manifest.permission.RECORD_AUDIO)
    Permission.SpeechRecognizer -> throw IllegalArgumentException("SpeechRecognizer not needed on Android")
    Permission.ExternalStorage -> if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && CommonBuildKonfig.QA) {
        listOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    } else emptyList()

    Permission.SetAlarms -> throw IllegalArgumentException("Shouldn't be calling this for SetAlarms")
    Permission.BatteryOptimization -> throw IllegalArgumentException("Shouldn't be calling this for BatteryOptimization")
    Permission.Beeper -> listOf(
        "com.beeper.android.permission.READ_PERMISSION",
        "com.beeper.android.permission.SEND_PERMISSION"
    )
    Permission.Reminders -> throw IllegalArgumentException("Not needed on Android")
}