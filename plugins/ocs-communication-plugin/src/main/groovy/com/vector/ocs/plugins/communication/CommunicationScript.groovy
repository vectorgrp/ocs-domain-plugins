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
/*      \file   CommunicationScript.groovy
 *      \brief  The Communication Plugin addresses configuration elements that belong to the communication stack.
 *
 *      \details  Depending on the customer input the plugin will configure following:
 *                  - Set Com Main Functions and cycle times
 *                  - Make PNC functionality work
 *                  - Correct incomplete PDU routing
 *                  - Create or set up default routing activation for DoIP
 *                  - Create Arp Config
 *                  - Find and configure response PDUs to Rx PDUs in the Xcp
 *********************************************************************************************************************/

package com.vector.ocs.plugins.communication

import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.automation.scripting.api.project.IProject
import com.vector.cfg.consistency.ui.ISolvingActionUI
import com.vector.cfg.consistency.ui.IValidationResultUI
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.PluginsCommon
import groovy.transform.PackageScope

/**
 * The CommunicationScript class address the configuration of the communication stack modules.
 */
@PackageScope
class CommunicationScript {
    /**
     * Calls functions to configure the communication stack.
     * @param model input for the processing of the configuration elements
     * @param logger instance for OCS specific logging
     */
    static void run(CommunicationModel model, OcsLogger logger) {
        logger.info("Start processing of the CommunicationPlugin model.")
        logger.info("Check that Com and ComM module are available.")
        if (PluginsCommon.ConfigPresent(CommunicationConstants.COM_DEFREF) && PluginsCommon.ConfigPresent(CommunicationConstants.COMM_DEFREF)) {
            CommunicationComConfig.ConfigureMainFunction(model, logger)
            CommunicationComConfig.ConfigureComMSignalAccess(model, logger)
            CommunicationComConfig.ConfigureUnconnectedPdus(model, logger)
        }

        logger.info("Check that DoIP module is available.")
        if (PluginsCommon.ConfigPresent(CommunicationConstants.DOIP_DEFREF) && model.autoExtendEthStack) {
            CommunicationEthConfig.ConfigureDoIP(logger)
        }
        logger.info("Check that TcpIp module is available.")
        if (PluginsCommon.ConfigPresent(CommunicationConstants.TCPIP_DEFREF) && model.autoExtendEthStack) {
            CommunicationEthConfig.ConfigureTcpIp(logger)
        }
        logger.info("Check that SoAd module is available.")
        if (PluginsCommon.ConfigPresent(CommunicationConstants.SOAD_DEFREF) && model.autoExtendEthStack) {
            CommunicationEthConfig.ConfigureSoAd(logger)
        }

        logger.info("Check that Xcp module is available.")
        if (PluginsCommon.ConfigPresent(CommunicationConstants.XCP_DEFREF)) {
            CommunicationXcpConfig.CorrectXcpConfig(model, logger)
        }
    }

    /**
     * Cleanup function to trigger all necessary solving actions to fix communication errors
     * @param logger Logger for logging messages
     */
    static void cleanup(OcsLogger logger) {
        logger.info("Start processing of the validation errors (more details on 'DEBUG' log level).")
        ScriptApi.activeProject() { IProject project ->
            validation {
                final String COM = "COM"
                final String PDUR = "PDUR"
                Collection<IValidationResultUI> pdur12506Results = validationResults.findAll { IValidationResultUI iValidationResults ->
                    iValidationResults.isId(PDUR, 12506) && iValidationResults.isActive()
                }
                if (!pdur12506Results.isEmpty()) {
                    logger.info("Trying to solve PDUR12506 configuration errors.")
                    solver.solve {
                        result {
                            isId(PDUR, 12506)
                        }.withAction {
                            containsString("Create a new routing path")
                        }
                    }
                    logger.info("Solved PDUR12506 configuration errors by creating new routing paths.")
                }
                Collection<IValidationResultUI> pdur13200Results = validationResults.findAll { IValidationResultUI iValidationResults ->
                    iValidationResults.isId(PDUR, 13200) && iValidationResults.isActive()
                }
                if (!pdur13200Results.isEmpty()) {
                    logger.info("Trying to solve PDUR13200 configuration errors.")
                    solver.solve {
                        result {
                            isId(PDUR, 13200)
                        }.withAction {
                            containsString("IGNORE")
                        }
                    }
                    logger.info("Solved PDUR13200 configuration errors by Pdu Length Handling Strategy to IGNORE.")
                }
                Collection<IValidationResultUI> pdur10510Results = validationResults.findAll { IValidationResultUI iValidationResults ->
                    iValidationResults.isId(PDUR, 10510) && iValidationResults.isActive()
                    iValidationResults.erroneousCEs
                }
                if (!pdur10510Results.isEmpty()) {
                    logger.info("Trying to solve PDUR10510 configuration errors.")
                    solver.solve {
                        result {
                            isId(PDUR, 10510)
                        }.withAction {
                            solvingActions.first
                        }
                    }
                    logger.info("Solved PDUR10510 configuration errors by updating the DestPduDataProvision.")
                }
                Collection<IValidationResultUI> pdur10520Results = validationResults.findAll { IValidationResultUI iValidationResults ->
                    iValidationResults.isId(PDUR, 10520) && iValidationResults.isActive()
                }
                if (!pdur10520Results.isEmpty()) {
                    logger.info("Trying to solve PDUR10520 configuration errors.")
                    solver.solve {
                        result { ISolvingActionUI
                            isId(PDUR, 10520)
                        }.withAction {
                            containsString("false")
                        }
                    }
                    logger.info("Solved PDUR10520 configuration errors by setting Transmission Confirmation to false.")
                }
                Collection<IValidationResultUI> com2304Results = validationResults.findAll { IValidationResultUI iValidationResults ->
                    iValidationResults.isId(COM, 2304) && iValidationResults.isActive()
                }
                if (!com2304Results.isEmpty()) {
                    logger.info("Trying to solve COM2304 configuration errors.")
                    solver.solve {
                        result {
                            isId(COM, 2304)
                        }.withAction {
                            solvingActions.first
                        }
                    }
                    logger.info("Solved COM2304 configuration errors by creating Transfer Properties.")
                }
            }

        }
        logger.info("Processing of the validation errors done.")
    }
}
