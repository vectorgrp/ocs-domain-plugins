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
/*!        \file  PluginsCommon.groovy
 *        \brief  Implements the common methods used by all plugins
 *
 *      \details  The following methods are implemented
 *                - ConfigPresent: Checks if the provided defRef BSW module is instantiated in the active project.
 *                - DefRefPresent: Checks if the provided defRef is present in the SIP.
 *                - isChangeable: Checks if the provided parameter can be modified.
 *                - modelSynchronization: Trigger model synchronization including exception handling.
 *                - ModuleActivation: Combining the ConfigPresent, DefRefPresent and module activation.
 *
 *********************************************************************************************************************/

package com.vector.ocs.lib.shared

import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.automation.api.version.IProductVersionApiEntryPoint
import com.vector.cfg.automation.scripting.api.project.IProject
import com.vector.cfg.model.access.DefRef
import com.vector.cfg.model.access.IReferrableAccess
import com.vector.cfg.model.mdf.MIObject
import com.vector.cfg.model.mdf.commoncore.autosar.MIReferrable
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIParameterValue
import com.vector.cfg.model.mdf.model.autosar.ecucparamdef.MIModuleDef
import com.vector.ocs.core.api.OcsLogger

class PluginsCommon {
    /**
     * Check if the provided defRef BSW module is instantiated in the active project.<br>
     * For example: /MICROSAR/NvM
     * @param defRef string representing the defRef
     * @return true if the BSW module is instantiated in the active project otherwise false
     */
    static boolean ConfigPresent(String defRef) {
        List<MIObject> cfgElements = ScriptApi.activeProject.mdfModel(defRef)
        if (cfgElements.size() != 0) {
            return true
        }
        return false
    }

    /**
     * Check if the provided defRef is present in the SIP. It must not be mandatory instantiated in the active project.<br>
     * For example: /MICROSAR/NvM
     * @param project instance of the project
     * @param defRef string representing the defRef
     * @param logger instance of the logger
     * @return true if the defRef is present in the SIP, otherwise false
     */
    static boolean DefRefPresent(IProject project, String defRef, OcsLogger logger) {
        IReferrableAccess referrableAccess = project.getInstance(IReferrableAccess)
        boolean result = false
        if ((defRef != null) && (!defRef.empty)) {
            MIReferrable miReferrable = referrableAccess.getReferrableByPath(defRef)
            if (miReferrable instanceof MIModuleDef) {
                result = true
                logger.info("$defRef found.")
            } else {
                logger.warn("$defRef not found.")
            }
        } else {
            logger.warn("Parameter of type String 'defRef' is not allowed to be empty or null.")
        }
        return result
    }

    /**
     * Check if the provided parameter has the configuration element state changeable. As the MIParameterValue is a MDF
     * model datatype, a transformation of BSWMD model parameters can be achieved via call of the <i>mdfObject</i>
     * method.
     * @param parameter MDF model representation of the parameter
     * @param logger instance of the logger
     * @return true if the parameter is changeable, otherwise false
     */
    static boolean isChangeable(MIParameterValue parameter, OcsLogger logger) {
        boolean result = false
        if (parameter != null) {
            result = parameter.ceState.isChangeable()
            if (!result) {
                logger.error("$parameter cannot be changed because the ceState is not changeable.")
            }
        } else {
            logger.warn("Could not check ceState because the provided parameter is null.")
        }

        return result
    }

    /**
     * Trigger the project.modelSynchronization.synchronize() API surrounded by a try catch block in case an exception
     * occurs. Based on the API description the model synchronization should not be triggered within a transaction block.
     * @param project instance of the active project
     * @param logger instance of the logger
     */
    static void modelSynchronization(IProject project, OcsLogger logger) {
        try {
            project.modelSynchronization.synchronize()
        } catch (Exception exception) {
            logger.error("Error occured during model synchronization:\n${exception.printStackTrace()}")
        }
    }

    /**
     * The ModuleActivation method is executed during the MAIN phase and will activate wanted modules in the CFG5
     * @param pack for packagesPartOfDefRef  of DefRef.create (needed for module activation)
     * @param module for defRefWithoutPackage of DefRef.create (needed for module activation)
     * @param logger instance of the logger
     */
    static void ModuleActivation(String pack, String module, OcsLogger logger) {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                String defRefName = pack + "/" + module

                if (!(ConfigPresent(defRefName))) {
                    if (DefRefPresent(prj, defRefName, logger)) {
                        transaction {
                            operations.activateModuleConfiguration((DefRef.create(pack, module)))
                        }
                        logger.info("Activating module $module since DefRef is inherited in SIP but no $defRefName activated.")
                    } else {
                        logger.info("No Activation of module $module since no $defRefName is found in SIP.")
                    }
                } else {
                    logger.info("No Activation of module $module since $defRefName is already activated.")
                }
            }
        }
    }

    /**
     * Determines the minor version of the DaVinci Configurator Classic (CFG5)
     * @return cfgMinorVersion is the minor version of the DaVinci Configurator Classic (CFG5)
     */
    static Number Cfg5MinorVersion() {
        String versionString = ScriptApi.scriptCode.getInstance(IProductVersionApiEntryPoint.class).versions.daVinciConfiguratorVersion
        String daVinciVersion = versionString.replace("DaVinciConfigurator\t", "")
        int cfgMinorVersion = daVinciVersion.substring(2, 4).toInteger()
        return cfgMinorVersion
    }
}
