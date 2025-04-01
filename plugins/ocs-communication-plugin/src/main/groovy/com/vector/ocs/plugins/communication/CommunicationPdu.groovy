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
/*!     \file   CommunicationPdu.groovy
 *      \brief  The CommunicationPdu provides functions to check the reference of Pdus to the communication modules.
 *
 *      \details
 *********************************************************************************************************************/
package com.vector.ocs.plugins.communication

import com.vector.cfg.automation.model.ecuc.microsar.canif.canifinitcfg.canifrxpducfg.canifrxpduref.CanIfRxPduRef
import com.vector.cfg.automation.model.ecuc.microsar.canif.canifinitcfg.caniftxpducfg.caniftxpduref.CanIfTxPduRef
import com.vector.cfg.automation.model.ecuc.microsar.cantp.cantpconfig.cantpchannel.cantprxnsdu.cantprxnsduref.CanTpRxNSduRef
import com.vector.cfg.automation.model.ecuc.microsar.cantp.cantpconfig.cantpchannel.cantptxnsdu.cantptxnsduref.CanTpTxNSduRef
import com.vector.cfg.automation.model.ecuc.microsar.ecuc.ecucpducollection.pdu.Pdu
import com.vector.cfg.automation.model.ecuc.microsar.frif.frifconfig.frifpdu.frifpdudirection.frifrxpdu.frifrxpduref.FrIfRxPduRef
import com.vector.cfg.automation.model.ecuc.microsar.frif.frifconfig.frifpdu.frifpdudirection.friftxpdu.friftxpduref.FrIfTxPduRef
import com.vector.cfg.automation.model.ecuc.microsar.frtp.frtpmultipleconfig.frtpconnection.frtprxsdu.frtprxsduref.FrTpRxSduRef
import com.vector.cfg.automation.model.ecuc.microsar.frtp.frtpmultipleconfig.frtpconnection.frtptxsdu.frtptxsduref.FrTpTxSduRef
import com.vector.cfg.automation.model.ecuc.microsar.ipdum.ipdumconfig.ipdumcontainedrxpdu.ipdumcontainedrxpduref.IpduMContainedRxPduRef
import com.vector.cfg.automation.model.ecuc.microsar.ipdum.ipdumconfig.ipdumcontainedtxpdu.ipdumcontainedtxpduref.IpduMContainedTxPduRef
import com.vector.cfg.automation.model.ecuc.microsar.ipdum.ipdumconfig.ipdumcontainerrxpdu.ipdumcontainerrxpduref.IpduMContainerRxPduRef
import com.vector.cfg.automation.model.ecuc.microsar.ipdum.ipdumconfig.ipdumcontainertxpdu.ipdumcontainertxpduref.IpduMContainerTxPduRef
import com.vector.cfg.automation.model.ecuc.microsar.linif.linifglobalconfig.linifchannel.linifframe.linifpdudirection.linifrxpdu.linifrxpduref.LinIfRxPduRef
import com.vector.cfg.automation.model.ecuc.microsar.linif.linifglobalconfig.linifchannel.linifframe.linifpdudirection.liniftxpdu.liniftxpduref.LinIfTxPduRef
import com.vector.cfg.automation.model.ecuc.microsar.lintp.lintpglobalconfig.lintprxnsdu.lintprxnsdupduref.LinTpRxNSduPduRef
import com.vector.cfg.automation.model.ecuc.microsar.lintp.lintpglobalconfig.lintptxnsdu.lintptxnsdupduref.LinTpTxNSduPduRef
import com.vector.cfg.automation.model.ecuc.microsar.pdur.pdurroutingtables.pdurroutingtable.pdurroutingpath.pdurdestpdu.pdurdestpduref.PduRDestPduRef
import com.vector.cfg.automation.model.ecuc.microsar.pdur.pdurroutingtables.pdurroutingtable.pdurroutingpath.pdursrcpdu.PduRSrcPdu
import com.vector.cfg.automation.model.ecuc.microsar.pdur.pdurroutingtables.pdurroutingtable.pdurroutingpath.pdursrcpdu.pdursrcpduref.PduRSrcPduRef
import com.vector.cfg.automation.model.ecuc.microsar.secoc.secocrxpduprocessing.secocrxauthenticpdulayer.secocrxauthenticlayerpduref.SecOCRxAuthenticLayerPduRef
import com.vector.cfg.automation.model.ecuc.microsar.secoc.secocrxpduprocessing.secocrxsecuredpdulayer.secocrxsecuredpdu.secocrxsecuredlayerpduref.SecOCRxSecuredLayerPduRef
import com.vector.cfg.automation.model.ecuc.microsar.secoc.secoctxpduprocessing.secoctxauthenticpdulayer.secoctxauthenticlayerpduref.SecOCTxAuthenticLayerPduRef
import com.vector.cfg.automation.model.ecuc.microsar.secoc.secoctxpduprocessing.secoctxsecuredpdulayer.secoctxsecuredpdu.secoctxsecuredlayerpduref.SecOCTxSecuredLayerPduRef
import com.vector.cfg.automation.model.ecuc.microsar.soad.soadconfig.soadpduroute.soadtxpduref.SoAdTxPduRef
import com.vector.cfg.automation.model.ecuc.microsar.soad.soadconfig.soadsocketroute.soadsocketroutedest.soadrxpduref.SoAdRxPduRef
import com.vector.cfg.gen.core.bswmdmodel.GIReferenceToContainer
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.PluginsCommon
import groovy.transform.PackageScope

/**
 * Helper functions to determine the PduTypes
 */
@PackageScope
class CommunicationPdu {
    /**
     * The determinePduTypeInnerSection method checks if a Pdu is referenced by CanIf, FrIf, LinIf, SoAd, SecOC or IpduM. If not the return value will be set to "N/A"
     * @param dPdu object from EcucPduCollection
     * @return string which state if the Pdu is referenced by CanIf, FrIf, LinIf, SoAd, SecOC or IpduM
     */
    static String determinePduTypeInnerSection(Pdu dPdu, OcsLogger logger) {
        String retString = "N/A"
        dPdu.referencesPointingToMe.each { rptMe ->
            /* Easiest case: direct reference */
            try {
                if (PluginsCommon.ConfigPresent(CommunicationConstants.CANIF_DEFREF)) {
                    if (rptMe.isInstanceOfDefRef(CanIfRxPduRef.DefRef)) {
                        retString = "IfRx"
                    }
                    if (rptMe.isInstanceOfDefRef(CanIfTxPduRef.DefRef)) {
                        retString = "IfTx"
                    }
                    if (rptMe.isInstanceOfDefRef(CanTpRxNSduRef.DefRef)) {
                        retString = "TpRx"
                    }
                    if (rptMe.isInstanceOfDefRef(CanTpTxNSduRef.DefRef)) {
                        retString = "TpTx"
                    }
                }
                if (PluginsCommon.ConfigPresent(CommunicationConstants.FRIF_DEFREF)) {
                    if (rptMe.isInstanceOfDefRef(FrIfRxPduRef.DefRef)) {
                        retString = "IfRx"
                    }
                    if (rptMe.isInstanceOfDefRef(FrIfTxPduRef.DefRef)) {
                        retString = "IfTx"
                    }
                    if (rptMe.isInstanceOfDefRef(FrTpRxSduRef.DefRef)) {
                        retString = "TpRx"
                    }
                    if (rptMe.isInstanceOfDefRef(FrTpTxSduRef.DefRef)) {
                        retString = "TpTx"
                    }
                }
                if (PluginsCommon.ConfigPresent(CommunicationConstants.LINIF_DEFREF)) {
                    if (rptMe.isInstanceOfDefRef(LinIfRxPduRef.DefRef)) {
                        retString = "IfRx"
                    }
                    if (rptMe.isInstanceOfDefRef(LinIfTxPduRef.DefRef)) {
                        retString = "IfTx"
                    }
                    if (rptMe.isInstanceOfDefRef(LinTpRxNSduPduRef.DefRef)) {
                        retString = "TpRx"
                    }
                    if (rptMe.isInstanceOfDefRef(LinTpTxNSduPduRef.DefRef)) {
                        retString = "TpTx"
                    }
                }
                if (PluginsCommon.ConfigPresent(CommunicationConstants.SOAD_DEFREF)) {
                    if (rptMe.isInstanceOfDefRef(SoAdTxPduRef.DefRef)) {
                        SoAdTxPduRef TxPduRef = (SoAdTxPduRef) rptMe
                        if (TxPduRef.parent.soAdTxUpperLayerType.value.toString() == "TP") {
                            retString = "TpTx"
                        } else {
                            retString = "IfTx"
                        }
                    }
                    if (rptMe.isInstanceOfDefRef(SoAdRxPduRef.DefRef)) {
                        SoAdRxPduRef RxPduRef = (SoAdRxPduRef) rptMe
                        if (RxPduRef.parent.soAdRxUpperLayerType.value.toString() == "TP") {
                            retString = "TpRx"
                        } else {
                            retString = "IfRx"
                        }
                    }
                }
                if (PluginsCommon.ConfigPresent(CommunicationConstants.SECOC_DEFREF)) {
                    if ((rptMe.isInstanceOfDefRef(SecOCRxSecuredLayerPduRef.DefRef)) || (rptMe.isInstanceOfDefRef(SecOCRxAuthenticLayerPduRef.DefRef))) {
                        retString = "IfRx"
                    }
                    if ((rptMe.isInstanceOfDefRef(SecOCTxAuthenticLayerPduRef.DefRef)) || (rptMe.isInstanceOfDefRef(SecOCTxSecuredLayerPduRef.DefRef))) {
                        retString = "IfTx"
                    }
                }
                if (PluginsCommon.ConfigPresent(CommunicationConstants.IPDUM_DEFREF)) {
                    if ((rptMe.isInstanceOfDefRef(IpduMContainedRxPduRef.DefRef)) || (rptMe.isInstanceOfDefRef(IpduMContainerRxPduRef.DefRef))) {
                        retString = "IfRx"
                    }
                    if ((rptMe.isInstanceOfDefRef(IpduMContainedTxPduRef.DefRef)) || (rptMe.isInstanceOfDefRef(IpduMContainerTxPduRef.DefRef))) {
                        retString = "IfTx"
                    }
                }
            } catch (NullPointerException exception) {
                logger.error("$exception")
            }
        }
        return retString
    }

    /**
     * The determinePduType method checks the Pdu Type of not completely connected Pdus
     * @param dPdu object from EcucPduCollection
     * @param logger instance for OCS specific logging
     * @return string which state if the Pdu is referenced by CanIf, FrIf, LinIf, SoAd, SecOC, IpduM or PduR
     */
    static String determinePduType(Pdu dPdu, OcsLogger logger) {
        String retString = "N/A"
        logger.info("Determining Pdu Type of not completely connected PDU " + dPdu.shortname + ".")
        retString = determinePduTypeInnerSection(dPdu, logger)
        if (retString == "N/A") {
            dPdu.referencesPointingToMe.each { GIReferenceToContainer rptMe ->
                /* More difficult case: reference from PduR defective */
                try {
                    if ((rptMe.isInstanceOfDefRef(PduRSrcPduRef.DefRef))) {
                        PduRSrcPduRef mySrcRef = rptMe
                        if (!mySrcRef.parent.existsPduRSrcPduPduRBswModulesRef()) {
                            /* Module of Src cannot be determined - if a Dst Pdu is dir Transmit, then transmit, if another dest is rx or only rx, then Rx */
                            mySrcRef.parent.parent.pduRDestPdu.each { destPdu ->
                                /* Check which Dest Pdu is defective and has Module Ref and Direction */
                                if (destPdu.existsPduRDestPduDirection()
                                        && destPdu.existsPduRDestPduPduRBswModulesRef()
                                        && destPdu.existsPduR_PduRRoutingTables_PduRRoutingTable_PduRRoutingPath_PduRDestPdu_PduRDestPduRef()) {
                                    retString = determinePduTypeInnerSection(destPdu.pduR_PduRRoutingTables_PduRRoutingTable_PduRRoutingPath_PduRDestPdu_PduRDestPduRef.refTarget, logger)
                                }
                            }
                        }
                    } else if (rptMe.isInstanceOfDefRef(PduRDestPduRef.DefRef)) {
                        PduRDestPduRef myDstRef = rptMe
                        PduRSrcPdu srcPduRPdu = myDstRef.parent.parent.pduRSrcPdu
                        if (srcPduRPdu.existsPduRSrcPduDirection() && srcPduRPdu.existsPduRSrcPduPduRBswModulesRef()) {
                            if (srcPduRPdu.pduRSrcPduRef.toString() != "") {
                                retString = determinePduTypeInnerSection(srcPduRPdu.pduRSrcPduRef.getRefTarget(), logger)
                            }
                        }
                    }
                } catch (NullPointerException exception) {
                    logger.error("$exception")
                }
            }
        }
        logger.info("PDU Type of PDU " + dPdu.shortname + " is: " + retString + ".")
        return retString
    }
}
