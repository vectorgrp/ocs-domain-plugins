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
/*!        \file  NvMemoryModel.kt
 *        \brief  Definition of the NvMemory model as user interface for the plugin.
 *
 *      \details  -
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.nvmemory

import com.vector.ocs.core.api.PluginModel
import com.vector.ocs.json.api.SchemaDescription
import com.vector.ocs.json.api.SchemaIntEnum
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

private const val VERSION = 5

@Serializable
data class NvMemoryModel(
    @SchemaDescription("Setup a default Fee/Ea Partition depending on the Sector definition of the lower layer.")
    @EncodeDefault
    val setupDefaultFeeEaPartition: Boolean? = true,
    @SchemaDescription("Assignment of default Fee/Ea Partition to Fee/Ea Blocks where the Partition Reference is missing.")
    @EncodeDefault
    val assignFeeEaPartition: Boolean? = true,
    @SchemaDescription("Creation of MemAbstraction (Fee/Ea) Blocks if NvM Blocks miss their Fee/Ea Reference.")
    @EncodeDefault
    val createFeeEaBlocks: Boolean? = true,
    @SchemaDescription("Reference the MemIf Hardware Abstraction Layer (MemIfHwA) of MemIf Module.")
    @EncodeDefault
    val referenceMemIfHwA: Boolean? = true,
    @SchemaDescription("Activation of NvM Stack related Modules (NvM, Fee, Fls, Ea, Crc, MemIf). First priority: MICROSAR Fee and Fls. Second priority: Infineon Fee/Fls. Third priority: MICROSAR Ea.")
    @EncodeDefault
    val activateMemModules: ActivationModel? = ActivationModel(
        activateMicrosarFeeFls = false,
        activateAurixFeeFls = false,
        activateMicrosarFeeFlexNorFls = true,
        activateMicrosarEaEep = false
    ),
    @SchemaDescription("Activation of the modules Fls_30_vMemAccM/Eep_30_vMemAccM, vMemAccM, vMem and Configuration of the vMem Solution.")
    @EncodeDefault
    val vMemSolution: VMemSolutionModel? = VMemSolutionModel(
        activateVMemSolution = false,
        flsNumberOfSectors = 16,
        flsPageSize = 8,
        flsSectorSize = 8192,
        flsStartAddress = 0,
        eepNumberOfSectors = 16,
        eepPageSize = 8,
        eepSectorSize = 512,
        eepStartAddress = 0
    )
) : PluginModel {
    @Suppress("unused")
    @Required
    @SchemaIntEnum(VERSION.toLong())
    @SchemaDescription("The version of this model. Must always be '$VERSION'.")
    private val version: Int = VERSION
}

@Serializable
data class ActivationModel(
    @SchemaDescription("Activation of MicrosarPluginsCommon Fee and Fls.")
    @EncodeDefault
    val activateMicrosarFeeFls: Boolean? = true,

    @SchemaDescription("Activation of Aurix Fee and Fls.")
    @EncodeDefault
    val activateAurixFeeFls: Boolean? = false,

    @SchemaDescription("Activation of Microsar FeeFlexNor and Fls.")
    @EncodeDefault
    val activateMicrosarFeeFlexNorFls: Boolean? = false,

    @SchemaDescription("Activation of Microsar FeeFlexNor and Fls.")
    @EncodeDefault
    val activateMicrosarEaEep: Boolean? = false
)

@Serializable
data class VMemSolutionModel(
    @SchemaDescription("Activation of the modules Fls_30_vMemAccM, vMemAccM, vMem and Configuration of the vMem Solution.")
    @EncodeDefault
    val activateVMemSolution: Boolean? = false,

    @SchemaDescription("Number of Fls Sectors.")
    @EncodeDefault
    val flsNumberOfSectors: Int? = 16,

    @SchemaDescription("Fls Page Size in Byte.")
    @EncodeDefault
    val flsPageSize: Int? = 8,

    @SchemaDescription("Fls Sector Size in Byte.")
    @EncodeDefault
    val flsSectorSize: Int? = 8192,

    @SchemaDescription("Fls Sector Start Address.")
    @EncodeDefault
    val flsStartAddress: Long? = 0,

    @SchemaDescription("Number of Ea Sectors.")
    @EncodeDefault
    val eepNumberOfSectors: Int? = 16,

    @SchemaDescription("Ea Page Size in Byte.")
    @EncodeDefault
    val eepPageSize: Int? = 8,

    @SchemaDescription("Ea Sector Size in Byte.")
    @EncodeDefault
    val eepSectorSize: Int? = 512,

    @SchemaDescription("Ea Sector Start Address.")
    @EncodeDefault
    val eepStartAddress: Long? = 0
)
