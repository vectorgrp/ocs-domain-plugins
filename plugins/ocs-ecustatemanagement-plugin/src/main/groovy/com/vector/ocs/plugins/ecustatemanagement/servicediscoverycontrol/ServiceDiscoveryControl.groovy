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
/*!        \file  ServiceDiscoveryControl.groovy
 *        \brief  Class which handles the processing of the data model and configuration in the Cfg.
 *
 *      \details  -
 *
 *********************************************************************************************************************/

package com.vector.ocs.plugins.ecustatemanagement.servicediscoverycontrol

import com.vector.cfg.dom.deprecated.modemgt.pai.api.IModeManagementApi
import com.vector.cfg.dom.deprecated.modemgt.pai.bswm.IBswMAutoConfigurationApi
import com.vector.cfg.dom.deprecated.modemgt.pai.bswm.IBswMAutoConfigurationFeature
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIContainer
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.plugins.ecustatemanagement.ClientSettings
import com.vector.ocs.plugins.ecustatemanagement.EcuStateManagementDomain
import com.vector.ocs.plugins.ecustatemanagement.EcuStateManagementModel
import com.vector.ocs.plugins.ecustatemanagement.Feature
import com.vector.ocs.plugins.ecustatemanagement.ServerSettings

/**
 * Implements the initializeDomainFeatures and processDomainFeatures method
 */
class ServiceDiscoveryControl extends EcuStateManagementDomain {
    ClientServices cServices = new ClientServices("Client Services", true)
    ServerServices sServices = new ServerServices("Server Services", true)

    /**
     * The method is reading out all information from the Service Discovery Control domain and filling the data model with it.
     * After that it will apply the configuration of the JSON configuration.
     * @param model EcuStateManagementModel which contains the specified configuration settings.
     * @param modeMngt Instance of ModeManagementApi
     */
    @Override
    void initializeDomainFeatures(EcuStateManagementModel model, IModeManagementApi modeMngt, MIContainer bswMCfg) {
        modeMngt.bswMAutoConfig("Service Discovery Control") {
            for (rootFeature in rootFeatures) {
                if (getIdentifier(rootFeature.identifier) == "/Client Services") {
                    for (subFeature in rootFeature.subFeatures) { // Get all Client Services
                        String client = getIdentifier(subFeature.identifier)
                        cServices.Clients.add(new Client(client, true))
                        for (subsubFeature in subFeature.subFeatures) { // Get all Event Groups
                            cServices.addEventGroup(new Feature(getIdentifier(subsubFeature.identifier), true), client)
                        }
                    }
                } else {
                    for (subFeature in rootFeature.subFeatures) { // Get all Server Services
                        String server = getIdentifier(subFeature.identifier)
                        sServices.Servers.add(new Server(server, true))
                        for (subsubFeature in subFeature.subFeatures) { // Get all Event Handlers
                            sServices.addEventHandler(new Feature(getIdentifier(subsubFeature.identifier), true), server)
                        }
                    }
                }
            }
        }

        // Apply JSON model to data model
        if (model.autoConfig_ServiceDiscovery_Enabled) {
            cServices.isActivated = true
            sServices.isActivated = true
        } else {
            cServices.isActivated = false
            sServices.isActivated = false
        }
        // Setting Immediate Request Processing parameters
        cServices.ImmediateRequestProcessing = model.autoConfig_SdClientImmediateProcessing_Enabled
        cServices.ImmediateRequestProcessing = model.autoConfig_SdServerImmediateProcessing_Enabled

        // Handle Client Settings
        cServices.Clients.each { client ->
            ClientSettings clientSetting = model.autoConfig_DisabledClientServices.find { client.name.contains(it.clientService) }
            if (clientSetting != null) {
                client.isActivated = clientSetting.clientService_Enabled
                clientSetting.consumedEventGroup.each { consumedEventGroup ->
                    Feature foundConsumedEventGroup = client.ConsumedEventGroups.find { it.name.contains(consumedEventGroup) }
                    if (foundConsumedEventGroup != null) {
                        foundConsumedEventGroup.isActivated = false
                    }
                }
            }
        }
        // Handle Server Settings
        sServices.Servers.each { server ->
            ServerSettings serverSettings = model.autoConfig_DisabledServerServices.find { server.name.contains(it.serverService) }
            if (serverSettings != null) {
                server.isActivated = serverSettings.serverService_Enabled
                serverSettings.eventHandlers.each { eventHandler ->
                    Feature foundEventHandler = server.EventHandlers.find { it.name.contains(eventHandler) }
                    if (foundEventHandler != null) {
                        foundEventHandler.isActivated = false
                    }
                }
            }
        }
    }

    /**
     * The method applies the configuration of the data model to the Service Discovery Control domain.
     * @param model EcuStateManagementModel which contains the specified configuration settings.
     * @param logger Instance of the OcsLogger
     * @param modeMngt Instance of ModeManagementApi
     */
    @Override
    void processDomainFeatures(EcuStateManagementModel model, OcsLogger logger, IModeManagementApi modeMngt, MIContainer bswMCfg) {
        modeMngt.bswMAutoConfig("Service Discovery Control") { autoConfigApi ->
            // Loop over the rootFeatures, i. e. Client Services and Server Services
            for (rootFeature in rootFeatures) {
                String service = getIdentifier(rootFeature.identifier)
                if (service == "/Client Services") {
                    // Process Client Services
                    if (cServices.isActivated) {
                        activateFeature(autoConfigApi, rootFeature.identifier, logger)
                        // Set parameter Immediate Request Processing
                        setParameters(autoConfigApi, rootFeature.parameters[0].identifier, cServices.ImmediateRequestProcessing, logger)
                        // Loop over the clients of the clients services
                        for (client in cServices.Clients) {
                            processSubFeatures(client, rootFeature, autoConfigApi, logger)
                        }
                    } else {
                        deactivateFeature(autoConfigApi, rootFeature.identifier, logger)
                    }
                } else {
                    // Process Server Services
                    if (sServices.isActivated) {
                        activateFeature(autoConfigApi, rootFeature.identifier, logger)
                        // Set parameter Immediate Request Processing
                        setParameters(autoConfigApi, rootFeature.parameters[0].identifier, sServices.ImmediateRequestProcessing, logger)
                        // Loop over the server of the server services
                        for (server in sServices.Servers) {
                            processSubFeatures(server, rootFeature, autoConfigApi, logger)
                        }
                    } else {
                        deactivateFeature(autoConfigApi, rootFeature.identifier, logger)
                    }
                }
            }
        }
    }

    /**
     * The method processes the subFeatures (clients or servers). It then calls another method to process the underlying subSubFeatures.
     * @param subFeature Client or Server
     * @param rootFeature Client Services or Server Services
     * @param autoConfigApi Instance of BswMAutoConfigurationApi
     * @param logger Instance of the OcsLogger
     */
    static void processSubFeatures(def subFeature, IBswMAutoConfigurationFeature rootFeature, IBswMAutoConfigurationApi autoConfigApi, OcsLogger logger) {
        IBswMAutoConfigurationFeature foundSubFeature = rootFeature.subFeatures.find { getIdentifier(it.identifier).contains(subFeature.name) }
        if (foundSubFeature != null) {
            if (subFeature.isActivated) {
                activateFeature(autoConfigApi, foundSubFeature.identifier, logger)
                if (subFeature instanceof Client) {
                    processConsumedEventGroups(subFeature, foundSubFeature, autoConfigApi, logger)
                } else {
                    processEventHandlers(subFeature, foundSubFeature, autoConfigApi, logger)
                }
            } else {
                deactivateFeature(autoConfigApi, foundSubFeature.identifier, logger)
            }
        }
    }

    /**
     * The method processes the subSubFeatures (ConsumedEventGroups).
     * @param client Client of the data model
     * @param foundSubFeature Found Client in the Client Services of the Service Discovery Control domain
     * @param autoConfigApi Instance of BswMAutoConfigurationApi
     * @param logger Instance of the OcsLogger
     */
    static void processConsumedEventGroups(Client client, IBswMAutoConfigurationFeature foundSubFeature, IBswMAutoConfigurationApi autoConfigApi, OcsLogger logger) {
        // Loop over the consumedEventGroups of the found client
        for (consumedEventGroup in client.ConsumedEventGroups) {
            IBswMAutoConfigurationFeature foundConsumedEventGroup = foundSubFeature.subFeatures.find { getIdentifier(it.identifier).contains(consumedEventGroup.name) }
            if (foundConsumedEventGroup != null) {
                consumedEventGroup.isActivated ? activateFeature(autoConfigApi, foundConsumedEventGroup.identifier, logger) : deactivateFeature(autoConfigApi, foundConsumedEventGroup.identifier, logger)
            }
        }
    }

    /**
     * The method processes the subSubFeatures (EventHandler).
     * @param server Server of the data model
     * @param foundSubFeature Found Server in the Server Services of the Service Discovery Control domain
     * @param autoConfigApi Instance of BswMAutoConfigurationApi
     * @param logger Instance of the OcsLogger
     */
    static void processEventHandlers(Server server, IBswMAutoConfigurationFeature foundSubFeature, IBswMAutoConfigurationApi autoConfigApi, OcsLogger logger) {
        // Loop over the EventHandler of the found server
        for (eventHandler in server.EventHandlers) {
            IBswMAutoConfigurationFeature foundEventHandler = foundSubFeature.subFeatures.find { getIdentifier(it.identifier).contains(eventHandler.name) }
            if (foundEventHandler != null) {
                eventHandler.isActivated ? activateFeature(autoConfigApi, foundEventHandler.identifier, logger) : deactivateFeature(autoConfigApi, foundEventHandler.identifier, logger)
            }
        }
    }
}
