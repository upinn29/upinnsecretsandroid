package upinn.tech.upinnsecretsandroid

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import upinn.tech.upinnsecretsandroid.PluginException
import java.io.InputStream
import kotlin.text.removeSuffix

class UpinnSecretsAndroid(private val isDebug: Boolean, private val context: Context, private val fileName: String){
    private var secrets: Secrets = Secrets(isDebug,context.getDatabasePath("secrets.db").absolutePath)
    private val deviceInfo = DeviceInfo(context)
    private lateinit var file_bytes_global:List<UByte>
    private lateinit var file_name_global: String

    companion object {
        private const val TAG = "UpinnSecrets" // El TAG para esta actividad
    }

    init {
        //secrets = Secrets(isDebug,dbPath)
        Log.d(TAG,"Call init")
    }



    private fun readFileFromRaw(fileName: String): ByteArray? {
        val resId = context.resources.getIdentifier(fileName, "raw", context.packageName)
        if (resId == 0) {
            Log.d(TAG, "Error file ${fileName} not found in raw")
            return null
        }
        val inputStream: InputStream = context.resources.openRawResource(resId)
        val fileBytes = inputStream.readBytes()
        inputStream.close()
        return fileBytes
    }

    @Throws(PluginException::class)
    fun login(): Long{
        try {
            file_name_global = fileName.removeSuffix(".bin")
            val fileBytes = readFileFromRaw(file_name_global) ?: throw PluginException.ErrorCode(1010)
            file_bytes_global = fileBytes.map { it.toUByte() }
            Log.d(TAG,deviceInfo.packageName)
            val args = SecretsArgs(
                fileBytes = file_bytes_global,
                fileName = file_name_global,
                packageName = deviceInfo.packageName,
                manufacturer = deviceInfo.manufacturer,
                model = deviceInfo.model,
                os = deviceInfo.os,
                osVersion = deviceInfo.osVersion,
                sdkVersion = deviceInfo.sdkVersion,
                deviceType = deviceInfo.deviceType,
                language = deviceInfo.language,
                region = deviceInfo.region,
                variable = "",
                version = ""
            )
            val resLogin = secrets.login(args)
            if (resLogin.statusCode != 200L) { // Asumiendo que 0 es éxito
                throw PluginException.ErrorCode(resLogin.statusCode)
            }
            return resLogin.statusCode

        } catch (e: Exception) {
            Log.d(TAG,e.message.toString());
            throw when (e) {
                is PluginException -> e // Ya es una PluginException
                else -> PluginException.ErrorCode(5000)
            }
        }
    }


    /** -------------------------- GET SECRET (ASYNC) -------------------------- **/
    @Throws(PluginException::class)
    suspend fun get_secret(variable: String, version: String?): SecretsResponse {
        try {
            if (!::file_bytes_global.isInitialized) {
                throw PluginException.ErrorCode(1010)
            }

            val nonNullVersion = version ?: "1"

            val args = SecretsArgs(
                fileBytes = file_bytes_global,
                fileName = file_name_global,
                packageName = deviceInfo.packageName,
                manufacturer = deviceInfo.manufacturer,
                model = deviceInfo.model,
                os = deviceInfo.os,
                osVersion = deviceInfo.osVersion,
                sdkVersion = deviceInfo.sdkVersion,
                deviceType = deviceInfo.deviceType,
                language = deviceInfo.language,
                region = deviceInfo.region,
                variable = variable,
                version = nonNullVersion
            )
            Log.d(TAG,args.toString())
            // 🔥 AHORA ES ASYNC: Rust expone secrets.getSecret(args) como Future
            val resGetSecrets = secrets.getSecret(args)

            if (resGetSecrets.statusCode != 200L) {
                throw PluginException.ErrorCode(resGetSecrets.statusCode)
            }

            return SecretsResponse(
                secretValue = resGetSecrets.secretValue,
                statusCode = resGetSecrets.statusCode
            )

        } catch (e: Exception) {
            val msg = e.message ?: "Unknown error"
            Log.d(TAG, msg)
            throw when (e) {
                is PluginException -> e
                else -> PluginException.ErrorCode(5000)
            }
        }
    }
}

class SecretsWrapper(private val secretsRef: UpinnSecretsAndroid) {

    fun getSecretBlocking(variable: String, version: String?): SecretsResponse {
        return runBlocking {
            secretsRef.get_secret(variable, version)
        }
    }

    fun getSecretsParallel(variables: List<String>, iterations: Int = 100) {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            repeat(iterations) { iteration ->
                val deferredCalls = variables.map { variable ->
                    async {
                        try {
                            val result = secretsRef.get_secret(variable, null)
                            if (result.statusCode == 200L) {
                                println("$variable -> ${result.secretValue}")
                                "$variable = ${result.secretValue}"
                            } else {
                                "$variable Error: ${result.statusCode}"
                            }
                        } catch (e: Exception) {
                            "$variable Exception: ${e.message}"
                        }
                    }
                }
                val resultList = deferredCalls.awaitAll()
                println("Iteración $iteration completada: $resultList")
            }

            withContext(Dispatchers.Main) {
                // UI toast si estás en Android
                // Toast.makeText(context, "Todas las iteraciones completadas", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

