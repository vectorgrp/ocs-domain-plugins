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
/*!     \file   CommunicationXcpConfig.groovy
 *      \brief  The CommunicationXcpConfig configures response PDUs to Rx PDUs in the Xcp module.
 *
 *      \details  Depending on the customer input the plugin will configure following:
 *                  - Find and configure response PDUs to Rx PDUs in the Xcp
 *********************************************************************************************************************/
package com.vector.ocs.plugins.communication

import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.automation.model.ecuc.microsar.xcp.Xcp
import com.vector.cfg.automation.model.ecuc.microsar.xcp.xcpconfig.xcppdu.xcptxpdu.XcpTxPdu
import com.vector.ocs.core.api.OcsLogger

/**
 * Handle the Xcp specific configuration aspects of the CommunicationPlugin.
 */
class CommunicationXcpConfig {
    /**
     * Adds the response Tx Pdu to the Rx Pdu of the Xcp module, if not yet existent
     * @param model input for the processing of the configuration elements
     * @param logger instance for OCS specific logging
     */
    static void CorrectXcpConfig(CommunicationModel model, OcsLogger logger){
        ScriptApi.activeProject(){
            ScriptApi.scriptCode(){
                if(model.correctXcpPdus){
                    transaction{
                        Xcp xcpCfg = bswmdModel(Xcp.DefRef).single
                        List<XcpTxPdu> currentTxPdus = []
                        /* Find all Tx Pdus in the Xcp config */
                        xcpCfg.xcpConfigOrCreate.xcpPdu.each{xcpPdu ->
                            xcpPdu.xcpTxPdu.each{txp->
                                currentTxPdus.add(txp)
                            }
                        }
                        xcpCfg.xcpConfig.xcpPdu.each{xcpPdu ->
                            xcpPdu.xcpRxPdu.each{rpdu ->
                                /* Reference Tx Pdu in Rx Pdu if not yet referenced */
                                if(!rpdu.existsXcpTxPduContRef()){
                                    if(currentTxPdus.size() == 1){
                                        logger.info("Xcp Response PDU " + currentTxPdus.first.shortname + " used for Rx Pdu " + rpdu.shortname + ".")
                                        rpdu.xcpTxPduContRefOrCreate.setRefTarget(currentTxPdus.first)
                                    }else if(currentTxPdus.size() > 1){
                                        /* Give warning that more than one Tx Pdu is available and another one might fit better than the first in the list */
                                        logger.warn("Xcp Response PDU " + currentTxPdus.first.shortname + " used for Rx Pdu " + rpdu.shortname + ". Please verify if another Tx PDU would fit better.")
                                        rpdu.xcpTxPduContRefOrCreate.setRefTarget(currentTxPdus.first)
                                    }else{
                                        logger.error("No Response PDU found - Xcp config corrupt.")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
