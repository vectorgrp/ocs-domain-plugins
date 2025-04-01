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
/*!        \file  RtsCore.groovy
 *        \brief  Consists of the RtsCore class declaration and its properties.
 *
 *      \details  This class includes:
 *                  - Declaration of class properties
 *                  - Constructor
 *                  - Getter & Setter methods
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.runtimesystem.dataclasses

import com.vector.ocs.plugins.runtimesystem.RuntimeSystemConstants

/**
 * This class represents the Core objects and their needed properties for the internal data model similar to the OsCore class.
 */
class RtsCore {
    private String name
    private Boolean isAutostartCore
    private Boolean isAutosarCore
    private Boolean isMasterCore
    private Integer coreID
    private List<RtsApplication> applications

    /**
     * Initialize a new instance of the RtsCore class with default values.
     */
    RtsCore(Integer coreID, String name) {
        this.coreID = coreID ?: RuntimeSystemConstants.firstCoreID
        this.name = name + coreID.toString()
        this.isAutosarCore = RuntimeSystemConstants.coreProperties.autosarCore
        this.applications = new ArrayList<RtsApplication>()
    }

    /**
     * Initialize a new instance of the RtsCore class with specific values for autostart and master core booleans.
     */
    RtsCore(Integer coreID, String name, Boolean isAutostartCore, Boolean isMasterCore) {
        this.coreID = coreID ?: RuntimeSystemConstants.firstCoreID
        this.name = name + coreID.toString()
        this.isAutosarCore = RuntimeSystemConstants.coreProperties.autosarCore
        this.isAutostartCore = isAutostartCore
        this.isMasterCore = isMasterCore
        this.applications = new ArrayList<RtsApplication>()
    }

    /**
     * Initialize a new instance of the RtsCore class with all properties initialized, ready for handover.
     */
    RtsCore(Integer coreID, String name, Boolean isAutostartCore, Boolean isAutosarCore, Boolean isMasterCore, List<RtsApplication> applications) {
        this.coreID = coreID ?: RuntimeSystemConstants.firstCoreID
        this.name = name
        this.isAutosarCore = isAutosarCore
        this.applications = applications
        this.isAutostartCore = isAutostartCore
        this.isMasterCore = isMasterCore
    }

    /**
     * Set the value of the property isMasterCore.
     * @param value value the property will be set to.
     */
    void setIsMasterCore(Boolean value) {
        this.isMasterCore = value
    }

    /**
     * Return the current value of the property isMasterCore.
     */
    Boolean getIsMasterCore() {
        return this.isMasterCore
    }

    /**
     * Set the value of the property isAutostartCore.
     * @param value value the property will be set to.
     */
    void setIsAutostartCore(Boolean value) {
        this.isAutostartCore = value
    }

    /**
     * Return the current value of the property isAutostartCore.
     */
    Boolean getIsAutostartCore() {
        return this.isAutostartCore
    }

    /**
     * Return the current value of the property isAutosarCore.
     */
    Boolean getIsAutosarCore() {
        return this.isAutosarCore
    }

    /**
     * Return the current value of the property name.
     */
    String getName() {
        return this.name
    }

    /**
     * Return the current value of the property coreID.
     */
    Integer getCoreID() {
        return this.coreID
    }

    /**
     * Add an entry of type RtsApplication to the list property applications.
     * @param application entry that will be added to the list.
     */
    void addApplication(RtsApplication application) {
        this.applications.add(application)
    }

    /**
     * Return a list of the present RtsApplication objects.
     */
    List<RtsApplication> getApplications() {
        return this.applications
    }
}
