package takagi.ru.saison.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import takagi.ru.saison.data.repository.CourseSettingsRepositoryImpl
import takagi.ru.saison.data.repository.CourseWidgetRepository
import takagi.ru.saison.data.repository.CourseWidgetRepositoryImpl
import takagi.ru.saison.data.repository.DefaultSemesterInitializer
import takagi.ru.saison.data.repository.DefaultSemesterInitializerImpl
import takagi.ru.saison.data.repository.EventRepositoryImpl
import takagi.ru.saison.data.repository.RoutineRepository
import takagi.ru.saison.data.repository.RoutineRepositoryImpl
import takagi.ru.saison.data.repository.SemesterRepository
import takagi.ru.saison.data.repository.SemesterRepositoryImpl
import takagi.ru.saison.data.repository.TaskWidgetRepository
import takagi.ru.saison.data.repository.TaskWidgetRepositoryImpl
import takagi.ru.saison.data.repository.backup.WebDavBackupRepository
import takagi.ru.saison.data.repository.backup.WebDavBackupRepositoryImpl
import takagi.ru.saison.data.repository.local.LocalExportImportRepository
import takagi.ru.saison.data.repository.local.LocalExportImportRepositoryImpl
import takagi.ru.saison.domain.repository.CourseSettingsRepository
import takagi.ru.saison.domain.repository.EventRepository
import takagi.ru.saison.util.CycleCalculator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindEventRepository(
        eventRepositoryImpl: EventRepositoryImpl
    ): EventRepository
    
    @Binds
    @Singleton
    abstract fun bindRoutineRepository(
        routineRepositoryImpl: RoutineRepositoryImpl
    ): RoutineRepository
    
    @Binds
    @Singleton
    abstract fun bindCourseSettingsRepository(
        courseSettingsRepositoryImpl: CourseSettingsRepositoryImpl
    ): CourseSettingsRepository
    
    @Binds
    @Singleton
    abstract fun bindSemesterRepository(
        semesterRepositoryImpl: SemesterRepositoryImpl
    ): SemesterRepository
    
    @Binds
    @Singleton
    abstract fun bindCourseWidgetRepository(
        courseWidgetRepositoryImpl: CourseWidgetRepositoryImpl
    ): CourseWidgetRepository
    
    @Binds
    @Singleton
    abstract fun bindDefaultSemesterInitializer(
        defaultSemesterInitializerImpl: DefaultSemesterInitializerImpl
    ): DefaultSemesterInitializer
    
    @Binds
    @Singleton
    abstract fun bindTaskWidgetRepository(
        taskWidgetRepositoryImpl: TaskWidgetRepositoryImpl
    ): TaskWidgetRepository
    
    @Binds
    @Singleton
    abstract fun bindWebDavBackupRepository(
        webDavBackupRepositoryImpl: WebDavBackupRepositoryImpl
    ): WebDavBackupRepository
    
    @Binds
    @Singleton
    abstract fun bindLocalExportImportRepository(
        localExportImportRepositoryImpl: LocalExportImportRepositoryImpl
    ): LocalExportImportRepository
    
    companion object {
        @Provides
        @Singleton
        fun provideCycleCalculator(): CycleCalculator {
            return CycleCalculator()
        }
        
        @Provides
        @Singleton
        @javax.inject.Named("applicationContext")
        fun provideApplicationContext(
            @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
        ): android.content.Context {
            return context
        }
    }
}
