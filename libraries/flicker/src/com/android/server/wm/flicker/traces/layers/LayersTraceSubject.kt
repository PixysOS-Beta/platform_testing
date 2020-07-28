/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.wm.flicker.traces.layers

import android.graphics.Rect
import android.graphics.Region
import com.android.server.wm.flicker.assertions.TraceAssertion
import com.android.server.wm.flicker.traces.SubjectBase
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth

/** Truth subject for [LayersTrace] objects.  */
class LayersTraceSubject private constructor(
    fm: FailureMetadata,
    val trace: LayersTrace
) : SubjectBase<LayersTrace, LayerTraceEntry>(fm, trace) {
    /**
     * Signal that the last assertion set is complete. The next assertion added will start a new
     * set of assertions.
     *
     * E.g.: checkA().then().checkB()
     *
     * Will produce two sets of assertions (checkA) and (checkB) and checkB will only be checked
     * after checkA passes.
     */
    fun then() = apply {
        newAssertion = true
        assertionsChecker.checkChangingAssertions()
    }

    /**
     * Signal that the last assertion set is not complete. The next assertion added will be
     * appended to the current set of assertions.
     *
     * E.g.: checkA().and().checkB()
     *
     * Will produce one sets of assertions (checkA, checkB) and the assertion will only pass is both
     * checkA and checkB pass.
     */
    fun and() = apply { newAssertion = false }

    /**
     * Ignores the first entries in the trace, until the first assertion passes. If it reaches the
     * end of the trace without passing any assertion, return a failure with the name/reason from
     * the first assertion
     *
     * @return
     */
    fun skipUntilFirstAssertion() = apply { assertionsChecker.skipUntilFirstAssertion() }

    fun failWithMessage(message: String) = fail(message)

    private fun test() {
        val failures = assertionsChecker.test(actual().entries)
        if (failures.isNotEmpty()) {
            val failureLogs = failures.joinToString("\n")
            var tracePath = ""
            if (actual().hasSource()) {
                tracePath = "Layers Trace can be found in: " +
                        "${actual().source.get().toAbsolutePath()}" +
                        "\nChecksum: " + actual().sourceChecksum + "\n"
            }
            fail(tracePath + failureLogs)
        }
    }

    fun coversRegion(rect: Rect) = this.coversRegion(Region(rect))

    fun coversRegion(region: Region)
            = apply{ addAssertion("coversRegion($region)") { it.coversRegion(region) } }

    fun hasVisibleRegion(layerName: String, size: Region) = apply {
        addAssertion("hasVisibleRegion($layerName$size)") {
            it.hasVisibleRegion(layerName, size)
        }
    }

    fun hasNotLayer(layerName: String)
            = apply { addAssertion("hasNotLayer($layerName)") { it.exists(layerName).negate() } }

    fun hasLayer(layerName: String)
            = apply { addAssertion("hasLayer($layerName)") { it.exists(layerName) } }

    fun showsLayer(layerName: String)
            = apply { addAssertion("showsLayer($layerName)") { it.isVisible(layerName) } }

    fun replaceVisibleLayer(previousLayerName: String, currentLayerName: String)
            = apply { hidesLayer(previousLayerName).and().showsLayer(currentLayerName) }

    fun hidesLayer(layerName: String)
            = apply {addAssertion("hidesLayer($layerName)") { it.isVisible(layerName).negate() } }

    operator fun invoke(name: String, assertion: TraceAssertion<LayerTraceEntry>)
            = apply { addAssertion(name, assertion) }

    override val traceName: String
        get() = "LayersTrace"

    companion object {
        /**
         * Boiler-plate Subject.Factory for LayersTraceSubject
         */
        private val FACTORY = Factory { fm: FailureMetadata, subject: LayersTrace ->
            LayersTraceSubject(fm, subject)
        }

        /**
         * User-defined entry point
         */
        @JvmStatic
        fun assertThat(entry: LayersTrace)
                = Truth.assertAbout(FACTORY).that(entry) as LayersTraceSubject


        /**
         * Static method for getting the subject factory (for use with assertAbout())
         */
        @JvmStatic
        fun entries(): Factory<SubjectBase<LayersTrace, LayerTraceEntry>, LayersTrace> {
            return FACTORY
        }
    }
}