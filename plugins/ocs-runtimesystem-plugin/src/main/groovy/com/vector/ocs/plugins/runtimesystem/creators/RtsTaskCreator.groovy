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
/*!        \file  RtsTaskCreator.groovy
 *        \brief  Consists of functions that initialize RtsTask objects with the desired property values.
 *
 *      \details  This class includes:
 *                  - initializer functions for RtsTask objects
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.runtimesystem.creators

import com.vector.ocs.plugins.runtimesystem.RuntimeSystemConstants
import com.vector.ocs.plugins.runtimesystem.dataclasses.RtsTask
import com.vector.ocs.plugins.runtimesystem.RuntimeSystemConstants.Companion.TaskType

/**
 * This class represents the creator functions needed for the RtsTask objects.
 */
class RtsTaskCreator {

    /**
     * Initialize an instance of the RtsTask class based on the provided type parameter.
     * Each type parameter corresponds to an individual setting of the RtsTask properties.
     * @param type Provided task type that corresponds to an default setting of the RtsTask instance properties.
     * @return initialized RtsTask instance with corresponding default values for provided task type.
     */
    static RtsTask initializeTask(TaskType type, String nameSuffix) {
        switch (type) {
            case TaskType.SystemInitTask:
                return new RtsTask(RuntimeSystemConstants.taskPropertiesMap[TaskType.SystemInitTask], nameSuffix)
            case TaskType.ApplInitTask:
                return new RtsTask(RuntimeSystemConstants.taskPropertiesMap[TaskType.ApplInitTask], nameSuffix)
            case TaskType.BswTask:
                return new RtsTask(RuntimeSystemConstants.taskPropertiesMap[TaskType.BswTask], nameSuffix)
            case TaskType.ApplTask:
                return new RtsTask(RuntimeSystemConstants.taskPropertiesMap[TaskType.ApplTask], nameSuffix)
            default:
                return null
        }
    }
}
