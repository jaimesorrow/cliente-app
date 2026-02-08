package com.cliente.app.domain

import com.cliente.app.domain.model.Appointment
import com.cliente.app.domain.model.AppointmentStatus
import com.cliente.app.domain.model.Role
import com.cliente.app.domain.model.TimeOffBlock
import com.cliente.app.domain.usecase.AppointmentCoordinator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class AppointmentCoordinatorTest {
    @Test
    fun detectsConflictAndIgnoresCanceled() = runBlocking {
        val c = AppointmentCoordinator()
        val start = LocalDateTime.of(2026, 1, 1, 10, 0)
        val a = Appointment("a", "b1", "c1", "s1", start, start.plusMinutes(60), AppointmentStatus.SCHEDULED, 1000)
        assertTrue(c.create(a).isSuccess)
        assertTrue(c.create(a.copy(id = "b", startsAt = start.plusMinutes(30), endsAt = start.plusMinutes(90))).isFailure)
        c.transitionStatus("a", AppointmentStatus.CANCELED, "u1", Role.OWNER, "test", "k1")
        assertTrue(c.create(a.copy(id = "c", startsAt = start.plusMinutes(30), endsAt = start.plusMinutes(90))).isSuccess)
    }

    @Test
    fun timeOffBlocksSchedulingAndIdempotencyPreventsDuplicates() = runBlocking {
        val c = AppointmentCoordinator()
        val start = LocalDateTime.of(2026, 1, 1, 10, 0)
        c.upsertTimeOff(TimeOffBlock("t1", "b1", start, start.plusHours(2)))
        val a = Appointment("a", "b1", "c1", "s1", start.plusMinutes(15), start.plusMinutes(45), AppointmentStatus.SCHEDULED, 1000)
        assertTrue(c.create(a).isFailure)

        val open = Appointment("o", "b1", "c1", "s1", start.plusHours(3), start.plusHours(4), AppointmentStatus.SCHEDULED, 1000)
        c.create(open)
        c.transitionStatus("o", AppointmentStatus.CONFIRMED, "u", Role.OWNER, null, "same")
        c.transitionStatus("o", AppointmentStatus.CONFIRMED, "u", Role.OWNER, null, "same")
        assertEquals(1, c.listLogs().size)
    }
}
