package ci.esatic.mbds.todo.data

import android.content.Context

object TaskRepositoryFactory {
    fun create(context: Context): TaskRepository {
        val database = AppDatabase.create(context.applicationContext)
        return TaskRepositoryImpl(
            sources = mapOf(
                DataSourceType.IN_MEMORY to InMemoryTaskDataSource(sampleTasks()),
                DataSourceType.ROOM to RoomTaskDataSource(database.taskDao()),
                DataSourceType.FIRESTORE to FirestoreTaskDataSource(),
            ),
            initialSource = DataSourceType.IN_MEMORY,
        )
    }

    private fun sampleTasks(): List<Task> = listOf(
        Task(
            id = "welcome",
            title = "Preparer le projet final",
            description = "Architecture MVVM, data module, recherche, rappels et tests.",
            tags = listOf("android", "todo"),
            priority = Priority.HIGH,
        ),
        Task(
            id = "map-demo",
            title = "Presenter la carte",
            description = "Tache exemple avec coordonnees GPS.",
            latitude = 5.3600,
            longitude = -4.0083,
            tags = listOf("gps"),
        ),
    )
}

