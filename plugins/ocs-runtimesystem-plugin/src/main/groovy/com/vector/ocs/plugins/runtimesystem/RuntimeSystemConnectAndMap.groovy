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
/*!        \file  RuntimeSystemConnectAndMap.groovy
 *        \brief  Create references between the Os and EcuC configuration RuntimeSystem.
 *
 *      \details  The following references will be applied:
 *                - OsTask <-> OsApplication
 *                - OsTask <-> OsResource
 *                - OsTask <-> OsCore
 *                - OsApplication <-> OsCore
 *                - OsApplication <-> OsCounter
 *                - OsApplication <-> EcucPartition
 *                - OsApplication <-> EcucCoreDefinition
 *                - OsCore <-> EcucCoreDefinition
 *                - OsCore <-> OsPhysicalCore
 *                - OsCore <-> OsOs (as master core)
 *********************************************************************************************************************/
package com.vector.ocs.plugins.runtimesystem

import com.vector.cfg.automation.model.ecuc.microsar.csm.Csm
import com.vector.cfg.automation.model.ecuc.microsar.ecuc.EcuC
import com.vector.cfg.automation.model.ecuc.microsar.ecuc.ecuchardware.EcucHardware
import com.vector.cfg.automation.model.ecuc.microsar.ecuc.ecuchardware.ecuccoredefinition.EcucCoreDefinition
import com.vector.cfg.automation.model.ecuc.microsar.ecuc.ecucpartitioncollection.ecucpartition.EcucPartition
import com.vector.cfg.automation.model.ecuc.microsar.ecum.EcuM
import com.vector.cfg.automation.model.ecuc.microsar.os.Os
import com.vector.cfg.automation.model.ecuc.microsar.os.osapplication.OsApplication
import com.vector.cfg.automation.model.ecuc.microsar.os.osapplication.osappecucpartitionref.OsAppEcucPartitionRef
import com.vector.cfg.automation.model.ecuc.microsar.os.osapplication.osapplicationcoreref.OsApplicationCoreRef
import com.vector.cfg.automation.model.ecuc.microsar.os.osapplication.osapptaskref.OsAppTaskRef
import com.vector.cfg.automation.model.ecuc.microsar.os.oscore.OsCore
import com.vector.cfg.automation.model.ecuc.microsar.os.oscore.oscoreecuccoreref.OsCoreEcucCoreRef
import com.vector.cfg.automation.model.ecuc.microsar.os.oscore.oscorephysicalcoreref.OsCorePhysicalCoreRef
import com.vector.cfg.automation.model.ecuc.microsar.os.oscounter.OsCounter
import com.vector.cfg.automation.model.ecuc.microsar.os.osisr.OsIsr
import com.vector.cfg.automation.model.ecuc.microsar.os.osos.osmastercore.OsMasterCore
import com.vector.cfg.automation.model.ecuc.microsar.os.osos.ossystemtimer.OsSystemTimer
import com.vector.cfg.automation.model.ecuc.microsar.os.ospublishedinformation.osderivativeinformation.oshardwaretimer.oshardwaretimerchannel.OsHardwareTimerChannel
import com.vector.cfg.automation.model.ecuc.microsar.os.ospublishedinformation.osderivativeinformation.osphysicalcore.OsPhysicalCore
import com.vector.cfg.automation.model.ecuc.microsar.os.osresource.OsResource
import com.vector.cfg.automation.model.ecuc.microsar.os.ostask.OsTask
import com.vector.cfg.automation.model.ecuc.microsar.os.ostask.ostaskresourceref.OsTaskResourceRef
import com.vector.cfg.automation.model.ecuc.microsar.rte.Rte
import com.vector.cfg.automation.model.ecuc.microsar.secoc.SecOC
import com.vector.cfg.automation.model.ecuc.microsar.vtt.vttos.VTTOs
import com.vector.cfg.automation.scripting.api.project.IProject
import com.vector.cfg.consistency.ui.ISolvingActionUI
import com.vector.cfg.consistency.ui.IValidationResultUI
import com.vector.cfg.gen.core.bswmdmodel.GICList
import com.vector.cfg.gen.core.bswmdmodel.GIOptional
import com.vector.cfg.gen.core.bswmdmodel.GIPList
import com.vector.cfg.gen.core.bswmdmodel.GIReferenceToContainer
import com.vector.cfg.gen.core.bswmdmodel.param.GInstanceReference
import com.vector.cfg.gen.core.genusage.groovy.api.IGenerationApi
import com.vector.cfg.model.access.AsrPath
import com.vector.cfg.model.access.DefRef
import com.vector.cfg.model.mdf.commoncore.autosar.MIReferrable
import com.vector.cfg.model.mdf.commoncore.autosar.MIReferrableARRef
import com.vector.cfg.model.mdf.model.autosar.base.MIARAnyInstanceRef
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIContainer
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIInstanceReferenceValue
import com.vector.cfg.model.state.IParameterStatePublished
import com.vector.cfg.model.sysdesc.api.taskmapping.IEvent
import com.vector.cfg.model.sysdesc.api.taskmapping.ITaskMapping
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.PluginsCommon
import com.vector.ocs.plugins.runtimesystem.dataclasses.RtsApplication
import com.vector.ocs.plugins.runtimesystem.dataclasses.RtsCore
import com.vector.ocs.plugins.runtimesystem.dataclasses.RtsDataModel
import com.vector.ocs.plugins.runtimesystem.dataclasses.RtsTask
import groovy.transform.PackageScope
import java.util.regex.Pattern
import static com.vector.cfg.automation.api.ScriptApi.activeProject

@PackageScope
class RuntimeSystemConnectAndMap {

    /**
     * Create the references based on the given RuntimeSystemModel.
     * <ul>
     *     <li>OsCore <-> OsPhysicalCore</li>
     *     <li>OsCore <-> EcucCoreDefinition</li>
     *     <li>OsCore <-> OsOs master core</li>
     *     <li>OsApplication <-> EcucCoreDefinition</li>
     *     <li>OsApplication <-> EcucPartition</li>
     *     <li>OsTask <-> OsResource (only for master core)</li>
     *     <li>OsTask <-> OsApplication</li>
     *     <li>trigger XSignal Channel solving action</li>
     * </ul>
     * @param model RuntimeSystemModel as base to create the new references.
     * @param logger instance of the OcsLogger.
     */
    static void createReferences(RtsDataModel model, OcsLogger logger) {
        logger.info("Create references for Os and EcuC specific " +
                "configuration elements.")
        activeProject() {
            Os osModule = bswmdModel(Os.DefRef).single
            EcuC ecucModule = bswmdModel(EcuC.DefRef).single
            logger.debug("Check that EcuM module is available.")
            Boolean isEcumPresent = PluginsCommon.ConfigPresent(RuntimeSystemConstants.ECUM_DEFREF)

            final int coresToProcess = RuntimeSystemOsConfig.checkOsPhysicalCoreConfiguration(osModule,
                    model.getCores().size(), logger)
            final List<RtsCore> coreModel = model.getCores()
            transaction {
                // Start processing of all RuntimeSystemModel cores
                coreModel.eachWithIndex { core, index ->
                    if ((index < coresToProcess)) {
                        OsCore osCore = RuntimeSystemOsConfig.getOsCoreByName(osModule, core.name)

                        // 1. Create the reference between the OsCore <-> EcucCoreDefinition
                        EcucCoreDefinition ecucCore = RuntimeSystemEcucConfig.getEcucCoreDefinitionByName(ecucModule,
                                osCore.shortname)
                        createOsCoreEcucCoreRef(osCore, ecucCore, logger)

                        // 2. Create the reference between OsOs.OsMasterCore <-> OsCore
                        Boolean isMasterCore = core.isMasterCore
                        if (isMasterCore) {
                            createOsMasterCore(osModule, osCore, logger)
                        }
                        // Start processing of all RuntimeSystemModel applications
                        core.applications.each { RtsApplication application ->
                            OsApplication osApp = RuntimeSystemOsConfig.getOsApplicationByName(osModule, application.name)
                            // 3. Create the reference between OsApplication <-> EcucCoreDefinition
                            createOsApplicationCoreRef(osApp, ecucCore, logger)

                            // 4. Create the reference between OsApplication <-> EcucPartition
                            EcucPartition ecucPartition = RuntimeSystemEcucConfig.getEcucPartitionByName(ecucModule, osApp.shortname)
                            createOsAppEcucPartitionRef(osApp, ecucPartition, logger)


                            // Start processing of all RuntimeSystemModel tasks
                            application.tasks.each { RtsTask task ->
                                OsTask osTask = RuntimeSystemOsConfig.getOsTaskByName(osModule, task.name)
                                // 5. Create the reference between OsTask <-> OsResource but currently only for master core
                                if (isMasterCore) {
                                    OsResource osResource = RuntimeSystemOsConfig.getOsResourceByName(osModule)
                                    createOsTaskResourceRef(osTask, osResource, logger)
                                }

                                // 6. Create the reference between OsTask <-> OsApplication
                                createOsAppTaskRef(osApp, osTask, logger)
                            }
                        }

                        if (isEcumPresent) {
                            EcuM ecumModule = bswmdModel(EcuM.DefRef).single
                            // 7. Create the reference between EcuMFlexConfiguration <-> EcucPartition
                            EcucPartition EcucPartitionQM = getEcucPartitionByQMLevel(ecucModule, core)
                            createEcuMFlexConfigurationPartitionRef(EcucPartitionQM, ecumModule, logger)
                        }
                    }

                    // 8. Remove reference to /MICROSAR/Os/OsOS/OsSystemTimer in case the refTarget does not exists
                    removeOsSystemTimerInvalidReference(osModule, logger)
                }
            }
            transaction {
                coreModel.each { core ->
                    // 9. Create References within OsCounter
                    OsIsr counterIsr = getCounterIsrByName(osModule, core)
                    OsCounter systemTimer = getSystemTimerByName(osModule, core)
                    createOsCounterRefs(counterIsr, systemTimer, core.coreID, logger)

                    // 10. Create the reference between OsApplication <-> EcucPartition for existing SystemApplication (created by OS)
                    OsApplication osSystemApplication = RuntimeSystemOsConfig.getOsApplicationByName(osModule, "SystemApplication_" + core.name)
                    EcucPartition ecucPartitionSystemApplication = RuntimeSystemEcucConfig.getEcucPartitionByName(ecucModule, osSystemApplication.shortname)
                    createOsAppEcucPartitionRef(osSystemApplication, ecucPartitionSystemApplication, logger)

                    // 11. Create the reference between SystemApplication <-> App Counter Ref (SystemTimer)
                    createOsAppCounterRef(osSystemApplication, systemTimer, logger)
                }
            }
        }
    }

    /**
     * The cleanup of the RuntimeSystem perform multiple actions to finalize the RuntimeSystem configuration:
     * <ul>
     *     <li>Synchronize the System Description</li>
     *     <li>Map the Interrupt Service Routines to the corresponding applications</li>
     *     <li>Map the runnables to the corresponding tasks </li>
     *     <li>Trigger RTE solving action to fix the runnable order, if necessary</li>
     *     <li>Trigger solving action to map the OsAlarms to an application</li>
     *     <li>Trigger calculation phase of the RTE to create additional Os configuration, e.g. OsAlarm</li>
     *     <li>Trigger solving actions related to Com Callback Header and deferred notification cache</li>
     *     <li>Trigger BSW Service Component mapping, e.g. DET or OS Service Component</li>
     *     <li>Synchronize the VTTOs with the Os</li>
     *     <li>Remove automatically created EcucCoreDefinition if necessary</li>
     * </ul>
     * @param model Data Model as source of information for the cleanup.
     * @param logger instance of the OcsLogger.
     */
    static void cleanupRuntimeSystemConfiguration(RtsDataModel model, OcsLogger logger) {
        activeProject() {
            Os osModule = bswmdModel(Os.DefRef).single
            final int coresToProcess = RuntimeSystemOsConfig.checkOsPhysicalCoreConfiguration(osModule, model.getCores().size(), logger)
            if (coresToProcess == 0) {
                logger.error("Due to incorrect core configuration in the JSON file compared to the available " +
                        "cores in the OsPhysicalCores the processing of the RuntimeSystem in the RUN phase will not be continued.")
                return
            }
            // 1. Before cleaning up the project, synchronize the System Description
            synchronizeSwcDescription(logger)

            // 2. Trigger solving action for the creation of the XSignalChannels and the corresponding ISRs
            if (model.cores.size() > 1) {
                triggerXSignalChannelSolvingAction(model.cores, logger)
            }

            Boolean isVttOsPresent = PluginsCommon.ConfigPresent(RuntimeSystemConstants.VTTOS_DEFREF)
            if (isVttOsPresent) {
                prepareVttOsIsr(logger)
            }

            // 3. Map ISRs to applications
            handleInterruptMapping(model.interruptMapping.userDefinedMapping, model.interruptMapping.defaultIsrApplication, logger)

            // 4. Map runnables to tasks
            handleTaskMapping(model.taskMapping.userDefinedMapping, model.taskMapping.defaultBswTask, model.cores, logger)

            // 5. Trigger RTE solving action to fix the runnable order
            logger.debug("Check that Rte module is available.")
            Boolean isRtePresent = PluginsCommon.ConfigPresent(RuntimeSystemConstants.RTE_DEFREF)
            if (isRtePresent) {
                triggerRteSolvingActionForRunnableOrder(logger)
            }

            // 6. Trigger calculation phase of the Rte to create additional Os configuration
            // Currently it is not clear if this operation could be also done in another context, e.g. individual file
            // which address the Rte configuration parts. Therefore for not it is kept here but the Rte.DefRef is not
            // directly used. instead the DefRef is created itself in the triggerRteCalculation()
            if (isRtePresent) {
                triggerRteCalculation(logger)
            }

            // 7. Trigger solving action to map the OsAlarms to an application different than the SystemApplication.
            if (model.cores.size() == 1) {
                triggerOsSolvingActionForAlarmMapping(logger)
            }

            // 8. Trigger Com Callback Header related solving actions and deferred notification cache to be
            // adapted since new data mappings were introduced.
            logger.debug("Check that Com module is available.")
            Boolean isComPresent = PluginsCommon.ConfigPresent(RuntimeSystemConstants.COM_DEFREF)
            if (isComPresent) {
                triggerComSolvingActions(logger)
            }

            // 9. Clean up OsMemoryProtection if there is a Scalability Class mismatch
            if (model.scalabilityClass == RtsOsScalabilityClass.SC1 || model.scalabilityClass == RtsOsScalabilityClass.SC2) {
                removeOsMemoryRegions(logger)
            } else {
                configureMemoryProtectionForSystemApplication(model, coresToProcess)
            }

            // 10. Trigger BSW ServiceComponent mapping
            triggerBswServiceComponentSwcMapping(model.cores, model.taskMapping.defaultBswTask, logger)

            // 11. Synchronize the VTTOs with the Os
            if (isVttOsPresent) {
                triggerVttOsOutOfSyncSolvingAction(logger)
            }

            // 12. Synchronize the VTTEcuC with the EcuC
            triggerVttEcucOutOfSyncSolvingAction(logger)

            // 13. Remove automatically created EcucCoreDefinition if necessary
            removeUnreferencedEcucCoreDefinitions(logger)

            // 14. Map the Runnables of Service Components which are created in a later point of time to an OsTask
            if (isRtePresent) {
                mapSecOCSwcRunnablesToTask(logger)
            }
        }
    }

    /**
     * Synchronize the SWC description based on the offered solving actions 'Full SWC description build'.
     * @param logger instance of the OcsLogger.
     */
    private static void synchronizeSwcDescription(OcsLogger logger) {
        logger.info("Synchronize SWC descriptions.")
        activeProject { IProject project ->
            PluginsCommon.modelSynchronization(project, logger)
            validation {
                it.validationResults.each { validationResult ->
                    if (validationResult.getPreferredSolvingAction() != null) {
                        if (validationResult.preferredSolvingAction.description.contains("Full SWC description build")) {
                            validationResult.preferredSolvingAction.solve()
                        }
                    }
                }
            }
        }
    }

    /**
     * Delete the automatically created VttOs Isr that corresponds to the original System Timer.
     * It is not needed anymore, as the correct System Timer Isr is mirrored from the Os.
     * @param logger instance of the OcsLogger.
     */
    private static void prepareVttOsIsr(OcsLogger logger) {
        activeProject() { project ->
            transaction {
                VTTOs vttOsModule = project.bswmdModel(VTTOs.DefRef).single
                vttOsModule.osIsr.byNameOrCreate("CounterIsr_SystemTimer").moRemove()
                logger.info("Remove automatically created VttOs Isr CounterIsr_SystemTimer because it isn't used.")
            }
        }
    }

    /**
     * Map all Isrs to the intended applications, in order:
     * Userdefined Mapping -> SystemTimer Mapping -> Remaining ISR Mapping to default Application
     * @param userDefinedMapping list of mappings which are defined by the user.
     * @param defaultIsrApplication application to which all remaining ISRs get mapped.
     * @param logger instance of the OcsLogger.
     */
    private static void handleInterruptMapping(List<UserDefinedInterruptMapping> userDefinedMapping, String defaultIsrApplication, OcsLogger logger) {
        activeProject() { project ->
            if (PluginsCommon.ConfigPresent(RuntimeSystemConstants.OS_DEFREF)) {
                Os osModule = project.bswmdModel(Os.DefRef).single
                /* 1. UserDefined Mapping */
                executeUserDefinedIsrMapping(userDefinedMapping, osModule, logger)

                /* 2. Core-Specific standard mapping */
                /* 2.1. SystemTimer ISRs */
                executeSystemTimerIsrMapping(logger)

                /* 3. Remaining Isr mapping to defaultIsrApplication */
                executeRemainingIsrMapping(defaultIsrApplication, logger)

                /* 4. Map VTTOs specific Isrs to the defaultApplication */
                mapVttOsIsrToIsrApplication(defaultIsrApplication, logger)
            } else {
                logger.warn("Cannot access the OsModule.")
            }
        }
    }

    /**
     * Generic mapping function used to map an isr by name to an application by name.
     * @param applicationName application to which the ISR shall be mapped.
     * @param isr ISR which shall be mapped.
     * @param logger instance of the OcsLogger.
     */
    private static void mapIsrToApplication(String applicationName, OsIsr isr, OcsLogger logger) {
        activeProject() { project ->
            transaction { transaction ->
                Os osModule = project.bswmdModel(Os.DefRef).single
                OsApplication application = osModule.osApplication.byNameOrNull(applicationName)
                if (application != null) {
                    Boolean referenceNeeded = true
                    isr.referencesOfOsAppIsrRefPointingToMe.each {
                        if (it.parent == application) {
                            referenceNeeded = false
                        }
                    }
                    if (referenceNeeded) {
                        logger.info("Create reference from application $application.shortname to ISR $isr.shortname.")
                        application.osAppIsrRef.createAndAdd().setRefTarget(isr)
                    } else {
                        logger.info("Reference not needed for ISR $isr.shortname because it is already mapped.")
                    }
                } else {
                    logger.error("Cannot find $applicationName.")
                }
            }
        }
    }

    /**
     * Trigger the mapping of the ISR specified by the user to the provided applications.
     * @param userDefinedMapping list of mappings which are defined by the user.
     * @param osModule instance of the Os BSW module.
     * @param logger instance of the OcsLogger.
     */
    private static void executeUserDefinedIsrMapping(List<UserDefinedInterruptMapping> userDefinedMapping, Os osModule, OcsLogger logger) {
        userDefinedMapping.each {
            OsIsr isr = osModule.osIsr.byNameOrNull(it.interrupt)
            if (isr != null) {
                mapIsrToApplication(it.application, isr, logger)
            } else {
                logger.error("Cannot find $it.interrupt.")
            }
        }
    }

    /**
     * Trigger the mapping of the System Timer of each core to the corresponding SystemApplication of the core.
     * @param logger instance of the OcsLogger.
     */
    private static void executeSystemTimerIsrMapping(OcsLogger logger) {
        activeProject() { project ->
            validation {
                solver.solve {
                    result {
                        isId("OS", 2210)
                    }.withAction {
                        containsString("Add reference ")
                    }
                    logger.info("Trigger OS02210 solving action to map the System Timer ISR.")
                }
            }
        }
    }

    /**
     * Trigger the Mapping of the remaining unmapped ISRs to the default application
     * @param defaultIsrApplication application to which all remaining tasks get mapped.
     * @param logger instance of the OcsLogger.
     */
    private static void executeRemainingIsrMapping(String defaultIsrApplication, OcsLogger logger) {
        List<OsIsr> unreferencedIsrs = getUnreferencedOsIsrs()
        unreferencedIsrs.each { unreferencedIsr ->
            mapIsrToApplication(defaultIsrApplication, unreferencedIsr, logger)
        }
    }

    /**
     * Map runnables to the intended tasks, in order:
     * Userdefined Mapping -> ServiceComponent Mapping -> Remaining Bsw Event Mapping to defaultBswTask
     * @param userDefinedTaskMapping list of mappings which are defined by the user.
     * @param defaultBswTask task to which all remaining Bsw events get mapped.
     * @param cores list of cores defined in the data model.
     * @param logger instance of the OcsLogger.
     */
    private static void handleTaskMapping(List<UserDefinedTaskMapping> userDefinedTaskMapping, String defaultBswTask, List<RtsCore> cores, OcsLogger logger) {
        activeProject() {
            /* 1. User defined task mapping of runnables */
            mapRunnablesToTasks(userDefinedTaskMapping, false, logger)

            /* 2. Mapping of Service Component runnables */
            mapEcuMMainFunctions(cores, logger)
            Pattern patternComM = RuntimeSystemConstants.patternComM
            Pattern patternBswM = RuntimeSystemConstants.patternBswM
            mapMainFunctions(defaultBswTask, patternComM, logger)
            mapMainFunctions(defaultBswTask, patternBswM, logger)

            /* 3. Mapping the remaining Bsw Events to defaultBswTask */
            mapBswEventsToTask(defaultBswTask, logger)

            /* 4. Mapping EcucPartitions to MainFunctions */
            mapEcuCPartitionsToMainFunctions(defaultBswTask, logger)
        }
    }

    /**
     * Maps the Service Components Runnables of SecOC which are created at a later point of time to an OsTask.
     * SecOC requires this special handling because new SWCs for SecOCMainFunction Tx and Rx get created with EcucPartition name
     * suffixed, after EcucPartition assignment to SecOC Main Functions.
     */
    private static void mapSecOCSwcRunnablesToTask(OcsLogger logger) {
        activeProject { IProject project ->
            Rte rteCfg = bswmdModel(Rte.DefRef).single()
            def osTaskRx
            def osTaskTx
            def positionInTaskRx
            def positionInTaskTx

            try {

                /* Get the mapped OsTask and Task Position for SecOC_MainFunctionRx from BswEventToTaskMappings */
                rteCfg.rteBswModuleInstance.findAll { it.shortname.contains("SecOC") }.each { rteBswMI ->
                    rteBswMI.rteBswEventToTaskMapping.findAll { it.shortname.contains("MainFunctionRx") }.each { rteEventToTask ->
                        osTaskRx = rteEventToTask.rteBswMappedToTaskRefOrCreate.refTarget
                        positionInTaskRx = rteEventToTask.rteBswPositionInTaskOrCreate.value
                    }
                }
                /* Get the mapped OsTask and Task Position for SecOC_MainFunctionTx from BswEventToTaskMappings */
                rteCfg.rteBswModuleInstance.findAll { it.shortname.contains("SecOC") }.each { rteBswMI ->
                    rteBswMI.rteBswEventToTaskMapping.findAll { it.shortname.contains("MainFunctionTx") }.each { rteEventToTask ->
                        osTaskTx = rteEventToTask.rteBswMappedToTaskRefOrCreate.refTarget
                        positionInTaskTx = rteEventToTask.rteBswPositionInTaskOrCreate.value
                    }
                }
                /* Set the extracted OsTask Ref and Task Position for SecOC_MainFunctionRx in SwcEventToTaskMappings */
                rteCfg.rteSwComponentInstance.findAll { it.shortname.contains("SecOC") }.each { rteSwcI ->
                    rteSwcI.rteEventToTaskMapping.findAll { it.shortname.contains("MainFunctionRx") }.each { rteEventToTask ->
                        transaction {
                            rteEventToTask.rteMappedToTaskRefOrCreate.refTarget = osTaskRx
                            rteEventToTask.rtePositionInTaskOrCreate.value = positionInTaskRx
                        }
                    }
                }
                /* Set the extracted OsTask Ref and Task Position for SecOC_MainFunctionTx in SwcEventToTaskMappings */
                rteCfg.rteSwComponentInstance.findAll { it.shortname.contains("SecOC") }.each { rteSwcI ->
                    rteSwcI.rteEventToTaskMapping.findAll { it.shortname.contains("MainFunctionTx") }.each { rteEventToTask ->
                        transaction {
                            rteEventToTask.rteMappedToTaskRefOrCreate.refTarget = osTaskTx
                            rteEventToTask.rtePositionInTaskOrCreate.value = positionInTaskTx
                        }
                    }
                }
            } catch (Exception exception) {
                logger.error("Error occured during task mapping of SecOC service component:\n${exception}")
            }
        }
    }

    /**
     * Maps the EcucPartition to the MainFunction of BSW modules. The EcucPartition is the identified via the OsApplication where the mentioned OsTask from the userDefinedMapping belongs to.
     * If the MainFunction is not mapped via the userDefinedMapping, the EcucPartition that belongs to the OsApplication where the defaultBswTask is mapped to is used.
     * @param defaultBswTask task to which all remaining Bsw events get mapped.
     */
    private static void mapEcuCPartitionsToMainFunctions(String defaultBswTask, OcsLogger logger) {
        activeProject { IProject project ->

            Os osCfg = bswmdModel(Os.DefRef).single()
            logger.debug("Check that Rte module is available.")
            if(PluginsCommon.ConfigPresent(RuntimeSystemConstants.RTE_DEFREF)){
                Rte rteCfg = bswmdModel(Rte.DefRef).single()
                if (PluginsCommon.ConfigPresent("/MICROSAR/SecOC")) {
                    SecOC secOCCfg = bswmdModel(SecOC.DefRef).single()

                    try {
                        rteCfg.rteBswModuleInstance.findAll { it.shortname.contains("SecOC") }.each { rteBswMI ->
                            /* Get the mapped OsTask for SecOC_MainFunctionRx from RteBswEventToTaskMappings,
                                * then find the OsApplication to which this OsTask belongs,
                                * then find the EcuCPartition to which this OsApplication belongs,
                                * then assign this EcuCPartition to the SecOC_MainFunctionRx. */
                            rteBswMI.rteBswEventToTaskMapping.findAll { it.shortname.contains("MainFunctionRx") }.each { rteEventToTask ->
                                OsTask osTask = rteEventToTask.rteBswMappedToTaskRef.get().refTarget
                                osCfg.osApplication.each { osApplication ->
                                    osApplication.osAppTaskRef.findAll { it.refTarget.toString().contains(osTask.toString()) }.each { osAppTaskRef ->
                                        EcucPartition foundEcuCPartition = osApplication.osAppEcucPartitionRef.get().refTarget
                                        transaction {
                                            secOCCfg.secOCMainFunctionRx.first.secOCMainFunctionRxPartitionRef.refTarget = foundEcuCPartition
                                            String secOCMainFunctionRxName = secOCCfg.secOCMainFunctionRx.first.shortname
                                            String foundEcuCPartitionName = foundEcuCPartition.shortname
                                            logger.info("Assigned $foundEcuCPartitionName to $secOCMainFunctionRxName.")
                                        }
                                    }
                                }
                            }

                            /* Get the mapped OsTask for SecOC_MainFunctionTx from RteBswEventToTaskMappings,
                                * then find the OsApplication to which this OsTask belongs,
                                * then find the EcuCPartition to which this OsApplication belongs
                                * then assign this EcuCPartition to the SecOC_MainFunctionTx. */
                            rteBswMI.rteBswEventToTaskMapping.findAll { it.shortname.contains("MainFunctionTx") }.each { rteEventToTask ->
                                OsTask osTask = rteEventToTask.rteBswMappedToTaskRef.get().refTarget
                                osCfg.osApplication.each { osApplication ->
                                    osApplication.osAppTaskRef.findAll { it.refTarget.toString().contains(osTask.toString()) }.each { osAppTaskRef ->
                                        EcucPartition foundEcuCPartition = osApplication.osAppEcucPartitionRef.get().refTarget
                                        transaction {
                                            secOCCfg.secOCMainFunctionTx.first.secOCMainFunctionTxPartitionRef.refTarget = foundEcuCPartition
                                            String secOCMainFunctionTxName = secOCCfg.secOCMainFunctionTx.first.shortname
                                            String foundEcuCPartitionName = foundEcuCPartition.shortname
                                            logger.info("Assigned $foundEcuCPartitionName to $secOCMainFunctionTxName.")
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception exception) {
                        logger.error("Error occured during setting of EcucPartitionRef for SecOC:\n${exception}")
                    }
                }

                if (PluginsCommon.ConfigPresent("/MICROSAR/Csm")) {
                    Csm csmCfg = bswmdModel(Csm.DefRef).single()

                    try {
                        /* Get the mapped OsTask for Csm_MainFunction from RteBswEventToTaskMappings,
                        * then find the OsApplication to which this OsTask belongs,
                        * then find the EcuCPartition to which this OsApplication belongs
                        * then assign this EcuCPartition to all the CsmMainFunctions. */
                        rteCfg.rteBswModuleInstance.findAll { it.shortname.contains("Csm") }.each { rteBswMI ->
                            rteBswMI.rteBswEventToTaskMapping.findAll { it.shortname.contains("MainFunction") }.each { rteEventToTask ->
                                OsTask osTask = rteEventToTask.rteBswMappedToTaskRef.get().refTarget
                                osCfg.osApplication.each { osApplication ->
                                    osApplication.osAppTaskRef.findAll { it.refTarget.toString().contains(osTask.toString()) }.each { osAppTaskRef ->
                                        EcucPartition foundEcuCPartition = osApplication.osAppEcucPartitionRef.get().refTarget
                                        transaction {
                                            csmCfg.csmMainFunction.each {
                                                it.csmMainFunctionPartitionRef.refTarget = foundEcuCPartition
                                                String csMMainFunctionName = it.shortname
                                                String foundEcuCPartitionName = foundEcuCPartition.shortname
                                                logger.info("Assigned $foundEcuCPartitionName to $csMMainFunctionName")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        /* If the EcuCPartition is still not set for any of the CsmMainFunctions (possible when no cyclic Csm_MainFunction is created),
                            * then find the OsApplication to which the defaultBswTask belongs,
                            * then find the EcuCPartition to which this OsApplication belongs
                            * then assign this EcuCPartition to that CsmMainFunction. */
                        csmCfg.csmMainFunction.each { csmMainFunction ->
                            if (!csmMainFunction.csmMainFunctionPartitionRef.hasRefTarget()) {
                                osCfg.osApplication.each { osApplication ->
                                    osApplication.osAppTaskRef.findAll { it.refTarget.toString().contains(defaultBswTask) }.each { osAppTaskRef ->
                                        EcucPartition foundEcuCPartition = osApplication.osAppEcucPartitionRef.get().refTarget
                                        transaction {
                                            csmMainFunction.csmMainFunctionPartitionRef.refTarget = foundEcuCPartition
                                            String csMMainFunctionName = csmMainFunction.shortname
                                            String foundEcuCPartitionName = foundEcuCPartition.shortname
                                            logger.info("Assigned $foundEcuCPartitionName to $csMMainFunctionName")
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception exception) {
                        logger.error("Error occured during setting of EcucPartitionRef for Csm:\n${exception}")
                    }
                }
            }
        }
    }

    /**
     * Maps the VTTOs specific Isrs to the default Application.
     * @param isrApplicationName mapping target application.
     * @param logger instance of the OcsLogger.
     */
    private static void mapVttOsIsrToIsrApplication(String isrApplicationName, OcsLogger logger) {
        activeProject() { IProject project ->
            project.validation {
                validationResults.each { IValidationResultUI iValidationResults ->
                    if ((iValidationResults.id.origin == "OS") &&
                            (iValidationResults.id.id == 2800) &&
                            iValidationResults.isActive()) {
                        if (!iValidationResults.solvingActions.isEmpty()) {
                            iValidationResults.solvingActions.each { sa ->
                                if (sa.description.contains("Add reference") &&
                                        sa.description.contains(isrApplicationName) &&
                                        iValidationResults.isActive()) {
                                    try {
                                        logger.info("Trigger OS02800 solving action to map VTTOs OsIsrs to an $isrApplicationName.")
                                        sa.solve()
                                    } catch (IllegalArgumentException | IllegalStateException exception) {
                                        logger.error("$exception")
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
     * Generic mapping function used to map an unmapped runnable by name to a task by name.
     * @param userDefinedTaskMapping list of mappings which are defined by the user.
     * @param eventBased Boolean to define if the task mapping selection should be eventbased (==true) or based on the executable entities (==false).
     * @param logger instance of the OcsLogger.
     */
    private static void mapRunnablesToTasks(List<UserDefinedTaskMapping> userDefinedTaskMapping, Boolean eventBased, OcsLogger logger) {
        activeProject() { project ->
            transaction { transaction ->
                domain { domain ->
                    userDefinedTaskMapping.each { entry ->
                        try {
                            runtimeSystem {
                                List<? extends ITaskMapping> taskMappings
                                if (eventBased) {
                                    taskMappings = selectEvents {
                                        unmapped()
                                        name(entry.runnable)
                                    } mapToTask {
                                        selectTask {
                                            name(entry.task)
                                        }
                                    }
                                } else {
                                    taskMappings = selectExecutableEntities {
                                        unmapped()
                                        name(entry.runnable)
                                    } mapToTask {
                                        selectTask {
                                            name(entry.task)
                                        }
                                    }
                                }
                                for (def taskMapping in taskMappings) {
                                    logger.info("Mapped ${taskMapping.getExecutableEntity().getName()} to ${taskMapping.getMappedTask().getName()}")
                                }
                            }
                        }
                        catch (Exception e) {
                            logger.error("An error occurred while trying to map ${entry.runnable} to ${entry.task} : ${e.message}")
                        }
                    }
                }
            }
        }
    }

    /**
     * Trigger the mapping of the EcuM Timing Events - one is individually mapped to one used core.
     * @param modelCores list of cores defined in the data model.
     * @param logger instance of the OcsLogger.
     */
    private static void mapEcuMMainFunctions(List<RtsCore> modelCores, OcsLogger logger) {
        def patternEcuM = RuntimeSystemConstants.patternEcuM
        List<IEvent> selectedEcuMEvents = null
        activeProject() { project ->
            transaction { transaction ->
                domain { domain ->
                    /* get EcuM MainFunction timing events */
                    runtimeSystem { runtimeSystem ->
                        selectedEcuMEvents = selectEvents {
                            unmapped()
                            bswEvent()
                            mandatory()
                            not { applicationComponent() }
                            timing()
                            moduleConfiguration(patternEcuM)
                        }.getEvents()
                    }
                    selectedEcuMEvents.sort { it.name }
                    // get the BswTask of the QM Application of each core.
                    // iterate over each core
                    List<String> bswTasks = []
                    for (core in modelCores) {
                        for (application in core.applications) {
                            if (application.asilLevel == Asil.QM) {
                                for (task in application.tasks) {
                                    if (task.name.contains('BswTask')) {
                                        bswTasks.add(task.name)
                                        break // Exit the innermost loop but continue with the next core
                                    }
                                }
                            }
                        }
                    }
                    // Combine the two lists into a list of UserDefinedTaskMapping objects
                    List<UserDefinedTaskMapping> taskMappings = []
                    int size = Math.min(selectedEcuMEvents.size(), bswTasks.size())
                    for (int i = 0; i < size; i++) {
                        taskMappings.add(new UserDefinedTaskMapping(
                                selectedEcuMEvents[i].name,
                                bswTasks[i]
                        ))
                    }
                    mapRunnablesToTasks(taskMappings, true, logger)
                }
            }
        }
    }

    /**
     * Generic mapping function to map bsw schedulable entities by a specific module pattern (module name) to a task by name.
     * @param bswTaskName task to which the bsw entity shall be mapped.
     * @param pattern Pattern by which the bsw entity is selected (module name).
     * @param logger instance of the OcsLogger.
     */
    private static void mapMainFunctions(String bswTaskName, Pattern pattern, OcsLogger logger) {
        activeProject() {
            transaction {
                domain {
                    try {
                        runtimeSystem { runtimeSystem ->
                            List<? extends ITaskMapping> taskMappings = selectExecutableEntities {
                                unmapped()
                                bswSchedulableEntity()
                                moduleConfiguration(pattern)
                            } mapToTask {
                                selectTask {
                                    name(bswTaskName)
                                }
                            }
                            for (def taskMapping in taskMappings) {
                                logger.info("Mapped ${taskMapping.getExecutableEntity().getName()} to ${taskMapping.getMappedTask().getName()}")
                            }
                        }
                    }
                    catch (Exception e) {
                        logger.error("An error occurred while trying to map runnable to ${bswTaskName} : ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Map the unmapped bsw events to the defaultBswTask.
     * @param bswTaskName task to which the bsw events shall be mapped.
     * @param logger instance of the OcsLogger.
     */
    private static void mapBswEventsToTask(String bswTaskName, OcsLogger logger) {
        activeProject() {
            transaction {
                domain {
                    try {
                        runtimeSystem { runtimeSystem ->
                            List<? extends ITaskMapping> taskMappings = selectEvents {
                                unmapped()
                                bswEvent()
                            } mapToTask {
                                selectTask {
                                    name(bswTaskName)
                                }
                            }
                            for (def taskMapping in taskMappings) {
                                logger.info("Mapped ${taskMapping.getExecutableEntity().getName()} to ${taskMapping.getMappedTask().getName()}")
                            }
                        }
                    }
                    catch (Exception e) {
                        logger.error("An error occurred while trying to map runnable to ${bswTaskName} : ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Trigger the following solving action of the OS BSW module.<br>
     * <ul>
     *     <li>OS4310</li>
     * </ul>
     * Correct priority and source for all XSignal ISRs.
     * @param logger
     */
    private static void triggerXSignalChannelSolvingAction(List<RtsCore> cores, OcsLogger logger) {
        activeProject() { IProject project ->
            boolean xSignalIsrCreated = false
            project.validation {
                for (int i = 0; i < cores.size(); i++) {
                    validationResults.each { IValidationResultUI iValidationResults ->
                        if ((iValidationResults.id.origin == "OS") &&
                                (iValidationResults.id.id == 4310) &&
                                (iValidationResults.isActive())) {
                            if (!iValidationResults.solvingActions.isEmpty()) {
                                iValidationResults.solvingActions.each { ISolvingActionUI sa ->
                                    if (sa.description.contains("Create all X-Signal channels") && iValidationResults.isActive()) {
                                        logger.info("Trigger OS04310 solving action.")
                                        sa.solve()
                                        xSignalIsrCreated = true
                                    }
                                }
                            }
                        }
                    }
                }
            }
            transaction {
                cores.each { currentCore ->
                    Os osModule = project.bswmdModel(Os.DefRef).single
                    OsIsr isr = osModule.osIsr.byNameOrNull("XSignalIsr_${currentCore.name}")
                    if (isr != null && xSignalIsrCreated) {
                        IParameterStatePublished xSignalIsrPriorityState = isr.osIsrInterruptPriority.ceState
                        IParameterStatePublished xSignalIsrIsrSourceState = isr.osIsrInterruptSource.ceState
                        if (xSignalIsrPriorityState.isChangeable()) {
                            isr.osIsrInterruptPriority.valueMdf = RuntimeSystemConstants.XSignalIsrPriority
                        }
                        if (xSignalIsrIsrSourceState.isChangeable()) {
                            isr.osIsrInterruptSource.valueMdf = RuntimeSystemConstants.XSignalIsrSource
                        }

                    }
                }
            }
        }
    }

    /**
     * Trigger the following solving action of the RTE BSW module.<br>
     * <ul>
     *     <li>RTE1068</li>
     * </ul>
     * @param logger
     */
    private static void triggerRteSolvingActionForRunnableOrder(OcsLogger logger) {
        activeProject() { IProject project ->
            PluginsCommon.modelSynchronization(project, logger)
            // Justify missing return statement because it could be the case that no solving action appears
            //noinspection GroovyMissingReturnStatement
            validation {
                final String RTE = "RTE"
                Collection<IValidationResultUI> rte1068Results = validationResults.findAll { IValidationResultUI iValidationResults ->
                    iValidationResults.isId(RTE, 1068) && iValidationResults.isActive()
                }
                if (!rte1068Results.isEmpty()) {
                    solver.solve {
                        result {
                            isId(RTE, 1068)
                        }.withAction {
                            logger.info("Trigger RTE1068 solving action.")
                            solvingActions.first
                        }
                    }
                }
            }
        }
    }

    /**
     * Trigger the following solving action of the COM BSW module.<br>
     * <ul>
     *     <li>COM2430</li>
     *     <li>COM2432</li>
     * </ul>
     * @param logger instance of the OcsLogger.
     */
    private static void triggerComSolvingActions(OcsLogger logger) {
        activeProject() { IProject project ->
            PluginsCommon.modelSynchronization(project, logger)
            project.validation {
                final String COM = "COM"
                validationResults.each { IValidationResultUI iValidationResults ->
                    if ((iValidationResults.id.origin == COM) &&
                            (iValidationResults.id.id == 2430) &&
                            (iValidationResults.isActive())) {
                        logger.info("Trigger COM2430 solving action.")
                        iValidationResults.preferredSolvingAction.solve()
                    }
                    if ((iValidationResults.id.origin == COM) &&
                            (iValidationResults.id.id == 2432) &&
                            (iValidationResults.isActive())) {
                        logger.info("Trigger COM2432 solving action.")
                        iValidationResults.preferredSolvingAction.solve()
                    }
                }
            }
        }
    }

    /**
     * Trigger the Os solving action for validation message: OS01600 - Invalid Alarm configuration. Currently every
     * OsApplication different to the SystemApplication is fine for the mapping.
     * Example: Add reference 'Rte_Al_TE2_ApplTask_Appl_QM_Core_0_0_20ms' to Appl_QM_Core_0
     * @param logger instance of the OcsLogger
     */
    private static void triggerOsSolvingActionForAlarmMapping(OcsLogger logger) {
        activeProject() { IProject project ->
            PluginsCommon.modelSynchronization(project, logger)
            project.validation {
                validationResults.each { IValidationResultUI iValidationResults ->
                    if ((iValidationResults.id.origin == "OS") &&
                            (iValidationResults.id.id == 1600) &&
                            iValidationResults.isActive()) {
                        if (!iValidationResults.solvingActions.isEmpty()) {
                            iValidationResults.solvingActions.each { sa ->
                                if (sa.description.contains("Add reference") &&
                                        !sa.description.contains("SystemApplication") &&
                                        iValidationResults.isActive()) {
                                    logger.info("Trigger Os1600 solving action to add an OsAlarm reference to an OsApplication.")
                                    sa.solve()
                                }
                            }
                            // Solve the remaining validation messages with the solving actions 'Delete parameter'
                            if (iValidationResults.solvingActions.first.description.contains("Delete parameter")) {
                                iValidationResults.solvingActions.first.solve()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Perform a model synchronization and trigger the calculation phase of the Rte. This could lead to new Os
     * configuration elements which maybe cause some configuration issues.
     * @param logger instance of the OcsLogger.
     */
    private static void triggerRteCalculation(OcsLogger logger) {
        logger.info("Trigger the Rte calculation phase.")
        DefRef rteDefRef = DefRef.create("/MICROSAR", "Rte")
        activeProject() { IProject project ->
            PluginsCommon.modelSynchronization(project, logger)
            IGenerationApi myValidation = project.generation
            myValidation.settings.deselectAll()
            myValidation.settings.selectGeneratorByDefRef(rteDefRef)
            myValidation.settings.ignoreErrors = true
            myValidation.generate()
        }
    }

    /**
     * Map unassigned BSW SWCs (currently Det, Csm, SecOC) to Master Application (Application containing defaultBswTask)
     * Map System-Os-SWCs to their corresponding EcuCPartitions.
     * The numberOfCores parameter will be checked against the number of OsPhysicalCores and adapted accordingly if
     * necessary. Currently this method assumes that there is a EcucPartition with QM Asil level.
     * @param cores list of RtsCores based on the RuntimeSystemModel.
     * @param logger instance of the OcsLogger.
     */
    private static void triggerBswServiceComponentSwcMapping(List<RtsCore> cores, String defaultBswTask, OcsLogger logger) {
        // Justify missing return statement because it could be the case that project is updated under all conditions
        //noinspection GroovyMissingReturnStatement
        activeProject() { IProject project ->
            PluginsCommon.modelSynchronization(project, logger)
            AsrPath flatExtractPath = project.getSystemDescription().getPaths().getFlatCompositionTypePath()
            MIReferrable flatExtractModel = project.mdfModel(flatExtractPath)
            EcuC ecuModule = bswmdModel(EcuC.DefRef).single

            if (flatExtractModel != null){
                triggerSwcInstanceMapping(defaultBswTask, flatExtractModel, logger)
                mapCoresSwcInstance(ecuModule, flatExtractModel, cores, logger)
            }
        }
    }

    /**
     * Trigger the mapping of the provided swc instance (via the module name) to the QM application of the master core.
     * @param defaultBswTask default bsw task from the model to get the correct partition.
     * @param flatExtractModel PAI internal model to access the API to set the reference.
     * @param logger instance of the OcsLogger.
     */
    private static void triggerSwcInstanceMapping(String defaultBswTask, MIReferrable flatExtractModel, OcsLogger logger) {
        activeProject() {
            Os osModule = bswmdModel(Os.DefRef).single
            OsApplication application = osModule.osApplication.find { application ->
                application.osAppTaskRef.find { taskRef ->
                    taskRef.refTarget.shortname == defaultBswTask
                }
            }
            if (application != null) {
                OsAppEcucPartitionRef ecucPartitionRef = application.osAppEcucPartitionRefOrNull
                if (ecucPartitionRef != null) {
                    EcucPartition ecucPartition = ecucPartitionRef.refTarget
                    logger.info("Assign Service Components for $ecucPartition.shortname.")
                    if (ecucPartition != null) {
                        RuntimeSystemConstants.swcInstances.each { swcInstance ->
                            if (swcInstance == "Csm") {
                                project.validation {
                                    validationResults.each { IValidationResultUI iValidationResult ->
                                        if (iValidationResult.id.origin == "RTE" &&
                                                iValidationResult.id.id == 13009 &&
                                                iValidationResult.isActive() &&
                                                iValidationResult.description.toString().startsWith("Component <Csm> is not assigned to an OS Application")) {
                                            mapSwcInstance(ecucPartition, flatExtractModel, swcInstance, logger)
                                        }
                                    }
                                }
                            } else {
                                mapSwcInstance(ecucPartition, flatExtractModel, swcInstance, logger)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Map the provided swc instance to the corresponding QM application of the master core.
     * @param partition Partition to which the provided swc instance will get mapped. (Contains the QM Application)
     * @param flatExtractModel PAI internal model to access the API to set the reference.
     * @param moduleName name of the module whose swc instance shall be mapped.
     * @param logger instance of the OcsLogger.
     */
    private static void mapSwcInstance(EcucPartition partition, MIReferrable flatExtractModel, String moduleName, OcsLogger logger) {
        MIReferrable serviceComponent = flatExtractModel.childByName(moduleName)
        if (serviceComponent != null) {
            if (partition != null) {
                logger.info("Process Service Component $serviceComponent.name.")
                boolean alreadyMapped = checkExistingSwcInstanceMapping(partition, serviceComponent)
                if (!alreadyMapped) {
                    createPartitionSwcInstanceRef(partition, serviceComponent, logger)
                }
            }
        }
    }

    /**
     * Map the core specific swc instances to the corresponding System applications of each core.
     * @param ecucModule Ecuc bsw module.
     * @param flatExtractModel PAI internal model to access the API to set the reference.
     * @param cores list of modelled cores.
     * @param logger instance of the OcsLogger.
     */
    private static void mapCoresSwcInstance(EcuC ecucModule, MIReferrable flatExtractModel, List<RtsCore> cores, OcsLogger logger) {
        cores.each { currentCore ->
            EcucPartition coreEcuCPartition = getEcucPartitionOfSystemApplication(ecucModule, currentCore, logger)
            if (null != coreEcuCPartition) {
                logger.info("Assign Service Component for $coreEcuCPartition.shortname.")
                MIReferrable osServiceCmp = flatExtractModel.childByName("Os_" + currentCore.name + "_swc")
                if (null != osServiceCmp) {
                    logger.info("Process Service Component $osServiceCmp.name.")
                    boolean osSwcIsMapped = checkExistingSwcInstanceMapping(coreEcuCPartition, osServiceCmp)
                    if (!osSwcIsMapped) {
                        createPartitionSwcInstanceRef(coreEcuCPartition, osServiceCmp, logger)
                    }
                } else {
                    if (cores.size() > 1) {
                        logger.error("Cannot find Service Component Os_${currentCore.name}_swc during Core SWC instance Mapping.")
                    }
                }
            }
        }
    }

    /**
     * Check for an already existing mapping of the swc instance.
     * @param partition ecuc partition which is checked for existing mappings.
     * @param serviceComponent object for which a mapping is checked.
     * @return Boolean if already mapped.
     */
    private static Boolean checkExistingSwcInstanceMapping(EcucPartition partition, MIReferrable serviceComponent) {
        Boolean isMapped = false
        partition.getEcucPartitionSoftwareComponentInstanceRef().each { GInstanceReference swCmpInsRefPtr ->
            MIInstanceReferenceValue refValue = swCmpInsRefPtr.getMdfObject()
            MIARAnyInstanceRef anyInstanceRef
            MIReferrableARRef referableARRef
            if (refValue != null) {
                anyInstanceRef = refValue.getValue()
                if (anyInstanceRef != null) {
                    referableARRef = anyInstanceRef.getTarget()
                    if (referableARRef != null &&
                            referableARRef.getValue() == serviceComponent.getAsrPath().toString().
                            substring("AsrPath: ".size())) {
                        isMapped = true
                    }
                }
            }
        }
        return isMapped
    }

    /**
     * Create a reference from the given EcuC partition to the given service component.
     * @param partition ecuc partition for which a mapping is added.
     * @param serviceComponent swc instance for which a mapping is added.
     * @param logger instance of the OcsLogger.
     */
    private static void createPartitionSwcInstanceRef(EcucPartition partition, MIReferrable serviceComponent, OcsLogger logger) {
        activeProject() { project ->
            transaction {
                GInstanceReference instanceRef = partition.getEcucPartitionSoftwareComponentInstanceRef().createAndAdd()
                MIInstanceReferenceValue refValue = instanceRef.getMdfObject()
                MIARAnyInstanceRef anyInstanceRef = refValue.getValue()
                MIReferrableARRef referableARREf = anyInstanceRef.getTarget()
                referableARREf.setValue(serviceComponent.getAsrPath())
                logger.info("Mapped ${serviceComponent.name} to ${partition.shortname}.")
            }
        }
    }

    /**
     * Trigger the following solving action of the VTTOs BSW module.<br>
     * <ul>
     *     <li>VTTOs54000 - VTTOs is out of sync with Os</li>
     * </ul>
     * @param logger instance of the OcsLogger.
     */
    private static void triggerVttOsOutOfSyncSolvingAction(OcsLogger logger) {
        activeProject() {
            validation {
                validationResults.each { IValidationResultUI iValidationResults ->
                    if ((iValidationResults.id.id == 54000) &&
                            iValidationResults.id.origin == "VTTOs") {
                        iValidationResults.solvingActions.each { ISolvingActionUI solvingAction ->
                            if (iValidationResults.isActive()) {
                                logger.info("Trigger VTTOs54000 solving action.")
                                solvingAction.solve()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Trigger the following solving action of the VTTEcuC BSW module.<br>
     * <ul>
     *     <li>VTTEcuc54000 - VTTEcuc is out of sync with Ecuc</li>
     * </ul>
     * @param logger instance of the OcsLogger.
     */
    private static void triggerVttEcucOutOfSyncSolvingAction(OcsLogger logger) {
        activeProject() {
            validation {
                validationResults.each { IValidationResultUI iValidationResults ->
                    if ((iValidationResults.id.id == 54000) &&
                            iValidationResults.id.origin == "VTTEcuC") {
                        iValidationResults.solvingActions.each { ISolvingActionUI solvingAction ->
                            if (iValidationResults.isActive()) {
                                logger.info("Trigger VTTEcuC54000 solving action.")
                                solvingAction.solve()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Remove all OsMemoryRegions present in the OsMemoryProtection
     * @param logger instance of the OcsLogger.
     */
    private static void removeOsMemoryRegions(OcsLogger logger) {
        activeProject() {
            transaction {
                Os osCfg = bswmdModel(Os.DefRef).single
                if (osCfg.existsOsMemoryProtection()) {
                    if (osCfg.osMemoryProtectionOrCreate.osMemoryRegion.size() > 0) {
                        try {
                            osCfg.osMemoryProtectionOrCreate.osMemoryRegion.each { region ->
                                region.moRemove()
                            }
                            logger.info("Removed unnecessary OsMemoryRegions as given Scalability Class does not allow them.")
                        } catch (Exception e) {
                            logger.warn("An exception occurred during OsMemoryRegion deletion: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    /**
     * Delete a EcuCCoreDefinition container it case it is not referenced by someone else.
     * @param logger instance of the OcsLogger
     */
    private static void removeUnreferencedEcucCoreDefinitions(OcsLogger logger) {
        activeProject() {
            EcuC ecucModule = it.bswmdModel(EcuC.DefRef).single
            GIOptional<EcucHardware> ecucHardwareOpt = ecucModule.ecucHardware
            if (ecucHardwareOpt.isPresent()) {
                EcucHardware ecucHardware = ecucHardwareOpt.get()
                GICList<EcucCoreDefinition> ecucCoreDefinitions = ecucHardware.ecucCoreDefinition
                for (EcucCoreDefinition ecucCoreDefinition in ecucCoreDefinitions) {
                    List<GIReferenceToContainer> referencesPointingToMe = ecucCoreDefinition.referencesPointingToMe
                    if (referencesPointingToMe.isEmpty()) {
                        transaction {
                            if (ecucCoreDefinition.ceState.isDeletable()) {
                                logger.info("Delete unreferenced " +
                                        "EcucCoreDefinition $ecucCoreDefinition.shortname.")
                                ecucCoreDefinition.moRemove()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Create a reference via OsCorePhysicalCoreRef to an OsPhysicalCore. To apply the reference an unreferenced
     * OsPhysicalCore must be available.
     * @param osModule instance of the Os BSW module.
     * @param coreName name of the OsCore for which the reference should be set.
     * @param logger instance of the OcsLogger.
     */
     static void createOsCorePhysicalCoreRef(Os osModule, OsCore osCore, OcsLogger logger) {
        String coreName = osCore?.shortname
        logger.info("Create OsCorePhysicalCoreRef for $coreName.")
        List<OsPhysicalCore> osPhysicalCoresReferenced = getReferencedOsPhysicalCores(osModule)
        GICList<OsPhysicalCore> osPhysicalCoresAvailable =
                RuntimeSystemOsConfig.getOsPhysicalCoresOfFirstOsDerivativeInformation(osModule, logger)
        OsPhysicalCore freePhysicalCore = getUnreferencedOsPhysicalCore(osPhysicalCoresReferenced, osPhysicalCoresAvailable)
        OsCorePhysicalCoreRef physicalCoreRefParameter = osCore.osCorePhysicalCoreRef
        if (!physicalCoreRefParameter.hasRefTarget()) {
            if (freePhysicalCore != null) {
                // Default behavior for an OsCore seems to be that this parameter is not changeable.
                // Instead a solving action should be triggered to modified it.
                physicalCoreRefParameter.ceState.setUserDefined(true)
                physicalCoreRefParameter.refTarget = freePhysicalCore
                physicalCoreRefParameter.ceState.setUserDefined(false)
                logger.info("Assign ${freePhysicalCore?.shortname} to $coreName.")
            } else {
                logger.error("Did not find unreferenced OsCorePhysicalCore for " +
                        "assignment as OsCorePhysicalCoreRef of $coreName.")
            }
        } else {
            logger.info("${physicalCoreRefParameter?.refTarget?.shortname} is already " +
                    "assigned to $coreName.")
        }
    }

    /**
     * Create a reference via OsCoreEcucCoreRef to an EcucCoreDefinition.
     * @param osCore OsCore container for which the EcucCoreDefinition reference should be set.
     * @param ecucCore EcucCoreDefinition container core that should be referenced in the OsCore.
     * @param logger instance of the OcsLogger.
     */
    private static void createOsCoreEcucCoreRef(OsCore osCore, EcucCoreDefinition ecucCore, OcsLogger logger) {
        String coreName = osCore?.shortname
        logger.info("Create $coreName OsCoreEcucCoreRef reference to ${ecucCore?.shortname}.")
        // Assumption is that the EcucCoreDefinitions are not already assigned at another OsCore because they where
        // created during the RuntimeSystem Plugin processing.
        OsCoreEcucCoreRef ecucCoreRefParameter = osCore.osCoreEcucCoreRefOrCreate
        if (PluginsCommon.isChangeable(ecucCoreRefParameter.mdfObject, logger)) {
            ecucCoreRefParameter.refTarget = ecucCore
        }
    }

    /**
     * Create a reference for the OsMasterCore to the given OsCore.
     * @param osModule instance of the Os BSW module.
     * @param osCore OsCore container that should be referenced as OsMasterCore.
     * @param logger instance of the OcsLogger.
     */
    private static void createOsMasterCore(Os osModule, OsCore osCore, OcsLogger logger) {
        logger.info("Create OsOs OsMasterCore reference to ${osCore?.shortname}.")
        OsMasterCore masterCoreParameter = osModule.osOS.osMasterCoreOrCreate
        if (PluginsCommon.isChangeable(masterCoreParameter.mdfObject, logger)) {
            masterCoreParameter.refTarget = osCore
        }
    }

    /**
     * Create a reference for the OsApplication to the given EcucCoreDefinition.
     * @param osApp OsApplication container for which the reference should be set.
     * @param ecucCore EcucCoreDefinition that should be referenced as OsApplicationCoreRef.
     * @param logger instance of the OcsLogger.
     */
    private static void createOsApplicationCoreRef(OsApplication osApp, EcucCoreDefinition ecucCore, OcsLogger logger) {
        logger.info("Create ${osApp?.shortname} OsApplicationCoreRef reference to ${ecucCore?.shortname}.")
        OsApplicationCoreRef ecucCoreRefParameter = osApp.osApplicationCoreRefOrCreate
        if (PluginsCommon.isChangeable(ecucCoreRefParameter.mdfObject, logger)) {
            ecucCoreRefParameter.refTarget = ecucCore
        }
    }

    /**
     * Create a reference for the OsApplication to the given EcucPartition.
     * @param osApp OsApplication container for which the reference should be set.
     * @param ecucPartition EcucPartition that should be referenced as OsAppEcucPartitionRef.
     * @param logger instance of the OcsLogger.
     */
    private static void createOsAppEcucPartitionRef(OsApplication osApp, EcucPartition ecucPartition, OcsLogger logger) {
        logger.info("Create ${osApp?.shortname} OsAppEcucPartitionRef reference to ${ecucPartition?.shortname}.")
        OsAppEcucPartitionRef osPartitionRefParameter = osApp.osAppEcucPartitionRefOrCreate
        if (PluginsCommon.isChangeable(osPartitionRefParameter.mdfObject, logger)) {
            osPartitionRefParameter.refTarget = ecucPartition
        }
    }


    /**
     * Create a reference for the SystemApplication to the given App Counter Ref.
     * @param osSystemApplication SystemApplication container for which the reference should be set.
     * @param systemTimer systemTimer that should be referenced as App Counter Ref.
     * @param logger instance of the OcsLogger.
     */
    private static void createOsAppCounterRef(OsApplication osSystemApplication, OsCounter systemTimer, OcsLogger logger) {
        String osAppCounterRef = osSystemApplication.osAppCounterRef.refTargetMdf.name
        if (!osAppCounterRef.contains(systemTimer.shortname)) {
            osSystemApplication.osAppCounterRef.createAndAdd().setRefTargetMdf(systemTimer as MIContainer)
            logger.info("Create ${osSystemApplication.shortname} AppCounterRef reference to ${systemTimer.shortname}.")
        }
    }

    /**
     * Create a reference for the OsTask to the given OsResource.
     * @param osTask OsTask container for which the reference should be set.
     * @param osResource OsResource that should be referenced as OsTaskResourceRef.
     * @param logger instance of the OcsLogger.
     */
    private static void createOsTaskResourceRef(OsTask osTask, OsResource osResource, OcsLogger logger) {
        logger.info("Create ${osTask?.shortname} OsTaskResourceRef reference to ${osResource?.shortname}.")
        GIPList<OsTaskResourceRef> osTaskResourceRefParameters = osTask.osTaskResourceRef
        // Assumption is that it is always allowed to add a new reference parameter to the list,
        // therefore no check for the PluginsCommon.isChangeable is done.
        // Check if the OsResource is already referenced.
        if (!osTaskResourceRefParameters.refTarget.contains(osResource)) {
            osTaskResourceRefParameters.createAndAdd().setRefTarget(osResource)
        }
    }

    /**
     * Create a reference for the OsTask to the given OsResource.
     * @param osApp OsApplication container for which the reference should be set.
     * @param osTask OsTask that should be referenced as OsAppTaskRef.
     * @param logger instance of the OcsLogger.
     */
    private static void createOsAppTaskRef(OsApplication osApp, OsTask osTask, OcsLogger logger) {
        logger.info("Create ${osApp?.shortname} OsAppTaskRef reference to ${osTask?.shortname}.")
        GIPList<OsAppTaskRef> taskRefParameters = osApp.osAppTaskRef
        // Assumption is that it is always allowed to add a new reference parameter to the list,
        // therefore no check for the PluginsCommon.isChangeable is done.
        // Check if the OsTask is already referenced.
        if (!taskRefParameters.refTarget.contains(osTask)) {
            taskRefParameters.createAndAdd().setRefTarget(osTask)
        }
    }

    /**
     * Create a reference for the EcuMFlexConfiguration to the given Partition Ref of each Core (QM Level).
     * @param EcucPartitionQM Partition that should be referenced as Partition Ref.
     * @param ecumModule instance of the EcuM BSW module.
     * @param logger instance of the OcsLogger.
     */
    private static void createEcuMFlexConfigurationPartitionRef(EcucPartition EcucPartitionQM, EcuM ecumModule, OcsLogger logger) {
        logger.info("Create the PartitionRef in EcuMFlexConfiguration reference to ${EcucPartitionQM.shortname}.")
        def ecuMPartitionRefParameters = ecumModule.ecuMConfigurationOrCreate.ecuMFlexConfigurationOrCreate.ecuMPartitionRef
        if (!ecuMPartitionRefParameters.refTarget.contains(EcucPartitionQM)) {
            ecuMPartitionRefParameters.createAndAdd().setRefTarget(EcucPartitionQM)
        }
    }

    /**
     * Remove the refTarget of the OsSystemTimer in case it does not point to a valid location.
     * @param osModule instance of the Os BSW module.
     * @param logger instance of the OcsLogger.
     */
    private static void removeOsSystemTimerInvalidReference(Os osModule, OcsLogger logger) {
        GIOptional<OsSystemTimer> systemTimerOpt = osModule.osOS.osSystemTimer
        if (systemTimerOpt.exists()) {
            OsSystemTimer osSystemTimer = systemTimerOpt.get()
            if (!osSystemTimer.hasRefTarget()) {
                logger.warn("OsSystemTimer has no valid refTarget.")
                if (osSystemTimer.mdfObject.ceState.isDeletable()) {
                    logger.info("OsSystemTimer referenced target will be removed.")
                    osSystemTimer.mdfObject.moRemove()
                }
            }
        }
    }

    /**
     * Create a reference for the SystemTimer to the given OsIsr and the osDriverHardwareTimerChannel
     * @param counterIsr counterISR that should be referenced as osDriverIsrRef.
     * @param systemTimer container for which the reference should be set.
     * @param index for selecting a HardwareTimerChannel. CoreID is used because it is an iterated value  (Channel usage should be unique).
     * @param logger instance of the OcsLogger.
     */
    private static void createOsCounterRefs(OsIsr counterIsr, OsCounter systemTimer, int index, OcsLogger logger) {
        List<OsHardwareTimerChannel> osHardwareTimerChannel = systemTimer.osDriverOrCreate.osDriverHardwareTimerChannelRefOrCreate.getPossibleRefTargets()
        try {
            systemTimer.osDriverOrCreate.osDriverHardwareTimerChannelRefOrCreate.setRefTargetMdf(osHardwareTimerChannel.get(index) as MIContainer)
        } catch (Exception e) {
            logger.error(
                    "An error occurred while trying to set the reference of the osDriverHardwareTimerChannel: ${e.message}"
            )
        }

        systemTimer.osDriverOrCreate.osDriverIsrRefOrCreate.setRefTargetMdf(counterIsr as MIContainer)
        logger.info("Set the osDriverIsr Reference of $systemTimer.shortname to $counterIsr.shortname.")
    }

    // Helper methods

    /**
     * Find an OsIsr of the counter of the current core by applying the name schema.
     * @param osModule instance of Os BSW module.
     * @param core instance for the name of the corresponding CounterIsr.
     * @return OsIsr container instance that matches the created name.
     */
    private static OsIsr getCounterIsrByName(Os osModule, RtsCore core) {
        return osModule.osIsr.byNameOrCreate("CounterIsr_SystemTimer_$core.name")
    }

    /**
     * Find an OsCounter of the current core by applying the name schema.
     * @param osModule instance of Os BSW module.
     * @param core for the name of the corresponding SystemTimer.
     * @return OsCounter container instance that matches the created name.
     */
    private static OsCounter getSystemTimerByName(Os osModule, RtsCore core) {
        return osModule.osCounter.byNameOrCreate("SystemTimer_$core.name")
    }

    /**
     * Find an EcucPartition by checking the ASIL level "QM" of the EcucPartition
     * @param ecucModule instance of Os BSW module.
     * @param core for the name of the corresponding EcucPartition
     * @return EcucPartitionQM EcucPartition with the ASIL level "QM"
     */
    private static EcucPartition getEcucPartitionByQMLevel(EcuC ecucModule, RtsCore core) {
        GICList<EcucPartition> ecucPartition = ecucModule.ecucPartitionCollectionOrCreate.getEcucPartition()
        def ecucPartitionQM = null
        core.applications.each { application ->
            ecucPartition.each { it ->
                if ((it.getASIL().value.toString() == "QM") && (it.shortname == "EcucPartition_$application.name")) {
                    ecucPartitionQM = it
                }
            }
        }
        return ecucPartitionQM
    }

    /**
     * Returns the EcucPartition corresponding to the System Application of the given core.
     * @param ecucModule instance of Os BSW module.
     * @param core for the name of the corresponding EcucPartition.
     * @return ecucPartitionSystemApplication EcucPartition corresponding to the System Application.
     */
    private static EcucPartition getEcucPartitionOfSystemApplication(EcuC ecucModule, RtsCore core, OcsLogger logger) {
        if (ecucModule.existsEcucPartitionCollection()) {
            GICList<EcucPartition> ecucPartitionList = ecucModule.ecucPartitionCollection.get().ecucPartition
            EcucPartition ecucPartitionSystemApplication = null
            ecucPartitionList.each { it ->
                if ((it.shortname.contains("$core.name") && (it.shortname.contains("SystemApplication")))) {
                    ecucPartitionSystemApplication = it
                }
            }
            return ecucPartitionSystemApplication
        } else {
            logger.error("No EcuCPartitionCollection available while trying to get EcuC Partition! Review your configuration!")
        }
    }

    /**
     * Process the list of OsPhysicalCores and check which of them is not yet referenced as osCorePhysicalCoreRefs.
     * @param osCorePhysicalCoreRefs list of already referenced OsPhysicalCores.
     * @param physicalCores list of available OsPhysicalCores.
     * @return unreferenced OsPhysicalCore in case one was found, otherwise null.
     */
    private static OsPhysicalCore getUnreferencedOsPhysicalCore(List<OsPhysicalCore> osCorePhysicalCoreRefs,
                                                                GICList<OsPhysicalCore> physicalCores) {
        OsPhysicalCore result = null
        for (OsPhysicalCore physicalCore in physicalCores) {
            if (!osCorePhysicalCoreRefs.contains(physicalCore)) {
                result = physicalCore
                break
            }
        }
        return result
    }

    /**
     * Get a list of all OsPhysicalCores which are referenced within the OsCores.
     * @param osModule instance of the Os BSW module.
     * @return list of OsPhysicalCores referenced in the OsCore. Can be empty in case no OsPhysicalCore is referenced.
     */
    private static List<OsPhysicalCore> getReferencedOsPhysicalCores(Os osModule) {
        ArrayList<OsCorePhysicalCoreRef> osPhysicalCoreRefs = osModule.osCore.osCorePhysicalCoreRefOrCreate
        List<OsPhysicalCore> result = []
        for (OsCorePhysicalCoreRef ref in osPhysicalCoreRefs) {
            if (ref.hasRefTarget()) {
                result.add(ref.refTarget)
            }
        }
        return result
    }

    /**
     * Get a list of all OsIsrs which are referenced by the OsApplication.
     * @return list of all referenced OsIrs from the OsApplication.
     */
    private static List<OsIsr> getUnreferencedOsIsrs() {
        activeProject {
            Os osModule = bswmdModel(Os.DefRef).single
            List<OsIsr> unreferencedIsrs = new ArrayList<OsIsr>()
            osModule.osIsr.each { isr ->
                if (isr.referencesOfOsAppIsrRefPointingToMe.isEmpty()) {
                    unreferencedIsrs.add(isr)
                }
            }
            return unreferencedIsrs
        }
    }

    /**
     * Process the list of SystemApplications and set the Memory Protection Identifier to 0.
     * Set the ServiceProtection to true.
     * @param model to access and iterate the amount of cores.
     * @param logger instance of the OcsLogger
     */
    private static configureMemoryProtectionForSystemApplication(RtsDataModel model, int coresToProcess) {
        activeProject {
            transaction {
                Os osModule = bswmdModel(Os.DefRef).single
                List<RtsCore> coreModel = model.getCores()
                OsApplication osSystemApplication
                coreModel.eachWithIndex { core, index ->
                    if ((index < coresToProcess)) {
                        osSystemApplication = RuntimeSystemOsConfig.getOsApplicationByName(osModule, "SystemApplication_$core.name")
                        osSystemApplication.osAppMemoryProtectionIdentifierOrCreate.setValueMdf(1)
                    }
                }
                osModule.osOS.osServiceProtectionOrCreate.setValueMdf(true)
            }
        }
    }
}
