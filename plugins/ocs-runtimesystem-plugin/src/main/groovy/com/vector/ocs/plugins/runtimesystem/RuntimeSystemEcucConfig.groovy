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
/*!        \file  RuntimeSystemScriptEcucConfig.groovy
 *        \brief  Handle the EcuC specific configuration aspects of the RuntimeSystem.
 *
 *      \details  Create the following configuration elements:
 *                - EcucCoreDefinition
 *                - EcucPartition
 *                Apply common EcuC configuration parameters
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.runtimesystem

import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.automation.model.ecuc.microsar.ecuc.EcuC
import com.vector.cfg.automation.model.ecuc.microsar.ecuc.ecuchardware.ecuccoredefinition.EcucCoreDefinition
import com.vector.cfg.automation.model.ecuc.microsar.ecuc.ecucpartitioncollection.ecucpartition.EcucPartition
import com.vector.cfg.automation.model.ecuc.microsar.ecuc.ecucpartitioncollection.ecucpartition.asil.ASIL
import com.vector.cfg.automation.model.ecuc.microsar.ecuc.ecucpartitioncollection.ecucpartition.asil.EASIL
import com.vector.cfg.automation.model.ecuc.microsar.os.Os
import com.vector.cfg.gen.core.bswmdmodel.param.GInteger
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.PluginsCommon
import com.vector.ocs.plugins.runtimesystem.dataclasses.RtsApplication
import com.vector.ocs.plugins.runtimesystem.dataclasses.RtsCore
import com.vector.ocs.plugins.runtimesystem.dataclasses.RtsDataModel
import groovy.transform.PackageScope

// define alias

/**
 * Handle the EcuC specific configuration aspects of the RuntimeSystem.
 */
@PackageScope
class RuntimeSystemEcucConfig {

    /**
     * Process the RuntimeSystemModel and created the corresponding EcuC BSW module configuration.<br>
     * Key configuration elements which will be added during processing:
     * <ul>
     *     <li>EcucCoreDefinition</li>
     *     <li>EcucPartition</li>
     * </ul>
     * @param model RuntimeSystemModel which contains the specified configuration settings.
     * @param logger instance of the OcsLogger.
     */
    static void processEcucConfiguration(RtsDataModel model, OcsLogger logger) {
        logger.info("Creating EcuC specific configuration elements...")
        ScriptApi.activeProject() {
            EcuC ecucModule = it.bswmdModel(EcuC.DefRef).single
            int numberOfCores = model.getCores().size()
            Os osModule
            final List<RtsCore> coreModel = model.getCores()
            if (PluginsCommon.ConfigPresent(RuntimeSystemConstants.OS_DEFREF)) {
                osModule = it.bswmdModel(Os.DefRef).single
                numberOfCores = RuntimeSystemOsConfig.checkOsPhysicalCoreConfiguration(osModule, numberOfCores, logger)
            } else {
                logger.warn("Cannot access the OsPhysicalCores. Ensure that " +
                        "the JSON file parameter 'defaultNrOfCores' matches your number of OsPhysicalCores.")
            }

            transaction {
                coreModel.eachWithIndex { core, index ->
                    if ((index < osModule.osCore.size()) && (index < numberOfCores)) {
                        String ecucCoreName = getEcucCoreDefinitionName(core.name)

                        ecucModule.ecucHardwareOrCreate.each {
                            // 1. Create the EcucCoreDefinition if none already exist
                            if (it.ecucCoreDefinition.size() < numberOfCores) {
                                createEcucCoreDefinition(ecucModule, ecucCoreName, index, logger)
                            }
                        }
                        // 2. Create the EcucPartition
                        core.applications.each { RtsApplication application ->
                            String ecucPartitionName = getEcucPartitionName(application.name)
                            createEcucPartition(ecucModule, ecucPartitionName, application, logger)
                        }

                        // 3. Create the EcucPartition for existing SystemApplication (created by OS)
                        String ecucPartitionNameSystemApplication = getEcucPartitionName("SystemApplication_" + core.name)
                        createEcucPartition(ecucModule, ecucPartitionNameSystemApplication, RuntimeSystemOsConfig.getHighestAsilLevel(core.applications), logger)


                        // 4. Configure ASIL of existing EcucPartition (created by OS)
                        ecucModule.ecucPartitionCollection.each {
                            it.ecucPartition.each {
                                if (!it.ASIL.hasValue()) {
                                    it.ASIL.setValue(EASIL.QM)
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    /**
     * Create an EcucCoreDefinition and set the basic parameters based on the RuntimeSystemMode. In case the
     * EcucCoreDefinition already exists based on the name it will be used for the processing.
     * @param ecucModule instance of the EcuC BSW Module.
     * @param ecucCoreDefinitionName name of the EcucCoreDefinition.
     * @param index as coreId number to which this EcucCoreDefinition belongs to.
     * @param logger instance of the OcsLogger.
     */
    private static void createEcucCoreDefinition(EcuC ecucModule, String ecucCoreDefinitionName, int index, OcsLogger logger) {
        logger.info("Create EcucCoreDefinition with name $ecucCoreDefinitionName.")
        EcucCoreDefinition createdEcucCore = ecucModule.ecucHardwareOrCreate.ecucCoreDefinition.byNameOrCreate(ecucCoreDefinitionName)
        GInteger coreIdParameter = createdEcucCore.ecucCoreIdOrCreate
        if (PluginsCommon.isChangeable(coreIdParameter.mdfObject, logger)) {
            coreIdParameter.setValueMdf(index.toLong())
        }
    }

    /**
     * Create an EcucPartition and set the basic parameters based on the RuntimeSystemMode -> ApplicationModel. In case
     * the EcucPartition already exists based on the name it will be used for the processing.
     * @param ecucModel ecucModule instance of the EcuC BSW Module.
     * @param ecucPartitionName name for the EcucPartition.
     * @param applicationModel RuntimeSystemModel based description of the ApplicationModel.
     * @param logger logger instance of the OcsLogger.
     */
    private static void createEcucPartition(EcuC ecucModel, String ecucPartitionName, RtsApplication applicationModel,
                                            OcsLogger logger) {
        logger.info("Create EcucPartition with name $ecucPartitionName.")
        EcucPartition createdEcucPartition = ecucModel.ecucPartitionCollectionOrCreate.ecucPartition.
                byNameOrCreate(ecucPartitionName)
        ASIL asilParameter = createdEcucPartition.getASILOrCreate()
        if (PluginsCommon.isChangeable(asilParameter.mdfObject, logger)) {
            asilParameter.setValueMdf(applicationModel.asilLevel.toString())
        }
    }

    /**
     * Create an EcucPartition and set the basic parameters based on the RuntimeSystemMode -> ApplicationModel. In case
     * the EcucPartition already exists based on the name it will be used for the processing.
     * @param ecucModel ecucModule instance of the EcuC BSW Module.
     * @param ecucPartitionName name for the EcucPartition.
     * @param highestAsil for setting the SystemApplication's asil level
     * @param logger logger instance of the OcsLogger.
     */
    private static void createEcucPartition(EcuC ecucModel, String ecucPartitionName, Asil highestAsil,
                                            OcsLogger logger) {
        logger.info("Create EcucPartition with name $ecucPartitionName.")
        EcucPartition createdEcucPartition = ecucModel.ecucPartitionCollectionOrCreate.ecucPartition.
                byNameOrCreate(ecucPartitionName)
        ASIL asilParameter = createdEcucPartition.getASILOrCreate()
        if (PluginsCommon.isChangeable(asilParameter.mdfObject, logger)) {
            asilParameter.setValueMdf(highestAsil.toString())
        }
    }

    // Helper methods

    /**
     * Find an EcucCoreDefinition by applying the EcucCoreDefinition name schema.
     * @param ecucModule instance of the EcuC BSW module.
     * @param coreName name of the OsCore which is used to apply the name schema.
     * @return EcucCoreDefinition container instance that matches the name.
     */
    static EcucCoreDefinition getEcucCoreDefinitionByName(EcuC ecucModule, String coreName) {
        String ecucCoreName = getEcucCoreDefinitionName(coreName)
        return ecucModule.ecucHardwareOrCreate.ecucCoreDefinition.byNameOrCreate(ecucCoreName)
    }

    /**
     * Find an EcucPartition by applying the EcucCoreDefinition name schema.
     * @param ecucModule instance of the EcuC BSW module.
     * @param applicationName name of the OsApplication which is used to apply the name schema.
     * @return EcucPartition container instance that matches the name.
     */
    static EcucPartition getEcucPartitionByName(EcuC ecucModule, String applicationName) {
        String ecucPartitionName = getEcucPartitionName(applicationName)
        return ecucModule.ecucPartitionCollectionOrCreate.ecucPartition.byNameOrCreate(ecucPartitionName)
    }

    /**
     * Create the EcucCoreDefinition name based on the name schema.<br>
     * <b>schema:</b> SysEcuC_coreName
     * @param coreName name of the OsCore.
     * @return EcucCoreDefinition name.
     */
    static String getEcucCoreDefinitionName(String coreName) {
        return "EcucCoreDefinition_" + coreName
    }

    /**
     * Create the EcucPartition name based on the name schema.<br>
     * <b>schema:</b> SysEcuC_applicationName
     * @param applicationName name of the OsApplication.
     * @return EcucPartition name.
     */
    static String getEcucPartitionName(String applicationName) {
        return "EcucPartition_" + applicationName
    }
}
