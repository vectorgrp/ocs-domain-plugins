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
/*!        \file  RuntimeSystemModelMerge.groovy
 *        \brief  Consists of functions that ensure the correct merge of the internal data model and the JSON model with
 *                possible user input into one complete instance of RtsDataModel, that can be used in the business logic.
 *
 *      \details  This class includes:
 *                  -
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.runtimesystem

import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.automation.model.ecuc.microsar.os.Os
import com.vector.cfg.automation.model.ecuc.microsar.os.ospublishedinformation.osderivativeinformation.osphysicalcore.OsPhysicalCore
import com.vector.cfg.gen.core.bswmdmodel.GICList
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.PluginsCommon
import com.vector.ocs.plugins.runtimesystem.creators.RtsApplicationCreator
import com.vector.ocs.plugins.runtimesystem.creators.RtsCoreCreator

import com.vector.ocs.plugins.runtimesystem.creators.RtsTaskCreator
import com.vector.ocs.plugins.runtimesystem.dataclasses.RtsApplication
import com.vector.ocs.plugins.runtimesystem.dataclasses.RtsCore
import com.vector.ocs.plugins.runtimesystem.dataclasses.RtsDataModel
import com.vector.ocs.plugins.runtimesystem.dataclasses.RtsTask

/**
 * This class represents the merging functions needed for the bundling of the two data models.
 */
class RuntimeSystemModelMerge {

    /**
     * Merge the internal data model and the json model into one consistent data model of type RtsDataModel ready
     * for handing over to the configuration logic.
     * @param jsonModel JSON data model filled with user input.
     * @param list of integers representing the physical CoreIDs.
     * @param logger instance for OCS specific logging.
     * @return final RtsDataModel containing the necessary information needed during the Rts implementation.
     */
    static RtsDataModel mergeModels(RuntimeSystemModel jsonModel, List<Integer> physicalCoreList, OcsLogger logger) {
        /* Handle Cores, Applications and Tasks */
        List<RtsCore> usedCores = mergeCores(jsonModel, physicalCoreList, logger)

        /* Handle OsHooks, SC, cleanupPhase enabling */
        RtsOsHooks hooks = jsonModel.hooks ?: RuntimeSystemConstants.rtsOsHooks

        RtsOsScalabilityClass scalabilityClass = jsonModel.osScalabilityClass ?: RuntimeSystemConstants.rtsOsScalabilityClass

        Boolean enableCleanupPhase = jsonModel.enableCleanupPhase ?: RuntimeSystemConstants.rtsEnableCleanupPhase

        /* Handle Mappings */
        TaskMappingModel taskMapping = jsonModel.runnable2TaskMapping

        InterruptMappingModel interruptMapping = jsonModel.interruptMapping


        /* Initialize the resulting model with all the above prepared properties resulting in a ready-to-use model */
        RtsDataModel resultingDataModel = new RtsDataModel(usedCores, scalabilityClass, hooks, taskMapping, interruptMapping, enableCleanupPhase)

        return resultingDataModel
    }

    /**
     * Merge the cores of  the internal data model and the json model together and make possible necessary adjustments to properties.
     * If at least one core is defined within the json model, all cores from the internal data model are neglected
     * @param jsonModel JSON data model filled with user input.
     * @param list of integers representing the physical CoreIDs.
     * @param logger instance for OCS specific logging.
     * @return list of RtsCore instances that can be merged into the used RtsDataModel.
     */
    static List<RtsCore> mergeCores(RuntimeSystemModel jsonModel, List<Integer> physicalCoreList, OcsLogger logger) {
        List<RtsCore> usedCores = new ArrayList<>()
        if (!jsonModel.cores.isEmpty()) {
            // JSON model contains core descriptions, use them
            jsonModel.cores.each { currentCore ->
                Boolean tempIsAutostartCore

                /* Handle general core properties */
                if (currentCore.masterCore && !currentCore.autostartCore) {
                    tempIsAutostartCore = true
                    logger.warn("Changed property isAutostartCore from Core $currentCore.name to true, because it is the master Core!")
                } else {
                    tempIsAutostartCore = currentCore.autostartCore
                }

                /* Handle core applications */
                List<RtsApplication> usedApplications = mergeApplications(currentCore)

                /* Transform JSON Core and initialize new Rts Core with the intended values - add it to core list */
                RtsCore rtsCore = transformRtsCore(currentCore, usedApplications)
                rtsCore.isAutostartCore = tempIsAutostartCore
                usedCores.add(rtsCore)
            }
        } else {
            /* JSON model does not contain information regarding cores, use the default model cores, applications and tasks */
            if (physicalCoreList != null) {
                if (physicalCoreList.size() >= 1) {
                    physicalCoreList.eachWithIndex { id, index ->
                        RtsCore core = RtsCoreCreator.initializeCore(id, index, RuntimeSystemConstants.OSCORE_NAME)
                        /* Add the core with initialized properties to the data model */
                        usedCores.add(core)
                    }
                } else {
                    logger.error("No physical cores were found in the OsDerivativeInformation, therefore no OsCores will be created.")
                }
            } else {
                logger.error("Physical cores container does not exist, therefore no OsCores will be created.")
            }

        }

        return usedCores
    }

    /**
     * Check if the current core contains a valid set of applications and handle the tasks within it.
     * Otherwise create a default set of applications with default tasks for the current core.
     * @param currentCore Core for which the applications shall be handled.
     * @return list of RtsApplication instances that can be merged into the used cores.
     */
    static List<RtsApplication> mergeApplications(CoreModel currentCore) {
        RtsCore currentRtsCore = new RtsCore(currentCore.physicalCore, currentCore.name)
        List<RtsApplication> usedApplications = new ArrayList<>()
        if (currentCore.applications == null) {
            /* Null value in applications list for current core in JSON model present
             * meaning the default applications shall be created */
            RuntimeSystemConstants.applicationMap.each { Asil asil, List<RuntimeSystemConstants.Companion.TaskType> taskTypeList ->
                usedApplications.add(RtsApplicationCreator.setupApplicationForCore(asil, taskTypeList, currentRtsCore))
            }
        } else if (!currentCore.applications.isEmpty()) {
            /* use applications within JSON model */
            currentCore.applications.each { ApplicationModel jsonApplication ->
                /* Handle application tasks */
                if (jsonApplication.tasks == null) {
                    /* Null value in tasks list for current application in JSON model present
                     * Create a default set of tasks for an default application, Rts Core is used for naming purposes of the application only */
                    RtsApplication localApplication = transformRtsApplication(jsonApplication)
                    /* Initialize default tasks */
                    RuntimeSystemConstants.defaultTaskList.each {
                        localApplication.addTask(RtsTaskCreator.initializeTask(it, localApplication.name))
                    }
                    usedApplications.add(localApplication)
                } else {
                    /* use tasks given in application
                     * even if the task list might be empty - then user doesn't want tasks within the application */
                    usedApplications.add(transformRtsApplication(jsonApplication))
                }
            }
        }
        return usedApplications
    }

    /**
     * Get the physical cores from the OsPublishedInformation to represent the correct core IDs.
     * @param logger instance of the OCS logger.
     * @return list of integers representing the numbering of the physical cores.
     */
    static List<Integer> getPhysicalCoreList(OcsLogger logger) {
        ScriptApi.activeProject() {
            ScriptApi.scriptCode {
                if (PluginsCommon.ConfigPresent(RuntimeSystemConstants.OS_DEFREF)) {
                    Os osModule = bswmdModel(Os.DefRef).single
                    List<Integer> physicalCoreList = new ArrayList<Integer>()
                    GICList<OsPhysicalCore> osPhysicalCores = RuntimeSystemOsConfig.getOsPhysicalCoresOfFirstOsDerivativeInformation(osModule, logger)
                    osPhysicalCores.each { physicalCore ->
                        physicalCoreList.add(physicalCore.osPhysicalCoreId.valueMdf as Integer)
                    }
                    return physicalCoreList
                } else {
                    logger.warn("Cannot access the OsModule.")
                    return null
                }
            }
        }
    }

    /* TRANSFORMERS */
    /**
     * Transform a core from the JSON model into a usable instance of RtsCore.
     * @param jsonCore Core instance of type CoreModel that shall be changed into an instance of RtsCore.
     * @return transformed Core as an instance of RtsCore.
     */
    static RtsCore transformRtsCore(CoreModel jsonCore) {
        List<RtsApplication> usedApplications = new ArrayList<>()
        jsonCore.applications.each {
            usedApplications.add(transformRtsApplication(it))
        }
        RtsCore transformedCore = new RtsCore(jsonCore.physicalCore, jsonCore.name, jsonCore.autostartCore, jsonCore.autosarCore, jsonCore.masterCore, usedApplications)
        return transformedCore
    }

    /**
     * Transform a core from the JSON model into a usable instance of RtsCore.
     * @param jsonCore Core instance of type CoreModel that shall be changed into an instance of RtsCore.
     * @param applicationList list of applications that shall be part of the core that will be transformed.
     * @return transformed Core as an instance of RtsCore.
     */
    static RtsCore transformRtsCore(CoreModel jsonCore, List<RtsApplication> applicationList) {
        RtsCore transformedCore = new RtsCore(jsonCore.physicalCore, jsonCore.name, jsonCore.autostartCore, jsonCore.autosarCore, jsonCore.masterCore, applicationList)
        return transformedCore
    }

    /**
     * Transform an application from the JSON model into a usable instance of RtsApplication.
     * @param jsonApplication application instance of type ApplicationModel that shall be changed into an instance of RtsApplication.
     * @return transformed Application as an instance of RtsApplication.
     */
    static RtsApplication transformRtsApplication(ApplicationModel jsonApplication) {
        List<RtsTask> usedTasks = new ArrayList<>()
        if (jsonApplication.tasks != null) {
            jsonApplication.tasks.each {
                usedTasks.add(transformRtsTask(it))
            }
        }
        RtsApplication transformedApplication = new RtsApplication(jsonApplication.name, jsonApplication.asilLevel, usedTasks)
        return transformedApplication
    }

    /**
     * Transform a task from the JSON model into a usable instance of RtsTask.
     * @param jsonTask task instance of type TaskModel that shall be changed into an instance of RtsTask.
     * @return transformed Task as an instance of RtsTask.
     */
    static RtsTask transformRtsTask(TaskModel jsonTask) {
        RtsTask transformedTask = new RtsTask(jsonTask.name, jsonTask.priority, jsonTask.stackSize, jsonTask.schedule, jsonTask.type, jsonTask.autostart)
        return transformedTask
    }
}
