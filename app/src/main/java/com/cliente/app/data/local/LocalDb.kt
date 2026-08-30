package com.cliente.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val name: String,
    val tagsCsv: String,
)

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients WHERE businessId = :businessId")
    suspend fun byBusiness(businessId: String): List<ClientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ClientEntity)
}

@Database(entities = [ClientEntity::class], version = 1, exportSchema = false)
abstract class ClienteDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
}
