package com.cliente.app.domain

import com.cliente.app.domain.model.Role
import com.cliente.app.domain.usecase.DeleteAccountPolicy
import com.cliente.app.domain.usecase.TeamMember
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteAccountPolicyTest {
    private val policy = DeleteAccountPolicy()

    @Test
    fun lastOwnerCanCloseBusiness() {
        val members = listOf(TeamMember("owner", Role.OWNER), TeamMember("staff", Role.STAFF))
        assertTrue(policy.canCloseBusiness("owner", members))
    }

    @Test
    fun nonLastOwnerCannotCloseBusiness() {
        val members = listOf(TeamMember("owner1", Role.OWNER), TeamMember("owner2", Role.OWNER))
        assertFalse(policy.canCloseBusiness("owner1", members))
    }
}
