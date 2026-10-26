package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("FitCalisthenics", appName)
  }

  @Test
  fun `verify exercises json in assets contains at least 40 exercises`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val jsonString = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
    val jsonArray = org.json.JSONArray(jsonString)
    org.junit.Assert.assertTrue("Debe contener al menos 40 ejercicios", jsonArray.length() >= 40)
  }
}
