package ci.esatic.mbds.todo.data

import kotlinx.coroutines.flow.Flow

internal class RoomTaskDataSource(private val dao: TaskDao) : TaskDataSource {
    override fun observeTasks(): Flow<List<Task>> = dao.observeTasks()

    override suspend fun getTask(id: String): Task? = dao.getTask(id)

    override suspend fun upsert(task: Task) = dao.upsert(task)

    override suspend fun delete(id: String) = dao.delete(id)

    override suspend fun clearCompleted() = dao.clearCompleted()
}

