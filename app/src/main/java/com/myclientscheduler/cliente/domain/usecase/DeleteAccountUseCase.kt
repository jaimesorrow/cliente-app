package com.myclientscheduler.cliente.domain.usecase

import com.myclientscheduler.cliente.core.model.Role
import com.myclientscheduler.cliente.core.model.UserContext
import java.time.Instant
import java.time.temporal.ChronoUnit

data class DeletionPlan(
    val deactivateNow: Boolean,
    val purgeAt: Instant,
    val closesBusiness: Boolean,
    val copy: String,
)

class DeleteAccountUseCase {
    fun plan(user: UserContext, ownerCount: Int): Result<DeletionPlan> {
        return when {
            user.role == Role.STAFF -> Result.success(
                DeletionPlan(true, Instant.now().plus(30, ChronoUnit.DAYS), false, "Delete user identity only")
            )
            ownerCount <= 1 -> Result.success(
                DeletionPlan(true, Instant.now().plus(30, ChronoUnit.DAYS), true, "Delete Account & Close Business")
            )
            else -> Result.failure(IllegalStateException("At least one owner must remain"))
        }
    }
}
