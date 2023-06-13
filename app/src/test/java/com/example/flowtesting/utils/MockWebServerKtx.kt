package com.example.flowtesting.utils

import okhttp3.mockwebserver.MockWebServer

val MockWebServer.url: String
    get() = url("/").toString()
