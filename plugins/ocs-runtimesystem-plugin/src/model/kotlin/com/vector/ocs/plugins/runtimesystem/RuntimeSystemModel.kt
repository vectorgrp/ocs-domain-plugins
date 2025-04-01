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
/*!        \file  RuntimeSystemModel.kt
 *        \brief  Definition of the RuntimeSystemScript model as user interface for the plugin.
 *
 *      \details  -
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.runtimesystem
import com.vector.ocs.core.api.ModelValidationException
import com.vector.ocs.core.api.PluginModel
import com.vector.ocs.json.api.SchemaDescription
import com.vector.ocs.json.api.SchemaIntEnum
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

private const val VERSION = 3

@Serializable
data class RuntimeSystemModel(
    @SchemaDescription("Define the settings for the core definition.")
    @EncodeDefault
    val cores: List<CoreModel> = mutableListOf(),
    @SchemaDescription("Define the scalability class of the Os configuration.")
    @EncodeDefault
    val osScalabilityClass: RtsOsScalabilityClass = RuntimeSystemConstants.rtsOsScalabilityClass,
    @SchemaDescription("Enable or disable the different type of RtsOsHooks.")
    @EncodeDefault
    val hooks: RtsOsHooks? = RtsOsHooks(),
    @SchemaDescription("Define the runnable to task mapping.")
    @EncodeDefault
    val runnable2TaskMapping: TaskMappingModel? = TaskMappingModel(),
    @SchemaDescription("Define interrupt mapping.")
    @EncodeDefault
    val interruptMapping: InterruptMappingModel? = InterruptMappingModel(),
    @SchemaDescription("Enable / disable the execution of the cleanup phase. More details about this phase can be found in the domain plugin documentation.")
    @EncodeDefault
    val enableCleanupPhase: Boolean = RuntimeSystemConstants.rtsEnableCleanupPhase
) : PluginModel {
    @Suppress("unused")
    @Required
    @SchemaIntEnum(VERSION.toLong())
    @SchemaDescription("The version of this model. Must always be '$VERSION'.")
    private val version: Int = VERSION

    init {
        if (cores.isNotEmpty()) {
            //Check if core name and physical core are unique
            //Check if only one core is set to MasterCore
            validateCoreModel(cores)
            //Check if task names are unique
            validateTaskModel(cores)
            //Check if application names are unique
            validateApplicationModel(cores)
        }
    }
}

@Serializable
data class CoreModel(
    @SchemaDescription("Define the name of the core.")
    val name: String,
    @SchemaDescription("Define the physicalCore to be used.")
    val physicalCore: Int,
    @SchemaDescription("Define if the core is the master core.")
    val isMasterCore: Boolean = RuntimeSystemConstants.coreProperties.isMasterCore,
    @SchemaDescription("A list of elements of type ApplicationModel that define all applications to be created for the default cores.")
    val applications: List<ApplicationModel>? = null,
    @SchemaDescription("Define if the core is starting automatically.")
    val isAutostartCore: Boolean = RuntimeSystemConstants.coreProperties.isAutostartCore,
    @SchemaDescription("Define if the core is dedicated to be used by the Rte (and is using Autosar API).")
    val isAutosarCore: Boolean = RuntimeSystemConstants.coreProperties.isAutosarCore
)

@Serializable
data class ApplicationModel(
    @SchemaDescription("Define the Name of the application.")
    val name: String,
    @SchemaDescription("Define the Safety level of the application.")
    val asilLevel: Asil = RuntimeSystemConstants.applicationProperties.asilLevel,
    @SchemaDescription("Define the list of elements of the type TaskModel which defines the Os Tasks that should be created for all default applications.")
    val tasks: List<TaskModel>? = null
)

@Serializable
data class TaskModel(
    @SchemaDescription("Name of the task.")
    val name: String,
    @SchemaDescription("Define the Os priority of the task.")
    val priority: Int,
    @SchemaDescription("Define the stack size of the task.")
    val stackSize: Int = RuntimeSystemConstants.userDefinedTaskProperties.stackSize,
    @SchemaDescription("Define if the task is \t\n" + "interruptible (FULL) or not interruptible (NON).")
    val schedule: RtsOsTaskSchedule = RuntimeSystemConstants.userDefinedTaskProperties.schedule,
    @SchemaDescription("Define the Os task type. Possible values are BASIC, EXTENDED or AUTO.")
    val type: RtsOsTaskType = RuntimeSystemConstants.userDefinedTaskProperties.type,
    @SchemaDescription("Define if the task is starting automatically on Os startup. Normally all tasks are started by the Rte, only the Init Task is started automatically.")
    val autostart: Boolean = RuntimeSystemConstants.userDefinedTaskProperties.autostart
)

@Serializable
data class RtsOsHooks(
    @SchemaDescription("OsErrorHook will be created. Default implementation can be found in Os_Callout_Stubs.c.")
    @EncodeDefault
    val errorHook: Boolean = RuntimeSystemConstants.rtsOsHooks.errorHook,
    @SchemaDescription("OsPanicHook will be created. Default implementation can be found in Os_Callout_Stubs.c.")
    @EncodeDefault
    val panicHook: Boolean = RuntimeSystemConstants.rtsOsHooks.panicHook,
    @SchemaDescription("OsPostTaskHook will be created. Default implementation can be found in Os_Callout_Stubs.c.")
    @EncodeDefault
    val postTaskHook: Boolean = RuntimeSystemConstants.rtsOsHooks.postTaskHook,
    @SchemaDescription("OsPreTaskHook will be created. Default implementation can be found in Os_Callout_Stubs.c.")
    @EncodeDefault
    val preTaskHook: Boolean = RuntimeSystemConstants.rtsOsHooks.preTaskHook,
    @SchemaDescription("OsProtectionHook will be created. Default implementation can be found in Os_Callout_Stubs.c.")
    @EncodeDefault
    val protectionHook: Boolean = RuntimeSystemConstants.rtsOsHooks.protectionHook,
    @SchemaDescription("OsShutdownHook will be created. Default implementation can be found in Os_Callout_Stubs.c.")
    @EncodeDefault
    val shutdownHook: Boolean = RuntimeSystemConstants.rtsOsHooks.shutdownHook,
    @SchemaDescription("OsStartupHook will be created. Default implementation can be found in Os_Callout_Stubs.c.")
    @EncodeDefault
    val startupHook: Boolean = RuntimeSystemConstants.rtsOsHooks.startupHook,
)

@Serializable
data class UserDefinedTaskMapping(
    @SchemaDescription("Name of the runnable.")
    @EncodeDefault
    val runnable: String,
    @SchemaDescription("Name of the task.")
    @EncodeDefault
    val task: String
)

@Serializable
data class TaskMappingModel(
    @SchemaDescription("Map specific runnables to specific tasks as defined by the user.")
    @EncodeDefault
    val userDefinedMapping: List<UserDefinedTaskMapping> = RuntimeSystemConstants.taskMappingModel.userDefinedMapping,
    @SchemaDescription("Name of the task where all BSW events shall be mapped to.")
    @EncodeDefault
    val defaultBswTask: String = RuntimeSystemConstants.taskMappingModel.defaultBswTask
)

@Serializable
data class InterruptMappingModel(
    @SchemaDescription("Map specific ISRs to specific applications as defined by the user.")
    @EncodeDefault
    val userDefinedMapping: List<UserDefinedInterruptMapping> = RuntimeSystemConstants.interruptMappingModel.userDefinedMapping,
    @SchemaDescription("Mapping to the default interrupt application.")
    @EncodeDefault
    val defaultIsrApplication: String = RuntimeSystemConstants.interruptMappingModel.defaultIsrApplication
)

@Serializable
data class UserDefinedInterruptMapping(
    @SchemaDescription("Name of the interrupt.")
    @EncodeDefault
    val interrupt: String,
    @SchemaDescription("Name of the application.")
    @EncodeDefault
    val application: String
)

@Serializable
enum class RtsOsScalabilityClass {
    SC1, SC2, SC3, SC4
}

@Serializable
enum class Asil {
    QM, A, B, C, D;

    fun getMaxAsilLevel(otherAsil: Asil): Asil {
        return if (this >= otherAsil) {
            this
        } else {
            otherAsil
        }
    }

    fun getMaxAsilLevel(asilLevels: List<Asil>): Asil {
        var result: Asil = QM
        for (asilLevel: Asil in asilLevels) {
            result = result.getMaxAsilLevel(asilLevel)
        }
        return result
    }
}

@Serializable
enum class RtsOsTaskType {
    AUTO, BASIC, EXTENDED
}

@Serializable
enum class RtsOsTaskSchedule {
    FULL, NON
}

/**
 * Validate the input for the Cores and throw exceptions if necessary.
 * @param cores list of cores that are provided by the user and shall be checked.
 */
private fun validateCoreModel(cores: List<CoreModel>) {
    // Check if there is exactly one CoreModel with isMasterCore set to true
    val masterCoreCount = cores.count { it.isMasterCore }
    if (masterCoreCount != 1) {
        // If there are zero or more than one master cores, throw an exception
        throw ModelValidationException("Exactly one core must be marked as the master core. (parameter: isMasterCore)")
    }

    val uniquePhysicalCores = cores.map { it.physicalCore }.distinct()
    if (uniquePhysicalCores.size != cores.size) {
        throw ModelValidationException("Physical core numbers must be unique.")
    }

    val uniqueCoreNames = cores.map { it.name }.distinct()
    if (uniqueCoreNames.size != cores.size) {
        throw ModelValidationException("Core names must be unique.")
    }
}

/**
 * Validate the input for the Tasks within the Cores and throw exceptions if necessary.
 * @param cores list of cores that include the applications and especially the tasks, which shall be checked.
 */
private fun validateTaskModel(cores: List<CoreModel>) {
    val taskNames = mutableListOf<String>()
    for (core in cores) {
        for (application in core.applications ?: emptyList()) {
            for (task in application.tasks ?: emptyList()) {
                val uniqueTaskName = task.name
                if (taskNames.contains(uniqueTaskName)) {
                    throw ModelValidationException("Task names must be unique. Duplicate task name: $uniqueTaskName")
                } else {
                    taskNames.add(uniqueTaskName)
                }
            }
        }
    }
}

/**
 * Validate the input for the Applications within the Cores and throw exceptions if necessary.
 * @param cores list of cores that include the applications, which shall be checked.
 */
private fun validateApplicationModel(cores: List<CoreModel>) {
    val applicationNames = mutableListOf<String>()
    for (core in cores) {
        for (application in core.applications ?: emptyList()) {
            val uniqueApplicationName = application.name
            if (applicationNames.contains(uniqueApplicationName)) {
                throw ModelValidationException("Application names must be unique. Duplicate application name: $uniqueApplicationName")
            } else {
                applicationNames.add(uniqueApplicationName)
            }
        }
    }
}
