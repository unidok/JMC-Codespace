package me.unidok.jmccodespace.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import me.unidok.jmccodespace.Mod

object Scope : CoroutineScope {
    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    override val coroutineContext = newSingleThreadContext(Mod.MOD_ID)
}