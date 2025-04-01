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
/*!     \file   CommunicationComConfig.groovy
 *      \brief  The CommunicationComConfig addresses configuration elements that belong to the communication stack.
 *
 *      \details  Depending on the customer input the plugin will configure following:
 *                  - Set Com Main Functions and cycle times
 *                  - Make PNC functionality work
 *                  - Correct incomplete PDU routing
 *********************************************************************************************************************/
package com.vector.ocs.plugins.communication

import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.automation.model.ecuc.microsar.cdd_asrcdd.cdd.Cdd
import com.vector.cfg.automation.model.ecuc.microsar.cdd_asrcdd.cdd.cddcomstackcontribution.cddpdurupperlayercontribution.CddPduRUpperLayerContribution
import com.vector.cfg.automation.model.ecuc.microsar.com.Com
import com.vector.cfg.automation.model.ecuc.microsar.com.comconfig.comipdu.ComIPdu
import com.vector.cfg.automation.model.ecuc.microsar.com.comconfig.comipdu.comipdudirection.EComIPduDirection
import com.vector.cfg.automation.model.ecuc.microsar.com.comconfig.comipdu.comipdumainfunctionref.ComIPduMainFunctionRef
import com.vector.cfg.automation.model.ecuc.microsar.com.comconfig.commainfunctionrx.ComMainFunctionRx
import com.vector.cfg.automation.model.ecuc.microsar.com.comconfig.commainfunctiontx.ComMainFunctionTx
import com.vector.cfg.automation.model.ecuc.microsar.com.comconfig.comsignal.comsignalaccess.EComSignalAccess
import com.vector.cfg.automation.model.ecuc.microsar.comm.ComM
import com.vector.cfg.automation.model.ecuc.microsar.comm.commconfigset.commpnc.ComMPnc
import com.vector.cfg.automation.model.ecuc.microsar.comm.commconfigset.commpnc.commpnccomsignal.ComMPncComSignal
import com.vector.cfg.automation.model.ecuc.microsar.ecuc.EcuC
import com.vector.cfg.automation.model.ecuc.microsar.ecuc.ecucpducollection.pdu.Pdu
import com.vector.cfg.automation.model.ecuc.microsar.pdur.PduR
import com.vector.cfg.automation.model.ecuc.microsar.pdur.pdurbswmodules.PduRBswModules
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.PluginsCommon

/**
 * Handle the Com specific configuration aspects of the CommunicationPlugin. Handles unconnected Pdus
 */
class CommunicationComConfig {
    /**
     * Configures two MainFunctions. One for Rx and one for Tx. Cycle time will be set to 5 ms.
     * @param model input for the processing of the configuration elements
     * @param logger instance for OCS specific logging
     */
    static void ConfigureMainFunction(CommunicationModel model, OcsLogger logger){
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                if (model.setupComMainFunctions) {
                    Com comCfg = bswmdModel(Com.DefRef).single()
                    /* Setup default Rx/Tx MainFunctions */
                    transaction {
                        logger.info("Setting up comMainFunctionRxDefault with Cycle time " + model.comMainFunctionSettings.defaultMainFunctionRxCycle.toInteger() / 1000 + "ms. Referencing all Com Rx PDUs to it.")
                        ComMainFunctionRx defRxMain = comCfg.comConfig.comMainFunctionRx.byNameOrCreate("comMainFunctionRxDefault")
                        defRxMain.comMainRxTimeBaseOrCreate.value = model.comMainFunctionSettings.defaultMainFunctionRxCycle.toInteger() / 1000
                        logger.info("Setting up comMainFunctionTxDefault with Cycle time " + model.comMainFunctionSettings.defaultMainFunctionTxCycle.toInteger() / 1000 + "ms. Referencing all Com Tx PDUs to it.")
                        ComMainFunctionTx defTxMain = comCfg.comConfig.comMainFunctionTx.byNameOrCreate("comMainFunctionTxDefault")
                        defTxMain.comMainTxTimeBaseOrCreate.value = model.comMainFunctionSettings.defaultMainFunctionTxCycle.toInteger() / 1000
                        /* Reference all signals not referenced by special lists to the default MainFunctions */
                        comCfg.comConfig.comIPdu.each { ComIPdu pdu ->
                            /* Direction is defined by referencing Ipdu - therefore iterate over them */
                            ComIPduMainFunctionRef mainFuncRef = pdu.comIPduMainFunctionRefOrCreate
                            if (pdu.comIPduDirection.value == EComIPduDirection.RECEIVE) {
                                mainFuncRef.setRefTargetMdf(defRxMain.mdfObject)
                            } else {
                                mainFuncRef.setRefTargetMdf(defTxMain.mdfObject)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Sets the signal access of Pnc signals to ACCESS_NEEDED_BY_OTHER
     * @param model input for the processing of the configuration elements
     * @param logger instance for OCS specific logging
     */
    static void ConfigureComMSignalAccess(CommunicationModel model, OcsLogger logger){
        ScriptApi.activeProject() {
            ScriptApi.scriptCode() {
                if (model.setupComMSignalAccess) {
                    logger.info("Marking PNC Signals referenced by ComM as used.")
                    transaction {
                        ComM commCfg = bswmdModel(ComM.DefRef).single()
                        /* Loop over all PNCs */
                        commCfg.comMConfigSet.comMPnc.each { ComMPnc pnc ->
                            /* Loop over all PNC Com Signals */
                            pnc.comMPncComSignal.each { ComMPncComSignal pcs ->
                                if (pcs.existsComMPncComSignalRef()) {
                                    logger.info("Setting " + pcs.comMPncComSignalRef.refTarget.shortname + " Signal Access to ACCESS_NEEDED_BY_OTHER, thus used by ComM.")
                                    pcs.comMPncComSignalRef.refTarget.comSignalAccessOrCreate.value = EComSignalAccess.ACCESS_NEEDED_BY_OTHER
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks for unconnected Pdus and creates a Cdd to connect these Pdus.
     * @param model input for the processing of the configuration elements
     * @param logger instance for OCS specific logging
     */
    static void ConfigureUnconnectedPdus(CommunicationModel model, OcsLogger logger){
        ScriptApi.activeProject() {
            ScriptApi.scriptCode() {
                if(model.stubUnconnectedPDUs && PluginsCommon.DefRefPresent(ScriptApi.activeProject(), "/MICROSAR/Cdd_AsrCdd/Cdd", logger)){
                    List<Pdu> defectivePdus = []
                    List<Pdu> pdus2Stub_ULPduRIfRx = []
                    List<Pdu> pdus2Stub_ULPduRIfTx = []
                    List<Pdu> pdus2Stub_ULPduRTpRx = []
                    List<Pdu> pdus2Stub_ULPduRTpTx = []

                    EcuC ecucC = bswmdModel(EcuC.DefRef).single
                    ecucC.ecucPduCollection.get().pdu.each{pdu->
                        /* Searching defective EcuC Pdus */
                        pdu.validationResults.each{vr->
                            if(vr.id.origin == "PDUR" && ((vr.id.id == 12506)||(vr.id.id == 7010)||(vr.id.id == 7000))){
                                defectivePdus.add(pdu)
                            }
                        }
                        /* Searching incomplete objects referencing to the PDUs */
                        pdu.referencesPointingToMe.each{rptm ->
                            rptm.validationResults.each{vri ->
                                if(vri.id.origin == "PDUR" && ((vri.id.id == 12506)||(vri.id.id == 7010)||(vri.id.id == 7000))){
                                    defectivePdus.add(pdu)
                                }
                            }
                        }
                    }
                    /* Defective PDUs identified - lets see if we can distinguish which type they are.. */
                    defectivePdus.each{dpdu ->
                        String pduType = CommunicationPdu.determinePduType(dpdu, logger)
                        logger.info("PDU " + dpdu.shortname + " has an incomplete connection. If possible (and only one connection is missing), then this PDU will be 'stubbed' and added to the module Cdd_Com or Cdd_Tp).")
                        if(pduType == "IfRx"){
                            pdus2Stub_ULPduRIfRx.add(dpdu)
                        }else if(pduType == "IfTx"){
                            pdus2Stub_ULPduRIfTx.add(dpdu)
                        }else if(pduType == "TpRx"){
                            pdus2Stub_ULPduRTpRx.add(dpdu)
                        }else if(pduType == "TpTx"){
                            pdus2Stub_ULPduRTpTx.add(dpdu)
                        }else{
                            logger.error("Pdu " + dpdu.shortname + " Type could not be determined.")
                        }
                        /* Already present OCS Cdds */
                        List<String> activeModules = []
                        activeEcuc.modules(Cdd.DefRef).each{ cdd->
                            activeModules.add(cdd.name)
                        }
                        /* Create Cdds if not already existent in the config */
                        if(pdus2Stub_ULPduRIfRx.size() != 0 || pdus2Stub_ULPduRIfTx.size() != 0){
                            /* Add or use PduRIfUpperLayerCdd */
                            logger.info("Inserting If Cdd Cdd_Com")
                            if(!activeModules.contains("Cdd_Com")){
                                transaction{
                                    try {
                                        operations.activateModuleConfiguration(Cdd.DefRef, "Cdd_Com")
                                    } catch (IllegalArgumentException exception) {
                                        logger.error("$exception" + " A problem occurred with the passed DefRef.")
                                    } catch (IllegalStateException exception) {
                                        logger.error("$exception" + " Activation of the module configuration failed inside the model.")
                                    }
                                }
                            }
                        }
                        if(pdus2Stub_ULPduRTpRx.size() != 0 || pdus2Stub_ULPduRTpTx.size() != 0){
                            /* Add or use PduRTpUpperLayerCdd */
                            logger.info("Inserting Tp Cdd Cdd_Tp")
                            if(!activeModules.contains("Cdd_Tp")){
                                transaction{
                                    try {
                                        operations.activateModuleConfiguration(Cdd.DefRef, "Cdd_Tp")
                                    } catch (IllegalArgumentException exception) {
                                        logger.error("$exception" + " A problem occurred with the passed DefRef.")
                                    } catch (IllegalStateException exception) {
                                        logger.error("$exception" + " Activation of the module configuration failed inside the model.")
                                    }
                                }
                            }
                        }
                        List<Cdd> cddCfg = bswmdModel(Cdd.DefRef)
                        PduR pduRCfg = bswmdModel(PduR.DefRef).single
                        transaction {
                            cddCfg.each{cddInst ->
                                if(cddInst.shortname == "Cdd_Com"){
                                    /* Add Cdd Stub to PduR */
                                    PduRBswModules ifStubPduRModule = pduRCfg.pduRBswModules.byNameOrCreate("Cdd_Com")
                                    ifStubPduRModule.pduRUpperModuleOrCreate.setValue(true)
                                    ifStubPduRModule.pduRCommunicationInterfaceOrCreate.setValue(true)
                                    ifStubPduRModule.pduRBswModuleRefOrCreate.setRefTargetMdf(cddInst.mdfObject)
                                    /* Stub If Pdus */
                                    CddPduRUpperLayerContribution ulPduR = cddInst.cddComStackContributionOrCreate.cddPduRUpperLayerContributionOrCreate
                                    pdus2Stub_ULPduRIfRx.each{pdu->
                                        logger.info("Inserting Cdd Rx Pdu " + "OCS_IfRx_" + pdu.shortname + " into Cdd_Com.")
                                        ulPduR.cddPduRUpperLayerRxPdu.byNameOrCreate("OCS_IfRx_" + pdu.shortname).cddPduRUpperLayerPduRefOrCreate.setRefTarget(pdu)
                                    }
                                    pdus2Stub_ULPduRIfTx.each{pdu->
                                        logger.info("Inserting Cdd Tx Pdu " + "OCS_IfTx_" + pdu.shortname + " into Cdd_Com.")
                                        ulPduR.cddPduRUpperLayerTxPdu.byNameOrCreate("OCS_IfTx_" + pdu.shortname).cddPduRUpperLayerPduRefOrCreate.setRefTarget(pdu)
                                    }
                                } else if(cddInst.shortname == "Cdd_Tp"){
                                    /* Add Cdd Stub to PduR */
                                    PduRBswModules ifStubPduRModule = pduRCfg.pduRBswModules.byNameOrCreate("Cdd_Tp")
                                    ifStubPduRModule.pduRUpperModuleOrCreate.setValue(true)
                                    ifStubPduRModule.pduRTransportProtocolOrCreate.setValue(true)
                                    ifStubPduRModule.pduRBswModuleRefOrCreate.setRefTargetMdf(cddInst.mdfObject)
                                    /* Stub Tp Pdus */
                                    CddPduRUpperLayerContribution ulPduR = cddInst.cddComStackContributionOrCreate.cddPduRUpperLayerContributionOrCreate
                                    pdus2Stub_ULPduRTpRx.each{pdu->
                                        logger.info("Inserting Cdd Tp Rx Pdu " + "OCS_TpRx_" + pdu.shortname + " into Cdd_Tp.")
                                        ulPduR.cddPduRUpperLayerRxPdu.byNameOrCreate("OCS_TpRx_" + pdu.shortname).cddPduRUpperLayerPduRefOrCreate.setRefTarget(pdu)
                                    }
                                    pdus2Stub_ULPduRTpTx.each{pdu->
                                        logger.info("Inserting Cdd Tp Tx Pdu " + "OCS_TpTx_" + pdu.shortname + " into Cdd_Tp.")
                                        ulPduR.cddPduRUpperLayerTxPdu.byNameOrCreate("OCS_TpTx_" + pdu.shortname).cddPduRUpperLayerPduRefOrCreate.setRefTarget(pdu)
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
