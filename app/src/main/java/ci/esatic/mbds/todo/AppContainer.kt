package ci.esatic.mbds.todo

import android.content.Context
import ci.esatic.mbds.todo.data.TaskRepository
import ci.esatic.mbds.todo.data.TaskRepositoryFactory

object AppContainer {
    @Volatile private var repository: TaskRepository? = null

    fun repository(context: Context): TaskRepository =
        repository ?: synchronized(this) {
            repository ?: TaskRepositoryFactory.create(context).also { repository = it }
        }
}

