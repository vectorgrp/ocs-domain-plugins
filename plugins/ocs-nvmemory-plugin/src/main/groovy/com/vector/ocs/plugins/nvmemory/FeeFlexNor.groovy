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
/*!        \file  FeeFlexNor.groovy
 *        \brief  consists of Microsar Fee specific configurations, actions and mdf model
 *
 *      \details  Module Activation of
 *                - NvM, Crc, MemIf, FeeFlexNor
 *
 *                - define FeeGeneralMdf
 *
 *                - SetupFeePartition for configuration of Microsar FeeFlexNor specific containers
*
 **********************************************************************************************************************/
package com.vector.ocs.plugins.nvmemory
import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.model.mdf.MIObject
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIModuleConfiguration
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.PluginsCommon
/**
 * FeeFlexNor consists of FeeFlexNor specific configurations, actions and mdf model
 */
class FeeFlexNor extends GeneralFee {
     public List<MIObject> FeeGeneralMdf
    /**
     * The method init is executed during the MAIN phase and is called after the object fee (NvMemoryImpl.groovy) is created
     * init will activate the corresponding modules
     * @param logger instance of logger
     */
    void init(OcsLogger logger){
        PluginsCommon.ModuleActivation("/MICROSAR", "NvM", logger)
        PluginsCommon.ModuleActivation("/MICROSAR", "Crc", logger)
        PluginsCommon.ModuleActivation("/MICROSAR", "MemIf", logger)
        PluginsCommon.ModuleActivation("/MICROSAR/Fee_30_FlexNor", "Fee", logger)
    }

    /**
     * The method initMdfModels is executed during the MAIN and Cleanup phase and is called after the object fee (NvMemoryImpl.groovy) is created
     * mdfModels will create the mdf models for specific Fee References which are needed for other module parameters.
     */
    void initMdfModels(){
        FeeGeneralMdf = ScriptApi.activeProject.mdfModel("/ActiveEcuC/Fee/FeeGeneral")
    }

    /**
     * The assignFeePartitionDeviceIndex method is executed during the MAIN phase within the activation of the model parameter setupFeePartition
     * and will assign the FeePartitionDeviceIndex within the feePartitionConfigurationBlock
     * @param flsConfigSet for setting the reference within fee partition device
     */
    @Override
    void assignFeePartitionDeviceIndex(List<MIObject> flsConfigSet) {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if (active_module.name == "Fee") {
                        transaction {
                            def FeePartitionConfigurationBlock = active_module.bswmdModel().feePartitionConfiguration.byNameOrCreate("FeePartitionConfiguration")
                            FeePartitionConfigurationBlock.each { fb ->
                                fb.feePartitionTargetOrCreate.setRefTargetMdf(flsConfigSet)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The FeeSetupFeePartition method is executed during the MAIN phase and is called by setupFeePartition
     * The method will set the FeePartition settings for FeeFlexNor within the FeePartition Configuration Block depending on fls settings
     * @param logger instance for OCS specific logging
     * @param flsSize for calculation of Fee Partition
     * @param sectorSplitFound for setting the FeePartitionBlock parameter values depending on if there is a sector split found
     * @param flsSectorStartAddress for setting the LowerSectorAddress in the FeePartitionBlock
     * @param flsNumberOfSectors for calculation/setup of Fee Partition
     * @param flsSectorSize for calculation/setup of Fee Partition
     */
    static FeeSetupFeePartition(OcsLogger logger, long flsSize, boolean sectorSplitFound, long flsSectorStartAddress, long flsNumberOfSectors, long flsSectorSize) {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if (active_module.name == "Fee") {
                        transaction {
                            /* split this into two virtual sectors of Fls */
                            def FeePartitionConfigurationBlock = active_module.bswmdModel().feePartitionConfiguration.byNameOrCreate("FeePartitionConfiguration")
                            def FeeSectorConfiguration1 = FeePartitionConfigurationBlock.feeSectorConfiguration.byNameOrCreate("FeeSectorConfiguration")
                            FeeSectorConfiguration1.feeSectorAddressOrCreate.setValueMdf(flsSectorStartAddress)
                            def FeeSectorConfiguration2 = FeePartitionConfigurationBlock.feeSectorConfiguration.byNameOrCreate("FeeSectorConfiguration_1")
                            if (!sectorSplitFound) {
                                /* exactly after this sectorList the half of the data flash is used - split after this sectorList */
                                FeeSectorConfiguration1.feeSectorSizeOrCreate.value = flsSectorStartAddress + (flsNumberOfSectors * flsSectorSize)
                                FeeSectorConfiguration2.feeSectorAddressOrCreate.value = flsSectorStartAddress + (flsNumberOfSectors * flsSectorSize)
                                FeeSectorConfiguration2.feeSectorSizeOrCreate.value = (flsSize - (flsSectorStartAddress + (flsNumberOfSectors * flsSectorSize)))
                            } else if (sectorSplitFound) {
                                /* sector split found, it is at the half of the count of the FlsSector since all sectors are equally long now */
                                FeeSectorConfiguration1.feeSectorSizeOrCreate.value = (flsSectorStartAddress + (flsNumberOfSectors * flsSectorSize / 2))
                                FeeSectorConfiguration2.feeSectorAddressOrCreate.value = (flsSectorStartAddress + (flsNumberOfSectors * flsSectorSize / 2))
                                FeeSectorConfiguration2.feeSectorSizeOrCreate.value = (flsSize - (flsSectorStartAddress + (flsNumberOfSectors * flsSectorSize / 2)))
                                logger.info("Sector split found - values are: LowerSectorSize and upper sector start address: " + flsSectorStartAddress + (flsNumberOfSectors * flsSectorSize / 2) + " Upper Sector Size: " + (flsSize - (flsSectorStartAddress + (flsNumberOfSectors * flsSectorSize / 2))) + ".")
                            }
                        }
                    }
                }
            }
        }
    }
}
