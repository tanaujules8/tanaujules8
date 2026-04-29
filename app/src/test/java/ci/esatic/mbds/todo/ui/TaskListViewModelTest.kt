package ci.esatic.mbds.todo.ui

import app.cash.turbine.test
import ci.esatic.mbds.todo.data.DataSourceType
import ci.esatic.mbds.todo.data.Task
import ci.esatic.mbds.todo.data.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TaskListViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun searchUpdatesVisibleTasks() = runTest {
        val viewModel = TaskListViewModel(FakeRepository())

        viewModel.uiState.test {
            awaitItem()
            viewModel.search("room")
            dispatcher.scheduler.advanceUntilIdle()
            val item = awaitItem()
            assertEquals("Room", item.tasks.single().title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeRepository : TaskRepository {
        private val tasks = MutableStateFlow(
            listOf(
                Task(id = "1", title = "Room", description = "Local database"),
                Task(id = "2", title = "Firestore", description = "Remote database"),
            ),
        )
        override val selectedSource: StateFlow<DataSourceType> = MutableStateFlow(DataSourceType.IN_MEMORY)

        override fun observeTasks(query: String): Flow<List<Task>> =
            tasks.map { list -> list.filter { it.title.contains(query, ignoreCase = true) } }

        override suspend fun getTask(id: String): Task? = tasks.value.firstOrNull { it.id == id }
        override suspend fun save(task: Task) {}
        override suspend fun delete(id: String) {}
        override suspend fun clearCompleted() {}
        override fun useSource(type: DataSourceType) {}
    }
}
