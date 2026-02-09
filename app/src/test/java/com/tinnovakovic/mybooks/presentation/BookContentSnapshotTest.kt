package com.tinnovakovic.mybooks.presentation

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.accessibility.AccessibilityRenderExtension
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class BookContentSnapshotTest(
    @TestParameter private val deviceConfig: Device
) {

    enum class Device(
        val config: DeviceConfig
    ) {
        PIXEL_5(DeviceConfig.PIXEL_5),
        PIXEL_5_2X(DeviceConfig.PIXEL_5.copy(fontScale = 2f)),
        PIXEL_FOLD(DeviceConfig.PIXEL_FOLD.copy(screenWidth = 2208, screenHeight = 1840)),
        PIXEL_FOLD_2X(DeviceConfig.PIXEL_FOLD.copy(screenWidth = 2208, screenHeight = 1840, fontScale = 2f))
    }

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = deviceConfig.config,
        renderExtensions = setOf(AccessibilityRenderExtension())
    )

    @Test
    fun bookContentSuccess() {
        paparazzi.snapshot {
            BookContentSuccessPreview()
        }
    }

    @Test
    fun bookContentError() {
        paparazzi.snapshot {
            BookContentErrorPreview()
        }
    }
}
