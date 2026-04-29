package ci.esatic.mbds.todo.data

import kotlinx.coroutines.flow.Flow

interface TaskDataSource {
    fun observeTasks(): Flow<List<Task>>
    suspend fun getTask(id: String): Task?
    suspend fun upsert(task: Task)
    suspend fun delete(id: String)
    suspend fun clearCompleted()
}

