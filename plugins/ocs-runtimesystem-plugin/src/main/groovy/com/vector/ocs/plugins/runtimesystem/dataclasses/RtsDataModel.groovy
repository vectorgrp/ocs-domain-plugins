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
/*!        \file  RtsDataModel.groovy
 *        \brief  Consists of the RtsDataModel class declaration and its properties.
 *
 *      \details  This class includes:
 *                  - Declaration of class properties
 *                  - Constructor
 *                  - Getter & Setter methods
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.runtimesystem.dataclasses

import com.vector.ocs.plugins.runtimesystem.InterruptMappingModel
import com.vector.ocs.plugins.runtimesystem.RtsOsHooks
import com.vector.ocs.plugins.runtimesystem.RtsOsScalabilityClass
import com.vector.ocs.plugins.runtimesystem.RuntimeSystemConstants
import com.vector.ocs.plugins.runtimesystem.TaskMappingModel

/**
 * This class represents the DataModel objects and their needed properties for the usage within the business logic.
 */
class RtsDataModel {
    private List<RtsCore> cores
    private RtsOsScalabilityClass scalabilityClass
    private RtsOsHooks osHooks
    private TaskMappingModel taskMapping
    private InterruptMappingModel interruptMapping
    private Boolean enableCleanupPhase

    /**
     * Initialize a new instance of the RtsDataModel class for default modelling.
     * Contains only an initialized Cores arraylist and the default SC.
     */
    RtsDataModel() {
        this.cores = new ArrayList<RtsCore>()
        this.scalabilityClass = RuntimeSystemConstants.rtsOsScalabilityClass
    }

    /**
     * Initialize a new instance of the RtsDataModel class containing all needed properties,
     * ready for handing over to business logic.
     */
    RtsDataModel(List<RtsCore> cores, RtsOsScalabilityClass scalabilityClass, RtsOsHooks osHooks, TaskMappingModel taskMapping, InterruptMappingModel interruptMapping, Boolean enableCleanupPhase) {
        this.cores = cores
        this.scalabilityClass = scalabilityClass
        this.osHooks = osHooks
        this.taskMapping = taskMapping
        this.interruptMapping = interruptMapping
        this.enableCleanupPhase = enableCleanupPhase
    }

    /**
     * This function adds an entry of type RtsCore to the list property cores.
     * @param core entry that will be added to the list.
     */
    void addCore(RtsCore core) {
        this.cores.add(core)
    }

    /**
     * Return a list of the present RtsCore objects.
     */
    List<RtsCore> getCores() {
        return this.cores
    }

    /**
     * Return the current value of the property scalabilityClass.
     */
    RtsOsScalabilityClass getScalabilityClass() {
        return this.scalabilityClass
    }

    /**
     * Return the current value of the property osHooks.
     */
    RtsOsHooks getOsHooks() {
        return this.osHooks
    }

    /**
     * Return the current value of the property taskMapping.
     */
    TaskMappingModel getTaskMapping() {
        return this.taskMapping
    }

    /**
     * Return the current value of the property interruptMapping.
     */
    InterruptMappingModel getInterruptMapping() {
        return this.interruptMapping
    }

    /**
     * Return the current value of the property enableCleanupPhase.
     */
    Boolean getEnableCleanupPhase() {
        return this.enableCleanupPhase
    }
}
