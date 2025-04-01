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
/*!        \file  RuntimeSystemScript.groovy
 *        \brief  The RuntimeSystem Plugin addresses configuration elements that belong to the Runtime System.
 *
 *      \details  Depending on the customer input the plugin will create multiple:
 *                - OsCores, OsApplications, OsTasks, OsCounters and OsResource
 *                - EcucCoreDefinition and EcucPartition
 *                - The RuntimeSystem plugin will apply the mapping of the different configuration elements
 *                  to each other.
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.runtimesystem

import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.PluginsCommon

import com.vector.ocs.plugins.runtimesystem.dataclasses.RtsDataModel
import groovy.transform.PackageScope

/**
 * The RuntimeSystemScript class address the configuration of the Os and EcuC BSW modules.
 * In addition it realize the runnable to task mappings.
 */
@PackageScope
class RuntimeSystemScript {

    /**
     * During the cleanup the given RuntimeSystemModel is processed to apply the runnable to task mapping.
     * In addition several solving actions are triggered to achieve a synchronization of the BSW modules.
     * @param model which should be processed in the context of the RuntimeSystem plugin.
     * @param logger instance of the OcsLogger.
     */
    static void cleanup(RuntimeSystemModel model, OcsLogger logger) {
        if (model.enableCleanupPhase) {
            logger.info("Cleanup after processing of the RuntimeSystemPlugin model.")
            logger.debug("Check that Os module is available.")
            Boolean isOsPresent = PluginsCommon.ConfigPresent(RuntimeSystemConstants.OS_DEFREF)
            logger.debug("Check that EcuC module is available.")
            Boolean isEcucPresent = PluginsCommon.ConfigPresent(RuntimeSystemConstants.ECUC_DEFREF)
            RtsDataModel dataModel = createInternalDataModel(model, logger)
            if (isOsPresent && isEcucPresent) {
                RuntimeSystemConnectAndMap.cleanupRuntimeSystemConfiguration(dataModel, logger)
            }
        } else {
            logger.info("Cleanup of the RuntimeSystemPlugin is disabled.")
        }
    }

    /**
     * Process the given RuntimeSystemModel and creates the configuration elements for the Os and EcuC BSW modules.
     * Beside creating new containers the references between them are applied too.
     * @param model which should be processed in the context of the RuntimeSystem plugin.
     * @param logger instance of the OcsLogger.
     */
    static void run(RuntimeSystemModel jsonModel, OcsLogger logger) {
        logger.info("Start processing of the RuntimeSystemPlugin model.")
        logger.debug("Check that Os module is available.")
        Boolean isOsPresent = PluginsCommon.ConfigPresent(RuntimeSystemConstants.OS_DEFREF)
        Boolean correctCoreConfiguration = false
        RtsDataModel dataModel = createInternalDataModel(jsonModel, logger)
        if (isOsPresent) {
            correctCoreConfiguration = RuntimeSystemOsConfig.processOsConfiguration(dataModel, logger)
        }

        if (correctCoreConfiguration) {
            logger.debug("Check that EcuC module is available.")
            Boolean isEcucPresent = PluginsCommon.ConfigPresent(RuntimeSystemConstants.ECUC_DEFREF)
            if (isEcucPresent) {
                RuntimeSystemEcucConfig.processEcucConfiguration(dataModel, logger)
            }

            if (isOsPresent && isEcucPresent) {
                RuntimeSystemConnectAndMap.createReferences(dataModel, logger)
            }
        } else {
            logger.error("Due to incorrect core configuration in the JSON file compared to the available " +
                    "cores in the OsPhysicalCores the processing of the RuntimeSystem in the RUN phase will not be continued.")
        }

    }

    /**
     * Create an internal data model from a merge of the given json model and necessary default model structures.
     * @param model which should be processed in the context of the RuntimeSystem plugin.
     * @param logger instance of the OcsLogger.
     * @return instance of RTSDataModel that is ready to be used in following business logic processing.
     */
    static RtsDataModel createInternalDataModel(RuntimeSystemModel jsonModel, OcsLogger logger) {
        List<Integer> physicalCoreList = RuntimeSystemModelMerge.getPhysicalCoreList(logger)
        RtsDataModel dataModel = RuntimeSystemModelMerge.mergeModels(jsonModel, physicalCoreList, logger)
        return dataModel
    }
}
