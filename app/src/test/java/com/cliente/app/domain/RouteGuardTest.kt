package com.cliente.app.domain

import com.cliente.app.domain.model.Role
import com.cliente.app.ui.navigation.AppRoute
import com.cliente.app.ui.navigation.RouteGuard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteGuardTest {
    @Test
    fun staffBlockedFromOwnerRoutes() {
        assertFalse(RouteGuard.canAccess(AppRoute.Settings, Role.STAFF))
        assertFalse(RouteGuard.canAccess(AppRoute.Analytics, Role.STAFF))
        assertFalse(RouteGuard.canAccess(AppRoute.Availability, Role.STAFF))
        assertFalse(RouteGuard.canAccess(AppRoute.Services, Role.STAFF))
    }

    @Test
    fun ownerCanAccessOwnerRoutes() {
        assertTrue(RouteGuard.canAccess(AppRoute.Settings, Role.OWNER))
    }
}
