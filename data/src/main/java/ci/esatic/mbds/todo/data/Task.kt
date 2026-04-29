package ci.esatic.mbds.todo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val description: String = "",
    val isCompleted: Boolean = false,
    val dueDate: Long? = null,
    val reminderAt: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoUris: List<String> = emptyList(),
    val priority: Priority = Priority.MEDIUM,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
}

enum class DataSourceType {
    IN_MEMORY,
    ROOM,
    FIRESTORE,
}
