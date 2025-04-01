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
/*!        \file  PluginConstants.kt
 *        \brief  Store general constants that are relevant for RuntimeSystem plugin.
 *
 *      \details  -
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.runtimesystem

import java.util.*
import java.util.regex.Pattern

/**
 * Provide common constant definitions for the RuntimeSystem plugin.
 */
class RuntimeSystemConstants {
    companion object {
        /** Provide the DefRef of the Os module. */
        @JvmStatic
        val OS_DEFREF: String = "/MICROSAR/Os"

        /** Provide the DefRef of the Os module. */
        @JvmStatic
        val VTTOS_DEFREF: String = "/MICROSAR/VTT/VTTOs"

        /** Provide the DefRef of the EcuC module. */
        @JvmStatic
        val ECUC_DEFREF: String = "/MICROSAR/EcuC"

        /** Provide the DefRef of the EcuM module. */
        @JvmStatic
        val ECUM_DEFREF: String = "/MICROSAR/EcuM"

        /** Provide the DefRef of the Rte module. */
        @JvmStatic
        val RTE_DEFREF: String = "/MICROSAR/Rte"

        /** Provide the DefRef of the Com module. */
        @JvmStatic
        val COM_DEFREF: String = "/MICROSAR/Com"

        /** Name of the processing context */
        @JvmStatic
        val RTS_PLUGIN_NAME: String = "RuntimeSystemPlugin"

        /** Prefix for the name schema of the EcuC configuration elements */
        @JvmStatic
        val ECUC_NAME_SCHEMA_PREFIX: String = "SysEcuC_"

        /** Core name for the default naming pattern */
        @JvmStatic
        val OSCORE_NAME: String = "OsCore"

        /** Pattern for matching the ComM module */
        @JvmStatic
        val patternComM: Pattern = Pattern.compile(".*ComM.*")

        /** Pattern for matching the BswM module */
        @JvmStatic
        val patternBswM: Pattern = Pattern.compile(".*BswM.*")

        /** Pattern for matching the EcuM module */
        @JvmStatic
        val patternEcuM: Pattern = Pattern.compile(".*EcuM.*")

        /** String list for SWC Instance mapping */
        @JvmStatic
        val swcInstances: MutableList<String> = mutableListOf("Det", "Csm", "SecOC")

        /** Default values for the OsHooks */
        @JvmStatic
        val rtsOsHooks: RtsOsHooks = RtsOsHooks(
            errorHook = true,
            panicHook = true,
            postTaskHook = false,
            preTaskHook = false,
            protectionHook = false,
            shutdownHook = true,
            startupHook = false
        )

        /** Default value for the ScalabilityClass */
        @JvmStatic
        val rtsOsScalabilityClass: RtsOsScalabilityClass = RtsOsScalabilityClass.SC1

        /** Default TaskMapping Model */
        @JvmStatic
        val taskMappingModel: TaskMappingModel = TaskMappingModel(
            defaultBswTask = "BswTask_Appl_QM_OsCore0",
            userDefinedMapping = mutableListOf()
        )

        /** Default InterruptMapping Model */
        @JvmStatic
        val interruptMappingModel: InterruptMappingModel = InterruptMappingModel(
            defaultIsrApplication = "Appl_QM_OsCore0",
            userDefinedMapping = mutableListOf()
        )

        /** Default value for enabling the Cleanup phase during plugin execution */
        @JvmStatic
        val rtsEnableCleanupPhase: Boolean = true

        /** Instantiate the collection of default values of the Core Model properties */
        @JvmStatic
        val coreProperties = CoreProperties()

        /** Instantiate the collection of default values of Application Model properties */
        @JvmStatic
        val applicationProperties = ApplicationProperties()

        /** Instantiate the collection of  Task Model properties, which can be applied to tasks defined by the user */
        @JvmStatic
        val userDefinedTaskProperties = UserDefinedTaskProperties()

        /* ----------------------------------------
           Constants specifically for the Internal Data Model
           ----------------------------------------  */

        /** Default value for the internal numbering of the coreIDs */
        @JvmStatic
        val firstCoreID: Int = 0

        /** Default value for the XSignal ISR interrupt priority */
        @JvmStatic
        val xSignalIsrPriority: Int = 19

        /** Default value for the XSignal ISR interrupt source */
        @JvmStatic
        val xSignalIsrSource: Int = 100

        /** Default value for the seconds per tick of the OsCounter.**/
        @JvmStatic
        val secondsPerTick: Double = 0.001

        /** Default value for the maximum allowed value of the system counter in ticks.*/
        @JvmStatic
        val maxAllowedValue: Long = 1073741823

        /** Default value for the value of the Interrupt service routine priority **/
        @JvmStatic
        val isrPriority: Int = 20

        /** Default List that implements the standard task configuration in an application */
        @JvmStatic
        val defaultTaskList =
            listOf(TaskType.SystemInitTask, TaskType.ApplInitTask, TaskType.ApplTask, TaskType.BswTask)

        @JvmStatic
        val applicationMap = EnumMap(
            mapOf(
                Asil.QM to listOf(TaskType.SystemInitTask, TaskType.ApplInitTask, TaskType.ApplTask, TaskType.BswTask)
            )
        )

        /** Declaration of the different task types that are currently used for the standard task configuration */
        enum class TaskType {
            SystemInitTask,
            ApplInitTask,
            ApplTask,
            BswTask;

            // Nested class representing properties of each task
            data class TaskProperties(
                val name: String,
                val priority: Int,
                val stackSize: Int,
                val schedule: RtsOsTaskSchedule,
                val type: RtsOsTaskType,
                val isAutoStart: Boolean
            )
        }

        /** Map for connecting each task type to the necessary default values of its properties */
        @JvmStatic
        val taskPropertiesMap = mapOf(
            TaskType.SystemInitTask to TaskType.TaskProperties(
                "SystemInitTask",
                100,
                2048,
                RtsOsTaskSchedule.NON,
                RtsOsTaskType.BASIC,
                true
            ),
            TaskType.ApplInitTask to TaskType.TaskProperties(
                "ApplInitTask",
                90,
                2048,
                RtsOsTaskSchedule.NON,
                RtsOsTaskType.BASIC,
                false
            ),
            TaskType.ApplTask to TaskType.TaskProperties(
                "ApplTask",
                40,
                4096,
                RtsOsTaskSchedule.NON,
                RtsOsTaskType.AUTO,
                false
            ),
            TaskType.BswTask to TaskType.TaskProperties(
                "BswTask",
                60,
                4096,
                RtsOsTaskSchedule.NON,
                RtsOsTaskType.AUTO,
                false
            )
        )

    }
}

/** Collection of default values of the Core Model properties */
class CoreProperties {
    val isMasterCore: Boolean = false
    val isAutostartCore: Boolean = false
    val isAutosarCore: Boolean = true
}

/** Collection of default values of the Application Model properties */
class ApplicationProperties {
    val asilLevel: Asil = Asil.QM
    val applicationNameSchema = "Appl_%s_%s"
}

/** Collection of default values of the Task Model properties */
class UserDefinedTaskProperties {
    val stackSize: Int = 1024
    val schedule: RtsOsTaskSchedule = RtsOsTaskSchedule.FULL
    val type: RtsOsTaskType = RtsOsTaskType.AUTO
    val autostart: Boolean = true
}
