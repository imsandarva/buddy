package com.sandarva.kotlinapps.brain

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Cheap online check so we do not wait on DNS when the radio is down. */
object Reachability {
    fun online(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isNetworkFailure(error: Throwable): Boolean {
        val chain = generateSequence(error) { it.cause }
        return chain.any {
            it is UnknownHostException || it is ConnectException || it is SocketTimeoutException ||
                it.message.orEmpty().contains("Unable to resolve host", ignoreCase = true) ||
                it.message.orEmpty().contains("Failed to connect", ignoreCase = true)
        }
    }
}
