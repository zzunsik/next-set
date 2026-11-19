package io.github.zzunsik.nextset.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NextSetNavDisplayTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun startsAtHome() {
        val app = RuntimeEnvironment.getApplication()

        compose.setContent { NextSetTheme { NextSetNavDisplay() } }

        compose.onNodeWithText(app.getString(R.string.home_placeholder)).assertIsDisplayed()
    }
}
