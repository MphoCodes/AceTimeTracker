package com.mphocodes.acetimetracker.util

import android.content.Context
import android.content.pm.ApplicationInfo

fun Context.isDebuggableBuild(): Boolean =
    applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
