package me.unidok.jmccodespace.util

import net.minecraft.util.math.Vec3d



fun square(n: Double) = n * n

fun Vec3d.isNear(pos: Vec3d, distance: Double) = this.squaredDistanceTo(pos) <= square(distance)

fun Char.repeat(n: Int) = String(CharArray(n) { this })
