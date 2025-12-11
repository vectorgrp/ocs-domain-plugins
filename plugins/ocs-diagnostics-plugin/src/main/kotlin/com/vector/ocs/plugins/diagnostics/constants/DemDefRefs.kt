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
/*!        \file  DemDefRefs.kt
 *        \brief  Class defining Dem def refs for different releases.
 *
 *      \details
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.diagnostics.constants

abstract class DemDefRefs {
    /** R31 */
    open val DEM: String = "/MICROSAR/Dem"
    open var DEM_CONFIG_SET: String = ""
    open var DEM_GENERAL: String = ""
    open var DEM_EVENT_MEMORY_SET = ""
    open var DEM_EVENT_PARAMETER: String = ""
    open var DEM_EVENT_CLASS: String = ""
    open var DEM_CLIENT: String = ""
    open var DEM_FREEZE_FRAME_RECORD_CLASS = ""
    open var DEM_FREEZE_FRAME_RECORD_UPDATE = ""
    open var DEM_DTC_CLASS = ""
    open var DEM_DTC_ATTRIBUTES = ""
    open var DEM_DTC_ATTRIBUTES_REF = ""
    open var DEM_PRIMARY_MEMORY = ""
    open var DEM_MAX_NUMBER_EVENT_ENTRY_PRIMARY = ""

    var DEM_DEBOUNCE_ALGORITHM_CLASS: String = ""
    open var DEM_DEBOUNCE_COUNTER_BASED: String = ""
    open var DEM_DEBOUNCE_MONITOR_INTERNAL: String = ""
    open var DEM_DEBOUNCE_TIME_BASE: String = ""
    var DEM_DEBOUNCE_TIME_PASSED_THRESHOLD: String = ""
    open var DEM_DEBOUNCE_TIME_FAILED_THRESHOLD: String = ""
    open var DEM_DEBOUNCE_TIME_BASED_CLASS: String = ""

    open var DEM_NV_RAM_BLOCK_ID: String = ""
    open var DEM_NV_RAM_BLOCK_ID_REF: String = ""
    open var DEM_NV_RAM_BLOCK_ID_TYPE: String = ""
    open var DEM_NV_RAM_BLOCK_ID_INDEX: String = ""
    open var DEM_NV_RAM_BLOCK_ID_EVENT_MEMORY_REF: String = ""

    init {
        (this).apply {
            DEM_CONFIG_SET = "$DEM/DemConfigSet"
            DEM_GENERAL = "$DEM/DemGeneral"
            DEM_EVENT_MEMORY_SET = "$DEM_GENERAL/DemEventMemorySet"
            DEM_EVENT_PARAMETER = "$DEM_CONFIG_SET/DemEventParameter"
            DEM_EVENT_CLASS = "$DEM_EVENT_PARAMETER/DemEventClass"
            DEM_CLIENT = "$DEM_GENERAL/DemClient"
            DEM_FREEZE_FRAME_RECORD_CLASS = "$DEM_GENERAL/DemFreezeFrameRecordClass"
            DEM_FREEZE_FRAME_RECORD_UPDATE = "$DEM_FREEZE_FRAME_RECORD_CLASS/DemFreezeFrameRecordUpdate"
            DEM_DTC_CLASS = "$DEM_CONFIG_SET/DemDTCClass"
            DEM_DTC_ATTRIBUTES = "$DEM_CONFIG_SET/DemDTCAttributes"
            DEM_DTC_ATTRIBUTES_REF = "$DEM_DTC_CLASS/DemDTCAttributesRef"
            DEM_PRIMARY_MEMORY = "$DEM_EVENT_MEMORY_SET/DemPrimaryMemory"
            DEM_MAX_NUMBER_EVENT_ENTRY_PRIMARY = "$DEM_PRIMARY_MEMORY/DemMaxNumberEventEntryPrimary"

            DEM_DEBOUNCE_ALGORITHM_CLASS = "$DEM_EVENT_PARAMETER/DemDebounceAlgorithmClass"
            DEM_DEBOUNCE_COUNTER_BASED = "$DEM_DEBOUNCE_ALGORITHM_CLASS/DemDebounceCounterBased"
            DEM_DEBOUNCE_MONITOR_INTERNAL = "$DEM_DEBOUNCE_ALGORITHM_CLASS/DemDebounceMonitorInternal"
            DEM_DEBOUNCE_TIME_BASE = "$DEM_DEBOUNCE_ALGORITHM_CLASS/DemDebounceTimeBase"
            DEM_DEBOUNCE_TIME_BASED_CLASS = "$DEM_CONFIG_SET/DemDebounceTimeBasedClass"
            DEM_DEBOUNCE_TIME_PASSED_THRESHOLD = "$DEM_DEBOUNCE_TIME_BASED_CLASS/DemDebounceTimePassedThreshold"
            DEM_DEBOUNCE_TIME_FAILED_THRESHOLD = "$DEM_DEBOUNCE_TIME_BASED_CLASS/DemDebounceTimeFailedThreshold"

            DEM_NV_RAM_BLOCK_ID = "$DEM_GENERAL/DemNvRamBlockId"
            DEM_NV_RAM_BLOCK_ID_REF = "$DEM_NV_RAM_BLOCK_ID/DemNvRamBlockIdRef"
            DEM_NV_RAM_BLOCK_ID_TYPE = "$DEM_NV_RAM_BLOCK_ID/DemNvRamBlockIdType"
            DEM_NV_RAM_BLOCK_ID_INDEX = "$DEM_NV_RAM_BLOCK_ID/DemNvRamBlockIdIndex"
            DEM_NV_RAM_BLOCK_ID_EVENT_MEMORY_REF = "$DEM_NV_RAM_BLOCK_ID/DemNvRamBlockIdEventMemoryRef"
        }
    }

    open class DemDefRefsR35() : DemDefRefs() {
        init {

        }
    }

    object DemDefRefConstantsFactory {
        fun getConstants(): DemDefRefs {
            return DemDefRefsR35()
        }
    }
}
