package takagi.ru.saison.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import takagi.ru.saison.data.local.database.SaisonDatabase
import takagi.ru.saison.data.local.database.dao.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideSaisonDatabase(
        @ApplicationContext context: Context
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
                SaisonDatabase.MIGRATION_10_11
            )
            .fallbackToDestructiveMigration()
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
}
