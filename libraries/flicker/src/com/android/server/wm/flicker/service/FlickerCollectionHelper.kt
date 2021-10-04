/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server.wm.flicker.service

import android.util.Log
import com.android.helpers.ICollectorHelper
import com.android.server.wm.flicker.getDefaultFlickerOutputDir
import com.android.server.wm.flicker.monitor.LayersTraceMonitor
import com.android.server.wm.flicker.monitor.TraceMonitor
import com.android.server.wm.flicker.monitor.WindowManagerTraceMonitor
import com.android.server.wm.flicker.service.FlickerService.Companion.getFassFilePath
import com.android.server.wm.traces.common.errors.ErrorTrace
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.parser.layers.LayersTraceParser
import com.android.server.wm.traces.parser.windowmanager.WindowManagerTraceParser
import java.nio.file.Files
import java.nio.file.Path

/**
 * An {@link ICollectorHelper} for collecting FASS assertions information.
 *
 * <p>This parses the output of {@link FlickerService} and returns a collection of
 * assertions metrics.
 */
class FlickerCollectionHelper : ICollectorHelper<Int> {
    private val LOG_TAG = FlickerCollectionHelper::class.java.simpleName
    private val outputDir = getDefaultFlickerOutputDir()

    private val traceMonitors: List<TraceMonitor> = listOf(
            WindowManagerTraceMonitor(outputDir),
            LayersTraceMonitor(outputDir)
    )

    internal var errorTrace: ErrorTrace = ErrorTrace(emptyArray(), source = "")
        private set

    private var wmTrace: WindowManagerTrace = WindowManagerTrace(emptyArray(), source = "")
    private var layersTrace: LayersTrace = LayersTrace(emptyArray(), source = "")

    /** Clear existing fass files and start the monitors.  */
    override fun startCollecting(): Boolean {
        Log.i(LOG_TAG, "startCollecting")
        cleanupTraceFiles()
        traceMonitors.forEach {
            it.start()
        }
        return true
    }

    /** Collect the assertions metrics for Flicker as a Service.  */
    override fun getMetrics(): Map<String, Int> {
        Log.i(LOG_TAG, "getMetrics")
        val testTag = "fass"
        traceMonitors.forEach {
            it.stop()
            it.save(testTag)
        }

        Files.createDirectories(outputDir)
        wmTrace = getWindowManagerTrace(getFassFilePath(outputDir, testTag, "wm_trace"))
        layersTrace = getLayersTrace(getFassFilePath(outputDir, testTag, "layers_trace"))

        val flickerService = FlickerService()
        val (errors, assertions) = flickerService.process(wmTrace, layersTrace, outputDir, testTag)
        errorTrace = errors

        return assertions
    }

    /** Do nothing, because nothing is needed to disable fass.  */
    override fun stopCollecting(): Boolean {
        Log.i(LOG_TAG, "stopCollecting")
        return true
    }

    /**
     * Remove the WM trace and layers trace files collected from previous test runs if the
     * directory exists.
     */
    private fun cleanupTraceFiles() {
        if (!Files.exists(outputDir)) {
            return
        }

        Files.list(outputDir).filter {
            file -> !Files.isDirectory(file.toAbsolutePath())
        }.forEach { file ->
            Files.delete(file)
        }
    }

    /**
     * Parse the window manager trace file.
     *
     * @param traceFilePath
     * @return parsed window manager trace.
     */
    private fun getWindowManagerTrace(traceFilePath: Path): WindowManagerTrace {
        val wmTraceByteArray: ByteArray = Files.readAllBytes(traceFilePath)
        return WindowManagerTraceParser.parseFromTrace(wmTraceByteArray)
    }

    /**
     * Parse the layers trace file.
     *
     * @param traceFilePath
     * @return parsed layers trace.
     */
    private fun getLayersTrace(traceFilePath: Path): LayersTrace {
        val layersTraceByteArray: ByteArray = Files.readAllBytes(traceFilePath)
        return LayersTraceParser.parseFromTrace(layersTraceByteArray)
    }
}