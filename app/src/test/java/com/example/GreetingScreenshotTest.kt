package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.DocumentEntity
import com.example.ui.components.DocumentItemCard
import com.example.ui.theme.MyApplicationTheme
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
  fun documind_card_screenshot() {
    val sampleDoc = DocumentEntity(
        id = "test-doc-1",
        title = "Neural Networks & Gradient Descent",
        fileType = "PDF",
        fileName = "lecture_notes.pdf",
        extractedText = "Backpropagation algorithm computing loss gradients using chain rule.",
        summary = "Concise summary of gradient calculation.",
        folderCategory = "Computer Science",
        tags = "ml, ai, deep-learning",
        isOfflinePrivacy = false,
        isCloudSynced = true,
        wordCount = 10,
        timestamp = 1700000000000L
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        DocumentItemCard(
            document = sampleDoc,
            onClick = {},
            onListenTts = {},
            onStudyClick = {},
            onDeleteClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
