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
/*!        \file  RtsCoreCreator.groovy
 *        \brief  Consists of functions that initialize the different RtsCore objects with the desired property values.
 *
 *      \details  This class includes:
 *                  - initializer functions for RtsCore objects
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.runtimesystem.creators

import com.vector.ocs.plugins.runtimesystem.Asil
import com.vector.ocs.plugins.runtimesystem.RuntimeSystemConstants
import com.vector.ocs.plugins.runtimesystem.dataclasses.RtsCore

/**
 * This class represents the creator functions needed for the RtsCore objects.
 */
class RtsCoreCreator {
    /**
     * Initialize an instance of the RtsCore class that can be used in a default single/multi core configuration.
     * Depending on the provided coreID the properties isMasterCore and isAutostartCore are set correspondingly.
     * Depending on the provided index the core an be used as single core / master core.
     * @param coreID Sets the property coreID of the returned object.
     * @param index of core in the list of OsPhysicalCores (index 0 will be the masterCore/single core).
     * @param coreName Sets the property coreName of the returned object.
     * @return initialized RtsCore instance.
     */
    static RtsCore initializeCore(Integer coreID, Integer index, String coreName) {
        RtsCore osCore
        if (index == 0) {
            osCore = new RtsCore(coreID, coreName, true, true)
        } else {
            osCore = new RtsCore(coreID, coreName, false, false)
        }
        /* Initialize applications including tasks */
        RuntimeSystemConstants.applicationMap.each { Asil asil, List<RuntimeSystemConstants.Companion.TaskType> taskTypeList ->
            osCore.addApplication(RtsApplicationCreator.setupApplicationForCore(asil, taskTypeList, osCore))
        }
        return osCore
    }
}
