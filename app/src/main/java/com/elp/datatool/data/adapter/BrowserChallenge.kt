package com.elp.datatool.data.adapter

import com.elp.datatool.data.remote.HttpClient
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import kotlinx.coroutines.CancellationException

/** Adds the trusted page URL needed for user-completed browser verification. */
internal suspend fun <T> withBrowserChallenge(
    challengeUrl: String,
    userAgent: String = HttpClient.USER_AGENT,
    load: suspend () -> T,
): T = try {
    load()
} catch (error: CancellationException) {
    throw error
} catch (error: FetchException) {
    if (error.kind != FetchErrorKind.BLOCKED || error.challengeUrl != null) throw error
    throw FetchException(
        kind = FetchErrorKind.BLOCKED,
        message = error.message,
        cause = error,
        challengeUrl = challengeUrl,
        challengeUserAgent = userAgent,
    )
}
