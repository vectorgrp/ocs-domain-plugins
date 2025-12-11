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
/*!        \file  HelperLib.kt
 *        \brief  Implements the helper methods used by various modules
 *
 *      \details  The following methods are implemented
 *                - getModule: Retrieves a module configuration based on the provided defRef.
 *                - hasModule: Checks if a module exists in the project.
 *                - hasModuleDefinition: Checks if a module definition exists in the project.
 *                - createOrGetContainer: Creates or retrieves a container based on the provided defRef.
 *                - getContainer: Retrieves a container based on the provided defRef.
 *                - getContainerList: Retrieves a list of containers based on the provided defRef.
 *                - setParam: Sets a parameter value in a container.
 *                - getParam: Retrieves a parameter value from a container.
 *                - delete: Deletes a container or parameter value.
 *
 *********************************************************************************************************************/
package com.vector.ocs.lib.shared

import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.automation.scripting.api.ScriptClientExecutionException
import com.vector.cfg.automation.scripting.api.project.IProject
import com.vector.cfg.model.access.AsrPath
import com.vector.cfg.model.access.DefRef
import com.vector.cfg.model.asr.ecuc.access.IAsrEcucShortnameAccess
import com.vector.cfg.model.asr.ecuc.access.IEcucModelAccess
import com.vector.cfg.model.asr.ecuc.access.ecuconfiguration.IEcuConfigurationAccess
import com.vector.cfg.model.asr.ecuc.access.ecuconfiguration.elements.IEcucValueParameter
import com.vector.cfg.model.asr.ecuc.access.ecuconfiguration.elements.parameter.IEcucBooleanParameter
import com.vector.cfg.model.asr.ecuc.access.ecuconfiguration.elements.parameter.IEcucFloatParameter
import com.vector.cfg.model.asr.ecuc.access.ecuconfiguration.elements.parameter.IEcucIntegerParameter
import com.vector.cfg.model.asr.ecuc.access.ecuconfiguration.elements.parameter.IEcucTextualParameter
import com.vector.cfg.model.asr.ecuc.access.ecuconfiguration.reference.IEcucSimpleReferenceParameter
import com.vector.cfg.model.asr.ecuc.exceptions.ModelDefinitionNotFoundException
import com.vector.cfg.model.asr.ecuc.operations.IAsrEcucModelOperations
import com.vector.cfg.model.mdf.commoncore.autosar.MIReferrable
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIContainer
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIModuleConfiguration
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIParameterValue
import com.vector.cfg.model.mdf.model.autosar.ecucparamdef.MIModuleDef
import com.vector.cfg.model.pai.api.activeEcuc
import com.vector.cfg.model.pai.api.extensions.*
import com.vector.ocs.core.api.OcsLogger

import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.file.Path
import kotlin.io.path.exists

/**
 * Base path
 *
 * @constructor Create empty Base path
 */
enum class BasePath {
    /**
     * Sip
     *
     * @constructor Create empty Sip
     */
    SIP,

    /**
     * Dpa
     *
     * @constructor Create empty Dpa
     */
    DPA
}

object HelperLib {
    private var logger: OcsLogger? = null

    // Region Helper functions (overloads), related to getting/checking modules

    /**
     * Get module
     *
     * @param def defRef of module
     * @return module object
     */
    @Suppress("unused")
    fun IProject.getModule(def: String): MIModuleConfiguration {
        // No error handling here. We assume the modules are present as they are checked by .hasModule
        val defRef = DefRef.createModuleDefRefFromString(def, this.projectContext)
        return this.activeEcuc.modules(defRef).first()
    }

    /**
     * Check if module exists
     *
     * @param pack package part of defRef
     * @param module defRef of module
     * @return boolean if module exists
     */
    @Suppress("unused")
    fun IProject.hasModule(pack: String, module: String): Boolean {
        val unchecked =
            DefRef.create(pack, module, MIModuleDef::class.java, MIModuleConfiguration::class.java, Void::class.java)
        val possible = unchecked.getPossibleDefinitionsForDefRefWithWildcard(project.projectContext)

        return if (possible.isNotEmpty()) {
            val list = this.activeEcuc.modules(unchecked)
            (list.isNotEmpty())
        } else {
            false
        }
    }

    /**
     * Has module definition
     *
     * @param pack package part of defRef
     * @param module defRef of module
     * @return boolean if module has definition
     */
    @Suppress("unused")
    fun IProject.hasModuleDefinition(pack: String, module: String): Boolean {
        val unchecked =
            DefRef.create(pack, module, MIModuleDef::class.java, MIModuleConfiguration::class.java, Void::class.java)
        val possible = unchecked.getPossibleDefinitionsForDefRefWithWildcard(project.projectContext)
        return possible.isNotEmpty()
    }
    // End region

    // Region Helper functions (overloads), related to getting/creating of containers

    /**
     * Create or get sub-container
     *
     * @param def defRef of container
     * @return container which was created or found
     */
    @Suppress("unused")
    fun MIContainer.createOrGetContainer(def: String): MIContainer? {
        val defRef = DefRef.tryCreate(this, def)
        val typedDefRef = defRef?.toTypedContainer()
        if (null == defRef || null == typedDefRef) {
            logger?.warn("DefRef $def not available in $this.")
            return null
        }
        if (!(typedDefRef.multiplicity.upper.equals(BigInteger("1")))) {
            logger?.warn("Upper Multiplicity != 1 with createOrGetContainer(without containerName) for $def $this.")
        }
        return if (this.subContainer(defRef).isEmpty()) {
            val created = try {
                val shortnameAcc = this.projectContext.getInstance(IAsrEcucShortnameAccess::class.java)
                val name = shortnameAcc.getValidShortnameForContainer(this, defRef.lastShortname, 3, defRef)
                val local = this.withSubContainer().create(name, typedDefRef)
                logger?.info("Created container: $local")
                local
            } catch (e: ModelDefinitionNotFoundException) {
                logger?.info("Could not create container with DefRef $defRef.")
                null
            }
            created
        } else {
            subContainer(defRef).first()
        }
    }

    /**
     * Create or get sub-container
     *
     * @param def defRef of container
     * @param containerName name of container
     * @return container which was created or found
     */
    @Suppress("unused")
    fun MIContainer.createOrGetContainer(def: String, containerName: String): MIContainer? {
        val defRef = DefRef.tryCreate(this, def)
        val typedDefRef = defRef?.toTypedContainer()
        if (null == defRef || null == typedDefRef) {
            logger?.warn("DefRef $def not available in $this.")
            return null
        }
        val found = this.subContainer(defRef).find { it.name == containerName }
        return if (null == found) {
            val created = try {
                val local = this.withSubContainer().create(containerName, typedDefRef)
                logger?.info("Created container: $local")
                local
            } catch (e: ModelDefinitionNotFoundException) {
                logger?.info("Could not create container with DefRef $defRef.")
                null
            }
            created
        } else {
            found
        }
    }

    /**
     * Create or get container of module
     *
     * @param def defRef of container
     * @return container which was created or found
     */
    @Suppress("unused")
    fun MIModuleConfiguration.createOrGetContainer(def: String): MIContainer? {
        val defRef = DefRef.tryCreate(this, def)
        val typedDefRef = defRef?.toTypedContainer()
        if (null == defRef || null == typedDefRef) {
            logger?.warn("DefRef $def not available in $this.")
            return null
        }
        if (!(typedDefRef.multiplicity.upper.equals(BigInteger("1")))) {
            logger?.warn("Upper Multiplicity != 1 with createOrGetContainer(without containerName) for $def $this.")
        }
        return if (this.subContainer(defRef).isEmpty()) {
            val created = try {
                val shortnameAcc = this.projectContext.getInstance(IAsrEcucShortnameAccess::class.java)
                val name = shortnameAcc.getValidShortnameForContainer(this, defRef.lastShortname, 3, defRef)
                val local = this.withSubContainer().create(name, typedDefRef)
                logger?.info("Created container: $local.")
                local
            } catch (e: ModelDefinitionNotFoundException) {
                logger?.info("Could not create container with DefRef $defRef.")
                null
            }
            created
        } else {
            subContainer(defRef).first()
        }
    }

    /**
     * Create or get container of module
     *
     * @param def defRef of container
     * @param containerName name of container
     * @return container which was created or found
     */
    @Suppress("unused")
    fun MIModuleConfiguration.createOrGetContainer(def: String, containerName: String): MIContainer? {
        val defRef = DefRef.tryCreate(this, def)
        val typedDefRef = defRef?.toTypedContainer()
        if (null == defRef || null == typedDefRef) {
            logger?.warn("DefRef $def not available in $this.")
            return null
        }
        val found = this.subContainer(defRef).find { it.name == containerName }
        return if (null == found) {
            val created = try {
                val local = this.withSubContainer().create(containerName, typedDefRef)
                logger?.info("Created container: $local.")
                local
            } catch (e: ModelDefinitionNotFoundException) {
                logger?.info("Could not create container with DefRef $defRef.")
                null
            }
            created
        } else {
            found
        }
    }
    // End region

    // Region Helper functions (overloads), related to getting of containers

    /**
     * Get sub-container
     *
     * @param def defRef of container
     * @return container object
     */
    @Suppress("unused")
    fun MIContainer.getContainer(def: String): MIContainer? {
        val defRef = DefRef.tryCreate(this, def)
        val typedDefRef = defRef?.toTypedContainer()
        if (null == defRef || null == typedDefRef) {
            logger?.warn("DefRef $def not available in $this.")
            return null
        }
        if (!(typedDefRef.multiplicity.upper.equals(BigInteger("1")))) {
            logger?.warn("Upper Multiplicity != 1 with getContainer(without containerName) for $def $this.")
        }
        val found = subContainer(defRef).find { true }
        if (null == found) {
            logger?.warn("Container $def not found in $this.")
        }
        return found
    }

    /**
     * Get sub-container of container
     *
     * @param def defRef of container
     * @param containerName name of container
     * @return container object
     */
    @Suppress("unused")
    fun MIContainer.getContainer(def: String, containerName: String): MIContainer? {
        val defRef = DefRef.tryCreate(this, def)
        val typedDefRef = defRef?.toTypedContainer()
        if (null == defRef || null == typedDefRef) {
            logger?.warn("DefRef $def not available in $this.")
            return null
        }
        val found = subContainer(defRef).find { it.name == containerName }
        if (null == found) {
            logger?.warn("Container $def with name $containerName not found in $this.")
        }
        return found
    }

    /**
     * Get container of module
     *
     * @param def defRef of container
     * @return container object
     */
    @Suppress("unused")
    fun MIModuleConfiguration.getContainer(def: String): MIContainer? {
        val defRef = DefRef.tryCreate(this, def)
        val typedDefRef = defRef?.toTypedContainer()
        if (null == defRef || null == typedDefRef) {
            logger?.warn("DefRef $def not available in $this.")
            return null
        }
        if (!(typedDefRef.multiplicity.upper.equals(BigInteger("1")))) {
            logger?.warn("Upper Multiplicity != 1 with getContainer(without containerName) for $def $this.")
        }
        val found = subContainer(defRef).find { true }
        if (null == found) {
            logger?.warn("Container $def not found in $this.")
        }
        return found
    }

    /**
     * Get container of module
     *
     * @param def defRef of container
     * @param containerName name of container
     * @return container object
     */
    @Suppress("unused")
    fun MIModuleConfiguration.getContainer(def: String, containerName: String): MIContainer? {
        val defRef = DefRef.tryCreate(this, def)
        val typedDefRef = defRef?.toTypedContainer()
        if (null == defRef || null == typedDefRef) {
            logger?.warn("DefRef $def not available in $this.")
            return null
        }
        val found = subContainer(defRef).find { it.name == containerName }
        if (null == found) {
            logger?.warn("Container $def with name $containerName not found in $this.")
        }
        return found
    }
    // End region

    // Region Helper functions (overloads), related to getting entire container lists

    /**
     * Get list of sub-container
     *
     * @param def defRef of container list
     * @return list of container objects
     */
    @Suppress("unused")
    fun MIContainer.getContainerList(def: String): List<MIContainer> {
        val defRef = DefRef.tryCreate(this, def)
        val typedDefRef = defRef?.toTypedContainer()
        if (null == defRef || null == typedDefRef) {
            logger?.warn("DefRef $def not available in $this.")
        }
        return defRef?.let { this.subContainer(it) } ?: emptyList()
    }

    /**
     * Get container list of module
     *
     * @param def defRef of container list
     * @return list of container objects
     */
    @Suppress("unused")
    fun MIModuleConfiguration.getContainerList(def: String): List<MIContainer> {
        val defRef = DefRef.tryCreate(this, def)
        val typedDefRef = defRef?.toTypedContainer()
        if (null == defRef || null == typedDefRef) {
            logger?.warn("DefRef $def not available in $this.")
        }
        return defRef?.let { this.subContainer(it) } ?: emptyList()
    }
    // End region

    // Region Helper functions (overloads), related to deleting a container

    /**
     * Deletes the container object
     *
     */
    @Suppress("unused")
    fun MIContainer.delete() {
        val modelAccess = projectContext.getService(IEcucModelAccess::class.java)
        if (ceState.isDeletable) {
            logger?.info("Removing container: $this")
            this.moRemove()
        } else if (modelAccess.isDerived(this)) {
            logger?.info("Removing derived container: $this.")
            this.moRemove()
        } else {
            logger?.warn("Container is not deletable: $this.")
        }
    }

    // Endregion

    // Region Helper functions (overloads), related to getting/setting of parameters (not the values)

    /**
     * Set param of container
     *
     * @param def defRef of parameter
     * @param value boolean
     */
    @Suppress("unused")
    fun MIContainer.setParam(def: String, value: Boolean) {
        this.setParam(def, value.toString()) /* default to single value */
    }

    /**
     * Set param of container
     *
     * @param def defRef of parameter
     * @param value long
     */
    @Suppress("unused")
    fun MIContainer.setParam(def: String, value: Long) {
        this.setParam(def, value.toString()) /* default to single value */
    }

    /**
     * Set param of container
     *
     * @param def defRef of parameter
     * @param value float
     */
    @Suppress("unused")
    fun MIContainer.setParam(def: String, value: Float) {
        this.setParam(def, value.toString()) /* default to single value */
    }

    /**
     * Set param of container
     *
     * @param def defRef of parameter
     * @param value integer
     */
    @Suppress("unused")
    fun MIContainer.setParam(def: String, value: Int) {
        this.setParam(def, value.toString()) /* default to single value */
    }

    /**
     * Set param of container
     *
     * @param def defRef of parameter
     * @param value string
     */
    @Suppress("unused")
    fun MIContainer.setParam(def: String, value: String) {
        this.setParam(def, value, 0)
    }

    /**
     * Set param of container
     *
     * @param def defRef of parameter
     * @param value boolean
     * @param idx index of parameter
     */
    @Suppress("unused")
    fun MIContainer.setParam(def: String, value: Boolean, idx: Int) {
        this.setParam(def, value.toString(), idx)
    }

    /**
     * Set param of container
     *
     * @param def defRef of parameter
     * @param value boolean
     * @param idx index of parameter
     */
    @Suppress("unused")
    fun MIContainer.setParam(def: String, value: Long, idx: Int) {
        this.setParam(def, value.toString(), idx)
    }

    /**
     * Set param of container
     *
     * @param def defRef of parameter
     * @param value string
     * @param idx index of parameter
     */
    @Suppress("unused")
    fun MIContainer.setParam(def: String, value: String, idx: Int) {
        val project = ScriptApi.getActiveProject()
        val modelOps = project.getInstance(IAsrEcucModelOperations::class.java)
        val defRef = DefRef.tryCreate(this, def)
        val typedDefRef = defRef?.toTypedContainer()

        if (null == defRef || null == typedDefRef) {
            logger?.warn("DefRef $def not available in $this.")
            return
        }

        var param = this.findParamOrNull(defRef, idx, value)

        if (null == param) {
            try {
                val created = modelOps.createParameterByDefinition(this, defRef)
                logger?.info("Created parameter: $created.")
                param = created
            } catch (e: ModelDefinitionNotFoundException) {
                logger?.info("Could not create parameter with DefRef $defRef.")
                return
            }
        }

        if (param?.ceState?.isChangeable == true) {
            param.setValue(value)
        } else {
            logger?.warn("Could not set parameter $param to $value.")
        }
    }

    /**
     * Get parameter of container
     *
     * @param def defRef of parameter
     * @return value of parameter
     */
    @Suppress("unused")
    fun MIContainer.getParam(def: String): MIParameterValue? {
        val defRef = DefRef.create(this, def)
        val param = this.parameter(defRef)
        return when (param.size) {
            0 -> null
            1 -> param.first()
            else -> {
                logger?.warn("get Parameter called for parameter with indexes: $param. Used first element.")
                param.first()
            }
        }
    }

    /**
     * Get list of parameter values
     *
     * @param def defRef of parameter list
     * @return list of parameter values
     */
    @Suppress("unused")
    fun MIContainer.getParameterList(def: String): List<MIParameterValue> {
        val defRef = DefRef.create(this, def)
        return this.parameter(defRef)
    }

    /**
     * Find param or null
     *
     * @param def defRef of parameter
     * @param idx index of parameter
     * @param value string
     * @return value of parameter
     */
    private fun MIContainer.findParamOrNull(def: DefRef, idx: Int, value: String): MIParameterValue? {
        val project = ScriptApi.getActiveProject()
        val configAccess = project.getInstance(IEcuConfigurationAccess::class.java)
        val existing = parameter(def)
        return when (idx) {
            in existing.indices -> existing[idx]
            existing.lastIndex + 1 -> null
            -1 -> existing.find {
                val param = configAccess.cfg(it)
                if (param !is IEcucValueParameter<*> || !param.hasValue()) {
                    return@find false
                }
                when (param) {
                    is IEcucBooleanParameter -> value.toBooleanStrict() == param.value
                    is IEcucIntegerParameter -> value.toBigInteger() == param.value
                    is IEcucFloatParameter -> value.toDouble().compareTo(param.value) == 0
                    is IEcucTextualParameter -> value == param.value
                    is IEcucSimpleReferenceParameter -> AsrPath.create(value, MIReferrable::class.java) == param.value
                    else -> throw ScriptClientExecutionException("Type of parameter is not supported: $it.")
                }
            }

            else -> {
                logger?.warn("Index '$idx' is out of bounds for parameter with definition '${def.lastShortname}' in container: $this. " + existing.lastIndex + 1 + "used instead.")
                null
            }
        }
    }
    // End region

    // Region Helper functions (overloads), related to getting/setting/deleting values of parameters

    /**
     * Delete the parameter value
     *
     */
    @Suppress("unused")
    fun MIParameterValue.delete() {
        if (ceState.isDeletable) {
            logger?.info("Removing parameter: $this.")
            this.moRemove()
        } else {
            logger?.warn("Parameter is not deletable: $this.")
        }
    }

    /**
     * Get integer value
     *
     * @return integer value of parameter
     */
    @Suppress("unused")
    fun MIParameterValue.getValueInteger(): Long? {
        val project = ScriptApi.getActiveProject()
        val configAccess = project.getInstance(IEcuConfigurationAccess::class.java)
        var x: Long? = null
        when (val param = configAccess.cfg(this)) {
            is IEcucIntegerParameter -> x = param.get(this)?.toLong()
            else -> logger?.warn("Type of parameter is not supported, parameter not possible as integer/long: $this.")
        }
        return x
    }

    /**
     * Get boolean value
     *
     * @return boolean value of parameter
     */
    @Suppress("unused")
    fun MIParameterValue.getValueBoolean(): Boolean? {
        val project = ScriptApi.getActiveProject()
        val configAccess = project.getInstance(IEcuConfigurationAccess::class.java)
        var x: Boolean? = null
        when (val param = configAccess.cfg(this)) {
            is IEcucBooleanParameter -> x = param.get(this)
            else -> logger?.warn("Type of parameter is not supported, parameter not possible as boolean: $this.")
        }
        return x
    }

    /**
     * Get string value
     *
     * @return string value of parameter
     */
    fun MIParameterValue.getValueString(): String? {
        val project = ScriptApi.getActiveProject()
        val configAccess = project.getInstance(IEcuConfigurationAccess::class.java)
        var x: String? = null
        when (val param = configAccess.cfg(this)) {
            is IEcucBooleanParameter -> x = param.get(this)?.toString()
            is IEcucIntegerParameter -> x = param.get(this)?.toString()
            is IEcucFloatParameter -> x = param.get(this)?.toString()
            is IEcucTextualParameter -> x = param.get(this)
            is IEcucSimpleReferenceParameter -> x = param.get(this)?.autosarPathString
            else -> logger?.warn("Type of parameter is not supported, parameter not possible as string: $this.")
        }
        return x
    }

    /**
     * Set string value of parameter
     *
     * @param value string value to set
     */
    private fun MIParameterValue.setValue(value: String) {
        val project = ScriptApi.getActiveProject()
        val configAccess = project.getInstance(IEcuConfigurationAccess::class.java)
        when (val param = configAccess.cfg(this)) {
            is IEcucBooleanParameter -> param.set(value.toBooleanStrict(), this)
            is IEcucIntegerParameter -> param.set(value.toBigInteger(), this)
            is IEcucFloatParameter -> param.set(value.toDouble(), this)
            is IEcucTextualParameter -> param.set(value, this)
            is IEcucSimpleReferenceParameter -> param.set(AsrPath.create(value, MIReferrable::class.java), this)
            else -> logger?.warn("Type of parameter is not supported, parameter will not be modified: $this.")
        }
    }

    /**
     * Get value of parameter
     *
     * @param T general value
     * @param original object
     * @return value
     */
    @Suppress("UNUSED_PARAMETER")
    private fun <T: Any> IEcucValueParameter<T>.get(original: MIParameterValue): T? {
        return if (hasValue()) value else {
            logger?.warn("Reading Parameter $this without any value.")
            null
        }
    }

    /**
     * Set parameter value
     *
     * @param T data type
     * @param new value
     * @param original parameter
     */
    private fun <T : Any> IEcucValueParameter<T>.set(new: T, original: MIParameterValue) {
        if (!hasValue()) {
            setValue(new)
            logger?.info("Set value of parameter: $original.")
            return
        }
        val old = value
        val same = if (new is BigDecimal && old is BigDecimal) {
            new.compareTo(old) == 0
        } else {
            new == old
        }
        if (same) {
            return
        }
        setValue(new)
        logger?.info("Updated value of parameter: $original.")
    }

    /**
     * Get path of project
     *
     * @param base enum which base path it is
     * @param path path of the project
     * @return file path
     */
    fun getPath(base: BasePath, path: String): Path? {
        val paths = ScriptApi.scriptCode().scriptContext.paths
        val filePath = when (base) {
            BasePath.SIP -> paths.resolveBswPackagePath(path)
            BasePath.DPA -> paths.resolveProjectPath(path)
        } ?: return null
        if (!(filePath.exists())) {
            return null
        }
        return filePath
    }

    /**
     * Get file from path
     *
     * @param base enum which base path it is
     * @param path path of the project
     * @return file object
     */
    fun getFileFromPath(base: BasePath, path: String): File? {
        val paths = ScriptApi.scriptCode().scriptContext.paths
        val filePath = when (base) {
            BasePath.SIP -> paths.resolveBswPackagePath(path)
            BasePath.DPA -> paths.resolveProjectPath(path)
        } ?: return null
        if (!(filePath.exists())) {
            return null
        }
        val file = filePath.toFile()
        if (!(file.exists())) {
            return null
        }
        return file
    }
    // End region
}
