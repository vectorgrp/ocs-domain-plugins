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
/*!        \file  Cfg5Client.kt
 *        \brief  Provides helper functions and classes for managing ECU configurations in an automotive project.
 *
 *      \details  This file contains utility functions to manage CFG5 configurations, including:
 *                - Container Management: Functions to create, retrieve, and delete containers.
 *                - Functions to set, get, and delete parameter values.
 *                - Path Handling: Defines paths and handles file operations related to the project.
 *                - Logging: Utilizes OcsLogger for logging information and warnings.
 *                - Enums: Defines BasePath enum for different base paths (SIP, DPA).
 *                - Error Handling: Includes basic error handling for container and parameter operations.
 *
 *
 *********************************************************************************************************************/
package com.vector.ocs.lib.shared

import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.gen.core.genusage.pai.api.generation
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIContainer
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIParameterValue
import com.vector.cfg.model.pai.api.transaction
import com.vector.cfg.validation.pai.validation

import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.HelperLib.createOrGetContainer
import com.vector.ocs.lib.shared.HelperLib.delete
import com.vector.ocs.lib.shared.HelperLib.getContainer
import com.vector.ocs.lib.shared.HelperLib.getContainerList
import com.vector.ocs.lib.shared.HelperLib.getModule
import com.vector.ocs.lib.shared.HelperLib.getParam
import com.vector.ocs.lib.shared.HelperLib.getParameterList
import com.vector.ocs.lib.shared.HelperLib.getValueInteger
import com.vector.ocs.lib.shared.HelperLib.setParam


@Suppress("unused")
object Cfg5Client {
    private val project = ScriptApi.getActiveProject()

    /**
     * Create container
     *
     * @param parentDefRef defRef of parent container
     * @param defRef defRef of container which should be created
     * @param shortName short name of the container to be created
     * @param logger OcsLogger object
     */
    @Suppress("unused")
    fun createContainer(
        parentDefRef: String,
        defRef: String,
        shortName: String,
        logger: OcsLogger
    ) {
        val moduleDefRef = extractModuleDefRef(parentDefRef)
        if (moduleDefRef != null) {
            project.transaction {
                val currentModule = project.getModule(moduleDefRef)
                val parentContainer = currentModule.getContainer(removeMicrosarSubstring(parentDefRef))
                parentContainer?.createOrGetContainer(removeMicrosarSubstring(defRef), shortName)
            }
        } else {
            logger.warn("ModuleDefRef not found!")
        }
    }

    /**
     * Create container under module
     *
     * @param defRef defRef of container which should be created
     * @param shortName short name of the container to be created
     * @param logger OcsLogger object
     */
    fun createContainerUnderModule(
        defRef: String,
        shortName: String,
        logger: OcsLogger
    ) {
        val moduleDefRef = extractModuleDefRef(defRef)
        if (moduleDefRef != null) {
            project.transaction {
                val currentModule = project.getModule(moduleDefRef)
                currentModule.createOrGetContainer(removeMicrosarSubstring(defRef), shortName)
            }
        } else {
            logger.warn("ModuleDefRef not found!")
        }
    }

    /**
     * Get container
     *
     * @param defRef defRef of container which should be found
     * @param shortName short name of the container
     * @param logger OcsLogger object
     * @return container
     */
    @Suppress("unused")
    fun getContainer(
        defRef: String,
        shortName: String,
        logger: OcsLogger
    ): MIContainer? {
        val container: MIContainer?
        val moduleDefRef = extractModuleDefRef(defRef)
        if (moduleDefRef != null) {
            val currentModule = project.getModule(moduleDefRef)
            container = currentModule.getContainer(removeMicrosarSubstring(defRef), shortName)
        } else {
            logger.warn("ModuleDefRef not found!")
            container = null
        }
        return container
    }

    /**
     * Get child container
     *
     * @param defRef defRef of the child container
     * @param shortName short name of child/sub container
     * @return child/sub container
     */
    fun MIContainer.getChildContainer(
        defRef: String,
        shortName: String
    ): MIContainer? {
        return this.getContainer(removeMicrosarSubstring(defRef), shortName)
    }

    /**
     * Get child container list
     *
     * @param defRef defRef of child container list
     * @return child/sub container list
     */
    fun MIContainer.getChildContainerList(
        defRef: String
    ): List<MIContainer> {
        return this.getContainerList(removeMicrosarSubstring(defRef))
    }

    /**
     * Delete container
     *
     */
    internal fun MIContainer.deleteContainer(
    ) {
        this.delete()
    }


    /**
     * Create or get child container
     *
     * @param defRef defRef of child/sub container to create/get
     * @param shortName short name of child/sub container
     * @return created/found child/sub container
     */
    @Suppress("unused")
    fun MIContainer.createOrGetChildContainer(
        defRef: String,
        shortName: String
    ): MIContainer? {
        var container: MIContainer? = null
        project.transaction {
            container = this@createOrGetChildContainer.createOrGetContainer(removeMicrosarSubstring(defRef), shortName)
        }
        return container
    }

    /**
     * Get list of containers
     *
     * @param defRef defRef of list of containers
     * @param logger OcsLogger object
     * @return list of containers
     */
    @Suppress("unused")
    fun getListOfContainer(
        defRef: String,
        logger: OcsLogger
    ): List<MIContainer> {
        val container: List<MIContainer>
        val moduleDefRef = extractModuleDefRef(defRef)
        container = if (moduleDefRef != null) {
            val currentModule = project.getModule(moduleDefRef)
            currentModule.getContainerList(removeMicrosarSubstring(defRef))
        } else {
            logger.warn("ModuleDefRef not found!")
            emptyList()
        }
        return container
    }

    /**
     * Overloaded set parameter boolean
     *
     * @param containerShortName short name of parent container
     * @param parameterDefRef defRef of parameter
     * @param value boolean value to set of parameter
     * @param logger OcsLogger object
     */
    @Suppress("unused")
    fun setParameter(containerShortName: String, parameterDefRef: String, value: Boolean, logger: OcsLogger) {
        project.transaction {
            val moduleDefRef = extractModuleDefRef(parameterDefRef)
            if (moduleDefRef != null) {
                val currentModule = project.getModule(moduleDefRef)
                val currentContainer = currentModule.getContainer(
                    removeMicrosarSubstring(getContainerOfParameter(parameterDefRef)),
                    containerShortName
                )

                currentContainer?.setParam(removeMicrosarSubstring(parameterDefRef), value)
            } else {
                logger.warn("ModuleDefRef not found!")
            }

        }
    }

    /**
     * Overloaded set parameter long
     *
     * @param containerShortName short name of parent container
     * @param parameterDefRef defRef of parameter
     * @param value long value to set of parameter
     * @param logger OcsLogger object
     */
    @Suppress("unused")
    fun setParameter(containerShortName: String, parameterDefRef: String, value: Long, logger: OcsLogger) {
        project.transaction {
            val moduleDefRef = extractModuleDefRef(parameterDefRef)
            if (moduleDefRef != null) {
                val currentModule = project.getModule(moduleDefRef)
                val currentContainer = currentModule.getContainer(
                    removeMicrosarSubstring(getContainerOfParameter(parameterDefRef)),
                    containerShortName
                )

                currentContainer?.setParam(removeMicrosarSubstring(parameterDefRef), value)
            } else {
                logger.warn("ModuleDefRef not found!")
            }

        }
    }

    /**
     * Overloaded set parameter float
     *
     * @param containerShortName short name of parent container
     * @param parameterDefRef defRef of parameter
     * @param value float value to set of parameter
     * @param logger OcsLogger object
     */
    @Suppress("unused")
    fun setParameter(containerShortName: String, parameterDefRef: String, value: Float, logger: OcsLogger) {
        project.transaction {
            val moduleDefRef = extractModuleDefRef(parameterDefRef)
            if (moduleDefRef != null) {
                val currentModule = project.getModule(moduleDefRef)
                val currentContainer = currentModule.getContainer(
                    removeMicrosarSubstring(getContainerOfParameter(parameterDefRef)),
                    containerShortName
                )

                currentContainer?.setParam(removeMicrosarSubstring(parameterDefRef), value)
            } else {
                logger.warn("ModuleDefRef not found!")
            }
        }
    }

    /**
     * Overloaded set parameter int
     *
     * @param containerShortName short name of parent container
     * @param parameterDefRef defRef of parameter
     * @param value int value to set of parameter
     * @param logger OcsLogger object
     */
    @Suppress("unused")
    fun setParameter(containerShortName: String, parameterDefRef: String, value: Int, logger: OcsLogger) {
        project.transaction {
            val moduleDefRef = extractModuleDefRef(parameterDefRef)
            if (moduleDefRef != null) {
                val currentModule = project.getModule(moduleDefRef)
                val currentContainer = currentModule.getContainer(
                    removeMicrosarSubstring(getContainerOfParameter(parameterDefRef)),
                    containerShortName
                )

                currentContainer?.setParam(removeMicrosarSubstring(parameterDefRef), value)
            } else {
                logger.warn("ModuleDefRef not found!")
            }

        }
    }


    /**
     * Overloaded set parameter string
     *
     * @param containerShortName short name of parent container
     * @param parameterDefRef defRef of parameter
     * @param value string value to set of parameter
     * @param logger OcsLogger object
     */
    @Suppress("unused")
    fun setParameter(containerShortName: String, parameterDefRef: String, value: String, logger: OcsLogger) {
        project.transaction {
            val moduleDefRef = extractModuleDefRef(parameterDefRef)
            if (moduleDefRef != null) {
                val currentModule = project.getModule(moduleDefRef)
                val currentContainer = currentModule.getContainer(
                    removeMicrosarSubstring(getContainerOfParameter(parameterDefRef)),
                    containerShortName
                )
                currentContainer?.setParam(removeMicrosarSubstring(parameterDefRef), value)
            } else {
                logger.warn("ModuleDefRef not found!")
            }

        }
    }


    /**
     * Get parameter
     *
     * @param containerShortName short name of container
     * @param parameterDefRef defRef of parameter
     * @param logger OcsLogger object
     * @return int value of parameter
     */
    @Suppress("unused")
    fun getParameter(containerShortName: String, parameterDefRef: String, logger: OcsLogger): Int {
        val moduleDefRef = extractModuleDefRef(parameterDefRef)
        val param: Int
        if (moduleDefRef != null) {
            val currentModule = project.getModule(moduleDefRef)
            val currentContainer = currentModule.getContainer(
                removeMicrosarSubstring(getContainerOfParameter(parameterDefRef)),
                containerShortName
            )
            param =
                currentContainer?.getParam(removeMicrosarSubstring(parameterDefRef))?.getValueInteger()?.toInt()
                    ?: 0
        } else {
            logger.warn("ModuleDefRef not found!")
            param = 0
        }
        return param
    }

    /**
     * Get list of parameter
     *
     * @param containerShortName short name of container
     * @param parameterDefRef defRef of parameter
     * @param logger OcsLogger object
     * @return list of parameters
     */
    @Suppress("unused")
    fun getParameterList(
        containerShortName: String,
        parameterDefRef: String,
        logger: OcsLogger
    ): List<MIParameterValue>? {
        val moduleDefRef = extractModuleDefRef(parameterDefRef)
        val paramList: List<MIParameterValue>? = if (moduleDefRef != null) {
            val currentModule = project.getModule(moduleDefRef)
            val currentContainer = currentModule.getContainer(
                removeMicrosarSubstring(getContainerOfParameter(parameterDefRef)),
                containerShortName
            )
            currentContainer?.getParameterList(removeMicrosarSubstring(parameterDefRef))
        } else {
            logger.warn("ModuleDefRef not found!")
            emptyList()
        }
        return paramList
    }

    /**
     * Validate modules
     *
     * @param modules modules to be validated
     */
    @Suppress("unused")
    fun validate(vararg modules: String) {
        val generation = project.generation
        generation.settings.deselectAll()
        for (module in modules) {
            generation.settings.selectGeneratorByDefRef(module)
        }
        generation.validate()
    }

    /**
     * Trigger solving action
     *
     * @param validationOrigin
     * @param validationId
     * @param logger OcsLogger object
     */
    fun solve(
        validationOrigin: String, validationId: Int, logger: OcsLogger
    ) {
        project.validation.validationResults.forEach { validationResult ->
            if ((validationResult.id.id == validationId) && (validationResult.id.origin == validationOrigin)) {
                validationResult.solvingActions.forEach { sa ->
                    if (validationResult.isActive) {
                        logger.info("Triggering SolvingAction ${validationResult.id.origin}${validationResult.id.id}")
                        sa.solve()
                    }
                }
            }
        }
    }

    /**
     * Extract defRef of module
     *
     * @param input string from which the module should be extracted
     * @return module defRef
     */
    @Suppress("unused")
    private fun extractModuleDefRef(input: String): String? {
        // Define the glob pattern
        val pattern = "/MICROSAR/[a-zA-Z_-]*/"
        val patternOfModule = "/MICROSAR/[a-zA-Z]"
        var matcher: MatchResult? = null
        if (pattern.toRegex().containsMatchIn(input)) {
            // Use find() to match the pattern
            matcher = pattern.toRegex().find(input)
            return matcher?.value?.removeSuffix("/")
        } else if (patternOfModule.toRegex().containsMatchIn(input)) {
            return input
        }
        return null
    }

    /**
     * Get container of parameter
     *
     * @param input defRef of parameter
     * @return name of container
     */
    @Suppress("unused")
    private fun getContainerOfParameter(input: String): String {
        return input.substringBeforeLast("/")
    }

    /**
     * Remove MICROSAR substring
     *
     * @param input defRef with MICROSAR substring
     * @return string without MICROSAR
     */
    @Suppress("unused")
    private fun removeMicrosarSubstring(input: String): String {
        // Replace "/MICROSAR/" with an empty string
        return input.replace("/MICROSAR/", "")
    }

    /**
     * Delete container
     *
     * @param container which should be deleted
     */
    @Suppress("unused")
    fun deleteContainer(container: MIContainer) {
        project.transaction {
            container.delete()
        }
    }
}
