package com.myclientscheduler.cliente.core.util

class BackStackSanitizer {
    fun sanitize(route: String?, allowed: Set<String>, fallback: String): String {
        return if (route != null && route in allowed) route else fallback
    }
}
