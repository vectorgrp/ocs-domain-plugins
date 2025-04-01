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
 *  ------------------------------------------------------------------------------------------------------------------*/
/*!        \file  GeneralFee.groovy
 *        \brief  consists of General Fee specific configurations and actions
 *
 *      \details  Configuration of
 *
 *                - setReferenceMemIfHwA
 *
 *                - SetupFeePartition
 *
 *                - create Fee Blocks
 *
 *                - set and assign Fee Device Index
 *
 **********************************************************************************************************************/
package com.vector.ocs.plugins.nvmemory

import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.automation.model.ecuc.microsar.memif.MemIf
import com.vector.cfg.automation.model.ecuc.microsar.nvm.NvM
import com.vector.cfg.automation.model.ecuc.microsar.nvm.nvmblockdescriptor.NvMBlockDescriptor
import com.vector.cfg.model.mdf.MIObject
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIContainer
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIModuleConfiguration
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.PluginsCommon
import groovy.transform.PackageScope

/**
 * GeneralFee consists of General Fee specific configurations
 */
@PackageScope
class GeneralFee {
    /**
     * The setReferenceMemIfHwa method is executed during the MAIN phase and will set the container reference of MemIfHwA
     * @param logger instance for OCS specific logging
     * @param FeeGeneralReference input for setting the right reference depending on the activated MemIf lower layers
     */
    static void setReferenceMemIfHwA(OcsLogger logger, List<MIObject> FeeGeneralReference) {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                def MemIfModule = bswmdModel(MemIf.DefRef).singleOrNull
                /* setting RefTarget within MemIfMemHwA */
                    if ((PluginsCommon.ConfigPresent("/MICROSAR/MemIf"))) {
                        transaction {
                            logger.info("Setting MemIfHwA Reference to Microsar Fee.")
                           MemIfModule.memIfMemHwA.byNameOrCreate("MemIfMemHwA").memIfMemHwARefOrCreate.setRefTargetMdf(FeeGeneralReference.first as MIContainer)
                        }
                    } else {
                        logger.info("As there is no MemIf Module in the current configuration available, the setup of the reference of the MemIfHwA container will not be executed.")
                    }
            }
        }
    }

    /**
     * The setupFeePartition method is executed during the MAIN phase and will setup the Fee Partition
     * @param logger instance for OCS specific logging
     * @param Object fee (NvMemoryImpl.groovy), to access the corresponding FeeSetupFeePartition() method of object
     */
    static void setupFeePartition(OcsLogger logger, Object fee) {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                ArrayList<Long> flsData = HelperFunctions.calculateFlsSize()
                long flsSize = flsData[0]
                long flsLowestStartAddress = flsData[1]
                /* split this into two virtual sectors of Fls */
                Boolean sectorSplit = Boolean.FALSE
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if (active_module.name == "Fls") {
                        transaction {
                            active_module.bswmdModel().flsConfigSetOrCreate.flsSectorListOrCreate.flsSector.each { def fls_sector ->
                                long flsSectorStartAddress = flsLowestStartAddress
                                long flsNumberOfSectors = fls_sector.flsNumberOfSectors.value
                                long flsSectorSize = fls_sector.flsSectorSize.value
                                if (sectorSplit == Boolean.FALSE) {
                                    /* Fls start address always starts at 0 (Fls Driver requirement), so only the middle sectors need to be found. */
                                    /* check if sector definition defines the point between lower and upper sector */
                                    if (flsSectorStartAddress + (flsNumberOfSectors * flsSectorSize) >= (flsSize / 2)) {
                                        if (flsSectorStartAddress + (flsNumberOfSectors * flsSectorSize) == (flsSize / 2)) {
                                            /* exactly after this sectorList the half of the data flash is used - split after this sectorList */
                                            sectorSplit = Boolean.TRUE
                                            boolean sectorSplitFound = Boolean.FALSE
                                            fee.FeeSetupFeePartition(logger, flsSize, sectorSplitFound, flsSectorStartAddress, flsNumberOfSectors, flsSectorSize)
                                        } else {
                                            /* sector split found, it is at the half of the count of the FlsSector since all sectors are equally long now */
                                            sectorSplit = Boolean.TRUE
                                            boolean sectorSplitFound = Boolean.TRUE
                                            fee.FeeSetupFeePartition(logger, flsSize, sectorSplitFound, flsSectorStartAddress, flsNumberOfSectors, flsSectorSize)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The assignFeePartitionDeviceIndex method is executed during the MAIN phase within the activation of the model parameter setupFeePartition
     * and will assign the FeePartitionDeviceIndex within the feePartitionConfigurationBlock
     * @param flsConfigSet for setting the reference within fee partition device
     */
    void assignFeePartitionDeviceIndex(List<MIObject> flsConfigSet) {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if (active_module.name == "Fee") {
                        transaction {
                            def FeePartitionConfigurationBlock = active_module.bswmdModel().feePartitionConfiguration.byNameOrCreate("FeePartitionConfiguration")
                            FeePartitionConfigurationBlock.each { fb ->
                                fb.feePartitionDeviceOrCreate.setRefTargetMdf(flsConfigSet)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The createFeeBlocks method is executed during the Cleanup phase and will create FeeBlocks depending on the available NvBlocks in the NvM module
     * @param logger instance for OCS specific logging
     */
    static void createFeeBlocks(OcsLogger logger) {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                logger.info("Create Fee blocks on need.")
                if (PluginsCommon.ConfigPresent("/MICROSAR/NvM")) {
                    NvM nvmCfg = bswmdModel(NvM.DefRef).single
                    activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                        if (active_module.name == "Fee") {
                            transaction {
                                nvmCfg.nvMBlockDescriptor.each { NvMBlockDescriptor nv_block ->
                                    logger.info("Creating (if not yet present) Fee Block " + "OCS_Fee_" + nv_block.shortname + " for NvM Block " + nv_block.shortname + ".")
                                    def feeBlock = active_module.bswmdModel().feeBlockConfiguration.byNameOrCreate("OCS_Fee_" + nv_block.shortname)
                                    nv_block.nvMTargetBlockReferenceOrCreate.nvMFeeRefOrCreate.nvMNameOfFeeBlockOrCreate.setRefTargetMdf(feeBlock.mdfObject as MIContainer)
                                }
                            }
                        }
                    }
                } else {
                    logger.info("As there is no NvM Module in the current configuration available, the creation of the Fee blocks will not be executed.")
                }
            }
        }
    }

    /**
     * The assignFeeBlockDeviceIndex method is executed during the Cleanup phase and the creation of fee blocks
     * It will set feeBlockDeviceIndex Reference within the fee blocks (only needed for FeeFlexNor and Aurix Fee)
     * @param flsMdfGeneral for setting the reference within feeDeviceIndex
     */
    static void assignFeeBlockDeviceIndex(List<MIObject> flsMdfGeneral) {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if (active_module.name == "Fee") {
                        transaction {
                            def feeBlock = active_module.bswmdModel().feeBlockConfiguration
                            feeBlock.each { fb ->
                                fb.feeDeviceIndexOrCreate.setRefTargetMdf(flsMdfGeneral)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The assignFeePartitions method is executed during the Cleanup phase and assign the Fee Partitions within the fee blocks
     */
    static void assignFeePartitions() {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if ((active_module.name) == "Fee") {
                        transaction {
                            def feeBlock = active_module.bswmdModel().feeBlockConfiguration
                            List<MIObject> FeePartitionConfigurationMdf = mdfModel("/ActiveEcuC/Fee/FeePartitionConfiguration")
                            feeBlock.each { fee_block ->
                                if (!fee_block.existsFeePartition()) {
                                    fee_block.feePartitionOrCreate
                                }
                                if (!fee_block.feePartitionOrCreate.hasRefTarget()) {
                                    fee_block.feePartitionOrCreate.setRefTargetMdf(FeePartitionConfigurationMdf)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
