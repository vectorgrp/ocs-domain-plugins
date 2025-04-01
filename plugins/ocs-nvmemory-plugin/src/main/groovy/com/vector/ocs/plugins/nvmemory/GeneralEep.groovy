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
/*!        \file  GeneralEep.groovy
 *        \brief  consists of General Eep specific configurations and actions
 *
 *      \details  Configuration of
 *
 *                - Eep Driver Index
 *                - set Eep default settings within InitConfiguration container
 *
 **********************************************************************************************************************/
package com.vector.ocs.plugins.nvmemory
import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIModuleConfiguration
import com.vector.ocs.core.api.OcsLogger
import groovy.transform.PackageScope
@PackageScope
class GeneralEep {
    /**
     * The method createEepDriverIndex is executed during the MAIN phase and is called after the object eep (NvMemoryImpl.groovy) is created
     * createEepDriverIndex will create the parameter Driver Index within EepGeneral and will bet set to value 0.
     */
    static void createEepDriverIndex(OcsLogger logger) {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if (active_module.name == "Eep") {
                        transaction {
                            active_module.bswmdModel().getEepGeneralOrCreate().eepDriverIndexOrCreate.setValue(0)
                            logger.info("EepDriverIndex is set within /ActiveEcuC/Eep/EepGeneral to: 0.")
                        }
                    }
                }
            }
        }
    }

    /**
     * The method setVTTEepDefaultSettingsInitConfiguration is executed during the MAIN phase
     * setEepDefaultSettingsInitConfiguration will set the default settings of the Eep within the InitConfiguration container:
     * - EepSize
     * - Normal- & FastReadBlockSize
     * - Normal- & FastWriteBlockSize
     * - JobCallCycle
     * - BaseAddress
     */
    static void setEepDefaultSettingsInitConfiguration(OcsLogger logger, NvMemoryModel model, int eaNumberOfSectors, int eaSectorSize, long eaStartAddress, String eepModule, Boolean activateVMemSolution) {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                int EepSize = eaNumberOfSectors * eaSectorSize
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if (active_module.name == eepModule) {
                        transaction {
                            def EepInitConfiguration = active_module.bswmdModel().eepInitConfigurationOrCreate
                            if(activateVMemSolution){
                                EepInitConfiguration.eepSizeOrCreate.ceState.setUserDefined(true)
                                EepInitConfiguration.eepBaseAddressOrCreate.ceState.setUserDefined(true)
                            }
                            EepInitConfiguration.eepDefaultModeOrCreate
                            EepInitConfiguration.eepSizeOrCreate.setValue(EepSize)
                            EepInitConfiguration.eepFastReadBlockSizeOrCreate.setValue(256)
                            EepInitConfiguration.eepFastWriteBlockSizeOrCreate.setValue(256)
                            EepInitConfiguration.eepJobCallCycleOrCreate.setValue(0.005)
                            EepInitConfiguration.eepNormalReadBlockSizeOrCreate.setValue(32)
                            EepInitConfiguration.eepNormalWriteBlockSizeOrCreate.setValue(32)
                            EepInitConfiguration.eepBaseAddressOrCreate.setValue(eaStartAddress)
                            logger.info("The following Eep default settings are set within /ActiveEcuC/Eep/Eep_vMemAccMInitConfiguration: BaseAddress: $eaStartAddress, EepSize: $EepSize, NormalWrite-/ReadBlockSize: 256, FastWrite-/ReadBlockSize: 32, JobCallCycle: 5ms.")
                        }
                        transaction{
                            //remove user defined
                            def EepInitConfiguration = active_module.bswmdModel().eepInitConfigurationOrCreate
                            EepInitConfiguration.eepSizeOrCreate.ceState.setUserDefined(false)
                            EepInitConfiguration.eepBaseAddressOrCreate.ceState.setUserDefined(false)
                        }
                    }
                }
            }
        }
    }

    /**
     * The method setEepDefaultMode is executed during the MAIN phase and is called after the object eep (NvMemoryImpl.groovy) is created
     * setEepDefaultMode will create the parameter Eep Default mode in VTTEep and Eep and set it to MEMIF_MODE_SLOW
     */
    static void setEepDefaultMode(){
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if ((active_module.name == "Eep") || (active_module.name == "VTTEep")) {
                        transaction {
                            active_module.bswmdModel().getEepInitConfigurationOrCreate().eepDefaultModeOrCreate
                        }
                    }
                }
            }
        }
    }
}
