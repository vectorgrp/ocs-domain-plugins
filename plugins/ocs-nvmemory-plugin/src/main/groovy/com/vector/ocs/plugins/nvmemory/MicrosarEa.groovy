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
/*!        \file  MicrosarEa.groovy
 *        \brief  consists of Microsar Ea specific configurations, actions and mdf model
 *
 *      \details  Module Activation of
 *                - NvM, Crc, MemIf, Ea
*
 **********************************************************************************************************************/
package com.vector.ocs.plugins.nvmemory
import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.automation.model.ecuc.microsar.nvm.NvM
import com.vector.cfg.automation.model.ecuc.microsar.nvm.nvmblockdescriptor.NvMBlockDescriptor
import com.vector.cfg.model.mdf.MIObject
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIContainer
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIModuleConfiguration
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.PluginsCommon

/**
 * MicrosarEa consists of Microsar Ea specific configurations, actions and mdf model
 */
class MicrosarEa {
    List<MIObject> EaGeneralMdf
    List<MIObject> EaPartitionConfigurationMdf
    /**
     * The method init is executed during the MAIN phase and is called after the object ea (NvMemoryImpl.groovy) is created
     * init will activate the corresponding modules
     * @param logger instance of the logger
     */
    void init(OcsLogger logger) {
        PluginsCommon.ModuleActivation("/MICROSAR", "NvM", logger)
        PluginsCommon.ModuleActivation("/MICROSAR", "Crc", logger)
        PluginsCommon.ModuleActivation("/MICROSAR", "MemIf", logger)
        PluginsCommon.ModuleActivation("/MICROSAR", "Ea", logger)
    }

    /**
     * The method initMdfModels is executed during the MAIN and Cleanup phase and is called after the object fee (NvMemoryImpl.groovy) is created
     * mdfModels will create the mdf models for specific fee References which are needed for other module parameters.
     */
    void initMdfModels() {
        EaPartitionConfigurationMdf = ScriptApi.activeProject.mdfModel("/ActiveEcuC/Ea/EaPartitionConfiguration")
        EaGeneralMdf = ScriptApi.activeProject.mdfModel("/ActiveEcuC/Ea/EaGeneral")
    }

    /**
     * The setReferenceMemIfHwa method is executed during the MAIN phase and will set the container reference of MemIfHwA
     * @param logger instance for OCS specific logging
     */
    void setReferenceMemIfHwA(OcsLogger logger) {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                /* setting RefTarget within MemIfMemHwA */
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if (active_module.name == "MemIf") {
                        transaction {
                            logger.info("Setting MemIfHwA Reference to Microsar Ea.")
                            active_module.bswmdModel().memIfMemHwA.byNameOrCreate("MemIfMemHwA").memIfMemHwARefOrCreate.setRefTargetMdf(EaGeneralMdf.first as MIContainer)
                        }
                    } else {
                        logger.info("As there is no MemIf Module in the current configuration available, the setup of the reference of the MemIfHwA container will not be executed.")
                    }
                }
            }
        }
    }

    /**
     * The SetupEaPartition method is executed during the MAIN phase
     * The method will set the Ea Partition settings for Microsar Ea within the Ea Partition Configuration Block depending on Eep settings
     * Method used for Microsar Eep module
     */
    static void setupEaPartition() {
        ArrayList<Long> EepData = MicrosarEep.getEepSettings()
        int EepSize = EepData[0] as int
        long EepBaseAddress = EepData[1]
        long EepEraseUnitSize = EepData[2]
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if (active_module.name == "Ea") {
                        transaction {
                            def EaPartitionConfigurationBlock = active_module.bswmdModel().eaPartitionConfiguration.byNameOrCreate("EaPartitionConfiguration")
                            def EaPartitionWriteAlignment = EaPartitionConfigurationBlock.eaPartitionWriteAlignment
                            def EaPartitionAddressAlignment = EaPartitionConfigurationBlock.eaPartitionAddressAlignment
                            EaPartitionConfigurationBlock.eaPartitionSize.value = EepSize
                            EaPartitionConfigurationBlock.eaPartitionStartAddress.value = EepBaseAddress
                            if (EaPartitionWriteAlignment.value < EepEraseUnitSize) {
                                EaPartitionWriteAlignment.setValue(EepEraseUnitSize)
                            }
                            EaPartitionAddressAlignment.setValue(EaPartitionWriteAlignment.value)
                        }
                    }
                }
            }
        }
    }

    /**
     * The SetupEaPartition method is executed during the MAIN phase
     * The method will set the Ea Partition settings for Microsar Ea within the Ea Partition Configuration Block depending on Eep settings
     * Method used for Eep_30_vMemAccM module
     */
    static void setupEaPartition(int eepNumberOfSectors, long eepBaseAddress, int eepSectorSize) {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                int EepSize = eepNumberOfSectors * eepSectorSize
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if (active_module.name == "Ea") {
                        transaction {
                            def EaPartitionConfigurationBlock = active_module.bswmdModel().eaPartitionConfiguration.byNameOrCreate("EaPartitionConfiguration")
                            EaPartitionConfigurationBlock.eaPartitionWriteAlignment.value = eepSectorSize
                            def EaPartitionAddressAlignment = EaPartitionConfigurationBlock.eaPartitionAddressAlignment
                            EaPartitionConfigurationBlock.eaPartitionSize.value = EepSize
                            EaPartitionConfigurationBlock.eaPartitionStartAddress.value = eepBaseAddress
                            EaPartitionAddressAlignment.setValue(eepSectorSize)
                        }
                    }
                }
            }
        }
    }

    /**
     * The assignEaPartitionDeviceIndex method is executed during the MAIN phase within the activation of the model parameter setupEaPartition
     * and will assign the EaPartitionDeviceIndex within the eaPartitionConfigurationBlock
     * @param eepConfigSet for setting the reference within ea partition device
     */
    static void assignEaPartitionDeviceIndex(List<MIObject> eepMdfConfigSet) {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if (active_module.name == "Ea") {
                        transaction {
                            def EaPartitionConfigurationBlock = active_module.bswmdModel().eaPartitionConfiguration.byNameOrCreate("EaPartitionConfiguration")
                            EaPartitionConfigurationBlock.each { eb ->
                                eb.eaPartitionDeviceOrCreate.setRefTargetMdf(eepMdfConfigSet)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The createEaBlocks method is executed during the Cleanup phase and will create EaBlocks depending on the available NvBlocks in the NvM module
     * @param logger instance for OCS specific logging
     */
    void createEaBlocks(OcsLogger logger) {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                logger.info("Create Fee blocks on need.")
                NvM nvmCfg = bswmdModel(NvM.DefRef).single
                if (PluginsCommon.ConfigPresent("/MICROSAR/NvM")) {
                    activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                        if (active_module.name == "Ea") {
                            transaction {
                                nvmCfg.nvMBlockDescriptor.each { NvMBlockDescriptor nv_block ->
                                    logger.info("Creating (if not yet present) Fee Block " + "OCS_Ea_" + nv_block.shortname + " for NvM Block " + nv_block.shortname + ".")
                                    def eaBlock = active_module.bswmdModel().eaBlockConfiguration.byNameOrCreate("OCS_Ea_" + nv_block.shortname)
                                    nv_block.nvMTargetBlockReferenceOrCreate.nvMEaRefOrCreate.nvMNameOfEaBlockOrCreate.setRefTargetMdf(eaBlock.mdfObject as MIContainer)
                                }
                            }
                        }
                    }
                } else {
                    logger.info("As there is no NvM Module in the current configuration available, the creation of the Ea blocks will not be executed.")
                }
            }
        }
    }

    /**
     * The setNumberOfWriteCycles method is executed during the Cleanup phase and will set the Number of Write Cycles of the available Ea blocks. It will be set to 1000.
     * @param numberOfWriteCycles for setting the number of write cycles
     */
    void setNumberOfWriteCycles(int numberOfWriteCycles) {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if (active_module.name == "Ea") {
                        transaction {
                            def eaBlock = active_module.bswmdModel().eaBlockConfiguration
                            eaBlock.each { fb ->
                                fb.eaNumberOfWriteCycles.setValue(numberOfWriteCycles)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The assignEaBlockDeviceIndex method is executed during the Cleanup phase and the creation of ea blocks
     * It will set eaBlockDeviceIndex Reference within the ea blocks
     * @param eepMdfGeneral for setting the reference within eaDeviceIndex
     */
    void assignEaBlockDeviceIndex(List<MIObject> eepMdfGeneral) {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if (active_module.name == "Ea") {
                        transaction {
                            def eaBlock = active_module.bswmdModel().eaBlockConfiguration
                            eaBlock.each { fb ->
                                fb.eaDeviceIndexOrCreate.setRefTargetMdf(eepMdfGeneral)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The assignEaPartitions method is executed during the Cleanup phase and assign the Ea Partitions within the Ea blocks
     */
    void assignEaPartitions() {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if ((active_module.name) == "Ea") {
                        transaction {
                            def eaBlock = active_module.bswmdModel().eaBlockConfiguration
                            eaBlock.each { ea_block ->
                                if (!ea_block.existsEaPartition()) {
                                    ea_block.eaPartitionOrCreate
                                }
                                if (!ea_block.eaPartitionOrCreate.hasRefTarget()) {
                                    ea_block.eaPartitionOrCreate.setRefTargetMdf(EaPartitionConfigurationMdf)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
