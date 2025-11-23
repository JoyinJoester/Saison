package takagi.ru.saison.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import takagi.ru.saison.data.local.database.dao.*
import takagi.ru.saison.data.local.database.entities.*
import takagi.ru.saison.data.local.database.entity.RoutineTaskEntity
import takagi.ru.saison.data.local.database.entity.CheckInRecordEntity

@Database(
    entities = [
        TaskEntity::class,
        TagEntity::class,
        CourseEntity::class,
        PomodoroSessionEntity::class,
        AttachmentEntity::class,
        EventEntity::class,
        RoutineTaskEntity::class,
        CheckInRecordEntity::class,
        SemesterEntity::class,
        SubscriptionEntity::class
    ],
    version = 11,
    exportSchema = true
)
abstract class SaisonDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun tagDao(): TagDao
    abstract fun courseDao(): CourseDao
    abstract fun pomodoroDao(): PomodoroDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun eventDao(): EventDao
    abstract fun routineTaskDao(): RoutineTaskDao
    abstract fun checkInRecordDao(): CheckInRecordDao
    abstract fun semesterDao(): SemesterDao
    abstract fun subscriptionDao(): SubscriptionDao
    
    companion object {
        const val DATABASE_NAME = "saison_database"
        
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add isFavorite column to tasks table with default value
                db.execSQL("ALTER TABLE tasks ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create events table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        eventDate INTEGER NOT NULL,
                        category INTEGER NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        reminderEnabled INTEGER NOT NULL DEFAULT 0,
                        reminderTime INTEGER,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Create indices for events table
                db.execSQL("CREATE INDEX IF NOT EXISTS index_events_eventDate ON events(eventDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_events_category ON events(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_events_isCompleted ON events(isCompleted)")
            }
        }
        
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create routine_tasks table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS routine_tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        icon TEXT,
                        cycle_type TEXT NOT NULL,
                        cycle_config TEXT NOT NULL,
                        is_active INTEGER NOT NULL DEFAULT 1,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Create check_in_records table with foreign key
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS check_in_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        routine_task_id INTEGER NOT NULL,
                        check_in_time INTEGER NOT NULL,
                        note TEXT,
                        cycle_start_date INTEGER NOT NULL,
                        cycle_end_date INTEGER NOT NULL,
                        FOREIGN KEY (routine_task_id) REFERENCES routine_tasks(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Create indices for check_in_records table
                db.execSQL("CREATE INDEX IF NOT EXISTS index_check_in_records_routine_task_id ON check_in_records(routine_task_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_check_in_records_check_in_time ON check_in_records(check_in_time)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_check_in_records_cycle_start_date_cycle_end_date ON check_in_records(cycle_start_date, cycle_end_date)")
            }
        }
        
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add duration_minutes column to routine_tasks table
                db.execSQL("ALTER TABLE routine_tasks ADD COLUMN duration_minutes INTEGER DEFAULT NULL")
            }
        }
        
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to pomodoro_sessions table for routine integration
                db.execSQL("ALTER TABLE pomodoro_sessions ADD COLUMN routineTaskId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE pomodoro_sessions ADD COLUMN actualDuration INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE pomodoro_sessions ADD COLUMN isEarlyFinish INTEGER NOT NULL DEFAULT 0")
                
                // Create index for routineTaskId
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pomodoro_sessions_routineTaskId ON pomodoro_sessions(routineTaskId)")
            }
        }
        
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to courses table for period-based scheduling
                db.execSQL("ALTER TABLE courses ADD COLUMN periodStart INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE courses ADD COLUMN periodEnd INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE courses ADD COLUMN isCustomTime INTEGER NOT NULL DEFAULT 1")
                
                // Set existing courses to use custom time mode
                db.execSQL("UPDATE courses SET isCustomTime = 1")
            }
        }
        
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add customWeeks column to courses table for custom week selection
                db.execSQL("ALTER TABLE courses ADD COLUMN customWeeks TEXT DEFAULT NULL")
            }
        }
        
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create semesters table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS semesters (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        startDate INTEGER NOT NULL,
                        endDate INTEGER NOT NULL,
                        totalWeeks INTEGER NOT NULL DEFAULT 18,
                        isArchived INTEGER NOT NULL DEFAULT 0,
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // 2. Create indices for semesters table
                db.execSQL("CREATE INDEX IF NOT EXISTS index_semesters_startDate ON semesters(startDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_semesters_isArchived ON semesters(isArchived)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_semesters_isDefault ON semesters(isDefault)")
                
                // 3. Create default semester
                val now = System.currentTimeMillis()
                // Calculate start date as the Monday of current week
                val currentTimeMillis = System.currentTimeMillis()
                val daysInMillis = 86400000L // 24 * 60 * 60 * 1000
                val currentDayOfWeek = ((currentTimeMillis / daysInMillis + 4) % 7).toInt() // 0=Monday, 6=Sunday
                val mondayOffset = if (currentDayOfWeek == 0) 0 else currentDayOfWeek
                val startDate = currentTimeMillis - (mondayOffset * daysInMillis)
                val endDate = startDate + (18 * 7 * daysInMillis) // 18 weeks
                
                db.execSQL("""
                    INSERT INTO semesters (name, startDate, endDate, totalWeeks, isArchived, isDefault, createdAt, updatedAt)
                    VALUES ('当前学期', $startDate, $endDate, 18, 0, 1, $now, $now)
                """.trimIndent())
                
                // 4. Add semesterId column to courses table with default value 1
                db.execSQL("ALTER TABLE courses ADD COLUMN semesterId INTEGER NOT NULL DEFAULT 1")
                
                // 5. Create index for semesterId in courses table
                db.execSQL("CREATE INDEX IF NOT EXISTS index_courses_semesterId ON courses(semesterId)")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create subscriptions table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS subscriptions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        category TEXT NOT NULL,
                        price REAL NOT NULL,
                        currency TEXT NOT NULL,
                        cycleType TEXT NOT NULL,
                        cycleDuration INTEGER NOT NULL,
                        startDate INTEGER NOT NULL,
                        nextRenewalDate INTEGER NOT NULL,
                        reminderEnabled INTEGER NOT NULL,
                        reminderDaysBefore INTEGER NOT NULL,
                        note TEXT,
                        isActive INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
        
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add autoRenewal column to subscriptions table with default value true
                // to maintain existing behavior for current subscriptions
                db.execSQL("ALTER TABLE subscriptions ADD COLUMN autoRenewal INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}
