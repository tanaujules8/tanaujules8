package ci.esatic.mbds.todo.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class InMemoryTaskDataSource(initialTasks: List<Task> = emptyList()) : TaskDataSource {
    private val tasks = MutableStateFlow(initialTasks.associateBy { it.id })

    override fun observeTasks(): Flow<List<Task>> = tasks.map { it.values.toList() }

    override suspend fun getTask(id: String): Task? = tasks.value[id]

    override suspend fun upsert(task: Task) {
        tasks.update { it + (task.id to task) }
    }

    override suspend fun delete(id: String) {
        tasks.update { it - id }
    }

    override suspend fun clearCompleted() {
        tasks.update { current -> current.filterValues { !it.isCompleted } }
    }
}

