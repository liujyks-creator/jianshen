package com.liujyks.trainflow.app

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Test

class TrainFlowApplicationHeartRateOwnerTest {
    @Test
    fun applicationTypeIsStableForStageBCompositionTests() {
        assertEquals(Application::class.java, TrainFlowApplication::class.java.superclass)
    }
}
