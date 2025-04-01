/**********************************************************************************************************************
 *  COPYRIGHT
 *  -------------------------------------------------------------------------------------------------------------------
 *  \verbatim
 *  Copyright (c) 2025 by Vector Informatik GmbH. MIT license:
 *
 *                Permission is hereby granted, free of charge, to any person obtaining
 *                a copy of this software and associated documentation files (the
 *                "Software"), to deal in the Software without restriction, including
 *                without limitation the rights to use, copy, modify, merge, publish,
 *                distribute, sublicense, and/or sell copies of the Software, and to
 *                permit persons to whom the Software is furnished to do so, subject to
 *                the following conditions:
 *
 *                The above copyright notice and this permission notice shall be
 *                included in all copies or substantial portions of the Software.
 *
 *                THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *                EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *                MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *                NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 *                LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 *                OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 *                WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *  \endverbatim
 *  -------------------------------------------------------------------------------------------------------------------
 *  FILE DESCRIPTION
 *  -----------------------------------------------------------------------------------------------------------------*/
/*!        \file  DiagnosticsModel.kt
 *        \brief  Definition of the Diagnostics model as user interface for the plugin.
 *
 *      \details  -
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.diagnostics

import com.vector.ocs.core.api.ModelValidationException
import com.vector.ocs.core.api.PluginModel
import com.vector.ocs.json.api.SchemaDescription
import com.vector.ocs.json.api.SchemaIntEnum
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
data class DiagnosticsModel(
    @SchemaDescription("Automatically setup Dem Primary Memory Blocks. The number of these blocks is calculated from the number of existing DTC Classes multiplied by the diagMemBlocksScaling factor (default: 20%), maximum 50, minimum 8.")
    @EncodeDefault
    val setupDiagMemBlocks: Boolean? = true,
    @SchemaDescription("Scaling factor for setting the amount of Dem Primary Memory Blocks in relation to the demDTCClass size.")
    @EncodeDefault
    val diagMemBlocksScaling: Float? = 0.2f,
    @SchemaDescription("Min Amount of Dem Blocks.")
    @EncodeDefault
    val DiagMemBlocksMin: Int? = 8,
    @SchemaDescription("Max Amount of Dem Blocks.")
    @EncodeDefault
    val DiagMemBlocksMax: Int? = 50,
    @SchemaDescription("Assign the first Dcm Service Table (mostly only one present) to all Dcm protocol connections if they are not referencing any Service Table.")
    @EncodeDefault
    val defaultDcmServiceTableAssignment: Boolean? = true,
    @SchemaDescription("Assign the first Dcm Buffer (mostly only one present) to all Dcm protocol connections if they are not referencing any Dcm Buffer. If no Buffer is available, create one. " +
                                 "There are three options. DISABLED: No buffer will be created or assigned to any Dcm protocol connection. ENABLED_WITH_CALC_SIZE: Buffer will be created and size will be calculated. " +
                                 "ENABLED_WITH_FIXED_SIZE: Buffer will be created and size will be set to defaultDcmBufferSize.")
    @EncodeDefault
    val defaultDcmBufferCreation: EDefaultDcmBuffer = EDefaultDcmBuffer.ENABLED_WITH_CALC_SIZE,
    @SchemaDescription("Sets the size of the present Dcm Buffer. If DefaultDcmBuffer is set to DISABLED or ENABLED_WITH_CALC_SIZE this parameter has no effect.")
    @EncodeDefault
    val defaultDcmBufferSize: Int? = 4095,
    @SchemaDescription("Create a DemClient if not present and reference it at all Dcm protocol connections.")
    @EncodeDefault
    val setupDefaultDemClient: Boolean? = true,
    @SchemaDescription("Setup the default debouncing algorithm of Dem EventClasses based upon defaultDebouncingStrategy.")
    @EncodeDefault
    val setupDefaultDebouncing: Boolean? = true,
    @SchemaDescription("Define Default debouncing strategy for setupDefaultDebouncing.")
    @EncodeDefault
    val defaultDebouncingStrategy: EDefaultDebouncingStrategy? = EDefaultDebouncingStrategy.CounterBased,
    @SchemaDescription("Setting the size of DIDs with variable length to the maximum value possible (65528 Bytes since it needs to be aligned) if the length they are configured with is producing a Validation Error.")
    @EncodeDefault
    val autoCorrectDspDidSignalLengths: Boolean? = true,
    @SchemaDescription("Select all possible communication channels to be handled by the Communication Control Diag Service.")
    @EncodeDefault
    val selectAllChannelsForControlAllChannels: Boolean? = true
) : PluginModel {
    @Suppress("unused")
    @Required
    @SchemaIntEnum(VERSION.toLong())
    @SchemaDescription("The version of this model. Must always be '$VERSION'.")
    private val version: Int = VERSION
    init {
        if ((diagMemBlocksScaling != null) && (diagMemBlocksScaling < 0.0)) {
            throw ModelValidationException("Model parameter \"diagMemBlocksScaling\" should have a value >= 0.0.")
        }
    }
}

enum class EDefaultDcmBuffer {
    DISABLED,
    ENABLED_WITH_FIXED_SIZE,
    ENABLED_WITH_CALC_SIZE
}

private const val VERSION = 2

enum class EDefaultDebouncingStrategy {
    CounterBased,
    TimeBased,
    MonitorInternal
}
