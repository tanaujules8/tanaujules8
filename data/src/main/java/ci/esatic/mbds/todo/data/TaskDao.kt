package ci.esatic.mbds.todo.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun observeTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTask(id: String): Task?

    @Upsert
    suspend fun upsert(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun clearCompleted()
}

