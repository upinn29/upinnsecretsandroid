package upinn.tech.upinnsecretsandroid

import android.app.Activity
import android.content.Context
import android.os.Build
import java.util.Locale
import java.util.UUID

open class DeviceInfo(private val context: Context) {
    private val activity: Activity?
        get() = if (context is Activity) context else null

    val manufacturer: String
        get() = Build.MANUFACTURER

    val model: String
        get() = Build.MODEL

    val os: String
        get() = "Android"

    val osVersion: String
        get() = Build.VERSION.RELEASE

    val sdkVersion: String
        get() = Build.VERSION.SDK_INT.toString()

    val deviceType: String
        get() = if (Build.DEVICE.contains("tablet", true)) "Tablet" else "Smartphone"

    val language: String
        get() = Locale.getDefault().language

    val region: String
        get() = Locale.getDefault().country

    val packageName: String
        get() = context.packageName

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}