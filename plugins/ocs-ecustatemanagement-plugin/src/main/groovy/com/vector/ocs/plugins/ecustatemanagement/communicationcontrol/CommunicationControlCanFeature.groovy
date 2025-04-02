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
/*!        \file  CommunicationControlCanFeature.groovy
 *        \brief  Class for CAN channels
 *
 *      \details  -
 *
 *********************************************************************************************************************/

package com.vector.ocs.plugins.ecustatemanagement.communicationcontrol

import com.vector.ocs.plugins.ecustatemanagement.AutoConfig_CC_Channel
import com.vector.ocs.plugins.ecustatemanagement.AutoConfig_CC_J1939Defs
import com.vector.ocs.plugins.ecustatemanagement.AutoConfig_CC_PncDefs
import com.vector.ocs.plugins.ecustatemanagement.Feature

/**
 * Defines the attributes which are part of a CAN channel
 */
class CommunicationControlCanFeature extends CommunicationControlFeature {
    List<PncComIPduGroupFeature> pncs
    List<J1939Feature> j1939NmNodes
    Boolean nmCommunication

    CommunicationControlCanFeature(String name, Boolean isActivated) {
        super(name, isActivated)
        this.pncs = new LinkedList<PncComIPduGroupFeature>()
        this.comIPduGroups = new LinkedList<Feature>()
        this.parameters = [:]
        this.j1939NmNodes = new LinkedList<J1939Feature>()
        this.nmCommunication = false
    }

    /**
     * Adds a pnc to the list of pncs
     * @param pnc New pncs feature which shall be added
     */
    @Override
    void addPnc(PncComIPduGroupFeature pnc) {
        pncs.add(pnc)
    }

    /**
     * Adds a subSubFeature to the corresponding subFeature
     * @param subSubFeature New subSubFeature feature which shall be added
     * @param subFeature Identifier of the subFeature
     */
    @Override
    void addSubSubFeature(Feature subSubFeature, String subFeature) {
        if (subFeature.contains("PNC") && subSubFeature.name.contains("Group")) {
            PncComIPduGroupFeature foundPnc = pncs.find {it.name.contains(subFeature)}
            foundPnc.comIPduGroups.add(subSubFeature)
        } else if (subFeature.contains("J1939") && subSubFeature.name.contains("Group")) {
            J1939Feature foundJ1939Node = j1939NmNodes.find {it.name.contains(subFeature)}
            foundJ1939Node.comIPduGroups.add(subSubFeature)
        } else if (subFeature.contains("J1939") && subSubFeature.name.contains("Routing Path")) {
            J1939Feature foundJ1939Node = j1939NmNodes.find {it.name.contains(subFeature)}
            foundJ1939Node.routingPaths.add(subSubFeature)
        }
    }

    /**
     * Applies the configuration of the PNCs in the Json file to the data model.
     * @param JsonChannel Corresponding channel in the Json file
     */
    @Override
    void applyPncJsonModel(AutoConfig_CC_Channel jsonChannel) {
        pncs.each { pnc ->
            AutoConfig_CC_PncDefs jsonPnc = jsonChannel.PNCIpduGroupSettings.find { pnc.name.contains(it.pncName) }
            if (pnc != null && jsonPnc != null) {
                pnc.isActivated = jsonPnc.enabled
                // Disable comIPduGroup of PNC if listed in DisabledIpduGroups parameter of JSON model
                jsonPnc.disabledIpduGroups.each { String disabledIpduGroup ->
                    Feature comIPduGroup = pnc.comIPduGroups.find {it.name.contains(disabledIpduGroup) }
                    if (comIPduGroup != null) {
                        comIPduGroup.isActivated = false
                    }
                }
            }
        }
    }

    /**
     * Applies the configuration of the J1939 in the Json file to the data model.
     * @param jsonChannel
     */
    void applyJ1939JsonModel(AutoConfig_CC_Channel jsonChannel) {
        j1939NmNodes.each {j1939NmNode ->
            AutoConfig_CC_J1939Defs jsonJ1939 = jsonChannel.j1939Settings.find { j1939NmNode.name.contains(it.j1939NmNodeName) }
            if (j1939NmNode != null & jsonJ1939 != null) {
                j1939NmNode.isActivated = jsonJ1939.enabled
                // Check for Rm node and Dcm node
                j1939NmNode.enableRmNode = jsonJ1939.enableRmNode
                j1939NmNode.enableDcmNode = jsonJ1939.enableDcmNode
                // Disable comIPduGroup of J1939 if listed in DisabledIpduGroups parameter of JSON model
                jsonJ1939.disabledIpduGroups.each { String disabledIpduGroup ->
                    Feature comIPduGroup = j1939NmNode.comIPduGroups.find {it.name.contains(disabledIpduGroup) }
                    if (comIPduGroup != null) {
                        comIPduGroup.isActivated = false
                    }
                }
                // Disable routingPath of J1939 if listed in DisabledRoutingPath parameter of JSON model
                jsonJ1939.disabledRoutingPaths.each { String disabledRoutingPath ->
                    Feature routingPath = j1939NmNode.routingPaths.find {it.name.contains(disabledRoutingPath) }
                    if (routingPath != null) {
                        routingPath.isActivated = false
                    }
                }
            }
        }
    }

    /**
     * Applies the configuration of the Nm Communication parameter of the Json file to the data model
     * @param jsonChannel Corresponding channel in the Json file
     */
    @Override
    void applyNmCommunication(AutoConfig_CC_Channel jsonChannel) {
        nmCommunication = jsonChannel.enableNmCommunication
    }
}
