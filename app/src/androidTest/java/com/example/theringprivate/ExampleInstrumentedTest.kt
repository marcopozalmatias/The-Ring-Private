package com.example.theringprivate

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Prueba instrumentada mínima que valida el package name de la app sobre un dispositivo o emulador Android.
 *
 * Se conserva como plantilla de verificación del entorno de instrumentación.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    // Verificamos que la app instalada expone el package name esperado.
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.theringprivate", appContext.packageName)
    }
}