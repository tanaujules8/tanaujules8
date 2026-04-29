package ci.esatic.mbds.todo.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class TaskRepositoryImpl(
    private val sources: Map<DataSourceType, TaskDataSource>,
    initialSource: DataSourceType = DataSourceType.IN_MEMORY,
) : TaskRepository {
    private val sourceType = MutableStateFlow(initialSource)

    override val selectedSource: StateFlow<DataSourceType> = sourceType

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeTasks(query: String): Flow<List<Task>> =
        sourceType
            .flatMapLatest { currentSource().observeTasks() }
            .map { tasks ->
                val normalizedQuery = query.trim().lowercase()
                val filtered = if (normalizedQuery.isBlank()) {
                    tasks
                } else {
                    tasks.filter { task ->
                        task.title.lowercase().contains(normalizedQuery) ||
                            task.description.lowercase().contains(normalizedQuery) ||
                            task.tags.any { it.lowercase().contains(normalizedQuery) }
                    }
                }
                filtered.sortedWith(compareBy<Task> { it.isCompleted }.thenByDescending { it.createdAt })
            }

    override suspend fun getTask(id: String): Task? = currentSource().getTask(id)

    override suspend fun save(task: Task) = currentSource().upsert(task.copy(updatedAt = System.currentTimeMillis()))

    override suspend fun delete(id: String) = currentSource().delete(id)

    override suspend fun clearCompleted() = currentSource().clearCompleted()

    override fun useSource(type: DataSourceType) {
        sourceType.value = type
    }

    private fun currentSource(): TaskDataSource =
        sources.getValue(sourceType.value)
}
