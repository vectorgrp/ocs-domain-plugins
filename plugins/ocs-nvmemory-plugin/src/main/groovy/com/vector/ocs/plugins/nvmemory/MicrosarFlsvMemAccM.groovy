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
 *  ------------------------------------------------------------------------------------------------------------------*/
/*!        \file  MicrosarFlsvMemAccM.groovy
 *        \brief  consists of Fls_30_vMemAccM specific configurations, actions and mdf model
 *
 *      \details  Module Activation of
 *                - Fls_30_vMemAccM
 *                - Fls_30_vMemAccM specific configurations
 **********************************************************************************************************************/
package com.vector.ocs.plugins.nvmemory

import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.automation.model.ecuc.microsar.vtt.vttfls.VTTFls
import com.vector.cfg.model.mdf.MIObject
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIModuleConfiguration
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.PluginsCommon

/**
 * MicrosarFlsvMemAccM consists of Fls_30_vMemAccM specific configurations, actions and mdf model
 */
class MicrosarFlsvMemAccM extends GeneralFee {
    public List<MIObject> flsConfigSet
    public List<MIObject> flsMdfGeneral
    /**
     * The method init is executed during the MAIN phase and is called after the object fls (NvMemoryImpl.groovy) is created
     * init will activate the corresponding modules and define needed mdf model
     * @param logger instance of the logger
     */
    void init(OcsLogger logger) {
        PluginsCommon.ModuleActivation("/MICROSAR/Fls_30_vMemAccM", "Fls", logger)
        PluginsCommon.ModuleActivation("/MICROSAR/VTT", "VTTFls", logger)
    }

    /**
     * The method createFlsConfigSetVTTFls is executed during the MAIN phase and is called within fls.init()
     * it adds the FlsConfigSet container within the VTTFls module.
     */
    static void createFlsConfigSetVTTFls() {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                if (PluginsCommon.ConfigPresent("/MICROSAR/VTT/VTTFls")) {
                    transaction {
                        VTTFls vttfls = bswmdModel(VTTFls.DefRef).single()
                        vttfls.flsConfigSetOrCreate
                    }
                }
            }
        }
    }

    /**
     * The method initMdfModels is executed during the MAIN and Cleanup phase and is called after the object fls (NvMemoryImpl.groovy) is created
     * mdfModels will create the mdf models for specific Fls References which are needed for other module parameters.
     */
    void initMdfModels() {
        flsConfigSet = ScriptApi.activeProject.mdfModel("/ActiveEcuC/Fls/Fls_30_vMemAccMConfigSet")
        flsMdfGeneral = ScriptApi.activeProject.mdfModel("/ActiveEcuC/Fls/Fls_30_vMemAccMGeneral")
    }

    /**
     * The setSubAddressAreaReference method is executed during the MAIN phase
     * The method will set the Reference to the vMemAccMSubAddressAreaConfiguration within the fls sector.
     * @param logger instance for OCS specific logging
     * @param vMemAccMSubAddressAreConfigurationReference for setting the reference in the fls Sector configuration block
     */
    static void setSubAddressAreaReference(OcsLogger logger, List<MIObject> vMemAccMSubAddressAreConfigurationReference) {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if (active_module.name == "Fls") {
                        transaction {
                            active_module.bswmdModel().flsConfigSetOrCreate.flsSectorListOrCreate.flsSector.first.flsSubAddressAreaRefOrCreate.setRefTargetMdf(vMemAccMSubAddressAreConfigurationReference)
                            logger.info("The FlsSubAddressAreaRef within Fls_30_vMemAccM/Fls/FlsConfigSet/FlsSectorList/FlsSector is set to: $vMemAccMSubAddressAreConfigurationReference.")
                        }
                    }
                }
            }
        }
    }

    /**
     * The setSpecifiedEraseCycles method is executed during the MAIN phase
     * The method will set the specified erase cycles within the fls published information depending on the settings set by the user in NvMemory.json
     * @param logger instance for OCS specific logging
     * @param specifiedEraseCycles to set the specified erased cycles value within the fls published information
     */
    static void setSpecifiedEraseCycles(OcsLogger logger) {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if (active_module.name == "Fls") {
                        transaction {
                            active_module.bswmdModel().flsPublishedInformationOrNull.flsSpecifiedEraseCyclesOrCreate.setValue(4294967295)
                            logger.info("The following value is set for the specified erase cycles within FlsPublishedInformation: 4294967295.")
                        }
                    }
                }
            }
        }
    }
}
