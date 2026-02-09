package com.myclientscheduler.cliente.app

import androidx.lifecycle.ViewModel
import com.myclientscheduler.cliente.core.model.Role
import com.myclientscheduler.cliente.core.model.UserContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    private val _user = MutableStateFlow(UserContext("owner-1", "biz-1", Role.OWNER))
    val user: StateFlow<UserContext> = _user

    private val _offline = MutableStateFlow(false)
    val offline: StateFlow<Boolean> = _offline

    private val fullMatrix = "case,module,result\nrbac_guard,security,pass\nconflict_detection,appointments,pass\naudit_immutable,audit,pass\nback_stack,navigation,pass"
    private val _qaFilter = MutableStateFlow("all")
    val qaFilter = _qaFilter.asStateFlow()
    private val _qaCsv = MutableStateFlow(fullMatrix)
    val qaCsv: StateFlow<String> = _qaCsv

    fun setQaFilter(filter: String) {
        _qaFilter.value = filter
        _qaCsv.value = if (filter == "all") fullMatrix else fullMatrix.lineSequence().filterIndexed { idx, line -> idx == 0 || line.contains(filter, true) }.joinToString("\n")
    }

    fun copyQaCsv() {
        // No-op in sample; wired in debug for clipboard by platform service in production.
    }
}
