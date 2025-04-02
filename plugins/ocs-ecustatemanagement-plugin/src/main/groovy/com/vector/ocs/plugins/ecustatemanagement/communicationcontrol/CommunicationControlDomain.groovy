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
/*!        \file  CommunicationControlDomain.groovy
 *        \brief  Class which handles the processing of the data model and configuration in the Cfg5
 *
 *      \details  -
 *
 *********************************************************************************************************************/

package com.vector.ocs.plugins.ecustatemanagement.communicationcontrol

import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.automation.model.ecuc.microsar.comm.ComM
import com.vector.cfg.dom.modemgt.groovy.api.IModeManagementApi
import com.vector.cfg.dom.modemgt.groovy.bswm.IBswMAutoConfigurationApi
import com.vector.cfg.dom.modemgt.groovy.bswm.IBswMAutoConfigurationFeature
import com.vector.cfg.dom.modemgt.groovy.bswm.IBswMAutoConfigurationParameter
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIContainer
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.plugins.ecustatemanagement.AutoConfig_CC_Channel
import com.vector.ocs.plugins.ecustatemanagement.EcuStateManagementDomain
import com.vector.ocs.plugins.ecustatemanagement.EcuStateManagementModel
import com.vector.ocs.plugins.ecustatemanagement.Feature

/**
 * Implements the initializeDomainFeatures and processDomainFeatures method
 */
class CommunicationControlDomain extends EcuStateManagementDomain {
    List<CommunicationControlFeature> channels = []

    /**
     * The method gets all channels, PNCs, Schedules, comIPduGroups from the configuration and save it in a data model.
     * After that the JSON configuration is applied to this data model.
     * @param model EcuStateManagementModel which contains the specified configuration settings
     * @param modeMngt IModeManagementApi
     */
    @Override
    void initializeDomainFeatures(EcuStateManagementModel model, IModeManagementApi modeMngt, MIContainer bswMCfg) {
        modeMngt.bswMAutoConfig(bswMCfg, "Communication Control") {
            // Get all channels, PNCs and IPdu Groups
            for (rootFeature in rootFeatures) { // Loop over all channels/rootFeatures
                CommunicationControlFeature channel = createCommunicationControlFeature(getIdentifier(rootFeature.identifier))
                // If the channel has the bus type internal or the channel is no channel at all (faulty configuration)
                // then the channel will be null.
                if (channel != null) {
                    for (subFeature in rootFeature.subFeatures) {
                        // Loop over all subFeatures of the channel/rootFeature
                        // Check for PNCs
                        if (subFeature.identifier.contains("PNC") &&
                                channel instanceof CommunicationControlCanFeature ||
                                channel instanceof CommunicationControlFlexRayFeature ||
                                channel instanceof CommunicationControlEthernetFeature) {
                            String pnc = getIdentifier(subFeature.identifier)
                            channel.addPnc(new PncComIPduGroupFeature(pnc, true))
                            for (subSubFeature in subFeature.subFeatures) {
                                // Loop over all subFeatures of the subFeature
                                channel.addSubSubFeature(new Feature(getIdentifier(subSubFeature.identifier), true), pnc)
                            }
                        } else if (subFeature.identifier.contains("Schedule") && channel instanceof CommunicationControlLinFeature) {
                            // Check for LIN Schedules
                            channel.schedule = new ScheduleFeature(subFeature.identifier, true)
                            for (subsubFeature in subFeature.subFeatures) {
                                // Loop over all subFeatures of the subFeature
                                channel.schedule.comIPduGroups.add(new Feature(getIdentifier(subsubFeature.identifier), true))
                            }
                        } else if (subFeature.identifier.contains("J1939") && channel instanceof CommunicationControlCanFeature) {
                            // Check for J1939 Nm nodes
                            String j1939NmNode = getIdentifier(subFeature.identifier)
                            channel.j1939NmNodes.add(new J1939Feature(j1939NmNode, true))
                            for (subSubFeature in subFeature.subFeatures) {
                                // Loop over all subFeatures of the subFeature
                                if (subSubFeature.identifier.contains("Group")) {
                                    channel.addSubSubFeature(new Feature(getIdentifier(subSubFeature.identifier), true), j1939NmNode)
                                } else if (subSubFeature.identifier.contains("Routing Path")) {
                                    channel.addSubSubFeature(new Feature(getIdentifier(subSubFeature.identifier), true), j1939NmNode)
                                }
                            }
                        } else if (!subFeature.identifier.contains("not available")) { // Check for IPdu Groups
                            channel.comIPduGroups.add(new Feature(getIdentifier(subFeature.identifier), true))
                        }
                    }
                    // Apply default values for parameters
                    if (channel instanceof CommunicationControlCanFeature) {
                        channel.parameters[channel.name + "/NM_STATE_BUS_OFF"] = true
                        channel.parameters[channel.name + "/NM_STATE_CHECK_WAKEUP"] = true
                        channel.parameters[channel.name + "/NM_STATE_WAIT_STARTUP"] = true
                        channel.parameters[channel.name + "/NM_STATE_BUS_SLEEP"] = true
                        channel.parameters[channel.name + "/NM_STATE_PREPARE_BUS_SLEEP"] = true
                    }
                    if (channel instanceof CommunicationControlFlexRayFeature ||
                            channel instanceof CommunicationControlEthernetFeature ||
                            channel instanceof CommunicationControlCanFeature) {
                        channel.parameters[channel.name + "/ReInitialize TX"] = true
                        channel.parameters[channel.name + "/ReInitialize RX"] = true
                        channel.parameters[channel.name + "/DCM_ENABLE_RX_DISABLE_TX_NORM"] = true
                        channel.parameters[channel.name + "/DCM_DISABLE_RX_ENABLE_TX_NORM"] = true
                        channel.parameters[channel.name + "/DCM_DISABLE_RX_TX_NORMAL"] = true
                        channel.parameters[channel.name + "/DCM_ENABLE_RX_DISABLE_TX_NM"] = true
                        channel.parameters[channel.name + "/DCM_DISABLE_RX_ENABLE_TX_NM"] = true
                        channel.parameters[channel.name + "/DCM_DISABLE_RX_TX_NM"] = true
                        channel.parameters[channel.name + "/DCM_ENABLE_RX_DISABLE_TX_NORM_NM"] = true
                        channel.parameters[channel.name + "/DCM_DISABLE_RX_ENABLE_TX_NORM_NM"] = true
                        channel.parameters[channel.name + "/DCM_DISABLE_RX_TX_NORM_NM"] = true
                    }
                    if (channel instanceof CommunicationControlLinFeature) {
                        channel.parameters[channel.name + "/ReInitialize TX"] = true
                        channel.parameters[channel.name + "/ReInitialize RX"] = true
                        channel.parameters[channel.name + "/Start Schedule"] = "NO_STARTUP_SCHEDULE"
                    }
                    channels.add(channel)
                }
            }
        }

        // Apply JSON model to data model
        channels.each { channel ->
            AutoConfig_CC_Channel jsonChannel = model.autoConfig_CC_CChannels.find { channel.name.contains(it.channelName) }
            if (jsonChannel != null) {
                channel.isActivated = jsonChannel.channelEnabled
                // Disable comIPduGroup if listed in DisabledIpduGroups parameter of JSON model
                jsonChannel.disabledIpduGroups.each { DisabledIpduGroup ->
                    if (channel instanceof CommunicationControlLinFeature) {
                        def comIPduGroupLin = channel.schedule.comIPduGroups.find { it.name.contains(DisabledIpduGroup) }
                        if (comIPduGroupLin != null) {
                            comIPduGroupLin.isActivated = false
                        }
                    }
                    def comIPduGroup = channel.comIPduGroups.find { it.name.contains(DisabledIpduGroup) }
                    if (comIPduGroup != null) {
                        comIPduGroup.isActivated = false
                    }
                }
                // Apply PNC setting of JSON model
                if (channel instanceof CommunicationControlCanFeature ||
                        channel instanceof CommunicationControlFlexRayFeature ||
                        channel instanceof CommunicationControlEthernetFeature) {
                    channel.applyPncJsonModel(jsonChannel)
                    channel.applyNmCommunication(jsonChannel)
                }
                if (channel instanceof CommunicationControlCanFeature) {
                    channel.applyJ1939JsonModel(jsonChannel)
                }
                // Apply parameter settings of JSON model
                if (channel instanceof CommunicationControlCanFeature) {
                    channel.parameters[channel.name + "/NM_STATE_BUS_OFF"] = jsonChannel.nmStateBusOff
                    channel.parameters[channel.name + "/NM_STATE_CHECK_WAKEUP"] = jsonChannel.nmStateCheckWakeup
                    channel.parameters[channel.name + "/NM_STATE_WAIT_STARTUP"] = jsonChannel.nmStateWaitStartup
                    channel.parameters[channel.name + "/NM_STATE_BUS_SLEEP"] = jsonChannel.nmStateBusSleep
                    channel.parameters[channel.name + "/NM_STATE_PREPARE_BUS_SLEEP"] = jsonChannel.nmStatePrepareBusSleep
                }
                if (channel instanceof CommunicationControlFlexRayFeature ||
                        channel instanceof CommunicationControlEthernetFeature ||
                        channel instanceof CommunicationControlCanFeature) {
                    channel.parameters[channel.name + "/ReInitialize TX"] = jsonChannel.reInitializeTx
                    channel.parameters[channel.name + "/ReInitialize RX"] = jsonChannel.reInitializeRx
                    channel.parameters[channel.name + "/DCM_ENABLE_RX_DISABLE_TX_NORM"] = jsonChannel.dcmEnableRxDisableTxNorm
                    channel.parameters[channel.name + "/DCM_DISABLE_RX_ENABLE_TX_NORM"] = jsonChannel.dcmDisableRxEnableTxNorm
                    channel.parameters[channel.name + "/DCM_DISABLE_RX_TX_NORMAL"] = jsonChannel.dcmDisableRxTxNorm
                    channel.parameters[channel.name + "/DCM_ENABLE_RX_DISABLE_TX_NM"] = jsonChannel.dcmEnableRxDisableTxNm
                    channel.parameters[channel.name + "/DCM_DISABLE_RX_ENABLE_TX_NM"] = jsonChannel.dcmDisableRxEnableTxNm
                    channel.parameters[channel.name + "/DCM_DISABLE_RX_TX_NM"] = jsonChannel.dcmDisableRxTxNm
                    channel.parameters[channel.name + "/DCM_ENABLE_RX_DISABLE_TX_NORM_NM"] = jsonChannel.dcmEnableRxDisableTxNormNm
                    channel.parameters[channel.name + "/DCM_DISABLE_RX_ENABLE_TX_NORM_NM"] = jsonChannel.dcmDisableRxEnableTxNormNm
                    channel.parameters[channel.name + "/DCM_DISABLE_RX_TX_NORM_NM"] = jsonChannel.dcmDisableRxTxNormNm
                }
                if (channel instanceof CommunicationControlLinFeature) {
                    channel.parameters[channel.name + "/ReInitialize TX"] = jsonChannel.reInitializeTx
                    channel.parameters[channel.name + "/ReInitialize RX"] = jsonChannel.reInitializeRx
                    channel.parameters[channel.name + "/Start Schedule"] = jsonChannel.startSchedule
                }
            }
        }
    }

    /**
     * The method applies the configuration of the data model to the configurator.
     * @param model EcuStateManagementModel which contains the specified configuration settings
     * @param logger Instance of the OcsLogger
     * @param modeMngt IModeManagementApi
     */
    @Override
    void processDomainFeatures(EcuStateManagementModel model, OcsLogger logger, IModeManagementApi modeMngt, MIContainer bswMCfg) {
        modeMngt.bswMAutoConfig(bswMCfg, "Communication Control") { autoConfigApi ->
            rootFeatures.each { IBswMAutoConfigurationFeature rootFeature ->
                CommunicationControlFeature channel = channels.find { getIdentifier(rootFeature.identifier).contains(it.name) }
                if (channel?.isActivated && model.autoConfig_CC_Enabled) {
                    activateFeature(autoConfigApi, rootFeature.identifier, logger)
                    rootFeature.subFeatures.each { IBswMAutoConfigurationFeature subFeature ->
                        if (channel instanceof CommunicationControlCanFeature ||
                                channel instanceof CommunicationControlFlexRayFeature ||
                                channel instanceof CommunicationControlEthernetFeature) {
                            if (subFeature.identifier.contains("PNC")) {
                                PncComIPduGroupFeature pnc = channel.pncs.find { getIdentifier(subFeature.identifier).contains(it.name) }
                                if (pnc != null && pnc.isActivated) {
                                    processSubGroup(autoConfigApi, subFeature, pnc, logger)
                                } else {
                                    deactivateFeature(autoConfigApi, subFeature.identifier, logger)
                                }
                            } else if (subFeature.identifier.contains("Group")) {
                                def comIPduGroup = channel.comIPduGroups.find { getIdentifier(subFeature.identifier).contains(it.name) }
                                if (comIPduGroup != null) {
                                    comIPduGroup.isActivated ? activateFeature(autoConfigApi, subFeature.identifier, logger) : deactivateFeature(autoConfigApi, subFeature.identifier, logger)
                                }
                            } else if (subFeature.identifier.contains("Nm Communication")) {
                                if (subFeature.enabled) {
                                    channel.nmCommunication ? activateFeature(autoConfigApi, subFeature.identifier, logger) : deactivateFeature(autoConfigApi, subFeature.identifier, logger)
                                }
                            }
                        } else if (channel instanceof CommunicationControlLinFeature) {
                            if (subFeature.identifier.contains("Schedule")) {
                                if (channel.schedule.isActivated) {
                                    processSubGroup(autoConfigApi, subFeature, channel.schedule, logger)
                                } else {
                                    deactivateFeature(autoConfigApi, subFeature.identifier, logger)
                                }
                            }
                        }
                        if (channel instanceof CommunicationControlCanFeature) {
                            if (subFeature.identifier.contains("J1939")) {
                                J1939Feature j1939NmNode = channel.j1939NmNodes.find { getIdentifier(subFeature.identifier).contains(it.name) }
                                if (j1939NmNode != null && j1939NmNode.isActivated) {
                                    processSubGroup(autoConfigApi, subFeature, j1939NmNode, logger)
                                } else {
                                    deactivateFeature(autoConfigApi, subFeature.identifier, logger)
                                }
                            }
                        }
                    }
                    channel.parameters.each { key, value ->
                        IBswMAutoConfigurationParameter param = rootFeature.parameters.find { getIdentifier(it.identifier) == key }
                        if (param != null && param.enabled) {
                            setParameters(autoConfigApi, key, value, logger)
                        }
                    }
                } else {
                    if (rootFeature.isEnabled()) {
                        deactivateFeature(autoConfigApi, rootFeature.identifier, logger)
                    }
                }
            }
        }
    }

    /**
     * The method applies the configuration of the PNCs or Schedule of the data model to the configurator.
     * @param autoConfigApi BswMAutoConfigurationApi
     * @param subFeature PNC for CAN, FlexRay or Ethernet or Schedule for LIN in the configuration
     * @param subGroup PNC for CAN, FlexRay or Ethernet or Schedule for LIN in the data model
     * @param logger Instance of the OcsLogger
     */
    static void processSubGroup(IBswMAutoConfigurationApi autoConfigApi, IBswMAutoConfigurationFeature subFeature, def subGroup, OcsLogger logger) {
        activateFeature(autoConfigApi, subFeature.identifier, logger)
        subFeature.subFeatures.each { IBswMAutoConfigurationFeature subSubFeature ->
            Feature comIPduGroup = subGroup.comIPduGroups.find { getIdentifier(subSubFeature.identifier).contains(it.name) }
            comIPduGroup.isActivated ? activateFeature(autoConfigApi, subSubFeature.identifier, logger) : deactivateFeature(autoConfigApi, subSubFeature.identifier, logger)
        }
    }

    /**
     * The method applies the configuration of the J1939 of the data model to the configurator.
     * @param autoConfigApi BswMAutoConfigurationApi
     * @param subFeature J1939Nm node in the configuration
     * @param subGroup J1939Nm node in the data model
     * @param logger Instance of the OcsLogger
     */
    static void processSubGroup(IBswMAutoConfigurationApi autoConfigApi, IBswMAutoConfigurationFeature subFeature, J1939Feature subGroup, OcsLogger logger) {
        activateFeature(autoConfigApi, subFeature.identifier, logger)
        subFeature.subFeatures.each { IBswMAutoConfigurationFeature subSubFeature ->
            // Handle RmNode
            if (subSubFeature.identifier.contains("RmNode") && subGroup.enableRmNode) {
                activateFeature(autoConfigApi, subSubFeature.identifier, logger)
            } else if (subSubFeature.identifier.contains("RmNode") && !subGroup.enableRmNode) {
                deactivateFeature(autoConfigApi, subSubFeature.identifier, logger)
            }
            // Handle DcmNode
            if (subSubFeature.identifier.contains("DcmNode") && subGroup.enableDcmNode) {
                activateFeature(autoConfigApi, subSubFeature.identifier, logger)
            } else if (subSubFeature.identifier.contains("DcmNode") && !subGroup.enableDcmNode) {
                deactivateFeature(autoConfigApi, subSubFeature.identifier, logger)
            }
            // Handle comIPduGroups
            if (subSubFeature.identifier.contains("Group")) {
                Feature comIPduGroup = subGroup.comIPduGroups.find { getIdentifier(subSubFeature.identifier).contains(it.name) }
                if (comIPduGroup != null) {
                    comIPduGroup.isActivated ? activateFeature(autoConfigApi, subSubFeature.identifier, logger) : deactivateFeature(autoConfigApi, subSubFeature.identifier, logger)
                }
            }
            // Handle RoutingPaths
            if (subSubFeature.identifier.contains("Routing Path")) {
                Feature routingPath = subGroup.routingPaths.find { getIdentifier(subSubFeature.identifier).contains(it.name) }
                if (routingPath != null) {
                    routingPath.isActivated ? activateFeature(autoConfigApi, subSubFeature.identifier, logger) : deactivateFeature(autoConfigApi, subSubFeature.identifier, logger)
                }
            }
        }
    }

    /**
     * The method gets the bus type of the channel.
     * @param channel Communication Channel (CAN, LIN, FlexRay, Ethernet)
     * @return busType The bus type of the channel
     */
    static String getBusType(String channel) {
        String busType = ""
        ScriptApi.activeProject() {
            ComM comM = bswmdModel(ComM.DefRef).single
            comM.comMConfigSet.comMChannel.each {
                if (channel.contains(it.shortname)) {
                    busType = it.comMBusType.value
                }
            }
        }
        return busType
    }

    /**
     * The method creates an object regarding which bus type the rootFeature has.
     * @param rootFeature Identifier of the rootFeature to find the right bus type
     * @return Can, FlexRay, Lin, or Ethernet Feature. Returns null if no bus type is found
     */
    static def createCommunicationControlFeature(String rootFeature) {
        switch (getBusType(rootFeature)) {
            case "COMM_BUS_TYPE_CAN":
                return new CommunicationControlCanFeature(rootFeature, true)
            case 'COMM_BUS_TYPE_FR':
                return new CommunicationControlFlexRayFeature(rootFeature, true)
            case 'COMM_BUS_TYPE_LIN':
                return new CommunicationControlLinFeature(rootFeature, true)
            case 'COMM_BUS_TYPE_ETH':
                return new CommunicationControlEthernetFeature(rootFeature, true)
            default:
                return null
        }
    }
}
