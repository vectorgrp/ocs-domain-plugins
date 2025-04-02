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
/*!        \file  NvmDefRefs.kt
 *        \brief  Class defining Nvm def refs for different releases.
 *
 *      \details
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.diagnostics.constants

import com.vector.ocs.lib.shared.PluginsCommon

abstract class NvMDefRefs {

    /** Name of the processing context */
    open var NVM: String = "/MICROSAR/NvM"
    open var NVM_BLOCK_DESCRIPTOR: String = "$NVM/NvMBlockDescriptor"
    open var NVM_BLOCK_USE_SET_RAM_BLOCK_STATUS: String = ""
    open var NVM_INIT_BLOCK_CALLBACK: String = ""
    open var NVM_SINGLE_BLOCK_CALLBACK: String = ""
    open var NVM_RAM_BLOCK_DATA_ADDRESS: String = ""
    open var NVM_ROM_BLOCK_DATA_ADDRESS = ""
    open var NVM_USE_INIT_CALLBACK = ""
    open var NVM_USE_JOBEND_CALLBACK = ""
    open var NVM_SELECT_BLOCK_FOR_READ_ALL = ""
    open var NVM_SELECT_BLOCK_FOR_WRITE_ALL = ""
    open var NVM_RESISTANT_TO_CHANGED_SOFTWARE = ""
    open var NVM_BLOCK_USE_CRC_COMP_MECHANISM = ""
    open var NVM_BLOCK_CRC_TYPE = ""
    open var NVM_BLOCK_USE_CRC = ""
    open var NVM_BLOCK_MANAGEMENT_TYPE = ""

    init {
        (this).apply {
            NVM_BLOCK_DESCRIPTOR = "$NVM/NvMBlockDescriptor"
            NVM_BLOCK_USE_SET_RAM_BLOCK_STATUS = "$NVM_BLOCK_DESCRIPTOR/NvMBlockUseSetRamBlockStatus"
            NVM_INIT_BLOCK_CALLBACK = "$NVM_BLOCK_DESCRIPTOR/NvMInitBlockCallback"
            NVM_SINGLE_BLOCK_CALLBACK = "$NVM_BLOCK_DESCRIPTOR/NvMSingleBlockCallback"
            NVM_RAM_BLOCK_DATA_ADDRESS = "$NVM_BLOCK_DESCRIPTOR/NvMRamBlockDataAddress"
            NVM_ROM_BLOCK_DATA_ADDRESS = "$NVM_BLOCK_DESCRIPTOR/NvMRomBlockDataAddress"
            NVM_USE_INIT_CALLBACK = "$NVM_BLOCK_DESCRIPTOR/NvMUseInitCallback"
            NVM_USE_JOBEND_CALLBACK = "$NVM_BLOCK_DESCRIPTOR/NvMUseJobendCallback"
            NVM_SELECT_BLOCK_FOR_READ_ALL = "$NVM_BLOCK_DESCRIPTOR/NvMSelectBlockForReadAll"
            NVM_SELECT_BLOCK_FOR_WRITE_ALL = "$NVM_BLOCK_DESCRIPTOR/NvMSelectBlockForWriteAll"
            NVM_RESISTANT_TO_CHANGED_SOFTWARE = "$NVM_BLOCK_DESCRIPTOR/NvMResistantToChangedSw"
            NVM_BLOCK_USE_CRC_COMP_MECHANISM = "$NVM_BLOCK_DESCRIPTOR/NvMBlockUseCRCCompMechanism"
            NVM_BLOCK_CRC_TYPE = "$NVM_BLOCK_DESCRIPTOR/NvMBlockCrcType"
            NVM_BLOCK_USE_CRC = "$NVM_BLOCK_DESCRIPTOR/NvMBlockUseCrc"
            NVM_BLOCK_MANAGEMENT_TYPE = "$NVM_BLOCK_DESCRIPTOR/NvMBlockManagementType"
        }
    }

    open class NvMDefRefsR31 : NvMDefRefs() {
        init {

        }
    }

    object NvMDefRefConstantsFactory {
        fun getConstants(): NvMDefRefs {
            val minorVersion = PluginsCommon.Cfg5MinorVersion()
            return when {
                minorVersion.toInt() >= 28 -> NvMDefRefsR31()
                else -> throw IllegalArgumentException("Unknown version: $minorVersion")
            }
        }
    }
}
