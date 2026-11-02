package com.biglv.recitelsland.data.remote

object ApiConfig {
    const val WEB_BASE = "https://recitelsland.biglv.asia"
    const val BASE_URL = "$WEB_BASE/api/v1/"
    const val GITHUB_LOGIN_URL = "${BASE_URL}auth/github/login?platform=android"

    fun absoluteUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        return WEB_BASE + if (path.startsWith("/")) path else "/$path"
    }
}
