package takagi.ru.saison.ui.navigation

import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

@Composable
fun SaisonNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Tasks.route,
        modifier = modifier
    ) {
        composable(Screen.Course.route) {
            takagi.ru.saison.ui.screens.course.CourseScreen(
                onCourseClick = { courseId ->
                    // TODO: 导航到课程详情
                },
                onNavigateToImportPreview = { uri, semesterId ->
                    val encodedUri = Uri.encode(uri.toString())
                    navController.navigate(Screen.ImportPreview.createRoute(encodedUri, semesterId))
                },
                onNavigateToAllCourses = {
                    navController.navigate(Screen.AllCourses.route)
                }
            )
        }

        composable(Screen.AllCourses.route) {
            takagi.ru.saison.ui.screens.course.AllCoursesScreen(
                onNavigateBack = { navController.popBackStack() },
                onCourseClick = { courseId ->
                    // TODO: 导航到课程详情或编辑
                }
            )
        }

        composable(Screen.Calendar.route) {
            takagi.ru.saison.ui.screens.calendar.CalendarScreen(
                onCourseClick = { courseId ->
                    // TODO: Show course details
                },
                onTaskClick = { taskId ->
                    navController.navigate(Screen.TaskPreview.createRoute(taskId))
                },
                onEventClick = { eventId ->
                    navController.navigate(Screen.EventDetail.createRoute(eventId))
                }
            )
        }
        
        // 课程表导入预览页面
        composable(
            route = Screen.ImportPreview.route,
            arguments = listOf(
                navArgument("uri") { 
                    type = NavType.StringType
                    nullable = false
                },
                navArgument("semesterId") { 
                    type = NavType.LongType
                    defaultValue = 0L // 不再使用，保留用于兼容性
                }
            )
        ) { backStackEntry ->
            val uriString = backStackEntry.arguments?.getString("uri") ?: return@composable
            // 将Uri保存到ViewModel的SavedStateHandle中
            val viewModel: takagi.ru.saison.ui.screens.course.ImportPreviewViewModel = hiltViewModel()
            
            takagi.ru.saison.ui.screens.course.ImportPreviewScreen(
                onNavigateBack = {
                    if (navController.currentBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
                onImportSuccess = { semesterId ->
                    if (navController.currentBackStackEntry != null) {
                        navController.popBackStack()
                    }
                    // 导航到新创建的学期
                    navController.navigate(Screen.Course.route) {
                        popUpTo(Screen.Settings.route) { inclusive = false }
                    }
                },
                viewModel = viewModel
            )
        }
        
        composable(Screen.Tasks.route) {
            takagi.ru.saison.ui.screens.task.TaskListScreen(
                onTaskClick = { taskId ->
                    try {
                        // 点击卡片导航到预览页面
                        navController.navigate(Screen.TaskPreview.createRoute(taskId))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                onTaskEdit = { taskId ->
                    try {
                        // 侧滑编辑按钮导航到编辑页面
                        navController.navigate(Screen.TaskEdit.createRoute(taskId))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                onNavigateToAddTask = {
                    try {
                        navController.navigate(Screen.TaskDetail.createRoute(0L))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                onNavigateToEvents = {
                    try {
                        navController.navigate(Screen.Events.route)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                onNavigateToRoutine = {
                    try {
                        navController.navigate(Screen.Routine.route)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            )
        }
        
        // 任务预览页面（点击卡片进入）
        composable(
            route = Screen.TaskPreview.route,
            arguments = listOf(
                navArgument("taskId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
            takagi.ru.saison.ui.screens.task.TaskPreviewScreen(
                taskId = taskId,
                onNavigateBack = {
                    if (navController.currentBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
                onNavigateToEdit = { editTaskId ->
                    navController.navigate(Screen.TaskEdit.createRoute(editTaskId))
                }
            )
        }
        
        // 任务编辑页面（侧滑编辑按钮进入）
        composable(
            route = Screen.TaskEdit.route,
            arguments = listOf(
                navArgument("taskId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
            takagi.ru.saison.ui.screens.task.TaskDetailScreen(
                taskId = taskId,
                onNavigateBack = {
                    if (navController.currentBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            )
        }
        
        // 保留旧路由以兼容现有代码
        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(
                navArgument("taskId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
            takagi.ru.saison.ui.screens.task.TaskDetailScreen(
                taskId = taskId,
                onNavigateBack = { 
                    if (navController.currentBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            )
        }
        
        composable(Screen.Pomodoro.route) {
            takagi.ru.saison.ui.screens.pomodoro.PomodoroScreen()
        }
        
        composable(Screen.Metronome.route) {
            takagi.ru.saison.ui.screens.metronome.MetronomeScreen()
        }
        
        composable(Screen.Subscription.route) {
            takagi.ru.saison.ui.screens.subscription.SubscriptionScreen(
                onNavigateBack = {
                    if (navController.currentBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            )
        }
        
        composable(Screen.Settings.route) {
            takagi.ru.saison.ui.screens.settings.SettingsScreen(
                onNavigateBack = { 
                    if (navController.currentBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
                onNavigateToBottomNavSettings = {
                    navController.navigate(Screen.BottomNavSettings.route)
                }
            )
        }
        
        composable(Screen.BottomNavSettings.route) {
            takagi.ru.saison.ui.screens.settings.BottomNavSettingsScreen(
                onNavigateBack = { 
                    if (navController.currentBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            )
        }
        
        composable(Screen.Events.route) {
            takagi.ru.saison.ui.screens.event.EventScreen(
                onEventClick = { eventId ->
                    navController.navigate(Screen.EventDetail.createRoute(eventId))
                },
                onNavigateToTasks = {
                    navController.navigate(Screen.Tasks.route)
                },
                onNavigateToRoutine = {
                    navController.navigate(Screen.Routine.route)
                }
            )
        }
        
        composable(
            route = Screen.EventDetail.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getLong("eventId") ?: 0L
            takagi.ru.saison.ui.screens.event.EventDetailScreen(
                eventId = eventId,
                onNavigateBack = { 
                    if (navController.currentBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            )
        }
        
        composable(Screen.Routine.route) {
            takagi.ru.saison.ui.screens.routine.RoutineScreen(
                onNavigateToDetail = { taskId ->
                    navController.navigate(Screen.RoutineDetail.createRoute(taskId))
                },
                onNavigateToCreate = {
                    // 创建任务通过 BottomSheet 实现，不需要导航
                },
                onNavigateToTasks = {
                    navController.navigate(Screen.Tasks.route)
                },
                onNavigateToEvents = {
                    navController.navigate(Screen.Events.route)
                }
            )
        }
        
        composable(
            route = Screen.RoutineDetail.route,
            arguments = listOf(
                navArgument("taskId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
            takagi.ru.saison.ui.screens.routine.RoutineDetailScreen(
                onNavigateBack = { 
                    if (navController.currentBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
                onNavigateToEdit = { editTaskId ->
                    // 编辑任务通过 BottomSheet 实现，不需要导航
                }
            )
        }
    }
}
