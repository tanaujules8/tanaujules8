package ci.esatic.mbds.todo

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ci.esatic.mbds.todo.data.DataSourceType
import ci.esatic.mbds.todo.data.Priority
import ci.esatic.mbds.todo.data.Task
import ci.esatic.mbds.todo.ui.TaskListViewModel
import ci.esatic.mbds.todo.ui.TaskListViewModelFactory
import ci.esatic.mbds.todo.ui.theme.ProjetFinalTodoTheme
import ci.esatic.mbds.todo.worker.ReminderWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleReminderWorker()
        setContent {
            ProjetFinalTodoTheme {
                TodoApp(initialTaskId = intent?.data?.lastPathSegment)
            }
        }
    }

    private fun scheduleReminderWorker() {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "task-reminders",
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}

@Composable
fun TodoApp(initialTaskId: String?) {
    val context = LocalContext.current
    val viewModel: TaskListViewModel = viewModel(
        factory = TaskListViewModelFactory(AppContainer.repository(context)),
    )
    val navController = rememberNavController()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
        LaunchedEffect(Unit) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }

    LaunchedEffect(initialTaskId) {
        if (!initialTaskId.isNullOrBlank()) navController.navigate("detail/$initialTaskId")
    }

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            TaskListScreen(
                viewModel = viewModel,
                onOpenTask = { navController.navigate("detail/$it") },
                onAddTask = { navController.navigate("detail/new") },
                onMap = { navController.navigate("map") },
            )
        }
        composable(
            "detail/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
        ) { entry ->
            TaskDetailScreen(
                viewModel = viewModel,
                taskId = entry.arguments?.getString("taskId").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
        composable("map") {
            TaskMapScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskListViewModel,
    onOpenTask: (String) -> Unit,
    onAddTask: () -> Unit,
    onMap: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var sourceMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = onMap) { Text(stringResource(R.string.map)) }
                    TextButton(onClick = { sourceMenuOpen = true }) { Text(state.selectedSource.name) }
                    DropdownMenu(expanded = sourceMenuOpen, onDismissRequest = { sourceMenuOpen = false }) {
                        DataSourceType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    viewModel.useSource(type)
                                    sourceMenuOpen = false
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = { Button(onClick = onAddTask) { Text(stringResource(R.string.add)) } },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::search,
                label = { Text(stringResource(R.string.search)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
                items(state.tasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        onToggle = { viewModel.toggle(task) },
                        onOpen = { onOpenTask(task.id) },
                        onDelete = { viewModel.delete(task.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun TaskRow(task: Task, onToggle: () -> Unit, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() })
                Column(Modifier.weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.titleMedium)
                    Text(task.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = true, onClick = {}, label = { Text(task.priority.name) })
                if (task.latitude != null && task.longitude != null) {
                    FilterChip(selected = true, onClick = {}, label = { Text(stringResource(R.string.gps)) })
                }
                if (task.photoUris.isNotEmpty()) {
                    FilterChip(selected = true, onClick = {}, label = { Text("${task.photoUris.size} photo") })
                }
            }
            Row {
                TextButton(onClick = onOpen) { Text(stringResource(R.string.edit)) }
                TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
            }
        }
    }
}

@Composable
fun TaskDetailScreen(viewModel: TaskListViewModel, taskId: String, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val existing = state.tasks.firstOrNull { it.id == taskId }
    var title by remember(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var description by remember(existing?.id) { mutableStateOf(existing?.description.orEmpty()) }
    var latitude by remember(existing?.id) { mutableStateOf(existing?.latitude?.toString().orEmpty()) }
    var longitude by remember(existing?.id) { mutableStateOf(existing?.longitude?.toString().orEmpty()) }
    var dueDate by remember(existing?.id) { mutableStateOf(existing?.dueDate?.let(::formatDate).orEmpty()) }
    var tags by remember(existing?.id) { mutableStateOf(existing?.tags?.joinToString(", ").orEmpty()) }
    var priority by remember(existing?.id) { mutableStateOf(existing?.priority ?: Priority.MEDIUM) }
    var photoUris by remember(existing?.id) { mutableStateOf(existing?.photoUris.orEmpty()) }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        photoUris = photoUris + uris.map { it.toString() }
    }

    Scaffold(topBar = { DetailTopBar(onBack) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.title)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it }, label = { Text(stringResource(R.string.description)) }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            OutlinedTextField(dueDate, { dueDate = it }, label = { Text(stringResource(R.string.due_date_hint)) }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(latitude, { latitude = it }, label = { Text(stringResource(R.string.latitude)) }, modifier = Modifier.weight(1f))
                OutlinedTextField(longitude, { longitude = it }, label = { Text(stringResource(R.string.longitude)) }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(tags, { tags = it }, label = { Text(stringResource(R.string.tags)) }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { item ->
                    FilterChip(selected = priority == item, onClick = { priority = item }, label = { Text(item.name) })
                }
            }
            TextButton(onClick = { galleryLauncher.launch("image/*") }) {
                Text(stringResource(R.string.attach_photos, photoUris.size))
            }
            Row {
                Button(
                    enabled = title.isNotBlank(),
                    onClick = {
                        viewModel.save(
                            Task(
                                id = existing?.id ?: UUID.randomUUID().toString(),
                                title = title.trim(),
                                description = description.trim(),
                                dueDate = parseDate(dueDate),
                                reminderAt = parseDate(dueDate)?.minus(TimeUnit.HOURS.toMillis(1)),
                                latitude = latitude.toDoubleOrNull(),
                                longitude = longitude.toDoubleOrNull(),
                                photoUris = photoUris,
                                priority = priority,
                                tags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                isCompleted = existing?.isCompleted ?: false,
                                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                            ),
                        )
                        onBack()
                    },
                ) { Text(stringResource(R.string.save)) }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onBack) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.task_detail)) },
        navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskMapScreen(viewModel: TaskListViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val locatedTasks = state.tasks.filter { it.latitude != null && it.longitude != null }
    Scaffold(topBar = { DetailTopBar(onBack) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.located_tasks), style = MaterialTheme.typography.titleLarge)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(locatedTasks, key = { it.id }) { task ->
                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Text(task.title, style = MaterialTheme.typography.titleMedium)
                            Text("${task.latitude}, ${task.longitude}")
                        }
                    }
                }
            }
        }
    }
}

private fun parseDate(value: String): Long? =
    runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value)?.time }.getOrNull()

private fun formatDate(value: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(value))

