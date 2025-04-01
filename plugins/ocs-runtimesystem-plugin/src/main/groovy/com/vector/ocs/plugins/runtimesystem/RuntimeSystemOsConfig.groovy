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
/*!        \file  RuntimeSystemScriptOsConfig.groovy
 *        \brief  Handle the Os specific configuration aspects of the RuntimeSystem.
 *
 *      \details  Create the following configuration elements:
 *                - OsCore
 *                - OsCounter
 *                - OsApplication
 *                - OsTask
 *                - OsResource
 *                - OsMemoryProtection
 *                Apply common Os configuration parameters, e.g. RtsOsHooks and ScalabilityClass
 *
 *                Current concept for ScalabilityClass <-> ASIL level handling:
 *                SC1 / SC2: - for OsApplications no OsAppMemoryProtectionIdentifier will be configured
 *                ========== - OsTrusted will be set to true
 *                           - highest ASIL is assumed to be QM
 *                SC3 / SC4: - higher ASIL than QM is assumed
 *                ========== - only the OsApplication with the highest ASIL will be trusted, make user aware that he
 *                             must adapt this depending on project needs
 *                           - OsAppMemoryProtectionIdentifier will be set to 0, make user aware that he must adapt
 *                             this depending on project needs, using the same identifier multiple times results in an
 *                             error
 *                           - OsMemoryProtectionHook should be enabled by the customer
 *                           - OsMemoryProtection container will be added, if not yet available
 *                           - //ToDo SC: by default for each OsCore a SystemApplication_CoreName will be created as soon
 *                                as the OsPhysicalCore gets referenced. This System Application is not taken into
 *                                account in the SC handling.
 *********************************************************************************************************************/
package com.vector.ocs.plugins.runtimesystem

import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.automation.model.ecuc.microsar.ecuc.EcuC
import com.vector.cfg.automation.model.ecuc.microsar.os.Os
import com.vector.cfg.automation.model.ecuc.microsar.os.osapplication.OsApplication
import com.vector.cfg.automation.model.ecuc.microsar.os.osappmode.OsAppMode
import com.vector.cfg.automation.model.ecuc.microsar.os.oscore.OsCore
import com.vector.cfg.automation.model.ecuc.microsar.os.oscounter.OsCounter
import com.vector.cfg.automation.model.ecuc.microsar.os.oscounter.oscountertype.OsCounterType
import com.vector.cfg.automation.model.ecuc.microsar.os.osisr.OsIsr
import com.vector.cfg.automation.model.ecuc.microsar.os.osmemoryprotection.OsMemoryProtection
import com.vector.cfg.automation.model.ecuc.microsar.os.osos.oshooks.OsHooks
import com.vector.cfg.automation.model.ecuc.microsar.os.osos.osscalabilityclass.OsScalabilityClass
import com.vector.cfg.automation.model.ecuc.microsar.os.ospublishedinformation.osderivativeinformation.OsDerivativeInformation
import com.vector.cfg.automation.model.ecuc.microsar.os.ospublishedinformation.osderivativeinformation.osphysicalcore.OsPhysicalCore
import com.vector.cfg.automation.model.ecuc.microsar.os.osresource.OsResource
import com.vector.cfg.automation.model.ecuc.microsar.os.ostask.OsTask
import com.vector.cfg.automation.model.ecuc.microsar.os.ostask.ostaskautostart.OsTaskAutostart
import com.vector.cfg.automation.model.ecuc.microsar.os.ostask.ostaskautostart.ostaskappmoderef.OsTaskAppModeRef
import com.vector.cfg.automation.model.ecuc.microsar.os.ostask.ostaskschedule.OsTaskSchedule
import com.vector.cfg.automation.model.ecuc.microsar.os.ostask.ostasktype.OsTaskType
import com.vector.cfg.gen.core.bswmdmodel.GICList
import com.vector.cfg.gen.core.bswmdmodel.GIPList
import com.vector.cfg.gen.core.bswmdmodel.param.GBoolean
import com.vector.cfg.gen.core.bswmdmodel.param.GFloat
import com.vector.cfg.gen.core.bswmdmodel.param.GInteger
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.PluginsCommon
import com.vector.ocs.plugins.runtimesystem.dataclasses.RtsApplication
import com.vector.ocs.plugins.runtimesystem.dataclasses.RtsCore
import com.vector.ocs.plugins.runtimesystem.dataclasses.RtsDataModel
import com.vector.ocs.plugins.runtimesystem.dataclasses.RtsTask
import groovy.transform.PackageScope


/**
 * Handle the Os specific configuration aspects of the RuntimeSystem.
 */
@PackageScope
class RuntimeSystemOsConfig {

    /**
     * Process the RuntimeSystemModel and created the corresponding Os BSW module configuration.<br>
     * Key configuration elements which will be added during processing:
     * <ul>
     *     <li>OsCore</li>
     *     <li>OsCounter</li>
     *     <li>OsApplication</li>
     *     <li>OsTask</li>
     *     <li>OsResource</li>
     *     <li>OsMemoryProtection<li>
     * </ul>
     * OsMemoryProtection will be only created for SC3/SC4.
     * @param model RuntimeSystemModel which contains the specified configuration settings.
     * @param logger instance of the OcsLogger.
     */
    static Boolean processOsConfiguration(RtsDataModel model, OcsLogger logger) {
        logger.info("Creating Os specific configuration elements...")
        ScriptApi.activeProject() {
            Boolean isCoreCountValid = true
            Os osModule = it.bswmdModel(Os.DefRef).single
            EcuC ecucModule = it.bswmdModel(EcuC.DefRef).single
            final int coresToProcess = checkOsPhysicalCoreConfiguration(osModule, model.getCores().size(), logger)
            if (coresToProcess == 0) {
                isCoreCountValid = false
                return isCoreCountValid
            }
            final List<RtsCore> coreModel = model.getCores()
            Asil highestAsilCore = null
            Asil highestAsilConfiguration = null

            transaction {
                // check if the RtsPlugin was already executed once -> true: the prepareOsModule method will not be executed
                String ecuCPartitionName = RuntimeSystemEcucConfig.getEcucPartitionName(coreModel.first.applications.first.name)
                if (!ecucModule.ecucPartitionCollectionOrCreate.ecucPartition.exists(ecuCPartitionName)) {
                    prepareOsModule(osModule)
                }

                coreModel.eachWithIndex { core, index ->
                    if ((index < coresToProcess)) {
                        // 1. Create the OsCore(s) if none already exists
                        createOsCore(osModule, core, logger)

                        // 2. Create the reference between the OsCore <-> OsPhysicalCore
                        // this is necessary for trigger the automatic creation of SystemApplication_core.name
                        OsCore osCore = getOsCoreByName(osModule, core.name)
                        RuntimeSystemConnectAndMap.createOsCorePhysicalCoreRef(osModule, osCore, logger)
                    }
                }
            }
            // new transaction needs to be opened as otherwise the SystemApplication_core.name would not be accessible
            transaction{
                coreModel.eachWithIndex { core, index ->
                    if ((index < coresToProcess)) {

                        //get the highest asil level of the applications of the current iterated core
                        highestAsilCore = getHighestAsilLevel(core.applications)
                        //check if the highestAsilCore is the highest asil level of the configuration
                        highestAsilConfiguration = updateHighestAsilConfiguration(highestAsilConfiguration, highestAsilCore)

                        // 3. Create the OsApplication(s) for the previously created OsCore
                        core.applications.each { RtsApplication application ->
                                createOsApplication(osModule, application, model.scalabilityClass, highestAsilCore, logger)


                            // 4. Create the OsTask(s) for the previously created OsApplication
                            application.tasks.each { RtsTask task ->
                                createOsTask(osModule, task, logger)
                            }
                        }

                        // 5. Create the SystemTimer for the current iterated core
                        String createdOsCounter = createOsCounter(osModule, core, RuntimeSystemConstants.secondsPerTick, RuntimeSystemConstants.maxAllowedValue, logger)

                        // 6. Create the OsIsr for the created SystemTimer.
                        createOsCounterIsr(osModule, createdOsCounter, RuntimeSystemConstants.isrPriority, logger)
                    }
                }
                checkScalabilityClass(highestAsilConfiguration, model.scalabilityClass, model.getOsHooks().protectionHook, logger)

                // 7. Set the scalability class in the OsOs Container
                setScalabilityClass(osModule, model.scalabilityClass, logger)

                // 8. Create the corresponding OsResource.
                String resourceName = getOsResourceName()
                createOsResource(osModule, resourceName, logger)

                // 9. Set the RtsOsHooks
                setOsHooks(osModule, model.getOsHooks(), logger)

                // 10. Create the OsMemoryProtection in case of memory protection
                if (model.scalabilityClass > RtsOsScalabilityClass.SC2) {
                    createOsMemoryProtection(osModule, logger)
                }
                return isCoreCountValid
            }
        }
    }


    /**
     * Perform some basic consistency checks for the RuntimeSystemMode and the existing project.
     * @param osModule instance of the Os BSW module taken into account for the checks.
     * @param numberOfCores number of cores which should be configured.
     * @param logger instance of the OcsLogger.
     * @return number of OsPhysical cores - in the case the number of cores in the model is bigger than the number of
     * physical core, the return value will be 0 and the processing will be stopped in the rest of the RTS.
     */
    static int checkOsPhysicalCoreConfiguration(Os osModule, Integer numberOfCores, OcsLogger logger) {
        GICList<OsPhysicalCore> osPhysicalCores = getOsPhysicalCoresOfFirstOsDerivativeInformation(osModule, logger)
        return checkNumberOfOsCores(osPhysicalCores.size(), numberOfCores, logger)
    }

    /**
     * Check that the number of cores that should be configured does not exceed the number of OsPhysicalCores.
     * @param numberOfPhysicalCores number of available OsPhysicalCores.
     * @param numberOfCores number of cores in the RuntimeSystemModel.
     * @param logger instance of the OcsLogger.
     * @return number of OsPhysicalCores in case it is smaller than numberOfCores, else numberOfCores - 0 in case the
     * processing of the RTS should be stopped because of a mismatch between modelled cores and physical cores
     */
    private static Integer checkNumberOfOsCores(int numberOfPhysicalCores, Integer numberOfCores, OcsLogger logger) {
        logger.info("Check number of cores from the model and OsPhysicalCores.")
        Integer result = numberOfCores
        if (numberOfPhysicalCores > numberOfCores) {
            logger.warn("Number of cores from the model ($numberOfCores) is " +
                    "smaller than OsPhysicalCores ($numberOfPhysicalCores). This could lead to an inconsistant " +
                    "configuration.")
        } else if (numberOfCores > numberOfPhysicalCores) {
            logger.error("Number of cores from the model ($numberOfCores) is " +
                    "bigger than OsPhysicalCores ($numberOfPhysicalCores). Limit number of cores to " +
                    "$numberOfPhysicalCores.")
            result = 0
        }
        return result
    }

    /**
     * Process the list of all applications configured for the default core and provide the highest Asil level.
     * @param applications which contains the list of application of the core.
     * @return highest Asil level of the applications.
     */
    static Asil getHighestAsilLevel(List<RtsApplication> applications) {
        ArrayList<Asil> asilLevels = applications.asilLevel
        return Asil.QM.getMaxAsilLevel(asilLevels)
    }


    /**
     * Updates the highest ASIL configuration based on the comparison between the current highest ASIL level
     * and a new ASIL level.
     *
     * @param currentHighest is the current highest ASIL level.
     * @param newAsil is the new ASIL level to compare with the current highest ASIL level.
     * @return the updated highest ASIL level configuration.
     */
    private static Asil updateHighestAsilConfiguration(Asil currentHighest, Asil newAsil) {
        if ((currentHighest == null) || (newAsil > currentHighest)) {
            return newAsil
        } else {
            return currentHighest
        }
    }

    /**
     * Check if the provided ScalabilityClass in the model fits to the highest Asil level configured for the
     * applications and the Os protection hook. This check does not yet has any impact on the processing.
     * @param highestAsil highest Asil level of the configured applications of the configuration.
     * @param scalabilityClass ScalabilityClass provided in the RuntimeSystemModel.
     * @param protectionHook is indicating if the OsProtectionHook should be en-/disabled.
     * @param logger instance of the OcsLogger.
     */
    private static void checkScalabilityClass(Asil highestAsil, RtsOsScalabilityClass scalabilityClass,
                                              boolean protectionHook, OcsLogger logger) {
        logger.info("Check the Scalability Class based on highest application Asil " +
                "level and Os protection hook.")
        // Justify Fallthrough warning because it is implemented intentionally this way
        //noinspection GroovyFallthrough
        switch (scalabilityClass) {
            case RtsOsScalabilityClass.SC1:
            case RtsOsScalabilityClass.SC2:
                // Highest Asil Level identified - if it is QM, then SC1 or SC2 are sufficient,
                // otherwise print an error but proceed with execution of plugin since this is no show-stopper.
                if (highestAsil > Asil.QM) {
                    logger.error("Scalability class (${scalabilityClass.toString()}) " +
                            "does not fit to the highest Asil requirements of Os. Check your configuration for " +
                            "consistency. Continue processing...")
                }
                break
            case RtsOsScalabilityClass.SC3:
            case RtsOsScalabilityClass.SC4:
                if (!protectionHook) {
                    logger.warn("For scalability class (${scalabilityClass.toString()}) " +
                            "the OsProtectionHook should be enabled.")
                }
                break
        }
    }

    /**
     * Prepare the OsModule and delete the already available OsCore0, SystemTimer, SystemApplication_OsCore0,
     * CounterIsr_SystemTimer_OsCore0, XSignalIsr_OsCore0 and IdleTask_OsCore0 to avoid duplicates.
     * @param osModule instance of the Os BSW module.
     */
    private static void prepareOsModule(Os osModule) {
        osModule.osCore.byNameOrCreate("OsCore0").moRemove()
        osModule.osCounter.byNameOrCreate("SystemTimer").moRemove()
        osModule.osApplication.byNameOrCreate("SystemApplication_OsCore0").moRemove()
        osModule.osIsr.byNameOrCreate("CounterIsr_SystemTimer_OsCore0").moRemove()
        osModule.osIsr.byNameOrCreate("CounterIsr_SystemTimer").moRemove()
        osModule.osTask.byNameOrCreate("IdleTask_OsCore0").moRemove()
    }

    /**
     * Create an OsCore and set the basic parameters based on the RuntimeSystemMode -> CoreModel. In case the OsCore
     * already exists based on the name it will be used for the processing.
     * @param osModule instance of the Os BSW module.
     * @param core instance of the RTSCore Data class.
     * @param logger instance of the OcsLogger.
     */
    private static void createOsCore(Os osModule, RtsCore core, OcsLogger logger) {
        logger.info("Create OsCore with name $core.name.")
        OsCore createdOsCore = osModule.osCore.byNameOrCreate(core.name)
        // Set the master core as autostart core otherwise take the model parameter.
        Boolean isAutostartCore = core.isMasterCore == true ? true : core.isAutostartCore
        GBoolean isAutoStartParameter = createdOsCore.osCoreIsAutostartOrCreate
        if (PluginsCommon.isChangeable(isAutoStartParameter.mdfObject, logger)) {
            isAutoStartParameter.setValue(isAutostartCore)
        }
        GBoolean isAutosarParameter = createdOsCore.osCoreIsAutosarOrCreate
        if (PluginsCommon.isChangeable(isAutosarParameter.mdfObject, logger)) {
            isAutosarParameter.setValue(core.isAutosarCore)
        }
    }

    /**
     * Create an OsCounter and set the basic parameters based on the RuntimeSystemMode -> SystemTimerModel. In case the
     * OsCore already exists based on the name it will be used for the processing.
     * @param osModule instance of the Os BSW module.
     * @param core for the information about the current core to process
     * @param secondsPerTick value of the seconds per tick of the OsCounter.
     * @param MaxAllowedValue maximum allowed value of the system counter in ticks.
     * @param logger instance of the OcsLogger.
     */
    private static createOsCounter(Os osModule, RtsCore core, Double secondsPerTick, Long maxAllowedValue, OcsLogger logger) {
        OsCounter createdOsCounter

        createdOsCounter = osModule.osCounter.byNameOrCreate("SystemTimer_$core.name")
        logger.info("Create OsCounter with name SystemTimer_$core.name.")

        GFloat secondsPerTickParameter = createdOsCounter.osSecondsPerTickOrCreate
        if (PluginsCommon.isChangeable(secondsPerTickParameter.mdfObject, logger)) {
            secondsPerTickParameter.setValue(secondsPerTick)
        }

        OsCounterType counterTypeParameter = createdOsCounter.osCounterTypeOrCreate
        if (PluginsCommon.isChangeable(counterTypeParameter.mdfObject, logger)) {
            counterTypeParameter.setValueMdf("HARDWARE")
        }

        GInteger counterMaxAllowedValue = createdOsCounter.osCounterMaxAllowedValueOrCreate
        if (PluginsCommon.isChangeable(counterMaxAllowedValue.mdfObject, logger)) {
            counterMaxAllowedValue.setValue(maxAllowedValue)
        }

        return createdOsCounter.shortname
    }

    /**
     * Create an OsIsr for the corresponding SystemTimer
     * @param osModule instance of the Os BSW module.
     * @param core for the information about the current core to process
     * @param createdOsCounter for the information for the naming schema
     * @param isrPriority for the value of the Interrupt service routine priority
     * @param logger instance of the OcsLogger.
     */
    private static void createOsCounterIsr(Os osModule, String createdOsCounter, int isrPriority, OcsLogger logger) {

        if (!osModule.osIsr.exists("CounterIsr_$createdOsCounter")) {
            OsIsr createdOsIsr = osModule.osIsr.createAndAdd("CounterIsr_$createdOsCounter")
            logger.info("Create CounterIsr with name CounterIsr_$createdOsCounter.")
            createdOsIsr.getOsIsrInitialEnableInterruptSourceOrCreate().setValueMdf(false)
            createdOsIsr.getOsIsrInterruptPriority().setValueMdf(isrPriority)
        }
    }

    /**
     * Create an OsApplication and set the basic parameters based on the RuntimeSystemModel -> ApplicationModel.
     * In case the OsApplication already exists based on the name it will be used for the processing.
     * @param osModule instance of the Os BSW module.
     * @param application instance of the RTSApplication data class.
     * @param scalabilityClass RuntimeSystemModel scalability class value
     * @param highestAsil highest Asil level of all available applications
     * @param logger instance of the OcsLogger
     */
    private static void createOsApplication(Os osModule, RtsApplication application,
                                            RtsOsScalabilityClass scalabilityClass, Asil highestAsil, OcsLogger logger) {
        logger.info("Create OsApplication with name $application.name.")
        OsApplication createdOsApp = osModule.osApplication.byNameOrCreate(application.name)
        // Trusted setting for the highest Asil Application
        Boolean isTrusted
        // Justify Fallthrough warning because it is implemented intentionally this way
        //noinspection GroovyFallthrough
        switch (scalabilityClass) {
            case RtsOsScalabilityClass.SC1:
            case RtsOsScalabilityClass.SC2:
                isTrusted = true
                break
            case RtsOsScalabilityClass.SC3:
            case RtsOsScalabilityClass.SC4:
                // true for highest Asil otherwise false
                isTrusted = application.asilLevel == highestAsil
                break
        }
        GBoolean trustedParameter = createdOsApp.osTrustedOrCreate
        if (PluginsCommon.isChangeable(trustedParameter.mdfObject, logger)) {
            trustedParameter.setValue(isTrusted)
        }
        // Set the memory protection identifier for SC3 and SC4 only.
        // Make the customer aware that he has to adjust it depending on the project needs.
        GInteger memProtectionIdentifierParameter = createdOsApp.osAppMemoryProtectionIdentifierOrCreate
        if (PluginsCommon.isChangeable(memProtectionIdentifierParameter.mdfObject, logger) &&
                (scalabilityClass > RtsOsScalabilityClass.SC2)) {
            memProtectionIdentifierParameter.setValue(0)
            logger.warn("OsAppMemoryProtectionIdentifier of ${application.name} " +
                    "must be adapted depending on the project needs.")
        }
    }

    /**
     * Create an OsTask and set the basic parameters based on the RuntimeSystemMode -> TaskModel.
     * In case the OsTask already exists based on the name it will be used for the processing.
     * For autostart applications the OsAppMode container and the corresponding reference will be created too.
     * @param osModule instance of the Os BSW module.
     * @param task instance of the RTSTask Data class.
     * @param logger instance of the OcsLogger.
     */
    private static void createOsTask(Os osModule, RtsTask task, OcsLogger logger) {
        logger.info("Create OsTask with name $task.name.")
        OsTask createdOsTask = osModule.osTask.byNameOrCreate(task.name)
        GInteger priorityParameter = createdOsTask.osTaskPriorityOrCreate
        if (PluginsCommon.isChangeable(priorityParameter.mdfObject, logger)) {
            priorityParameter.setValue(task.priority)
        }
        GInteger stackSizeParameter = createdOsTask.osTaskStackSizeOrCreate
        if (PluginsCommon.isChangeable(stackSizeParameter.mdfObject, logger)) {
            stackSizeParameter.setValue(task.stackSize)
        }
        OsTaskSchedule scheduleParameter = createdOsTask.osTaskScheduleOrCreate
        if (PluginsCommon.isChangeable(scheduleParameter.mdfObject, logger)) {
            scheduleParameter.setValueMdf(task.schedule.toString())
        }
        OsTaskType typeParameter = createdOsTask.osTaskTypeOrCreate
        if (PluginsCommon.isChangeable(typeParameter.mdfObject, logger)) {
            typeParameter.setValueMdf(task.type.toString())
        }
        // Create OsTaskAutostart container - if configured,
        // set the setting and create (if not present) OSDEFAULTAPPMODE to reference at Autostart.
        if (task.isAutostart) {
            OsTaskAutostart autostart = createdOsTask.osTaskAutostartOrCreate
            // Assumption is that it is allowed to modify the list of parameters and add a new entry,
            // therefore the PluginsCommon.isChangeable check is not done.
            OsAppMode appMode = osModule.osAppMode.byNameOrCreate("OSDEFAULTAPPMODE")
            GIPList<OsTaskAppModeRef> taskAppModeRefParameters = autostart.osTaskAppModeRef
            // Check if the OsAppMode is already referenced.
            if (!taskAppModeRefParameters.refTarget.contains(appMode)) {
                autostart.osTaskAppModeRef.createAndAdd().setRefTarget(appMode)
            }
        }
    }

    /**
     * Set the scalability class within the OsOs container.
     * @param osModule instance of the Os BSW module.
     * @param scalabilityClass scalability class to apply.
     * @param logger instance of the OcsLogger.
     */
    private static void setScalabilityClass(Os osModule, RtsOsScalabilityClass scalabilityClass, OcsLogger logger) {
        logger.info("Set OsScalabilityClass to $scalabilityClass.")
        OsScalabilityClass scalabilityClassParameter = osModule.osOSOrCreate.osScalabilityClassOrCreate
        if (PluginsCommon.isChangeable(scalabilityClassParameter.mdfObject, logger)) {
            scalabilityClassParameter.setValueMdf(scalabilityClass.toString())
        }
    }

    /**
     * Create an OsResource container. In case the OsResource already exists based on the name it will be used for the
     * processing.
     * @param osModule instance of the Os BSW module.
     * @param resourceName name for the OsResource.
     * @param logger instance of the OcsLogger.
     */
    private static void createOsResource(Os osModule, String resourceName, OcsLogger logger) {
        logger.info("Create OsResource with name $resourceName.")
        osModule.osResource.byNameOrCreate(resourceName)
    }

    /**
     * Set the RtsOsHooks as configured in the RuntimeSystemModel -> RtsOsHooks. The following RtsOsHooks are configured:
     * <ul>
     *     <li>Error Hook</li>
     *     <li>Panic Hook</li>
     *     <li>Post Task Hook</li>
     *     <li>Pre Task Hook</li>
     *     <li>Protection Hook</li>
     *     <li>Shutdown Hook</li>
     *     <li>Startup Hook</li>
     * </ul>
     * @param osModule instance of the Os BSW module.
     * @param modelOsHooks RtsOsHooks of the RuntimeSystemModel.
     * @param logger instance of the OcsLogger.
     */
    // To avoid name conflicts between the RtsOsHooks and the RuntimeSystemModel RtsOsHooks one of both must be imported with
    // absolute path or alternatively an alias for the import could be defined.
    private static void setOsHooks(Os osModule, RtsOsHooks modelOsHooks, OcsLogger logger) {
        OsHooks osHooks = osModule.osOSOrCreate.osHooksOrCreate
        GBoolean errorHook = osHooks.osErrorHookOrCreate
        logger.info("Set Error Hook to $modelOsHooks.errorHook.")
        if (PluginsCommon.isChangeable(errorHook.mdfObject, logger)) {
            errorHook.setValueMdf(modelOsHooks.errorHook)
        }
        GBoolean panicHook = osHooks.osPanicHookOrCreate
        logger.info("Set Panic Hook to $modelOsHooks.panicHook.")
        if (PluginsCommon.isChangeable(panicHook.mdfObject, logger)) {
            panicHook.setValueMdf(modelOsHooks.panicHook)
        }
        GBoolean postTaskHook = osHooks.osPostTaskHookOrCreate
        logger.info("Set Post Task Hook to $modelOsHooks.postTaskHook.")
        if (PluginsCommon.isChangeable(postTaskHook.mdfObject, logger)) {
            postTaskHook.setValueMdf(modelOsHooks.postTaskHook)
        }
        GBoolean preTaskHook = osHooks.osPreTaskHookOrCreate
        logger.info("Set Pre Task Hook to $modelOsHooks.preTaskHook.")
        if (PluginsCommon.isChangeable(preTaskHook.mdfObject, logger)) {
            preTaskHook.setValueMdf(modelOsHooks.preTaskHook)
        }
        GBoolean protectionTaskHook = osHooks.osProtectionHookOrCreate
        logger.info("Set Protection Hook to $modelOsHooks.protectionHook.")
        if (PluginsCommon.isChangeable(protectionTaskHook.mdfObject, logger)) {
            protectionTaskHook.setValueMdf(modelOsHooks.protectionHook)
        }
        GBoolean shutdownTaskHook = osHooks.osShutdownHookOrCreate
        logger.info("Set Shutdown Hook to $modelOsHooks.shutdownHook.")
        if (PluginsCommon.isChangeable(shutdownTaskHook.mdfObject, logger)) {
            shutdownTaskHook.setValueMdf(modelOsHooks.shutdownHook)
        }
        GBoolean startupTaskHook = osHooks.osStartupHookOrCreate
        logger.info("Set Startup Hook to $modelOsHooks.startupHook.")
        if (PluginsCommon.isChangeable(startupTaskHook.mdfObject, logger)) {
            startupTaskHook.setValueMdf(modelOsHooks.startupHook)
        }
    }

    /**
     * Create the OsMemoryProtection container but only in case it does not already exists. Make the customer aware via
     * warning message that the OsMemoryProtection container must be configured depending on the project needs.
     * @param osModule instance of the Os BSW module.
     * @param logger instance of the OcsLogger.
     */
    private static void createOsMemoryProtection(Os osModule, OcsLogger logger) {
        if (!osModule.existsOsMemoryProtection()) {
            OsMemoryProtection memoryProtection = osModule.osMemoryProtectionOrCreate
            if (memoryProtection != null) {
                logger.info("Create OsMemoryProtection container based on SC3/SC4.")
                logger.warn("OsMemoryProtection must be configured depending on " +
                        "the project needs.")
            }
        }
    }

    // Helper methods

    /**
     * Find an OsCore by applying the OsCore name schema.
     * @param osModule instance of Os BSW module.
     * @param coreName name of the OsCare.
     * @return OsCore container instance that matches the created name.
     */
    static OsCore getOsCoreByName(Os osModule, String coreName) {
        return osModule.osCore.byNameOrCreate(coreName)
    }

    /**
     * Find an OsApplication by applying the OsApplication name schema.
     * @param osModule instance of Os BSW module.
     * @param modelAppName name of the OsApplication.
     * @param asilLevel Asil level of the OsApplication.
     * @param coreName name of the OsCore where the OsApplication belong to.
     * @return OsApplication container instance that matches the created name.
     */
    static OsApplication getOsApplicationByName(Os osModule, String modelAppName) {
        return osModule.osApplication.byNameOrCreate(modelAppName)
    }

    /**
     * Find an OsTask by applying the OsTask name schema.
     * @param osModule instance of Os BSW module.
     * @param modelTaskName name of the OsTask.
     * @param applicationName name of the OsApplication where the OsTask belong to.
     * @return OsTask container instance that matches the created name.
     */
    static OsTask getOsTaskByName(Os osModule, String modelTaskName) {
        return osModule.osTask.byNameOrCreate(modelTaskName)
    }

    /**
     * Find an OsResource by applying the OsResource name schema.
     * @param osModule instance of Os BSW module.
     * @return OsResource container instance that matches the created name.
     */
    static OsResource getOsResourceByName(Os osModule) {
        return osModule.osResource.byNameOrCreate(getOsResourceName())
    }

    /**
     * Get the OsPhysicalCores list. In case more than one OsDerivativeInformation was found the first one is taken to
     * access the list of OsPhysicalCores.
     * @param osModule instance of the Os BSW module.
     * @param logger instance of the OcsLogger.
     * @return list of PhysicalCores found in the first OsDerivativeInformation container.
     */
    static GICList<OsPhysicalCore> getOsPhysicalCoresOfFirstOsDerivativeInformation(Os osModule, OcsLogger logger) {
        GICList<OsDerivativeInformation> derivativeInfos = osModule.osPublishedInformation?.osDerivativeInformation
        if (derivativeInfos.size() > 1) {
            logger.warn("Found more than one OsDerivativeInformation. " +
                    "Continue processing with the first one called $derivativeInfos.first.shortname.")
        }
        return derivativeInfos.first.osPhysicalCore
    }

    /**
     * Create the OsResource name based on the name schema.<br>
     * <b>schema:</b> EcuM_Resource
     * @return OsResourceName
     */
    private static String getOsResourceName() {
        return "EcuM_Resource"
    }
}