package ci.esatic.mbds.todo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ci.esatic.mbds.todo.data.DataSourceType
import ci.esatic.mbds.todo.data.Task
import ci.esatic.mbds.todo.data.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val query: String = "",
    val selectedSource: DataSourceType = DataSourceType.IN_MEMORY,
)

class TaskListViewModel(private val repository: TaskRepository) : ViewModel() {
    private val query = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TaskListUiState> =
        combine(
            query.flatMapLatest { repository.observeTasks(it) },
            query,
            repository.selectedSource,
        ) { tasks, currentQuery, source ->
            TaskListUiState(tasks = tasks, query = currentQuery, selectedSource = source)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskListUiState())

    fun search(value: String) {
        query.value = value
    }

    fun useSource(type: DataSourceType) {
        repository.useSource(type)
    }

    fun save(task: Task) = viewModelScope.launch {
        repository.save(task)
    }

    fun toggle(task: Task) = viewModelScope.launch {
        repository.save(task.copy(isCompleted = !task.isCompleted))
    }

    fun delete(id: String) = viewModelScope.launch {
        repository.delete(id)
    }
}

class TaskListViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TaskListViewModel(repository) as T
    }
}

