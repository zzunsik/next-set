package io.github.zzunsik.nextset.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class HomeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()

    @Test
    fun showsAppNameAndPlaceholder() {
        compose.setContent { NextSetTheme { HomeScreen(onOpenSettings = {}) } }

        compose.onNodeWithText(app.getString(R.string.app_name)).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.home_placeholder)).assertIsDisplayed()
    }

    @Test
    fun settingsButtonOpensSettings() {
        var opened = 0

        compose.setContent { NextSetTheme { HomeScreen(onOpenSettings = { opened++ }) } }
        compose.onNodeWithText(app.getString(R.string.home_open_settings)).performClick()

        assertEquals(1, opened)
    }
}
