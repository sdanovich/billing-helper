package com.example.claims.android

import android.app.Application
import com.example.claims.android.data.AppGraph

/** Holds the app-wide dependency graph. */
class ClaimsApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
    }
}
