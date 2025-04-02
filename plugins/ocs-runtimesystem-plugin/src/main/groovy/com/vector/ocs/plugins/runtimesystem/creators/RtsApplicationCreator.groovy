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
/*!        \file  RtsApplicationCreator.groovy
 *        \brief  Consists of functions that initialize the different RtsApplication objects with the desired property values.
 *
 *      \details  This class includes:
 *                  - initializer functions for RtsApplication objects
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.runtimesystem.creators

import com.vector.ocs.plugins.runtimesystem.Asil
import com.vector.ocs.plugins.runtimesystem.RuntimeSystemConstants
import com.vector.ocs.plugins.runtimesystem.dataclasses.RtsApplication
import com.vector.ocs.plugins.runtimesystem.dataclasses.RtsCore

/**
 * This class represents the creator functions needed for the RtsApplication objects.
 */
class RtsApplicationCreator {

    /**
     * Initialize and return an instance of the RtsApplication class based on the provided parameters.
     * @param name Sets the property name of the returned object.
     * @param asilLevel Sets the property asilLevel of the returned object.
     * @param isMasterApplication Sets the property isMasterApplication of the returned object.
     * @return initialized RtsApplication instance.
     */
    static RtsApplication initializeApplication(String name, Asil asilLevel) {
        // Create RtsApplication object
        RtsApplication application = new RtsApplication(name, asilLevel)

        // Initialize desired class properties
        //...

        return application
    }

    /**
     * Initialize an application, populate it with tasks depending on the taskTypeList and then return the result.
     * @param asilLevel Sets the property asilLevel of the application.
     * @param taskTypeList List of task types, needed for initializing and adding the correct tasks to the application.
     * @param core RtsCore instance which the application is added to.
     * @return initialized RtsApplication instance including default tasks.
     */
    static RtsApplication setupApplicationForCore(Asil asilLevel, List<RuntimeSystemConstants.Companion.TaskType> taskTypeList, RtsCore core) {
        String applicationName = String.format(RuntimeSystemConstants.applicationProperties.applicationNameSchema, asilLevel.toString(), core.name)
        RtsApplication application = initializeApplication(applicationName, asilLevel)

        //Initialize tasks
        taskTypeList.each {
            application.addTask(RtsTaskCreator.initializeTask(it, applicationName))
        }

        return application
    }
}
