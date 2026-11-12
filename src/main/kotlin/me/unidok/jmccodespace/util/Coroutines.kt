package me.unidok.jmccodespace.util

import kotlinx.coroutines.suspendCancellableCoroutine
import net.minecraft.client.Minecraft
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun runInMainThread(block: () -> Unit) {
    val client = Minecraft.getInstance()
    if (client.isSameThread) return block()
    client.execute(block)
}

suspend fun <T> runInMainThreadSuspend(block: () -> T): T = suspendCancellableCoroutine { cont ->
    runInMainThread {
        try {
            cont.resume(block())
        } catch (e: Throwable) {
            cont.resumeWithException(e)
        }
    }
}