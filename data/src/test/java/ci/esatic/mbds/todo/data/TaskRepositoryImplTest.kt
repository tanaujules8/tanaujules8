package ci.esatic.mbds.todo.data

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskRepositoryImplTest {
    @Test
    fun repositoryFiltersTasksByTitleDescriptionAndTags() = runTest {
        val repository = TaskRepositoryImpl(
            sources = mapOf(
                DataSourceType.IN_MEMORY to InMemoryTaskDataSource(
                    listOf(
                        Task(id = "1", title = "Acheter du pain", description = "Boulangerie"),
                        Task(id = "2", title = "Revision Android", description = "MVVM", tags = listOf("cours")),
                    ),
                ),
            ),
        )

        repository.observeTasks("android").test {
            assertEquals(listOf("Revision Android"), awaitItem().map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun repositoryCanSwitchDataSource() = runTest {
        val repository = TaskRepositoryImpl(
            sources = mapOf(
                DataSourceType.IN_MEMORY to InMemoryTaskDataSource(listOf(Task(id = "mem", title = "Memoire", description = ""))),
                DataSourceType.ROOM to InMemoryTaskDataSource(listOf(Task(id = "room", title = "Room", description = ""))),
            ),
        )

        repository.useSource(DataSourceType.ROOM)

        repository.observeTasks().test {
            assertEquals(listOf("Room"), awaitItem().map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }
}
