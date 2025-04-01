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
/*!        \file  RtsTask.groovy
 *        \brief  Consists of the RtsTask class declaration and its properties.
 *
 *      \details  This class includes:
 *                  - Declaration of class properties
 *                  - Constructor
 *                  - Getter & Setter methods
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.runtimesystem.dataclasses

import com.vector.ocs.plugins.runtimesystem.RtsOsTaskSchedule
import com.vector.ocs.plugins.runtimesystem.RtsOsTaskType
import com.vector.ocs.plugins.runtimesystem.RuntimeSystemConstants.Companion.TaskType

/**
 * This class represents the Task objects and their needed properties for the internal data model similar to the OsTask class.
 */
class RtsTask {
    private String name
    private Integer priority
    private Integer stackSize
    private RtsOsTaskSchedule schedule
    private RtsOsTaskType type
    private Boolean isAutostart

    /**
     * Initialize a new instance of the RtsTask class with task properties.
     */
    RtsTask(TaskType.TaskProperties properties, String nameSuffix) {
        this.name = properties.name + "_" + nameSuffix
        this.priority = properties.priority
        this.stackSize = properties.stackSize
        this.schedule = properties.schedule
        this.type = properties.type
        this.isAutostart = properties.autoStart
    }

    /**
     * Initialize a new instance of the RtsTask class.
     */
    RtsTask(String name, Integer priority, Integer stackSize, RtsOsTaskSchedule schedule, RtsOsTaskType type, Boolean isAutostart) {
        this.name = name
        this.priority = priority
        this.stackSize = stackSize
        this.schedule = schedule
        this.type = type
        this.isAutostart = isAutostart
    }

    /**
     * Return the current value of the property name.
     */
    String getName() {
        return this.name
    }

    /**
     * Return the current value of the property priority.
     */
    Integer getPriority() {
        return this.priority
    }

    /**
     * Return the current value of the property stackSize.
     */
    Integer getStackSize() {
        return this.stackSize
    }

    /**
     * Return the current value of the property schedule.
     */
    RtsOsTaskSchedule getSchedule() {
        return this.schedule
    }

    /**
     * Return the current value of the property type.
     */
    RtsOsTaskType getType() {
        return this.type
    }

    /**
     * Return the current value of the property isAutostart.
     */
    Boolean getIsAutostart() {
        return this.isAutostart
    }
}
