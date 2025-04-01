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
/*!        \file  NvMemoryImpl.groovy
 *        \brief  The NvMemoryImpl addresses configuration elements that belong to the memory stack.
 *
 *      \details  Depending on the customer input the plugin will process the following implementation:
 *                - Activation of the Module Fee/Ea/Fls/Eep/NvM/MemIf/Crc if present in the SIP
 *                (activateMemModules)
 *
 *                - Creation of Memory Abstraction Fee/Ea Blocks if NvM Blocks miss their Fee/Ea Reference
 *                (createFeeEaBlocks)
 *
 *                - Assignment of default Fee/Ea Partition to Fee/Ea Blocks where the Partition Reference is missing
 *                (assignFeeEaPartition)
 *
 *                - Setup of default Fee/Ea Partition depending on the Sector definition of the lower layer (Fls/Eep)
 *                (setupDefaultFeeEaPartition)
 *
 *                - Execution of vMem Solution: Configuration of vMem, vMemAccM, Fls_30_vMemAccM
 **********************************************************************************************************************/
package com.vector.ocs.plugins.nvmemory
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.PluginsCommon
import groovy.transform.PackageScope

/**
 * NvMemoryImpl adds two actions to the pipeline phase [PipelinePhase.MAIN, PipelinePhase.CLEANUP].
 * MAIN creates a default partition for the classic Fee and sets the reference of the MemIfHwA container
 * CLEANUP iterates over all NvM Blocks and adds - if necessary - another Fee block, assigns fee partitions and executes Solving Actions
 *
 * Supported MemIf lower layers: Vector classic Fee, Vector FeeFlexNor, Infineon Fee
 *
 * When the plugin is executed, it is expected that a project is loaded and active.
 */
@PackageScope
class NvMemoryImpl {
    /**
     * The run method is executed during the MAIN phase and will address the configuration related to the NvM stack.
     * @param model input for the processing of the configuration elements
     * @param logger instance for OCS specific logging
     */
    static void runNvMemoryConfig(NvMemoryModel model, OcsLogger logger) {
        /* Check if VTT is included in the target type (VTT or DualTarget). If yes the vMem Solution can be processed */
        Boolean activateVMemSolution = false
        if (model.VMemSolution.activateVMemSolution) {
            logger.info("The vMem Solution can currently not be used with a Real Target configuration. If the Target Type of the project is set to Real Target the vMem Solution will not be processed")
        }
        if (PluginsCommon.ConfigPresent("/MICROSAR/VTT/VTTvSet") && model.VMemSolution.activateVMemSolution) {
            activateVMemSolution = true
        }

        def fee
        def fls
        logger.info("activateMemModules: If more than one model parameter within activateMemModules has been set to true by the user, the priority of the set parameters determines which MemIf lower layer is activated. Priorities: Microsar Fee > Microsar FeeFlexNor > Aurix Fee > Microsar Ea.")
            /**
            ********************************************* Microsar Fee/Fls *********************************************
            */
        if (model.activateMemModules.activateMicrosarFeeFls || ((PluginsCommon.ConfigPresent("/MICROSAR/Fee")) && ((PluginsCommon.ConfigPresent("/MICROSAR/Fls")) || (PluginsCommon.ConfigPresent("/MICROSAR/Fls_30_vMemAccM/Fls"))))) {
            fee = new MicrosarFee()
            if ((model.activateMemModules.activateMicrosarFeeFls) || activateVMemSolution) {
                fee.init(logger)
            }
            fee.initMdfModels()
            if (activateVMemSolution) {
                fls = new MicrosarFlsvMemAccM()
                fls.init(logger)
                fls.initMdfModels()
                def vmem = new vMem(logger)
                def vmemaccm = new vMemAccM(logger)
                vmemaccm.setAddressAreaConfigurationSettings(logger, vmem.vMemSectorReference, model.VMemSolution.flsNumberOfSectors, model.VMemSolution.flsStartAddress)
                vmem.setSectorSettings(logger, model.VMemSolution.flsNumberOfSectors, model.VMemSolution.flsPageSize, model.VMemSolution.flsSectorSize, model.VMemSolution.flsStartAddress)
                fls.setSubAddressAreaReference(logger, vmemaccm.vMemAccMSubAddressAreConfigurationReference)
                fls.setSpecifiedEraseCycles(logger)
                fls.createFlsConfigSetVTTFls()
            } else {
                fls = new MicrosarFls()
                if (model.activateMemModules.activateMicrosarFeeFls) {
                    fls.init(logger)
                }
                fls.initMdfModels()
                logger.info("Due to disabled model parameter activate_vMemSolution, no configuration related to the vMemSolution (vMemAccM, vMem and Fls_30_vMemAccM) will be executed.")
            }
            /**
             ********************************************* Microsar FeeFlexNor/Fls and vMemSolution *********************************************
             */
        } else if (model.activateMemModules.activateMicrosarFeeFlexNorFls || (((PluginsCommon.ConfigPresent("/MICROSAR/Fls")) || (PluginsCommon.ConfigPresent("/MICROSAR/Fls_30_vMemAccM/Fls"))) && (PluginsCommon.ConfigPresent("/MICROSAR/Fee_30_FlexNor/Fee")))) {
            fee = new FeeFlexNor()
            if (model.activateMemModules.activateMicrosarFeeFlexNorFls || activateVMemSolution) {
                fee.init(logger)
            }
            fee.initMdfModels()
            if (activateVMemSolution) {
                fls = new MicrosarFlsvMemAccM()
                fls.init(logger)
                fls.initMdfModels()
                def vmem = new vMem(logger)
                def vmemaccm = new vMemAccM(logger)
                vmemaccm.setAddressAreaConfigurationSettings(logger, vmem.vMemSectorReference, model.VMemSolution.flsNumberOfSectors, model.VMemSolution.flsStartAddress)
                vmem.setSectorSettings(logger, model.VMemSolution.flsNumberOfSectors, model.VMemSolution.flsPageSize, model.VMemSolution.flsSectorSize, model.VMemSolution.flsStartAddress)
                fls.setSubAddressAreaReference(logger, vmemaccm.vMemAccMSubAddressAreConfigurationReference)
                fls.setSpecifiedEraseCycles(logger)
                fls.createFlsConfigSetVTTFls()
            } else {
                fls = new MicrosarFls()
                if (model.activateMemModules.activateMicrosarFeeFlexNorFls) {
                    fls.init(logger)
                }
                fls.initMdfModels()
                logger.info("Due to disabled model parameter activate_vMemSolution, no configuration related to the vMemSolution (vMemAccM, vMem and Fls_30_vMemAccM) will be executed.")
            }
            /**
             ********************************************* Aurix Fee/Fls *********************************************
             */
        } else if ((model.activateMemModules.activateAurixFeeFls == true) || ((PluginsCommon.ConfigPresent("/AURIX2G/EcucDefs/Fee")) && (PluginsCommon.ConfigPresent("/AURIX2G/EcucDefs/Fls")))) {
            fee = new AurixFee()
            fls = new AurixFls()
            if (model.activateMemModules.activateAurixFeeFls == true) {
                fee.init(logger)
                fls.init(logger)
            }
            fee.initMdfModels()
            /**
             ********************************************* Microsar Ea/Eep and vMemSolution *********************************************
             */
        } else if (model.activateMemModules.activateMicrosarEaEep || (((PluginsCommon.ConfigPresent("/MICROSAR/Eep")) || (PluginsCommon.ConfigPresent("/MICROSAR/Eep_30_vMemAccM/Eep"))) && (PluginsCommon.ConfigPresent("/MICROSAR/Ea")))) {
            MicrosarEa ea = new MicrosarEa()
            def eep
            if ((model.activateMemModules.activateMicrosarEaEep) || activateVMemSolution) {
                ea.init(logger)
            }
            ea.initMdfModels()
            if (activateVMemSolution) {
                def vmem = new vMem(logger)
                def vmemaccm = new vMemAccM(logger)
                eep = new MicrosarEepvMemAccM()
                eep.init(logger)
                eep.initMdfModels()
                eep.setEepDefaultSettingsInitConfiguration(logger, model, model.VMemSolution.eepNumberOfSectors, model.VMemSolution.eepSectorSize, model.VMemSolution.eepStartAddress, "Eep", activateVMemSolution)
                eep.setEepDefaultSettingsPublishedInformation(logger, model.VMemSolution.eepSectorSize, model.VMemSolution.eepPageSize)
                eep.setAddressAreaReference(logger, vmemaccm.vMemAccMAddressAreaConfigurationReference)
                eep.createEepDriverIndex(logger)
                eep.setEepDefaultMode()
                eep.setupEepMainFunction(logger)
                vmem.setSectorSettings(logger, model.VMemSolution.eepNumberOfSectors, model.VMemSolution.eepPageSize, model.VMemSolution.eepSectorSize, model.VMemSolution.eepStartAddress)
                vmemaccm.setAddressAreaConfigurationSettings(logger, vmem.vMemSectorReference, model.VMemSolution.eepNumberOfSectors, model.VMemSolution.eepStartAddress)
            } else {
                eep = new MicrosarEep()
                if (model.activateMemModules.activateMicrosarEaEep) {
                    eep.init(logger)
                }
                eep.initMdfModels()
                eep.createEepDriverIndex(logger)
                eep.setEepDefaultSettingsInitConfiguration(logger, model, 16, 512, 0, "Eep", activateVMemSolution)
                eep.setEepDefaultMode()
                logger.info("Due to disabled model parameter activate_VMemSolution, no configuration related to the vMemSolution (vMemAccM, vMem and Eep_30_vMemAccM) will be executed.")
            }
            if (((PluginsCommon.ConfigPresent("/MICROSAR/Eep")) || (PluginsCommon.ConfigPresent("/MICROSAR/Eep_30_vMemAccM/Eep"))) && (PluginsCommon.ConfigPresent("/MICROSAR/Ea"))) {
                if (model.referenceMemIfHwA == true) {
                    ea.setReferenceMemIfHwA(logger)
                } else {
                    logger.info("Due to disabled model parameter referenceMemIfHwA, no setup of the reference of the MemIfHwA container will be executed.")
                }
                if (model.setupDefaultFeeEaPartition == true) {
                    if (activateVMemSolution) {
                        ea.setupEaPartition(model.VMemSolution.eepNumberOfSectors, model.VMemSolution.eepStartAddress, model.VMemSolution.eepSectorSize)
                    } else {
                        ea.setupEaPartition()
                    }
                    ea.assignEaPartitionDeviceIndex(eep.eepMdfConfigSet)
                } else {
                    logger.info("Due to disabled model parameter setupDefaultFeeEaPartition, no setup of the Fee Partition will be executed.")
                }
            } else {
                logger.info("As there are no Microsar Ea or no Eep Module in the current configuration available, neither the setup of the default EaPartition nor the setup of the reference of the MemIfHwA container will be executed.")
            }
        }
        /**
         ********************************************* General Fee/Fls *********************************************
         */
        if (((PluginsCommon.ConfigPresent("/MICROSAR/Fee")) && ((PluginsCommon.ConfigPresent("/MICROSAR/Fls")) || (PluginsCommon.ConfigPresent("/MICROSAR/Fls_30_vMemAccM/Fls"))) ) || ((PluginsCommon.ConfigPresent("/AURIX2G/EcucDefs/Fee")) && (PluginsCommon.ConfigPresent("/AURIX2G/EcucDefs/Fls"))) || (((PluginsCommon.ConfigPresent("/MICROSAR/Fls")) || (PluginsCommon.ConfigPresent("/MICROSAR/Fls_30_vMemAccM/Fls"))) && (PluginsCommon.ConfigPresent("/MICROSAR/Fee_30_FlexNor/Fee")))) {
            if (model.referenceMemIfHwA == true) {
                fee.setReferenceMemIfHwA(logger, fee.FeeGeneralMdf)
            } else {
                logger.info("Due to disabled model parameter referenceMemIfHwA, no setup of the reference of the MemIfHwA container will be executed.")
            }
        }
        if (((PluginsCommon.ConfigPresent("/MICROSAR/Fee")) && ((PluginsCommon.ConfigPresent("/MICROSAR/Fls")) || (PluginsCommon.ConfigPresent("/MICROSAR/Fls_30_vMemAccM/Fls")))) || (((PluginsCommon.ConfigPresent("/MICROSAR/Fls")) || (PluginsCommon.ConfigPresent("/MICROSAR/Fls_30_vMemAccM/Fls"))) && (PluginsCommon.ConfigPresent("/MICROSAR/Fee_30_FlexNor/Fee")))) {
            if (model.setupDefaultFeeEaPartition == true) {
                fee.setupFeePartition(logger, fee)
                fee.assignFeePartitionDeviceIndex(fls.flsConfigSet)
            } else {
                logger.info("Due to disabled model parameter setupDefaultFeeEaPartition, no setup of the Fee Partition will be executed.")
            }
        }
        MicrosarNvM.createCbkHeaderFile()
    }


    /**
     * The cleanup method is executed during the CLEANUP phase and will create Fee Blocks, assign Fee Partitions
     * as well as trigger solving actions project synchronizations.
     * @param model input for the processing of the configuration elements
     * @param logger instance for OCS specific logging
     */
    static void cleanupNvMemoryConfig(NvMemoryModel model, OcsLogger logger) {
        /* Check if VTT is included in the target type (VTT or DualTarget). If yes the vMem Solution can be processed */
        Boolean activateVMemSolution = false
        if (PluginsCommon.ConfigPresent("/MICROSAR/VTT/VTTvSet") && model.VMemSolution.activateVMemSolution) {
            activateVMemSolution = true
        }

        /**
         ********************************************* Microsar Fee/Fls *********************************************
         */
        if (model.activateMemModules.activateMicrosarFeeFls || ((PluginsCommon.ConfigPresent("/MICROSAR/Fee")) && ((PluginsCommon.ConfigPresent("/MICROSAR/Fls")) || (PluginsCommon.ConfigPresent("/MICROSAR/Fls_30_vMemAccM/Fls"))))) {
            MicrosarFee fee = new MicrosarFee()
            def fls
            if (activateVMemSolution) {
                fls = new MicrosarFlsvMemAccM()
                fls.initMdfModels()
            } else {
                fls = new MicrosarFls()
                fls.initMdfModels()
            }
            if ((PluginsCommon.ConfigPresent("/MICROSAR/Fee")) && ((PluginsCommon.ConfigPresent("/MICROSAR/Fls")) || (PluginsCommon.ConfigPresent("/MICROSAR/Fls_30_vMemAccM/Fls")))) {
                if (model.createFeeEaBlocks == true) {
                    fee.createFeeBlocks(logger)
                } else {
                    logger.info("Due to disabled model parameter createFeeEaBlocks no creation of Fee Blocks if NvM Blocks miss their Fee Reference.")
                }
                if (model.assignFeeEaPartition == true) {
                    fee.assignFeePartitions()
                } else {
                    logger.info("Due to disabled model parameter assignFeeEaPartitions no assignment of Fee Partition will be executed.")
                }
                NvMemorySolvingActions.solveNvMemory(logger)
            } else {
                logger.info("As there are no Microsar Fee or Microsar Fls in the current configuration available, neither the creation of fee blocks nor the assignmenf of the fee partition will be executed.")
            }
            /**
             ********************************************* Microsar FeeFlexNor/Fls *********************************************
             */
        } else if (model.activateMemModules.activateMicrosarFeeFlexNorFls || (((PluginsCommon.ConfigPresent("/MICROSAR/Fls")) || (PluginsCommon.ConfigPresent("/MICROSAR/Fls_30_vMemAccM/Fls"))) && (PluginsCommon.ConfigPresent("/MICROSAR/Fee_30_FlexNor/Fee")))) {
            FeeFlexNor fee = new FeeFlexNor()
            def fls
            if (activateVMemSolution) {
                fls = new MicrosarFlsvMemAccM()
                fls.initMdfModels()
            } else {
                fls = new MicrosarFls()
               fls.initMdfModels()
            }
            if (((PluginsCommon.ConfigPresent("/MICROSAR/Fls")) || (PluginsCommon.ConfigPresent("/MICROSAR/Fls_30_vMemAccM/Fls"))) && (PluginsCommon.ConfigPresent("/MICROSAR/Fee_30_FlexNor/Fee"))) {
                if (model.createFeeEaBlocks == true) {
                    fee.createFeeBlocks(logger)
                    fee.assignFeeBlockDeviceIndex(fls.flsMdfGeneral)
                } else {
                    logger.info("Due to disabled model parameter createFeeEaBlocks no creation of Fee Blocks if NvM Blocks miss their Fee Reference.")
                }
                if (model.assignFeeEaPartition == true) {
                    fee.assignFeePartitions()
                } else {
                    logger.info("Due to disabled model parameter assignFeeEaPartitions no assignment of Fee Partition will be executed.")
                }
                NvMemorySolvingActions.solveNvMemory(logger)

            } else {
                logger.info("As there are no FeeFlexNor or Microsar Fls in the current configuration available, neither the creation of fee blocks nor the assignmenf of the fee partition will be executed.")
            }
            /**
             ********************************************* Aurix Fee/Fls *********************************************
             */
        } else if ((model.activateMemModules.activateAurixFeeFls == true) || ((PluginsCommon.ConfigPresent("/AURIX2G/EcucDefs/Fee")) && (PluginsCommon.ConfigPresent("/AURIX2G/EcucDefs/Fls")))) {
            AurixFee fee = new AurixFee()
            AurixFls fls = new AurixFls()
            fls.initMdfModels()
            if ((PluginsCommon.ConfigPresent("/AURIX2G/EcucDefs/Fee")) && (PluginsCommon.ConfigPresent("/AURIX2G/EcucDefs/Fls"))) {
                if (model.createFeeEaBlocks == true) {
                    fee.createFeeBlocks(logger)
                    fee.assignFeeBlockDeviceIndex(fls.flsMdfGeneral)
                } else {
                    logger.info("Due to disabled model parameter createFeeEaBlocks no creation of Fee Blocks if NvM Blocks miss their Fee Reference.")
                }
                NvMemorySolvingActions.solveNvMemory(logger)
            } else {
                logger.info("As there are no Aurix Fee or Aurix Fls in the current configuration available, the creation of fee blocks will not be executed.")
            }
            /**
             ********************************************* Microsar Ea/Eep *********************************************
             */
        } else if (model.activateMemModules.activateMicrosarEaEep || (((PluginsCommon.ConfigPresent("/MICROSAR/Eep")) || (PluginsCommon.ConfigPresent("/MICROSAR/Eep_30_vMemAccM/Eep"))) && (PluginsCommon.ConfigPresent("/MICROSAR/Ea")))) {
            MicrosarEa ea = new MicrosarEa()
            ea.initMdfModels()
            def eep
            if (activateVMemSolution) {
                eep = new MicrosarEepvMemAccM()
            } else {
                eep = new MicrosarEep()
            }
            eep.initMdfModels()
            if (((PluginsCommon.ConfigPresent("/MICROSAR/Eep")) || (PluginsCommon.ConfigPresent("/MICROSAR/Eep_30_vMemAccM/Eep"))) && (PluginsCommon.ConfigPresent("/MICROSAR/Ea"))) {
                if (model.createFeeEaBlocks == true) {
                    ea.createEaBlocks(logger)
                    ea.setNumberOfWriteCycles(1000)
                    ea.assignEaBlockDeviceIndex(eep.eepMdfGeneral)
                } else {
                    logger.info("Due to disabled model parameter createFeeEaBlocks no creation of Ea Blocks if NvM Blocks miss their Ea Reference.")
                }
                if (model.assignFeeEaPartition == true) {
                    ea.assignEaPartitions()
                } else {
                    logger.info("Due to disabled model parameter assignFeeEaPartitions no assignment of Ea Partition will be executed.")
                }
                NvMemorySolvingActions.solveNvMemory(logger)
            } else {
                logger.info("As there are no Microsar Ea or no Eep Module in the current configuration available, neither the creation of ea blocks nor the assignment of the Ea partition will be executed.")
            }
        }
    }

    /**
     * Method checks whether the configuration contains VTT or not.
     * @return targetType
     */
    static String checkTargetType() {
        String targetType = "RealTarget"
        if (PluginsCommon.ConfigPresent("/MICROSAR/VTT/VTTvSet")) {
            targetType = "VTT"
        }
        return targetType
    }
}
