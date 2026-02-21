package com.theveloper.pixelplay.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.theveloper.pixelplay.data.gdrive.GDriveRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class GDriveSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: GDriveRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("GDriveSyncWorker: Starting sync...")
        if (!repository.isLoggedIn) {
            Timber.d("GDriveSyncWorker: Not logged in, skipping.")
            return Result.success()
        }

        return try {
            val result = repository.syncAllFoldersAndSongs()
            if (result.isSuccess) {
                val data = result.getOrNull()
                Timber.d("GDriveSyncWorker: Sync complete. Folders: ${data?.folderCount}, Songs: ${data?.syncedSongCount}")
                Result.success()
            } else {
                Timber.e(result.exceptionOrNull(), "GDriveSyncWorker: Sync failed")
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "GDriveSyncWorker: Unexpected error")
            Result.failure()
        }
    }
}
