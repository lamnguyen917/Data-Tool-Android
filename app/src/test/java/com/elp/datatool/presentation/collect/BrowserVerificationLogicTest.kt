package com.elp.datatool.presentation.collect

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserVerificationLogicTest {

    @Test
    fun challengeHtml_isNotSubmittedAsVerifiedContent() {
        assertTrue(looksLikeChallengeHtml("<form id='challenge-form'>Verify you are human</form>"))
        assertTrue(looksLikeChallengeHtml("<div class='g-recaptcha'></div>"))
        assertTrue(looksLikeChallengeHtml("<title>Attention Required | Cloudflare</title>"))
    }

    @Test
    fun productHtml_canCompleteVerification() {
        assertFalse(looksLikeChallengeHtml("<html><h1>Product title</h1><table></table></html>"))
    }
}
