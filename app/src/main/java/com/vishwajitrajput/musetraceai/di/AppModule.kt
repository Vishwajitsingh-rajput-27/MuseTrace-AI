package com.vishwajitrajput.musetraceai.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vishwajitrajput.musetraceai.core.common.DefaultDispatchersProvider
import com.vishwajitrajput.musetraceai.core.common.DispatchersProvider
import com.vishwajitrajput.musetraceai.core.storage.AppPreferencesStore
import com.vishwajitrajput.musetraceai.data.local.GeneratedImageDao
import com.vishwajitrajput.musetraceai.data.local.MuseTraceDatabase
import com.vishwajitrajput.musetraceai.data.local.TraceProjectDao
import com.vishwajitrajput.musetraceai.data.repository.GeminiImageRepositoryImpl
import com.vishwajitrajput.musetraceai.data.repository.GenerationRepositoryImpl
import com.vishwajitrajput.musetraceai.data.repository.OpenCvImageEditorRepository
import com.vishwajitrajput.musetraceai.data.repository.OpenCvImageProcessorRepository
import com.vishwajitrajput.musetraceai.data.repository.TraceProjectRepositoryImpl
import com.vishwajitrajput.musetraceai.domain.AiImageRepository
import com.vishwajitrajput.musetraceai.domain.GenerationRepository
import com.vishwajitrajput.musetraceai.domain.ImageEditorRepository
import com.vishwajitrajput.musetraceai.domain.ImageProcessorRepository
import com.vishwajitrajput.musetraceai.domain.SettingsRepository
import com.vishwajitrajput.musetraceai.domain.TraceProjectRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindings {
    @Binds
    @Singleton
    abstract fun bindDispatchersProvider(impl: DefaultDispatchersProvider): DispatchersProvider

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: AppPreferencesStore): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindTraceProjectRepository(impl: TraceProjectRepositoryImpl): TraceProjectRepository

    @Binds
    @Singleton
    abstract fun bindImageProcessorRepository(impl: OpenCvImageProcessorRepository): ImageProcessorRepository

    @Binds
    @Singleton
    abstract fun bindImageEditorRepository(impl: OpenCvImageEditorRepository): ImageEditorRepository

    @Binds
    @Singleton
    abstract fun bindAiImageRepository(impl: GeminiImageRepositoryImpl): AiImageRepository

    @Binds
    @Singleton
    abstract fun bindGenerationRepository(impl: GenerationRepositoryImpl): GenerationRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MuseTraceDatabase =
        Room.databaseBuilder(context, MuseTraceDatabase::class.java, "musetrace.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun provideTraceProjectDao(database: MuseTraceDatabase): TraceProjectDao = database.traceProjectDao()

    @Provides
    fun provideGeneratedImageDao(database: MuseTraceDatabase): GeneratedImageDao = database.generatedImageDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS generated_images (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    prompt TEXT NOT NULL,
                    enhancedPrompt TEXT NOT NULL,
                    negativePrompt TEXT NOT NULL,
                    styleName TEXT NOT NULL,
                    aspectRatioName TEXT NOT NULL,
                    imageUri TEXT NOT NULL,
                    providerName TEXT NOT NULL,
                    createdAtMillis INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE trace_projects ADD COLUMN originalImageUri TEXT")
            db.execSQL("ALTER TABLE trace_projects ADD COLUMN geminiGeneratedImageUri TEXT")
            db.execSQL("ALTER TABLE trace_projects ADD COLUMN processedImageUri TEXT")
            db.execSQL("ALTER TABLE trace_projects ADD COLUMN previewImageUri TEXT")
            db.execSQL("ALTER TABLE trace_projects ADD COLUMN paletteJson TEXT NOT NULL DEFAULT '[]'")
            db.execSQL("ALTER TABLE trace_projects ADD COLUMN calibrationJson TEXT")
            db.execSQL("ALTER TABLE trace_projects ADD COLUMN overlayJson TEXT")
            db.execSQL("ALTER TABLE trace_projects ADD COLUMN drawingSettingsJson TEXT")
            db.execSQL("ALTER TABLE trace_projects ADD COLUMN workflowJson TEXT")
            db.execSQL("ALTER TABLE trace_projects ADD COLUMN updatedAtMillis INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE trace_projects SET updatedAtMillis = createdAtMillis WHERE updatedAtMillis = 0")
        }
    }
}
