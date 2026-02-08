package com.myclientscheduler.cliente

import com.google.common.truth.Truth.assertThat
import com.myclientscheduler.cliente.core.model.*
import com.myclientscheduler.cliente.core.rbac.AppRoute
import com.myclientscheduler.cliente.core.rbac.PermissionPolicy
import com.myclientscheduler.cliente.core.time.ConflictDetector
import com.myclientscheduler.cliente.core.util.BackStackSanitizer
import com.myclientscheduler.cliente.data.repo.InMemoryRepository
import com.myclientscheduler.cliente.domain.usecase.DeleteAccountUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.LocalDateTime

class AppPolicyTests {

    @Test fun `rbac blocks staff owner routes`() {
        val staff = UserContext("s1", "b1", Role.STAFF)
        assertThat(PermissionPolicy.canAccessRoute(AppRoute.Settings, staff)).isFalse()
        assertThat(PermissionPolicy.canAccessRoute(AppRoute.Clients, staff)).isTrue()
    }

    @Test fun `conflict detector ignores canceled`() {
        val existing = Appointment("a1", "b", "c", "s", LocalDateTime.parse("2026-01-01T10:00:00"), LocalDateTime.parse("2026-01-01T11:00:00"), AppointmentStatus.CANCELED, 100)
        val hasConflict = ConflictDetector.hasConflict(
            LocalDateTime.parse("2026-01-01T10:15:00"),
            LocalDateTime.parse("2026-01-01T10:45:00"),
            listOf(existing), emptyList()
        )
        assertThat(hasConflict).isFalse()
    }

    @Test fun `audit is append only and idempotent`() = runBlocking {
        val repo = InMemoryRepository()
        val owner = UserContext("o", "b", Role.OWNER)
        repo.upsertService(Service("svc", "b", "Cut", 60, 1000))
        val created = repo.createAppointment(owner, "c", "svc", LocalDateTime.parse("2026-01-01T09:00:00"), "tok-1").getOrThrow()
        val first = repo.transitionStatus(owner, created.id, AppointmentStatus.CONFIRMED, null, "tok-2")
        val dup = repo.transitionStatus(owner, created.id, AppointmentStatus.COMPLETED, null, "tok-2")
        assertThat(first.isSuccess).isTrue()
        assertThat(dup.isFailure).isTrue()
        assertThat(repo.canMutateAuditLog()).isFalse()
    }

    @Test fun `delete flow allows last owner close business`() {
        val useCase = DeleteAccountUseCase()
        val plan = useCase.plan(UserContext("o", "b", Role.OWNER), ownerCount = 1).getOrThrow()
        assertThat(plan.closesBusiness).isTrue()
        assertThat(plan.deactivateNow).isTrue()
    }

    @Test fun `delete flow allows non-last owner removal`() {
        val useCase = DeleteAccountUseCase()
        val plan = useCase.plan(UserContext("o", "b", Role.OWNER), ownerCount = 2).getOrThrow()
        assertThat(plan.closesBusiness).isFalse()
        assertThat(plan.copy).isEqualTo("Delete user identity only")
    }

    @Test fun `delete flow rejects non-positive owner count`() {
        val useCase = DeleteAccountUseCase()
        val result = useCase.plan(UserContext("o", "b", Role.OWNER), ownerCount = 0)
        assertThat(result.isFailure).isTrue()
    }

    @Test fun `back stack sanitizer prevents leakage`() {
        val sanitizer = BackStackSanitizer()
        val safe = sanitizer.sanitize("settings", setOf("dashboard", "clients"), "dashboard")
        assertThat(safe).isEqualTo("dashboard")
    }
}
