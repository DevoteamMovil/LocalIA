package com.arcadiapps.localIA.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.arcadiapps.localIA.data.model.ModelStatus
import com.arcadiapps.localIA.data.repository.ModelRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ModelRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "ModelDownload"
        const val KEY_MODEL_ID   = "model_id"
        const val KEY_MODEL_URL  = "model_url"
        const val KEY_MODEL_FILE = "model_file"
        const val CHANNEL_ID     = "model_download"
        const val NOTIFICATION_ID = 1001

        fun buildRequest(modelId: String, url: String, fileName: String): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setInputData(workDataOf(
                    KEY_MODEL_ID   to modelId,
                    KEY_MODEL_URL  to url,
                    KEY_MODEL_FILE to fileName
                ))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag(modelId)
                .build()
    }

    // Cliente HTTP propio con timeouts generosos para archivos grandes
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)   // sin timeout de lectura
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    override suspend fun doWork(): Result {
        val modelId  = inputData.getString(KEY_MODEL_ID)   ?: return Result.failure()
        val url      = inputData.getString(KEY_MODEL_URL)  ?: return Result.failure()
        val fileName = inputData.getString(KEY_MODEL_FILE) ?: return Result.failure()

        Log.d(TAG, "Iniciando descarga: $fileName desde $url")

        createNotificationChannel()
        try { setForeground(createForegroundInfo(0)) } catch (e: Exception) {
            Log.w(TAG, "No se pudo iniciar foreground: ${e.message}")
        }

        return try {
            val modelsDir = File(applicationContext.filesDir, "models")
            modelsDir.mkdirs()
            val outputFile = File(modelsDir, fileName)

            if (outputFile.exists() && outputFile.length() > 1024) {
                Log.d(TAG, "Archivo ya existe (${outputFile.length()} bytes), marcando como listo")
                repository.updateDownloadProgress(modelId, 100, ModelStatus.READY)
                return Result.success()
            }

            Log.d(TAG, "Conectando a: $url")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android)")
                .build()

            val response = httpClient.newCall(request).execute()
            Log.d(TAG, "Respuesta HTTP: ${response.code} ${response.message}")
            Log.d(TAG, "Content-Type: ${response.header("Content-Type")}")
            Log.d(TAG, "Content-Length: ${response.header("Content-Length")}")

            if (!response.isSuccessful) {
                val errorMsg = "HTTP ${response.code}: ${response.message}"
                Log.e(TAG, "Error HTTP: $errorMsg")
                repository.updateDownloadProgress(modelId, 0, ModelStatus.ERROR)
                return Result.failure(workDataOf("error" to errorMsg))
            }

            val body = response.body ?: run {
                Log.e(TAG, "Body vacío")
                repository.updateDownloadProgress(modelId, 0, ModelStatus.ERROR)
                return Result.failure(workDataOf("error" to "Respuesta vacía del servidor"))
            }

            val totalBytes = body.contentLength()
            Log.d(TAG, "Tamaño total: $totalBytes bytes")
            var downloadedBytes = 0L
            var lastReportedProgress = -1

            FileOutputStream(outputFile).use { fos ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(32 * 1024)
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        fos.write(buffer, 0, bytes)
                        downloadedBytes += bytes

                        val progress = if (totalBytes > 0)
                            ((downloadedBytes * 100) / totalBytes).toInt() else 0

                        if (progress != lastReportedProgress && (progress % 2 == 0 || progress == 1)) {
                            lastReportedProgress = progress
                            Log.d(TAG, "Progreso: $progress% ($downloadedBytes/$totalBytes)")
                            repository.updateDownloadProgress(modelId, progress, ModelStatus.DOWNLOADING)
                            try { setForeground(createForegroundInfo(progress)) } catch (e: Exception) { }
                        }
                    }
                }
            }

            Log.d(TAG, "Descarga completada: ${outputFile.length()} bytes")
            repository.updateDownloadProgress(modelId, 100, ModelStatus.READY)
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Excepción durante descarga", e)
            repository.updateDownloadProgress(modelId, 0, ModelStatus.ERROR)
            Result.failure(workDataOf("error" to (e.javaClass.simpleName + ": " + e.message)))
        }
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Descargando modelo")
            .setContentText(if (progress > 0) "$progress%" else "Iniciando…")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setSilent(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Descarga de modelos", NotificationManager.IMPORTANCE_LOW)
            .apply { setShowBadge(false) }
        applicationContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
