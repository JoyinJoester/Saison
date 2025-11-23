package takagi.ru.saison.ui.navigation

sealed class Screen(val route: String) {
    object Course : Screen("course")
    object AllCourses : Screen("all_courses")
    object Calendar : Screen("calendar")
    object ImportPreview : Screen("import_preview?uri={uri}&semesterId={semesterId}") {
        fun createRoute(uri: String, semesterId: Long) = "import_preview?uri=$uri&semesterId=$semesterId"
    }
    object Tasks : Screen("tasks")
    object TaskPreview : Screen("task_preview/{taskId}") {
        fun createRoute(taskId: Long) = "task_preview/$taskId"
    }
    object TaskEdit : Screen("task_edit/{taskId}") {
        fun createRoute(taskId: Long) = "task_edit/$taskId"
    }
    // 保留旧路由以兼容现有代码
    object TaskDetail : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: Long) = "task_detail/$taskId"
    }
    object Events : Screen("events")
    object EventDetail : Screen("event_detail/{eventId}") {
        fun createRoute(eventId: Long) = "event_detail/$eventId"
    }
    object Pomodoro : Screen("pomodoro")
    object Metronome : Screen("metronome")
    object Settings : Screen("settings")
    object BottomNavSettings : Screen("bottom_nav_settings")
    object Routine : Screen("routine")
    object RoutineDetail : Screen("routine_detail/{taskId}") {
        fun createRoute(taskId: Long) = "routine_detail/$taskId"
    }
    object Subscription : Screen("subscription")
    object SubscriptionDetail : Screen("subscription_detail/{subscriptionId}") {
        fun createRoute(subscriptionId: Long) = "subscription_detail/$subscriptionId"
    }
}
