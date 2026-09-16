package com.legalmetrology.inspector.di

import android.content.Context
import androidx.room.Room
import com.legalmetrology.inspector.data.db.InspectionDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideInspectionDatabase(
        @ApplicationContext context: Context
    ): InspectionDatabase {
        return Room.databaseBuilder(
            context,
            InspectionDatabase::class.java,
            "legal_metrology_inspections.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideInspectionDao(database: InspectionDatabase) = database.inspectionDao()
}
