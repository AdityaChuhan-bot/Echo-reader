package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.theme.AudioBookTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val mockBook = com.example.data.database.BookEntity(
        id = 1,
        title = "The Great Gatsby",
        author = "F. Scott Fitzgerald",
        filePath = "/dummy/path.pdf",
        coverColor = 0xFF1E3A8A.toInt(),
        totalChapters = 9,
        totalSentences = 120,
        currentChapterIndex = 0,
        currentSentenceIndex = 5
    )

    composeTestRule.setContent { 
        AudioBookTheme { 
            MiniPlayer(
                book = mockBook,
                isPlaying = true,
                onPlayPause = {},
                onClick = {}
            ) 
        } 
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
