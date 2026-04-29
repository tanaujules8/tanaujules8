package ci.esatic.mbds.todo.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TaskRepository {
    val selectedSource: StateFlow<DataSourceType>
    fun observeTasks(query: String = ""): Flow<List<Task>>
    suspend fun getTask(id: String): Task?
    suspend fun save(task: Task)
    suspend fun delete(id: String)
    suspend fun clearCompleted()
    fun useSource(type: DataSourceType)
}

