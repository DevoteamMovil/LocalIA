package com.arcadiapps.localIA.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
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

@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ModelRepository,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, workerParams) {

    companion object {
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

    override suspend fun doWork(): Result {
        val modelId  = inputData.getString(KEY_MODEL_ID)   ?: return Result.failure()
        val url      = inputData.getString(KEY_MODEL_URL)  ?: return Result.failure()
        val fileName = inputData.getString(KEY_MODEL_FILE) ?: return Result.failure()

        createNotificationChannel()

        // Foreground info con tipo correcto según versión de Android
        try {
            setForeground(createForegroundInfo(0))
        } catch (e: Exception) {
            // Si falla el foreground, continuar igualmente en background
        }

        return try {
            val modelsDir = File(applicationContext.filesDir, "models")
            modelsDir.mkdirs()
            val outputFile = File(modelsDir, fileName)

            // Si ya existe y está completo, marcar como listo
            if (outputFile.exists() && outputFile.length() > 0) {
                repository.updateDownloadProgress(modelId, 100, ModelStatus.READY)
                return Result.success()
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "LocalIA-Android/1.0")
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                repository.updateDownloadProgress(modelId, 0, ModelStatus.ERROR)
                return Result.failure(
                    workDataOf("error" to "HTTP ${response.code}: ${response.message}")
                )
            }

            val body = response.body ?: run {
                repository.updateDownloadProgress(modelId, 0, ModelStatus.ERROR)
                return Result.failure()
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L
            var lastReportedProgress = -1

            FileOutputStream(outputFile).use { fos ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(16 * 1024) // 16KB buffer
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        fos.write(buffer, 0, bytes)
                        downloadedBytes += bytes

                        val progress = if (totalBytes > 0)
                            ((downloadedBytes * 100) / totalBytes).toInt()
                        else -1

                        // Actualizar solo cada 2% para no saturar la DB
                        if (progress != lastReportedProgress && progress % 2 == 0) {
                            lastReportedProgress = progress
                            repository.updateDownloadProgress(
                                modelId,
                                if (progress < 0) 0 else progress,
                                ModelStatus.DOWNLOADING
                            )
                            try { setForeground(createForegroundInfo(if (progress < 0) 0 else progress)) }
                            catch (e: Exception) { /* ignorar */ }
                        }
                    }
                }
            }

            repository.updateDownloadProgress(modelId, 100, ModelStatus.READY)
            Result.success()

        } catch (e: Exception) {
            repository.updateDownloadProgress(modelId, 0, ModelStatus.ERROR)
            Result.failure(workDataOf("error" to (e.message ?: "Error desconocido")))
        }
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Descargando modelo")
            .setContentText(if (progress > 0) "$progress%" else "Iniciando descarga…")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setSilent(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Descarga de modelos",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
        }
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }
}
