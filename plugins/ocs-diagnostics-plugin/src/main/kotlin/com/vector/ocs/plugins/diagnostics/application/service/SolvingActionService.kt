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
/*!        \file  SolvingActionService.kt
 *        \brief  Service responsible for implementing use cases of port SolvingActionUseCase.
 *
 *      \details
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.diagnostics.application.service

import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.plugins.diagnostics.application.port.`in`.SolvingActionUseCase
import com.vector.ocs.plugins.diagnostics.application.port.out.cfgAPI.ConfigServiceAPI
import com.vector.ocs.plugins.diagnostics.constants.DcmDefRefs.DcmDefRefConstantsFactory
import com.vector.ocs.plugins.diagnostics.constants.DemDefRefs.DemDefRefConstantsFactory

class SolvingActionService(private val configServiceApi: ConfigServiceAPI) :SolvingActionUseCase {

    private val dcmDefRefs = DcmDefRefConstantsFactory.getConstants()
    private val demDefRefs = DemDefRefConstantsFactory.getConstants()

    /**
     * Execute the solving actions
     * @param logger OcsLogger object
     * */
    override fun executeSolvingActions(logger: OcsLogger){
        configServiceApi.validate(dcmDefRefs.DCM, demDefRefs.DEM)
        configServiceApi.solve("DCM", 25000, logger)
    }
}
