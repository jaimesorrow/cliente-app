package com.myclientscheduler.cliente.core.time

import com.myclientscheduler.cliente.core.model.Appointment
import com.myclientscheduler.cliente.core.model.AppointmentStatus
import com.myclientscheduler.cliente.core.model.TimeOffBlock
import java.time.LocalDateTime

object ConflictDetector {
    fun hasConflict(
        start: LocalDateTime,
        end: LocalDateTime,
        appointments: List<Appointment>,
        timeOffBlocks: List<TimeOffBlock>,
        ignoreAppointmentId: String? = null,
    ): Boolean {
        val blocksConflict = timeOffBlocks.any { overlap(start, end, it.start, it.end) }
        if (blocksConflict) return true
        return appointments
            .filter { it.id != ignoreAppointmentId }
            .filter { it.status != AppointmentStatus.CANCELED }
            .any { overlap(start, end, it.startsAt, it.endsAt) }
    }

    private fun overlap(aStart: LocalDateTime, aEnd: LocalDateTime, bStart: LocalDateTime, bEnd: LocalDateTime): Boolean {
        return aStart < bEnd && bStart < aEnd
    }
}
