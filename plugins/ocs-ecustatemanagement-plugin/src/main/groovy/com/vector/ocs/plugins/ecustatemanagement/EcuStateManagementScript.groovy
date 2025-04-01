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
/*!        \file  EcuStateManagementScript.groovy
 *        \brief  Configure the modules which are affected by ECU's State Management
 *
 *      \details  Set up the EcuC configuration and realize the configuration of BswM and EcuM using Mode Management
 *                Auto Configuration
 *
 *********************************************************************************************************************/

package com.vector.ocs.plugins.ecustatemanagement

import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.automation.model.ecuc.microsar.bswm.BswM
import com.vector.cfg.automation.model.ecuc.microsar.bswm.bswmconfig.BswMConfig
import com.vector.cfg.automation.model.ecuc.microsar.ecuc.EcuC
import com.vector.cfg.automation.model.ecuc.microsar.ecuc.ecucgeneral.bswinitialization.initfunction.InitFunction
import com.vector.cfg.automation.model.ecuc.microsar.ecuc.ecucpartitioncollection.ecucpartition.EcucPartition
import com.vector.cfg.automation.model.ecuc.microsar.ecum.EcuM
import com.vector.cfg.automation.model.ecuc.microsar.ecum.ecumconfiguration.ecumcommonconfiguration.ecumdriverinitlistone.EcuMDriverInitListOne
import com.vector.cfg.automation.model.ecuc.microsar.ecum.ecumconfiguration.ecumcommonconfiguration.ecumdriverinitlistzero.EcuMDriverInitListZero
import com.vector.cfg.automation.model.ecuc.microsar.ecum.ecumconfiguration.ecumcommonconfiguration.ecumdriverinitlistzero.ecumdriverinititem.EcuMDriverInitItem
import com.vector.cfg.automation.model.ecuc.microsar.os.Os
import com.vector.cfg.automation.model.ecuc.microsar.os.osevent.OsEvent
import com.vector.cfg.automation.model.ecuc.microsar.os.ostask.OsTask
import com.vector.cfg.automation.model.ecuc.microsar.vtt.vttecuc.VTTEcuC
import com.vector.cfg.automation.scripting.api.project.IProject
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIContainer
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIModuleConfiguration
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.PluginsCommon
import com.vector.ocs.plugins.ecustatemanagement.servicediscoverycontrol.ServiceDiscoveryControl
import com.vector.ocs.plugins.ecustatemanagement.communicationcontrol.CommunicationControlDomain
import com.vector.ocs.plugins.ecustatemanagement.moduleinitialization.ModuleInitializationDomain
import com.vector.ocs.plugins.ecustatemanagement.ecustatehandling.EcuStateHandlingDomain
import groovy.transform.PackageScope

/**
 * The EcuStateManagementScript configures the modules which are affected by the domain 'ECU's State Management': BswM, EcuM and ComM.
 * It configures the features and parameters of the auto-configuration domains of Mode Management: Communication Control, Ecu State Handling, Module Initialization and Service Discovery Control.
 */

@PackageScope
class EcuStateManagementScript {

    /**
     * Set up the EcuC configuration and realizes the configuration of BswM and EcuM modules.
     * @param model EcuStateManagementModel which contains the specified configuration settings.
     * @param logger Instance of the OcsLogger.
     */
    static void run(EcuStateManagementModel model, OcsLogger logger) {

        logger.info("Start processing of the EcuStateManagementPlugin model.")
        ScriptApi.activeProject { project ->
            boolean isEcucDefrefPresent = PluginsCommon.DefRefPresent(project, "/MICROSAR/EcuC", logger)
            boolean isEcucPresent = PluginsCommon.ConfigPresent("/MICROSAR/EcuC")
            boolean isBswmPresent = PluginsCommon.ConfigPresent("/MICROSAR/BswM")
            boolean isEcumPresent = PluginsCommon.ConfigPresent("/MICROSAR/EcuM")

            if (isEcucDefrefPresent) {
                logger.info("Setting up EcuC configuration.")
                setupEcucConfiguration(logger)
            }

            if (isEcucPresent && isBswmPresent && isEcumPresent) {
                transaction {
                    // Configure BswM module in Basic Editor
                    configureBswM(project, model, logger)
                    // Configure the driver init lists in EcuM
                    configureDriverInitLists(project, model, logger)
                }
                // Process the Auto Configuration domains
                processDomains(model, logger)
            }
        }
    }

    /**
     * Cleanup method called in CLEANUP phase. Adds Init Task and Init Task Event in BswMConfig containers. These
     * containers are only available after the CLEANUP phase of the Runtime System plugin.
     * @param model
     * @param logger
     */
    static void cleanup(EcuStateManagementModel model, OcsLogger logger) {
        ScriptApi.activeProject { project ->
            boolean isOsPresent = PluginsCommon.ConfigPresent("/MICROSAR/Os")
            boolean isBswmPresent = PluginsCommon.ConfigPresent("/MICROSAR/BswM")

            if (isOsPresent && isBswmPresent) {
                BswM bswM = project.bswmdModel(BswM.DefRef).single()
                Os os = project.bswmdModel(Os.DefRef).single()
                transaction {
                    for (i in 0..<model.partitionConfig.size()) {
                        if (PluginsCommon.Cfg5MinorVersion() < 29 && i > 0) {
                            logger.warn("MultiPartitioning is not possible in R31! Only first element in the PartitionConfig set " +
                                    "is processed.")
                            break
                        }
                        BswMConfig bswMConfig = bswM.bswMConfig.byName(model.partitionConfig[i].bswMConfigName)

                        //Configure Init Task
                        OsTask foundOsTask = os.osTask.find { osTask ->
                            osTask.shortname == model.partitionConfig[i].initTask
                        }
                        if (foundOsTask != null) {
                            bswMConfig.bswMInitTaskOrCreate.refTarget = foundOsTask
                        } else if (!model.partitionConfig[i].initTask.empty) {
                            logger.error("Could not find OsTask ${model.partitionConfig[i].initTask}")
                        }

                        //Configure Init Task Event
                        OsEvent foundOsEvent = os.osEvent.find { osEvent ->
                            osEvent.shortname == model.partitionConfig[i].initTaskEvent
                        }
                        if (foundOsEvent != null && !model.partitionConfig[i].initTaskEvent.empty) {
                            bswMConfig.bswMInitTaskEventOrCreate.refTarget = foundOsEvent
                        } else if (!model.partitionConfig[i].initTaskEvent.empty) {
                            logger.error("Could not find OsEvent ${model.partitionConfig[i].initTaskEvent}")
                        }
                    }
                }
            }
        }
    }

    /**
     * Activate the EcuC and VTTEcuC modules.
     * @param logger Instance of the OcsLogger.
     */
    static void setupEcucConfiguration(OcsLogger logger) {
        ScriptApi.activeProject { project ->
            transaction {
                // If configuration of EcuC is not present, just activate it since it is needed in all projects
                if (!PluginsCommon.ConfigPresent("/MICROSAR/EcuC")) {
                    logger.info("Activating EcuC since it is needed by several other modules.")
                    try {
                        operations.activateModuleConfiguration(EcuC.DefRef)
                    } catch (IllegalArgumentException | IllegalStateException exception) {
                        logger.error("$exception")
                    }
                } else {
                    logger.info("EcuC module already present in the configuration.")
                }
                // Activate VTTEcuC if it is a VTT or Dual Target project
                if (PluginsCommon.DefRefPresent(project, "/MICROSAR/VTT/VTTEcuC", logger)
                        && PluginsCommon.ConfigPresent("/MICROSAR/VTT/VTTvSet")) {
                    if (!PluginsCommon.ConfigPresent("/MICROSAR/VTT/VTTEcuC")) {
                        logger.info("Activating VTTEcuC.")
                        try {
                            operations.activateModuleConfiguration(VTTEcuC.DefRef)
                        } catch (IllegalArgumentException | IllegalStateException exception) {
                            logger.error("$exception")
                        }
                    } else {
                        logger.info("VTTEcuC module already present in the configuration.")
                    }
                }
            }
        }
    }

    /**
     * Configure the containers and parameters of BswM module.
     * @param logger Instance of the OcsLogger.
     */
    static void configureBswM(IProject project, EcuStateManagementModel model, OcsLogger logger) {
        BswM bswM = project.bswmdModel(BswM.DefRef).single()
        logger.info("Create BswMConfig container, if not yet available.")
        EcuC ecuC = project.bswmdModel(EcuC.DefRef).single()
        for (i in 0..<model.partitionConfig.size()) {
            // Stop after first element in case R31 is used
            if (PluginsCommon.Cfg5MinorVersion() < 29 && i > 0) {
                logger.warn("MultiPartitioning is not possible in R31! Only first element in the PartitionConfig set " +
                            "is processed.")
                break
            }
            BswMConfig bswMConfig = bswM.bswMConfig.byNameOrCreate(model.partitionConfig[i].bswMConfigName)
            //Configure EcucPartition
            EcucPartition foundEcucPartition = ecuC.ecucPartitionCollection.first.ecucPartition.find { ecucPartition ->
                ecucPartition.shortname == model.partitionConfig[i].ecucPartitionName
            }
            if (foundEcucPartition != null) {
                bswMConfig.getBswMPartitionRefOrCreate().refTarget = foundEcucPartition
            } else if (model.partitionConfig[i].ecucPartitionName == "") {
                logger.error("No EcucPartition is configured in the model!")
            } else {
                logger.error("Could not find EcucPartition ${model.partitionConfig[i].ecucPartitionName}")
            }
        }
        //Create parameter BswMMainFunctionPeriod
        bswM.bswMGeneral.bswMMainFunctionPeriodOrCreate
    }

    /**
     * Configure the driver init lists in EcuM
     * @param model EcuStateManagementModel which contains the specified configuration settings.
     * @param logger Instance of the OcsLogger.
     */
    static void configureDriverInitLists(IProject project, EcuStateManagementModel model, OcsLogger logger) {
        List<String> alreadyInitializedFunctions = []
        EcuC ecucModule = project.bswmdModel(EcuC.DefRef).single()
        EcuM ecumModule = project.bswmdModel(EcuM.DefRef).single()

        // Configure PreInit functions in EcuMDriverInitListOne and InitMemory functions in EcuMDriverInitListZero
        if (model.autoGenerate_PreAndMemoryInitFunctions) {
            List<InitFunction> initFunctions_Mcu = []
            List<InitFunction> initFunctions_Zero = []
            List<InitFunction> initFunctions_OneEarly = []
            List<InitFunction> initFunctions_One = []
            List<InitFunction> initFunctions_OneLate = []
            List<InitFunction> initFunctions_Mem = []
            if (ecucModule.ecucGeneral.bswInitialization.exists()) {
                ecucModule.ecucGeneral.bswInitialization.get().initFunction.each { InitFunction initFunc ->
                    Boolean mcuInserted = false
                    // Check if the module reference exists for the available init functions in EcuC module
                    if (initFunc.existsModuleRef()) {
                        if (initFunc.moduleRef.refTargetMdf.name == "Mcu") {
                            initFunctions_Mcu.add(initFunc)
                            alreadyInitializedFunctions.add(initFunc.shortname)
                            mcuInserted = true
                        }
                    }
                    if (mcuInserted) {
                        /* Mcu is first */
                    } else if (initFunc.initPhase.value.toString() == "INIT_MEMORY") {
                        initFunctions_Mem.add(initFunc)
                        alreadyInitializedFunctions.add(initFunc.shortname)
                    } else if (initFunc.initPhase.value.toString() == "INIT_ONE_EARLY") {
                        initFunctions_OneEarly.add(initFunc)
                        alreadyInitializedFunctions.add(initFunc.shortname)
                    } else if (initFunc.initPhase.value.toString().contains("INIT_ONE")) {
                        initFunctions_One.add(initFunc)
                        alreadyInitializedFunctions.add(initFunc.shortname)
                    } else if (initFunc.initPhase.value.toString().contains("INIT_ZERO")) {
                        initFunctions_Zero.add(initFunc)
                        alreadyInitializedFunctions.add(initFunc.shortname)
                    } else if (initFunc.initPhase.value.toString().contains("INIT_ONE_LATE")) {
                        initFunctions_OneLate.add(initFunc)
                        alreadyInitializedFunctions.add(initFunc.shortname)
                    }
                }
            }

            // add EcuC InitFunction Mcu_Init and InitFunctions with Init Phase INIT_MEMORY and INIT_ZERO to EcuMDriverInitListZero
            EcuMDriverInitListZero initItemListZero = ecumModule.ecuMConfiguration.ecuMCommonConfigurationOrCreate.ecuMDriverInitListZeroOrCreate
            logger.info("Configuring Driver Init List 0.")
            initFunctions_Mcu.each { initFunction ->
                processDriverInitListZero(initItemListZero, initFunction)
                // add the InitItem AdditionalCode for Mcu_Init
                EcuMDriverInitItem initItem2 = initItemListZero.ecuMDriverInitItem.byNameOrCreate("Mcu_Init_AdditionalCode")
                // configure the parameters for Mcu_Init_AdditionalCode
                if (initFunction.existsHeader()) {
                    initItem2.ecuMModuleHeaderOrCreate.value = initFunction.header.value
                } else {
                    initItem2.ecuMModuleHeaderOrCreate.value = initFunction.header.value = "Mcu.h"
                }
                initItem2.ecuMAdditionalInitCodeOrCreate.value = "Mcu_InitClock(0);\nwhile (MCU_PLL_LOCKED != Mcu_GetPllStatus());\nMcu_DistributePllClock();\n"
                initItem2.ecuMModuleIDOrCreate.value = "Code"
            }
            initFunctions_Mem.each { initFunction ->
                processDriverInitListZero(initItemListZero, initFunction)
            }
            initFunctions_Zero.each { initFunction ->
                processDriverInitListZero(initItemListZero, initFunction)
            }

            // add EcuC InitFunctions with Init Phase INIT_ONE_EARLY, INIT_ONE and INIT_ONE_LATE to EcuMDriverInitListOne
            EcuMDriverInitListOne initItemListOne = ecumModule.ecuMConfiguration.ecuMCommonConfigurationOrCreate.ecuMDriverInitListOneOrCreate
            logger.info("Configuring Driver Init List 1.")
            initFunctions_OneEarly.each { initFunction ->
                processDriverInitListOne(initItemListOne, initFunction)
            }
            initFunctions_One.each { initFunction ->
                processDriverInitListOne(initItemListOne, initFunction)
            }
            initFunctions_OneLate.each { initFunction ->
                processDriverInitListOne(initItemListOne, initFunction)
            }
        }
        //todo: false condition
    }

    /**
     * Add driver init item in EcuMDriverInitListZero and configure its parameters
     * @param initItemListZero Instance of EcuMDriverInitListZero
     * @param initFunc Init function added to the list
     */
    private static void processDriverInitListZero(EcuMDriverInitListZero initItemListZero, InitFunction initFunc) {
        // create InitItem in EcuM from corresponding InitFunction in EcuC using the same shortname
        EcuMDriverInitItem initItem = initItemListZero.ecuMDriverInitItem.byNameOrCreate(initFunc.shortname)
        // configure the parameters of InitItem in EcuM using the parameter values of InitFunction in EcuC
        setupInitItemZero(initItem, initFunc)
    }

    /**
     * Add driver init item in EcuMDriverInitListOne and configure its parameters
     * @param initItemListOne Instance of EcuMDriverInitListOne
     * @param initFunction Init function added to the list
     */
    private static void processDriverInitListOne(EcuMDriverInitListOne initItemListOne, InitFunction initFunction) {
        // create InitItem in EcuM from corresponding InitFunction in EcuC using the same shortname
        com.vector.cfg.automation.model.ecuc.microsar.ecum.ecumconfiguration.ecumcommonconfiguration.ecumdriverinitlistone.ecumdriverinititem.EcuMDriverInitItem initItem = initItemListOne.ecuMDriverInitItem.byNameOrCreate(initFunction.shortname)
        // configure the parameters of InitItem in EcuM using the parameter values of InitFunction in EcuC
        setupInitItemOne(initItem, initFunction)
    }

    /**
     * Configure the features and parameters of the auto-configuration domains of Mode Management:
     * <ul>
     *     <li>Communication Control</li>
     *     <li>Ecu State Handling</li>
     *     <li>Module Initialization</li>
     *     <li>Service Discovery Control</li>
     * </ul>
     * Multi-partition use case is currently not supported.
     * @param model EcuStateManagementModel which contains the specified configuration settings.
     * @param logger Instance of the OcsLogger.
     */
    static void processDomains(EcuStateManagementModel model, OcsLogger logger) {
        CommunicationControlDomain CommunicationControl = new CommunicationControlDomain()
        EcuStateHandlingDomain EcuStateHandling = new EcuStateHandlingDomain()
        ModuleInitializationDomain ModuleInitialization = new ModuleInitializationDomain()
        ServiceDiscoveryControl ServiceDiscoveryControl = new ServiceDiscoveryControl()

        ScriptApi.activeProject() {project ->
            MIModuleConfiguration bswM = activeEcuc.modules(BswM.DefRef).first
            List<MIContainer> bswMConfigs = bswM.subContainer(BswMConfig.DefRef)
            transaction {
                domain {
                    modeManagement { modeMngt ->
                        if (bswMConfigs.size() > 0) {
                            try {
                                getBswMAutoConfigDomains(bswMConfigs.first).each { autoConfigurationDomain ->
                                    if (null != autoConfigurationDomain.identifier && "Module Initialization" != autoConfigurationDomain.identifier) {
                                        switch (autoConfigurationDomain.identifier) {
                                            case "Communication Control":
                                                logger.info("Processing Communication Control domain.")
                                                CommunicationControl.initializeAndProcessDomain(model, modeMngt, autoConfigurationDomain.identifier, bswMConfigs.first, logger)
                                                break
                                            case "Ecu State Handling":
                                                logger.info("Processing Ecu State Handling domain.")
                                                EcuStateHandling.initializeAndProcessDomain(model, modeMngt, autoConfigurationDomain.identifier, bswMConfigs.first, logger)
                                                break
                                            case "Service Discovery Control":
                                                logger.info("Processing Service Discovery Control domain.")
                                                ServiceDiscoveryControl.initializeAndProcessDomain(model, modeMngt, autoConfigurationDomain.identifier, bswMConfigs.first, logger)
                                                break
                                            default:
                                                logger.info("Unknown AutoConfigurationDomain identifier detected '$autoConfigurationDomain.identifier'")
                                        }
                                    }
                                }
                                bswMConfigs.each { MIContainer bswMConfig ->
                                    logger.info("Processing Module Initialization domain.")
                                    ModuleInitialization.initializeAndProcessDomain(model, modeMngt, "Module Initialization", bswMConfig, logger)
                                }
                            } catch (ignored) {
                                logger.error("The AutoConfigurationDomain cannot be accessed.")
                            }
                        }
                    }
                }
            }
            project.modelSynchronization.synchronize()
        }
    }

    /**
     * Configure the parameters of EcuMDriverInitItems in EcuMDriverInitListZero.
     * @param initItem Instance of EcuMDriverInitItem.
     * @param initFunction Instance of InitFunction.
     */
    static void setupInitItemZero(EcuMDriverInitItem initItem, InitFunction initFunction) {
        if (initFunction.existsHeader()) {
            initItem.ecuMModuleHeaderOrCreate.setValue(initFunction.header.value)
        }
        if (initFunction.existsAdditionalInitCode()) {
            initItem.ecuMAdditionalInitCodeOrCreate.value = initFunction.additionalInitCode.get().value
        }
        if (initFunction.existsModuleRef()) {
            initItem.ecuMModuleIDOrCreate.value = initFunction.moduleRef.refTargetMdf.name
        }
        initItem.ecuMModuleServiceOrCreate.value = initFunction.shortname
    }

    /**
     * Configure the parameters of EcuMDriverInitItems in EcuMDriverInitListOne.
     * @param initItem Instance of EcuMDriverInitItem.
     * @param initFunction Instance of InitFunction.
     */
    static void setupInitItemOne(com.vector.cfg.automation.model.ecuc.microsar.ecum.ecumconfiguration.ecumcommonconfiguration.ecumdriverinitlistone.ecumdriverinititem.EcuMDriverInitItem initItem, InitFunction initFunction) {
        if (initFunction.existsHeader()) {
            initItem.ecuMModuleHeaderOrCreate.setValue(initFunction.header.value)
        }
        if (initFunction.existsAdditionalInitCode()) {
            initItem.ecuMAdditionalInitCodeOrCreate.value = initFunction.additionalInitCode.get().value
        }
        if (initFunction.existsModuleRef()) {
            initItem.ecuMModuleIDOrCreate.value = initFunction.moduleRef.refTargetMdf.name
        }
        initItem.ecuMModuleServiceOrCreate.value = initFunction.shortname
    }
}
