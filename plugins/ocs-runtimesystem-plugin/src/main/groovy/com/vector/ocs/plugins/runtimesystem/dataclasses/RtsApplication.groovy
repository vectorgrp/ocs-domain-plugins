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
/*!        \file  RtsApplication.groovy
 *        \brief  Consists of the RtsApplication class declaration and its properties.
 *
 *      \details  This class includes:
 *                  - Declaration of class properties
 *                  - Constructor
 *                  - Getter & Setter methods
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.runtimesystem.dataclasses

import com.vector.ocs.plugins.runtimesystem.Asil

/**
 * This class represents the Application objects and their needed properties for the internal data model similar to the OsApplication class.
 */
class RtsApplication {
    private String name
    private Integer memProtectionID
    private Asil asilLevel
    private List<RtsTask> tasks

    /**
     * Initialize a new instance of the RtsApplication class with designated ASIL level.
     */
    RtsApplication(String name, Asil asil) {
        this.asilLevel = asil
        this.name = name
        this.tasks = new ArrayList<RtsTask>()
    }

    /**
     * Initialize a new instance of the RtsApplication class with designated ASIL level and list of tasks.
     */
    RtsApplication(String name, Asil asil, List<RtsTask> tasks) {
        this.asilLevel = asil
        this.name = name
        this.tasks = tasks
    }

    /**
     * This function adds an entry of type RtsTask to the list property tasks.
     * @param task entry that will be added to the list.
     */
    void addTask(RtsTask task) {
        this.tasks.add(task)
    }

    /**
     * Return a list of the present RtsTask objects.
     */
    List<RtsTask> getTasks() {
        return this.tasks
    }

    /**
     * Return the current value of the property name.
     */
    String getName() {
        return this.name
    }

    /**
     * Return the current value of the property memProtectionID.
     */
    Integer getMemProtectionID() {
        return this.memProtectionID
    }

    /**
     * Return the current value of the property asilLevel.
     */
    Asil getAsilLevel() {
        return this.asilLevel
    }

}
