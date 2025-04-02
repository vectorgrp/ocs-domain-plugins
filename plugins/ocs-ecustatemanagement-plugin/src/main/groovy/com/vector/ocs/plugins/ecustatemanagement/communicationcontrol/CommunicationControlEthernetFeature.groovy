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
/*!        \file  CommunicationControlEthernetFeature.groovy
 *        \brief  Class for Ethernet channels
 *
 *      \details  -
 *
 *********************************************************************************************************************/

package com.vector.ocs.plugins.ecustatemanagement.communicationcontrol

import com.vector.ocs.plugins.ecustatemanagement.AutoConfig_CC_Channel
import com.vector.ocs.plugins.ecustatemanagement.AutoConfig_CC_PncDefs
import com.vector.ocs.plugins.ecustatemanagement.Feature

/**
 * Defines the attributes which are part of a Ethernet channel
 */
class CommunicationControlEthernetFeature extends CommunicationControlFeature {
    List<PncComIPduGroupFeature> pncs
    Boolean nmCommunication

    CommunicationControlEthernetFeature(String name, Boolean isActivated) {
        super(name, isActivated)
        this.pncs = new LinkedList<PncComIPduGroupFeature>()
        this.comIPduGroups = new LinkedList<Feature>()
        this.parameters = [:]
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
     * Adds a comIpduGroup to the corresponding pnc
     * @param comIpduGroup New comIpduGroup feature which shall be added
     * @param pnc Identifier of the pnc
     */
    @Override
    void addSubSubFeature(Feature comIpduGroup, String pnc) {
        PncComIPduGroupFeature foundPnc = pncs.find {it.name.contains(pnc)}
        foundPnc.comIPduGroups.add(comIpduGroup)
    }

    /**
     * Applies the configuration of the PNCs in the Json file to the data model.
     * @param jsonChannel Corresponding channel in the Json file
     */
    @Override
    void applyPncJsonModel(AutoConfig_CC_Channel jsonChannel) {
        pncs.each { pnc ->
            AutoConfig_CC_PncDefs JsonPnc = jsonChannel.PNCIpduGroupSettings.find { pnc.name.contains(it.pncName) }
            if (pnc != null && JsonPnc != null) {
                pnc.isActivated = JsonPnc.enabled
                // Disable comIDuGroup of PNC if listed in DisabledIpduGroups parameter of JSON model
                JsonPnc.disabledIpduGroups.each { String DisabledIpduGroup ->
                    Feature comIPduGroup = pnc.comIPduGroups.find {it.name.contains(DisabledIpduGroup) }
                    if (comIPduGroup != null) {
                        comIPduGroup.isActivated = false
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
