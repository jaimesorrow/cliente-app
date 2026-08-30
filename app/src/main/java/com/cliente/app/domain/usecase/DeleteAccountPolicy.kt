package com.cliente.app.domain.usecase

import com.cliente.app.domain.model.Role

data class TeamMember(val id: String, val role: Role, val active: Boolean = true)

class DeleteAccountPolicy {
    fun canDeleteSelf(member: TeamMember): Boolean = member.active

    fun canCloseBusiness(requesterId: String, members: List<TeamMember>): Boolean {
        val requester = members.firstOrNull { it.id == requesterId } ?: return false
        if (requester.role != Role.OWNER) return false
        val activeOwners = members.count { it.active && it.role == Role.OWNER }
        return activeOwners <= 1
    }
}
