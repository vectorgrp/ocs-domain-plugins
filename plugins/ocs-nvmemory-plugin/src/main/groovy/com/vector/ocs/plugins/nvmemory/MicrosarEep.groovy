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
/*!        \file  MicrosarEep.groovy
 *        \brief  consists of Microsar Eep specific configurations, actions and mdf model
 *
 *      \details  Module Activation of
 *                - Eep & Eep VTT
*
 **********************************************************************************************************************/
package com.vector.ocs.plugins.nvmemory

import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.model.mdf.MIObject
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIModuleConfiguration
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.PluginsCommon
/**
 * MicrosarEep consists of Microsar Ea specific configurations, actions and mdf model
 */
class MicrosarEep extends GeneralEep{
    List<MIObject> eepMdfGeneral
    List <MIObject> eepMdfConfigSet
    /**
     * The method init is executed during the MAIN phase and is called after the object eep (NvMemoryImpl.groovy) is created
     * init will activate the corresponding modules
     * @param logger instance of the logger
     */
    void init(OcsLogger logger) {
        PluginsCommon.ModuleActivation("/MICROSAR", "Eep", logger)
        PluginsCommon.ModuleActivation("/MICROSAR/VTT", "VTTEep", logger)
    }

    /**
     * The method initMdfModels is executed during the MAIN and Cleanup phase and is called after the object eep (NvMemoryImpl.groovy) is created
     * mdfModels will create the mdf models for specific eep References which are needed for other module parameters.
     */
    void initMdfModels(){
        eepMdfGeneral = ScriptApi.activeProject.mdfModel("/ActiveEcuC/Eep/EepGeneral")
        eepMdfConfigSet= ScriptApi.activeProject.mdfModel("/ActiveEcuC/Eep/EepInitConfiguration")
    }


    /**
     * The method getEepSettings is executed during the MAIN phase and is called within SetupEaPartition
     * getEepSettings will get the settings of the Eep related to size, erase unit size and base address
     * @return: EepData consists of size, base address of eep and the erase unit size
     */
    static getEepSettings(){
        int EepSize = 0
        long EepBaseAddress = 0
        long EepEraseUnitSize = 0
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if (active_module.name == "Eep") {
                        transaction {
                            def EepInitConfiguration = active_module.bswmdModel().eepInitConfigurationOrCreate
                            def EepPublishedInformation = active_module.bswmdModel().eepPublishedInformationOrCreate
                            EepSize = EepInitConfiguration.eepSizeOrCreate.getValue()
                            EepBaseAddress = EepInitConfiguration.eepBaseAddressOrCreate.getValue()
                            EepEraseUnitSize = EepPublishedInformation.eepEraseUnitSizeOrCreate.getValue()
                        }
                    }
                }
            }
        }
        ArrayList<Long> EepData = [EepSize, EepBaseAddress, EepEraseUnitSize]
        return EepData
    }

}
