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
/*!      \file   CommunicationEthConfig.groovy
 *      \brief  The CommunicationEthConfig addresses configuration issues in the Eth stack.
 *
 *      \details  Depending on the customer input the plugin will configure following:
 *                  - Create or set up default routing activation for DoIP
 *                  - Create Arp Config
 *********************************************************************************************************************/
package com.vector.ocs.plugins.communication

import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.automation.model.ecuc.microsar.doip.DoIP
import com.vector.cfg.automation.model.ecuc.microsar.doip.doipconfigset.doipinterface.DoIPInterface
import com.vector.cfg.automation.model.ecuc.microsar.doip.doipconfigset.doipinterface.doipchannel.DoIPChannel
import com.vector.cfg.automation.model.ecuc.microsar.doip.doipconfigset.doipinterface.doiproutingactivation.DoIPRoutingActivation
import com.vector.cfg.automation.model.ecuc.microsar.doip.doipconfigset.doipinterface.doiptester.DoIPTester
import com.vector.cfg.automation.model.ecuc.microsar.doip.doipconfigset.doipinterface.doiptester.doiproutingactivationref.DoIPRoutingActivationRef
import com.vector.cfg.automation.model.ecuc.microsar.soad.SoAd
import com.vector.cfg.automation.model.ecuc.microsar.soad.soadconfig.soadinstance.SoAdInstance
import com.vector.cfg.automation.model.ecuc.microsar.soad.soadconfig.soadsocketconnectiongroup.SoAdSocketConnectionGroup
import com.vector.cfg.automation.model.ecuc.microsar.tcpip.TcpIp
import com.vector.ocs.core.api.OcsLogger

/**
 * Handle the Eth specific configuration aspects of the CommunicationPlugin.
 */
class CommunicationEthConfig {
    /**
     * Checks if routing activation is configured in a logical way.
     * @param logger instance for OCS specific logging
     */
    static void ConfigureDoIP(OcsLogger logger) {
        ScriptApi.activeProject() {
            ScriptApi.scriptCode() {
                logger.info("Checking if Routing Activation is configured in a logical way.")
                DoIP doipCfg = bswmdModel(DoIP.DefRef).single
                /* Add default routing activations for all Testers */
                doipCfg.doIPConfigSet.doIPInterface.each { DoIPInterface dif ->
                    dif.doIPTester.each { DoIPTester dte ->
                        logger.info("Check for DoIP Tester " + dte.shortname + ".")
                        Boolean raOk = false
                        if (dte.existsDoIPRoutingActivationRef()) {
                            dte.doIPRoutingActivationRef.each { DoIPRoutingActivationRef raref ->
                                if (raref.hasRefTarget()) {
                                    /* At least one reference target to the routing activations is present - ok so far, otherwise create a reference */
                                    logger.info("Check for Tester " + dte.shortname + ": Routing activation reference found: " + raref.refTarget.shortname + ".")
                                    raOk = true
                                } else {
                                    transaction {
                                        raref.moRemove()
                                    }
                                }
                            }
                        }
                        if (!raOk) {
                            logger.info("No Routing activation found for this tester - create default one: " + dte.shortname + "_RoutingActivation.")
                            transaction {
                                DoIPRoutingActivation rac = dif.doIPRoutingActivation.byNameOrCreate(dte.shortname + "_RoutingActivation")
                                dte.doIPRoutingActivationRef.createAndAdd().setRefTarget(rac)
                                /* No Routing activation present - collect all connections associated to this tester and add a routingGroup for them */
                                dif.doIPChannel.each { DoIPChannel dch ->
                                    if (dch.doIPChannelSARef.hasRefTarget()) {
                                        if (dch.doIPChannelSARef.refTarget.shortname == dte.shortname) {
                                            logger.info("Adding Reference to " + dch.doIPChannelTARef.refTarget.shortname + " to Routing Activation " + dte.shortname + "_RoutingActivation since this route references the tester that is using this Routing Activation.")
                                            rac.doIPTargetAddressRef.createAndAdd().setRefTarget(dch.doIPChannelTARef.refTarget)
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

    /**
     * Creates an Arp container if not yet existent.
     * @param logger instance for OCS specific logging
     */
    static void ConfigureTcpIp(OcsLogger logger) {
        ScriptApi.activeProject() {
            ScriptApi.scriptCode() {
                TcpIp tcpIpCfg = bswmdModel(TcpIp.DefRef).single
                transaction {
                    if (tcpIpCfg.tcpIpGeneralOrCreate.existsTcpIpIpV4General()) {
                        if (tcpIpCfg.tcpIpGeneral.tcpIpIpV4General.existsTcpIpArpEnabled()) {
                            if (tcpIpCfg.tcpIpGeneral.tcpIpIpV4General.tcpIpArpEnabled.value) {
                                logger.info("Arp enabled - create arp config.")
                                /* TcpIp controllers need arp config - add it if needed */
                                tcpIpCfg.tcpIpConfigOrCreate.tcpIpIpConfigOrCreate.tcpIpIpV4ConfigOrCreate.tcpIpArpConfig.byNameOrCreate("TcpIpArpConfig")
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates one SoAd Instance if not yet existent which references all SocketConnectionGroups
     * @param logger instance for OCS specific logging
     */
    static void ConfigureSoAd(OcsLogger logger) {
        ScriptApi.activeProject() {
            ScriptApi.scriptCode() {
                SoAd soadCfg = bswmdModel(SoAd.DefRef).single
                if (soadCfg.soAdConfig.soAdInstance.size() == 0) {
                    /* auto-create one SoAd instance that references all SocketConnectionGroups - use case for all Non-Multicore Projects */
                    transaction {
                        logger.info("Adding OcsDefault_SoAdInstance and assigning all SoAd Socket Connection Groups to it.")
                        SoAdInstance inst = soadCfg.soAdConfig.soAdInstance.byNameOrCreate("OcsDefault_SoAdInstance")
                        soadCfg.soAdConfig.soAdSocketConnectionGroup.each { SoAdSocketConnectionGroup scg ->
                            inst.soAdSocketConnectionGroupRef.createAndAdd().setRefTarget(scg)
                        }
                    }
                }
            }
        }
    }
}
