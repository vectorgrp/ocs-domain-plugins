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
/*!        \file  EcuStateMachineFeature.groovy
 *        \brief  Subclass of class 'Feature' to handle 'ECU State Machine' feature of 'Ecu State Handling' domain
 *
 *      \details  -
 *
 *********************************************************************************************************************/

package com.vector.ocs.plugins.ecustatemanagement.ecustatehandling

import com.vector.ocs.plugins.ecustatemanagement.Feature

class EcuStateMachineFeature extends Feature{

    /* list of sub-features */
    Boolean demHandling
    CommHandling supportComM
    NvmHandling nvmHandling
    Boolean rteModeSynchronization
    Boolean enableCallouts
    Boolean ecumModeHandling
    Boolean restartEthifSwitchPorts // currently not supported

    /* list of parameters */
    BigDecimal selfRunRequestTimeout
    BigInteger numberOfRunRequestUser
    BigInteger numberOfPostRunRequestUser
    Boolean killAllRunRequestPort

    EcuStateMachineFeature(String name, Boolean isActivated) {
        super(name, isActivated)
        this.demHandling = true
        this.supportComM = new CommHandling(name, isActivated)
        this.nvmHandling = new NvmHandling(name, isActivated)
        this.rteModeSynchronization = true
        this.enableCallouts = true
        this.ecumModeHandling = true
        this.restartEthifSwitchPorts = true
        this.selfRunRequestTimeout = 0.1f
        this.numberOfRunRequestUser = 2
        this.numberOfPostRunRequestUser = 2
        this.killAllRunRequestPort = true
    }
}
