package upinn.tech.upinnsecretsandroid

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import upinn.tech.upinnsecretsandroid.PluginException
import java.io.InputStream
import java.util.concurrent.CompletableFuture
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

sealed class SecretResponseWrapper {
    data class Success(val secret: SecretsResponse) : SecretResponseWrapper()
    data class Error(val code: Long, val message: String) : SecretResponseWrapper()
}

class SecretsWrapper(private val secretsRef: UpinnSecretsAndroid) {

    // Llama a tu suspend fun y devuelve un CompletableFuture
    @RequiresApi(Build.VERSION_CODES.N)
    fun getSecretAsync(variable: String, version: String?): CompletableFuture<SecretResponseWrapper> {
        val future = CompletableFuture<SecretResponseWrapper>()

        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Llamada a tu función suspend original
                val res = secretsRef.get_secret(variable, version)

                // Chequea statusCode y envuelve en SecretResponse
                if (res.statusCode == 200L) {
                    future.complete(SecretResponseWrapper.Success(res))
                } else {
                    future.complete(SecretResponseWrapper.Error(res.statusCode, "Error retrieving secret"))
                }
            } catch (e: Exception) {
                future.complete(SecretResponseWrapper.Error(5000, e.message ?: "Unknown error"))
            }
        }
        return future
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun getSecretsParallel(variables: List<String>): CompletableFuture<List<SecretResponseWrapper>> {
        val future = CompletableFuture<List<SecretResponseWrapper>>()

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val deferreds = variables.map { variable ->
                    async {
                        try {
                            val res = secretsRef.get_secret(variable, null)
                            if (res.statusCode == 200L) SecretResponseWrapper.Success(res)
                            else SecretResponseWrapper.Error(res.statusCode, "Error retrieving secret")
                        } catch (e: Exception) {
                            SecretResponseWrapper.Error(5000, e.message ?: "Unknown error")
                        }
                    }
                }
                val results = deferreds.awaitAll()
                future.complete(results)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }

        return future
    }

}


