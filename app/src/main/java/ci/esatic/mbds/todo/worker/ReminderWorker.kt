package ci.esatic.mbds.todo.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ci.esatic.mbds.todo.AppContainer
import ci.esatic.mbds.todo.R
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val soon = now + TimeUnit.HOURS.toMillis(24)
        val tasks = AppContainer.repository(applicationContext).observeTasks().first()
            .filter { !it.isCompleted && it.dueDate != null && it.dueDate in now..soon }

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Task reminders", NotificationManager.IMPORTANCE_DEFAULT))
        }
        tasks.forEach { task ->
            manager.notify(
                task.id.hashCode(),
                NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(applicationContext.getString(R.string.notification_title))
                    .setContentText(task.title)
                    .setAutoCancel(true)
                    .build(),
            )
        }
        return Result.success()
    }

    private companion object {
        const val CHANNEL_ID = "task-reminders"
    }
}

