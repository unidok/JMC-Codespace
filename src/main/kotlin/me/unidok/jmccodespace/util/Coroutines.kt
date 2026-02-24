package me.unidok.jmccodespace.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import me.unidok.jmccodespace.JMCCodespace
import net.minecraft.client.Minecraft
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object AsyncScope : CoroutineScope {
    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    override val coroutineContext = newSingleThreadContext(JMCCodespace.MOD_ID)
}

fun runInMainThread(block: () -> Unit) {
    val client = Minecraft.getInstance()
    if (client.isSameThread) return block()
    client.execute(block)
}

suspend fun <T> runInMainThreadSuspend(block: () -> T): T = suspendCoroutine { cont ->
    runInMainThread {
        try {
            cont.resume(block())
        } catch (e: Throwable) {
            cont.resumeWithException(e)
        }
    }
}