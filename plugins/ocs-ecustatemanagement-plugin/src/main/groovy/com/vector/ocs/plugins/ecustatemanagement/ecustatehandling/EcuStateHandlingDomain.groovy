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
/*!        \file  EcuStateHandlingDomain.groovy
 *        \brief  Subclass of class 'EcuStateManagementDomain' to handle initialization and processing of
 *                'Ecu State Handling' domain
 *
 *      \details  Initialize data model for 'Ecu State Handling' domain and process all it features and parameters
 *
 *********************************************************************************************************************/

package com.vector.ocs.plugins.ecustatemanagement.ecustatehandling

import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.automation.model.ecuc.microsar.ecum.EcuM
import com.vector.cfg.dom.deprecated.modemgt.pai.api.IModeManagementApi
import com.vector.cfg.dom.deprecated.modemgt.pai.bswm.IBswMAutoConfigurationApi
import com.vector.cfg.dom.deprecated.modemgt.pai.bswm.IBswMAutoConfigurationDomain
import com.vector.cfg.dom.deprecated.modemgt.pai.bswm.IBswMAutoConfigurationFeature
import com.vector.cfg.dom.deprecated.modemgt.pai.bswm.IBswMAutoConfigurationParameter
import com.vector.cfg.gen.core.bswmdmodel.param.GBoolean
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIContainer
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.plugins.ecustatemanagement.EcuStateManagementDomain
import com.vector.ocs.plugins.ecustatemanagement.EcuStateManagementModel
import com.vector.ocs.plugins.ecustatemanagement.Feature

class EcuStateHandlingDomain extends EcuStateManagementDomain {
    EcuStateMachineFeature ecuStateHandlingDataModel = null

    @Override
    void initializeDomainFeatures(EcuStateManagementModel model, IModeManagementApi modeMgmt, MIContainer bswMCfg) {

        IBswMAutoConfigurationDomain eshDomain = modeMgmt.bswMAutoConfigDomain("Ecu State Handling")

        // Store the activate/deactivate state of the Ecu State Handling domain according to the json setting. Default = true
        ecuStateHandlingDataModel = new EcuStateMachineFeature("Ecu State Machine", model.autoConfig_EcuStateHandling_Enabled)

        // Store the values of all features and parameters in the data model from the json
        initializeDataWithJson(model, eshDomain)
    }

    @Override
    void processDomainFeatures(EcuStateManagementModel model, OcsLogger logger, IModeManagementApi modeMgmt, MIContainer bswMCfg) {
        // Handling basic editor parameter for EcuM Mode Handling
        EcuM ecumModule = ScriptApi.activeProject.bswmdModel(EcuM.DefRef).single()
        GBoolean ecuMModeHandling = ecumModule.ecuMFlexGeneralOrCreate.ecuMModeHandlingOrCreate
        // This Basic Editor parameter must be activated for "EcuM Mode Handling" to be enabled in the autoconfig domain
        ecuMModeHandling.value = ecuStateHandlingDataModel.ecumModeHandling
        IBswMAutoConfigurationDomain eshDomain = modeMgmt.bswMAutoConfigDomain("Ecu State Handling")
        IBswMAutoConfigurationFeature ecuStateMachine = eshDomain.getRootFeatures().getFirst()
        IBswMAutoConfigurationFeature demHandlingFeature = getSubfeatureByName(ecuStateMachine, "Dem Handling")
        IBswMAutoConfigurationFeature supportComMFeature = getSubfeatureByName(ecuStateMachine, "Support ComM")
        IBswMAutoConfigurationFeature nvmHandlingFeature = getSubfeatureByName(ecuStateMachine, "NvM Handling")
        IBswMAutoConfigurationFeature rteModeFeature = getSubfeatureByName(ecuStateMachine, "Rte Mode Synchronization")
        IBswMAutoConfigurationFeature enableCalloutsFeature = getSubfeatureByName(ecuStateMachine, "Enable Callouts")
        IBswMAutoConfigurationFeature ecumModeHandlingFeature = getSubfeatureByName(ecuStateMachine, "EcuM Mode Handling")
        IBswMAutoConfigurationFeature restartEthIfSwitchPortsParameter = getSubfeatureByName(ecuStateMachine, "Restart EthIf Switch Ports")
        IBswMAutoConfigurationParameter selfRunRequestTimeoutParameter = getParameterByName(ecuStateMachine, "Self Run Request Timeout")
        IBswMAutoConfigurationParameter numberOfRunRequestUserParameter = getParameterByName(ecuStateMachine, "Number of Run Request User")
        IBswMAutoConfigurationParameter numberOfPostRunRequestUserParameter = getParameterByName(ecuStateMachine, "Number of PostRun Request User")
        IBswMAutoConfigurationParameter killAllRunRequestPortParameter = getParameterByName(ecuStateMachine, "Kill All Run Request Port")
        IBswMAutoConfigurationParameter nvmWriteAllTimeoutParameter = getParameterByName(nvmHandlingFeature, "NvM Write All Timeout")
        IBswMAutoConfigurationParameter nvmCancelWriteAllTimeoutParameter = getParameterByName(nvmHandlingFeature, "NvM Cancel Write All Timeout")

        modeMgmt.bswMAutoConfig(eshDomain.identifier) { autoConfigApi ->
            if (ecuStateHandlingDataModel.isActivated == true) {
                // Activate the root feature Ecu State Machine: It is needed to activate it before
                // processing the sub-features due to missing references of BswMModeControl
                if (ecuStateMachine.isEnabled()) {
                    activateFeature(autoConfigApi, ecuStateMachine.identifier, logger)
                }
                // Deactivate sub-features according to data model
                if (demHandlingFeature.isEnabled()) {
                    processFeatureDeactivation(autoConfigApi, demHandlingFeature.identifier, ecuStateHandlingDataModel.demHandling, logger)
                }
                if (supportComMFeature.isEnabled()) {
                    processFeatureDeactivation(autoConfigApi, supportComMFeature.identifier, ecuStateHandlingDataModel.supportComM.isActivated, logger)
                }
                if (nvmHandlingFeature.isEnabled()) {
                    processFeatureDeactivation(autoConfigApi, nvmHandlingFeature.identifier, ecuStateHandlingDataModel.nvmHandling.isActivated, logger)
                }
                if (rteModeFeature.isEnabled()) {
                    processFeatureDeactivation(autoConfigApi, rteModeFeature.identifier, ecuStateHandlingDataModel.rteModeSynchronization, logger)
                }
                if (enableCalloutsFeature.isEnabled()) {
                    processFeatureDeactivation(autoConfigApi, enableCalloutsFeature.identifier, ecuStateHandlingDataModel.enableCallouts, logger)
                }
                if (ecumModeHandlingFeature.isEnabled()) {
                    processFeatureDeactivation(autoConfigApi, ecumModeHandlingFeature.identifier, ecuStateHandlingDataModel.ecumModeHandling, logger)
                }
                if (restartEthIfSwitchPortsParameter.isEnabled()) {
                    processFeatureDeactivation(autoConfigApi, restartEthIfSwitchPortsParameter.identifier, ecuStateHandlingDataModel.restartEthifSwitchPorts, logger)
                }

                // Set parameters according to data model
                if (ecuStateMachine.isEnabled()) {
                    if (selfRunRequestTimeoutParameter.isEnabled()) {
                        setParameters(autoConfigApi, selfRunRequestTimeoutParameter.identifier, ecuStateHandlingDataModel.selfRunRequestTimeout, logger)
                    }
                    if (numberOfRunRequestUserParameter.isEnabled()) {
                        setParameters(autoConfigApi, numberOfRunRequestUserParameter.identifier, ecuStateHandlingDataModel.numberOfRunRequestUser, logger)
                    }
                    if (numberOfPostRunRequestUserParameter.isEnabled()) {
                        setParameters(autoConfigApi, numberOfPostRunRequestUserParameter.identifier, ecuStateHandlingDataModel.numberOfPostRunRequestUser, logger)
                    }
                    if (killAllRunRequestPortParameter.isEnabled()) {
                        setParameters(autoConfigApi, killAllRunRequestPortParameter.identifier, ecuStateHandlingDataModel.killAllRunRequestPort, logger)
                    }
                }
                if (nvmHandlingFeature.isEnabled()) {
                    if (nvmWriteAllTimeoutParameter.isEnabled()) {
                        setParameters(autoConfigApi, nvmWriteAllTimeoutParameter.identifier, ecuStateHandlingDataModel.nvmHandling.nvmWriteAllTimeout, logger)
                    }
                    if (nvmCancelWriteAllTimeoutParameter.isEnabled()) {
                        setParameters(autoConfigApi, nvmCancelWriteAllTimeoutParameter.identifier, ecuStateHandlingDataModel.nvmHandling.nvmCancelWriteAllTimeout, logger)
                    }
                }
                // Deactivate ComM channels according to data model
                supportComMFeature.getSubFeatures().each { commChannel ->
                    if (ecuStateHandlingDataModel.supportComM.channels.find { channel ->
                        (commChannel.identifier == channel.name) && !channel.isActivated
                    }) {
                        deactivateFeature(autoConfigApi, commChannel.identifier, logger)
                    }
                }
            } else {
                // Deactivate all features if domain is deactivated
                deactivateFeature(autoConfigApi, ecuStateMachine.identifier, logger)
            }
        }
    }

    /**
     * Initialize data model with json settings
     * @param model Input for the processing of the configuration elements
     * @param eshDomain Instance of the Ecu State Handling domain of Mode Management
     */
    private void initializeDataWithJson(EcuStateManagementModel model, IBswMAutoConfigurationDomain eshDomain) {
        ecuStateHandlingDataModel.selfRunRequestTimeout = model.autoConfig_EcuStateMachineHandling.selfRunRequestTimeout.toBigDecimal()
        ecuStateHandlingDataModel.numberOfRunRequestUser = model.autoConfig_EcuStateMachineHandling.numberOfRunRequestUsers.toBigInteger()
        ecuStateHandlingDataModel.numberOfPostRunRequestUser = model.autoConfig_EcuStateMachineHandling.numberOfPostRunRequestUsers.toBigInteger()
        ecuStateHandlingDataModel.killAllRunRequestPort = model.autoConfig_EcuStateMachineHandling.killAllRunRequestPortEnabled
        ecuStateHandlingDataModel.demHandling = model.autoConfig_EcuStateMachineHandling.demHandlingEnabled
        ecuStateHandlingDataModel.supportComM.isActivated = model.autoConfig_EcuStateMachineHandling.comMHandlingEnabled
        ecuStateHandlingDataModel.nvmHandling.isActivated = model.autoConfig_EcuStateMachineHandling.nvMHandlingEnabled
        ecuStateHandlingDataModel.nvmHandling.nvmWriteAllTimeout = model.autoConfig_EcuStateMachineHandling.nvMWriteAllTimeout.toBigDecimal()
        ecuStateHandlingDataModel.nvmHandling.nvmCancelWriteAllTimeout = model.autoConfig_EcuStateMachineHandling.nvMCancelWriteAllTimeout.toBigDecimal()
        ecuStateHandlingDataModel.rteModeSynchronization = model.autoConfig_EcuStateMachineHandling.rteModeSynchronisationEnabled
        ecuStateHandlingDataModel.enableCallouts = model.autoConfig_EcuStateMachineHandling.enableCallouts
        ecuStateHandlingDataModel.ecumModeHandling = model.autoConfig_EcuStateMachineHandling.ecuMModeHandlingEnabled
        ecuStateHandlingDataModel.restartEthifSwitchPorts = model.autoConfig_EcuStateMachineHandling.restartEthIfSwitchPorts

        // Store status of ComM channels in the data model
        IBswMAutoConfigurationFeature ecuStateMachine = eshDomain.getRootFeatures().getFirst()
        IBswMAutoConfigurationFeature supportComMFeature = getSubfeatureByName(ecuStateMachine, "Support ComM")
        if (!model.autoConfig_EcuStateMachineHandling.disabledComMChannels.isEmpty()) {
            supportComMFeature.getSubFeatures().each { commChannel ->
                model.autoConfig_EcuStateMachineHandling.disabledComMChannels.find { disabledChannel ->
                    // Store channel as deactivated if it is configured in disabledComMChannels in the json
                    if (commChannel.identifier.split("/").last() == disabledChannel) {
                        ecuStateHandlingDataModel.supportComM.channels.add(new Feature(commChannel.identifier, false))
                    }
                    // Store all remaining channels as activated
                    else {
                        ecuStateHandlingDataModel.supportComM.channels.add(new Feature(commChannel.identifier, true))
                    }
                    //todo: report invalid json entries of channel names
                }
            }
        }
    }

    /**
     * Get the parameter of a feature by providing its name
     * @param feature Feature of Ecu State Handling domain
     * @param parameterName String to identify the parameter name
     * @return Parameter which is identified by matching the name
     */
    private static IBswMAutoConfigurationParameter getParameterByName(IBswMAutoConfigurationFeature feature, String parameterName) {
        feature.getParameters().find { param ->
            param.identifier.contains(parameterName)
        }
    }

    /**
     * Get the sub-feature of a root feature by providing its name
     * @param rootFeature Root feature of Ecu State Handling domain
     * @param featureName String to identify the sub-feature name
     * @return Sub-feature which is identified by matching the name
     */
    private static IBswMAutoConfigurationFeature getSubfeatureByName(IBswMAutoConfigurationFeature rootFeature, String featureName) {
        rootFeature.getSubFeatures().find { subFeature ->
            subFeature.identifier.contains(featureName)
        }
    }

    /**
     * Deactivate a feature depending on it state in the data model
     * @param autoConfigApi Instance of IBswMAutoConfigurationApi
     * @param featureName Name of the feature to be processed
     * @param isActivated Boolean state of the feature in the data model
     * @param logger Instance of OcsLogger
     */
    private static void processFeatureDeactivation(IBswMAutoConfigurationApi autoConfigApi, String featureName, Boolean isActivated, OcsLogger logger) {
        if (!isActivated) {
            deactivateFeature(autoConfigApi, featureName, logger)
        }
    }
}
