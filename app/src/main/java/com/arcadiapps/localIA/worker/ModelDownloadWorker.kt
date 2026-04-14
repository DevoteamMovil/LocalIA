package com.arcadiapps.localIA.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
        const val KEY_MODEL_ID = "model_id"
        const val KEY_MODEL_URL = "model_url"
        const val KEY_MODEL_FILE = "model_file"
        const val CHANNEL_ID = "model_download"
        const val NOTIFICATION_ID = 1001

        fun buildRequest(modelId: String, url: String, fileName: String): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setInputData(workDataOf(
                    KEY_MODEL_ID to modelId,
                    KEY_MODEL_URL to url,
                    KEY_MODEL_FILE to fileName
                ))
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())
                .addTag(modelId)
                .build()
    }

    override suspend fun doWork(): Result {
        val modelId = inputData.getString(KEY_MODEL_ID) ?: return Result.failure()
        val url = inputData.getString(KEY_MODEL_URL) ?: return Result.failure()
        val fileName = inputData.getString(KEY_MODEL_FILE) ?: return Result.failure()

        createNotificationChannel()
        setForeground(createForegroundInfo(0))

        return try {
            val modelsDir = File(applicationContext.filesDir, "models")
            modelsDir.mkdirs()
            val outputFile = File(modelsDir, fileName)

            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                repository.updateDownloadProgress(modelId, 0, ModelStatus.ERROR)
                return Result.failure()
            }

            val body = response.body ?: return Result.failure()
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            FileOutputStream(outputFile).use { fos ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        fos.write(buffer, 0, bytes)
                        downloadedBytes += bytes
                        val progress = if (totalBytes > 0)
                            ((downloadedBytes * 100) / totalBytes).toInt() else 0
                        repository.updateDownloadProgress(modelId, progress, ModelStatus.DOWNLOADING)
                        setForeground(createForegroundInfo(progress))
                    }
                }
            }

            repository.updateDownloadProgress(modelId, 100, ModelStatus.READY)
            Result.success()
        } catch (e: Exception) {
            repository.updateDownloadProgress(modelId, 0, ModelStatus.ERROR)
            Result.failure()
        }
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Descargando modelo")
            .setContentText("$progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Descarga de modelos", NotificationManager.IMPORTANCE_LOW
        )
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }
}
