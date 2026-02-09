package com.myclientscheduler.cliente.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val name: String,
    val phone: String?,
    val email: String?,
)

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val clientId: String,
    val serviceId: String,
    val startsAtIso: String,
    val endsAtIso: String,
    val status: String,
)

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name")
    fun observeAll(): Flow<List<ClientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<ClientEntity>)
}

@Database(entities = [ClientEntity::class, AppointmentEntity::class], version = 1, exportSchema = false)
abstract class ClienteDatabase : RoomDatabase() {
    abstract fun clients(): ClientDao
}
