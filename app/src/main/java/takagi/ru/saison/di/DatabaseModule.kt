package takagi.ru.saison.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import takagi.ru.saison.data.local.database.DatabaseMigrationHelper
import takagi.ru.saison.data.local.database.SaisonDatabase
import takagi.ru.saison.data.local.database.dao.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    private const val TAG = "DatabaseModule"
    
    @Provides
    @Singleton
    fun provideSaisonDatabase(
        @ApplicationContext context: Context,
        migrationHelper: DatabaseMigrationHelper
    ): SaisonDatabase {
        return Room.databaseBuilder(
            context,
            SaisonDatabase::class.java,
            SaisonDatabase.DATABASE_NAME
        )
            .addMigrations(
                SaisonDatabase.MIGRATION_1_2,
                SaisonDatabase.MIGRATION_2_3,
                SaisonDatabase.MIGRATION_3_4,
                SaisonDatabase.MIGRATION_4_5,
                SaisonDatabase.MIGRATION_5_6,
                SaisonDatabase.MIGRATION_6_7,
                SaisonDatabase.MIGRATION_7_8,
                SaisonDatabase.MIGRATION_8_9,
                SaisonDatabase.MIGRATION_9_10,
                SaisonDatabase.MIGRATION_10_11,
                SaisonDatabase.MIGRATION_11_12,
                SaisonDatabase.MIGRATION_12_13,
                SaisonDatabase.MIGRATION_13_14,
                SaisonDatabase.MIGRATION_14_15
            )
            // ⚠️ 移除了 fallbackToDestructiveMigration()！
            // 这个配置会在迁移失败时删除整个数据库，导致数据丢失
            // 现在如果缺少迁移脚本，应用会崩溃并提示开发者添加迁移
            // 这样可以强制我们为每个 schema 变化提供安全的迁移路径
            
            // 添加回调，在数据库打开时自动创建备份
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    Log.d(TAG, "数据库已打开，版本: ${db.version}")
                    
                    // 在后台创建自动备份
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        try {
                            migrationHelper.createDatabaseBackup("auto").onSuccess {
                                Log.d(TAG, "自动备份创建成功: ${it.name}")
                            }.onFailure {
                                Log.w(TAG, "自动备份创建失败", it)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "自动备份异常", e)
                        }
                    }
                }
                
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    Log.d(TAG, "数据库首次创建，版本: ${db.version}")
                }
            })
            .build()
    }
    
    @Provides
    fun provideTaskDao(database: SaisonDatabase): TaskDao {
        return database.taskDao()
    }
    
    @Provides
    fun provideTagDao(database: SaisonDatabase): TagDao {
        return database.tagDao()
    }
    
    @Provides
    fun provideCourseDao(database: SaisonDatabase): CourseDao {
        return database.courseDao()
    }
    
    @Provides
    fun providePomodoroDao(database: SaisonDatabase): PomodoroDao {
        return database.pomodoroDao()
    }
    
    @Provides
    fun provideAttachmentDao(database: SaisonDatabase): AttachmentDao {
        return database.attachmentDao()
    }
    
    @Provides
    fun provideEventDao(database: SaisonDatabase): EventDao {
        return database.eventDao()
    }
    
    @Provides
    fun provideRoutineTaskDao(database: SaisonDatabase): RoutineTaskDao {
        return database.routineTaskDao()
    }
    
    @Provides
    fun provideCheckInRecordDao(database: SaisonDatabase): CheckInRecordDao {
        return database.checkInRecordDao()
    }
    
    @Provides
    fun provideSemesterDao(database: SaisonDatabase): SemesterDao {
        return database.semesterDao()
    }
    
    @Provides
    fun provideSubscriptionDao(database: SaisonDatabase): SubscriptionDao {
        return database.subscriptionDao()
    }
    
    @Provides
    fun provideSubscriptionHistoryDao(database: SaisonDatabase): SubscriptionHistoryDao {
        return database.subscriptionHistoryDao()
    }
    
    @Provides
    fun provideCategoryDao(database: SaisonDatabase): CategoryDao {
        return database.categoryDao()
    }
}
