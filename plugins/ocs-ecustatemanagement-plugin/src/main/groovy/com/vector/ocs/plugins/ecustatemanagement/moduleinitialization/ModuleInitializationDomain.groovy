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
/*!        \file  ModuleInitializationDomain.groovy
 *        \brief  Handle the processing of the Mode Management domain 'Module Initialization'
 *
 *      \details  Initialize the data model for Module Initialization domain and process the domain features according
 *                to the data model
 *
 *********************************************************************************************************************/

package com.vector.ocs.plugins.ecustatemanagement.moduleinitialization

import com.vector.cfg.dom.deprecated.modemgt.pai.api.IModeManagementApi
import com.vector.cfg.dom.deprecated.modemgt.pai.bswm.IBswMAutoConfigurationDomain
import com.vector.cfg.dom.deprecated.modemgt.pai.bswm.IBswMAutoConfigurationFeature
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIContainer
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.plugins.ecustatemanagement.EcuStateManagementDomain
import com.vector.ocs.plugins.ecustatemanagement.EcuStateManagementModel
import com.vector.ocs.plugins.ecustatemanagement.Feature
import com.vector.ocs.plugins.ecustatemanagement.PartitionConfigSettings

/**
 * Subclass of class EcuStateManagementDomain to handle processing of Mode Management domain 'Module Initialization'.
 * Creates data model for module initializations by initializing known and foreign modules' lists according to json
 * settings and then processes activation/deactivation of init functions according to the data model.
 */
class ModuleInitializationDomain extends EcuStateManagementDomain {
    ModuleInitializationFeature moduleInitializationDataModel = new ModuleInitializationFeature("", true)

    @Override
    void initializeDomainFeatures(EcuStateManagementModel model, IModeManagementApi modeMgmt, MIContainer bswMCfg) {
        IBswMAutoConfigurationDomain moduleInitDomain = modeMgmt.bswMAutoConfigDomain(bswMCfg, "Module Initialization")
        IBswMAutoConfigurationFeature knownModules = getRootFeatureByName(moduleInitDomain, "Initialize known modules")
        IBswMAutoConfigurationFeature foreignModules = getRootFeatureByName(moduleInitDomain, "Initialize foreign modules")

        // Populate the known and foreign modules' lists if user settings for DisabledModuleInitialisations are provided in the json
        model.partitionConfig.each { PartitionConfigSettings partition ->
            // Store the activate/deactivate state of the Module Initialization domain according to the json setting. Default = true
            moduleInitializationDataModel.isActivated = partition.autoConfig_ModuleInitialisationEnabled
            if (bswMCfg.name == partition.bswMConfigName) {
                moduleInitializationDataModel.executeNvMReadAll = partition.executeNvMReadAll
                moduleInitializationDataModel.enableInterrupts = partition.enableInterrupts
                if (!partition.autoConfig_DisabledModuleInitialisations.isEmpty()) {
                    knownModules.getSubFeatures().each { initPhase ->
                        initPhase.getSubFeatures().each { function ->
                            partition.autoConfig_DisabledModuleInitialisations.findAll { disabledModule ->
                                if (function.enabled) {
                                    // Store module init function as deactivated if it is configured in DisabledModuleInitializations in the json
                                    if (function.identifier.split("/").last() == disabledModule) {
                                        moduleInitializationDataModel.knownModulesList.add(new Feature(function.identifier, false))
                                    }
                                    // Store all remaining module init functions as activated
                                    else {
                                        moduleInitializationDataModel.knownModulesList.add(new Feature(function.identifier, true))
                                    }
                                }
                            }
                        }
                    }
                    foreignModules.getSubFeatures().each { function ->
                        partition.autoConfig_DisabledModuleInitialisations.findAll { disabledModule ->
                            if (function.enabled) {
                                // Store module init function as deactivated if it is configured in DisabledModuleInitializations in the json
                                if (function.identifier.split("/").last() == disabledModule) {
                                    moduleInitializationDataModel.foreignModulesList.add(new Feature(function.identifier, false))
                                }
                                // Store all remaining module init functions as activated
                                else {
                                    moduleInitializationDataModel.foreignModulesList.add(new Feature(function.identifier, true))
                                }
                            }
                        }
                    }
                }
            }
        }
        // todo: report not found modules
    }

    @Override
    void processDomainFeatures(EcuStateManagementModel model, OcsLogger logger, IModeManagementApi modeMgmt, MIContainer bswMCfg) {
        IBswMAutoConfigurationDomain moduleInitDomain = modeMgmt.bswMAutoConfigDomain(bswMCfg, "Module Initialization")
        IBswMAutoConfigurationFeature knownModules = getRootFeatureByName(moduleInitDomain, "Initialize known modules")
        IBswMAutoConfigurationFeature foreignModules = getRootFeatureByName(moduleInitDomain, "Initialize foreign modules")
        IBswMAutoConfigurationFeature executeNvmReadAll = getRootFeatureByName(moduleInitDomain, "Execute NvM ReadAll")
        IBswMAutoConfigurationFeature enableInterrupts = getRootFeatureByName(moduleInitDomain, "Enable Interrupts")

        modeMgmt.bswMAutoConfig(bswMCfg, moduleInitDomain.identifier) { autoConfigApi ->
            if (moduleInitializationDataModel.isActivated == true) {
                // All root features are activated by default if domain is activated
                if (moduleInitializationDataModel.executeNvMReadAll) {
                    activateFeature(autoConfigApi, executeNvmReadAll.identifier, logger)
                } else {
                    deactivateFeature(autoConfigApi, executeNvmReadAll.identifier, logger)
                }
                if (moduleInitializationDataModel.enableInterrupts) {
                    activateFeature(autoConfigApi, enableInterrupts.identifier, logger)
                } else {
                    deactivateFeature(autoConfigApi, enableInterrupts.identifier, logger)
                }
                activateFeature(autoConfigApi, knownModules.identifier, logger)
                activateFeature(autoConfigApi, foreignModules.identifier, logger)

                // Deactivate init functions of known modules according to data model
                knownModules.getSubFeatures().each { initPhase ->
                    initPhase.getSubFeatures().each { function ->
                        if (moduleInitializationDataModel.knownModulesList.findAll { module ->
                            (function.identifier == module.name) && !module.isActivated
                        }) {
                            deactivateFeature(autoConfigApi, function.identifier, logger)
                        }
                    }
                }

                // Deactivate init functions of foreign modules according to data model
                foreignModules.getSubFeatures().each { function ->
                    if (moduleInitializationDataModel.foreignModulesList.findAll { module ->
                        (function.identifier == module.name) && !module.isActivated
                    }) {
                        deactivateFeature(autoConfigApi, function.identifier, logger)
                    }
                }
            }
            // Deactivate all root features if domain is deactivated
            else {
                deactivateFeature(autoConfigApi, executeNvmReadAll.identifier, logger)
                deactivateFeature(autoConfigApi, enableInterrupts.identifier, logger)
                deactivateFeature(autoConfigApi, knownModules.identifier, logger)
                deactivateFeature(autoConfigApi, foreignModules.identifier, logger)
            }
        }
        // Reset model for multi partition use case
        moduleInitializationDataModel.reset()
    }

    /**
     * Get the root feature of a domain by providing the feature name
     * @param domain Mode management domain
     * @param featureName String to identify the root feature
     * @return Root feature which is identified by matching the name
     */
    private static IBswMAutoConfigurationFeature getRootFeatureByName(IBswMAutoConfigurationDomain domain, String featureName) {
        domain.getRootFeatures().find { rootFeature ->
            rootFeature.identifier.contains(featureName)
        }
    }
}
