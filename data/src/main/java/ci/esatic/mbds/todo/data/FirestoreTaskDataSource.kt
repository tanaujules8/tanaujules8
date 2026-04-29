package ci.esatic.mbds.todo.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

internal class FirestoreTaskDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : TaskDataSource {
    private val collection = firestore.collection("tasks")

    override fun observeTasks(): Flow<List<Task>> = callbackFlow {
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.documents.orEmpty().mapNotNull { it.toObject(Task::class.java) })
        }
        awaitClose { listener.remove() }
    }

    override suspend fun getTask(id: String): Task? =
        collection.document(id).get().await().toObject(Task::class.java)

    override suspend fun upsert(task: Task) {
        collection.document(task.id).set(task).await()
    }

    override suspend fun delete(id: String) {
        collection.document(id).delete().await()
    }

    override suspend fun clearCompleted() {
        collection.whereEqualTo("completed", true).get().await().documents.forEach {
            it.reference.delete().await()
        }
    }
}

