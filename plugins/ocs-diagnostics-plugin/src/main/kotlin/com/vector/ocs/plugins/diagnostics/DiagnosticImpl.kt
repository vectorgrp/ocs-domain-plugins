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
/*!        \file  DiagnosticImpl.kt
 *        \brief  Entry point for the Diagnostics plugin that initializes adapters and calls services.
 *
 *      \details
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.diagnostics

import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.plugins.diagnostics.adapter.out.cfg5API.Cfg5ApiDiagnosticsAdapter
import com.vector.ocs.lib.shared.PluginsCommon
import com.vector.ocs.plugins.diagnostics.application.service.ConfigureDiagnosticsService
import com.vector.ocs.plugins.diagnostics.application.service.LoggerService
import com.vector.ocs.plugins.diagnostics.application.service.SolvingActionService
import com.vector.ocs.plugins.diagnostics.constants.DcmDefRefs.DcmDefRefConstantsFactory

internal class DiagnosticImpl {

    companion object {

        private val dcmDefRefs = DcmDefRefConstantsFactory.getConstants()

        /**
         * Initializes adapters and calls services
         * @param model DiagnosticsModel object
         * @param ocsLogger OcsLogger object
         */
        @JvmStatic
        fun runConfiguration(model: DiagnosticsModel, ocsLogger: OcsLogger) {
            if (PluginsCommon.ConfigPresent(dcmDefRefs.DCM)) {
                // Initialize adapters
                val logger = LoggerService(ocsLogger)
                val cfg5ServiceApi = Cfg5ApiDiagnosticsAdapter()
                val configureDiagnosticsService = ConfigureDiagnosticsService(cfg5ServiceApi, model, logger)
                val solvingActionService = SolvingActionService(cfg5ServiceApi)

                // Call services
                configureDiagnosticsService.configureDiagnostics(ocsLogger)
                solvingActionService.executeSolvingActions(ocsLogger)
            }
        }
    }
}
