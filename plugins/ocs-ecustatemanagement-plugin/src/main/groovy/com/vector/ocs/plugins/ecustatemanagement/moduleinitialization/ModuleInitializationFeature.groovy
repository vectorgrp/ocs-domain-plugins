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
/*!        \file  ModuleInitializationFeature.groovy
 *        \brief  Subclass of class 'Feature' to handle processing of the Mode Management domain 'Module Initialization'
 *
 *      \details
 *
 *********************************************************************************************************************/

package com.vector.ocs.plugins.ecustatemanagement.moduleinitialization

import com.vector.ocs.plugins.ecustatemanagement.Feature

/**
 * Subclass of class Feature to handle processing of init functions in known and foreign modules list.
 */
class ModuleInitializationFeature extends Feature{
    /* lists of module init functions */
    List<Feature> knownModulesList
    List<Feature> foreignModulesList
    Boolean executeNvMReadAll
    Boolean enableInterrupts
    // currently not supported
    Boolean restoreDefaultSequence

    ModuleInitializationFeature(String name, Boolean isActivated) {
        super(name, isActivated)
        this.knownModulesList = new LinkedList<Feature>()
        this.foreignModulesList = new LinkedList<Feature>()
        this.executeNvMReadAll = true
        this.enableInterrupts = true
        this.restoreDefaultSequence = true
    }

    /**
     * Reset method to reset the object for MultiPartition use case.
     */
    void reset() {
        this.name = ""
        this.isActivated = true
        this.knownModulesList.clear()
        this.foreignModulesList.clear()
        this.executeNvMReadAll = true
        this.enableInterrupts = true
        this.restoreDefaultSequence = true
    }
}
