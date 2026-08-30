package com.myclientscheduler.cliente.core.util

fun sanitizeBackStack(route: String?, allowed: Set<String>, fallback: String): String =
    if (route != null && route in allowed) route else fallback
