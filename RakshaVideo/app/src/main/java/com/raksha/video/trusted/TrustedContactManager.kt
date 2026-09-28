package com.raksha.video.trusted

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class TrustedContactManager {
    private val _contacts = MutableStateFlow<List<TrustedContact>>(emptyList())
    val contacts: StateFlow<List<TrustedContact>> = _contacts
    private val lastAlerts = mutableMapOf<String,Long>()
    fun add(name:String, phone:String?, email:String?, relationship:String?) { _contacts.value = _contacts.value + TrustedContact(UUID.randomUUID().toString(),name,phone,email,relationship) }
    fun remove(id:String) { _contacts.value = _contacts.value.filterNot { it.id==id } }
    fun toggle(id:String) { _contacts.value = _contacts.value.map { if(it.id==id) it.copy(enabled=!it.enabled) else it } }
    fun canAlert(contactId:String, cooldownMs:Long=15*60*1000L):Boolean = System.currentTimeMillis() - (lastAlerts[contactId] ?: 0L) >= cooldownMs
    fun markAlert(contactId:String) { lastAlerts[contactId]=System.currentTimeMillis() }
}
