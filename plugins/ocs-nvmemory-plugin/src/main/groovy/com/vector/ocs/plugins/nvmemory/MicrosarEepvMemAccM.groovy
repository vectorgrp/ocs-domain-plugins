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
/*!        \file  MicrosarEepvMemAccM.groovy
 *        \brief  consists of Eep_30_vMemAccM specific configurations, actions and mdf model
 *
 *      \details  Module Activation of
 *                - Eep_30_vMemAccM
 *                - Eep_30_vMemAccM specific configurations
 **********************************************************************************************************************/
package com.vector.ocs.plugins.nvmemory
import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.model.mdf.MIObject
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIModuleConfiguration
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.PluginsCommon
/**
 * MicrosarEepvMemAccM consists of Eep_30_vMemAccM specific configurations, actions and mdf model
 */
class MicrosarEepvMemAccM extends GeneralEep {
    List <MIObject> eepMdfConfigSet
    List<MIObject> eepMdfGeneral
    /**
     * The method init is executed during the MAIN phase and is called after the object eep (NvMemoryImpl.groovy) is created
     * init will activate the corresponding modules
     * @param logger instance of the logger
     */
    void init(OcsLogger logger) {
        PluginsCommon.ModuleActivation("/MICROSAR/Eep_30_vMemAccM", "Eep", logger)
        PluginsCommon.ModuleActivation("/MICROSAR/VTT", "VTTEep", logger)
    }

    /**
     * The method initMdfModels is executed during the MAIN and Cleanup phase and is called after the object eep (NvMemoryImpl.groovy) is created
     * mdfModels will create the mdf models for specific eep References which are needed for other module parameters.
     */
    void initMdfModels(){
        eepMdfConfigSet= ScriptApi.activeProject.mdfModel("/ActiveEcuC/Eep/Eep_30_vMemAccMInitConfiguration")
        eepMdfGeneral = ScriptApi.activeProject.mdfModel("/ActiveEcuC/Eep/Eep_30_vMemAccMGeneral")
    }

    /**
     * The setAddressAreaReference method is executed during the MAIN phase
     * The method will set the Reference to the vMemAccMAddressAreaConfiguration within Eep Init Configuration container.
     * @param logger instance for OCS specific logging
     * @param vMemAccMAddressAreConfigurationReference for setting the area address reference in the Eep Init Configuration container
     */
    static void setAddressAreaReference(OcsLogger logger, List<MIObject> vMemAccMAddressAreaConfigurationReference) {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if (active_module.name == "Eep") {
                        transaction {
                            active_module.bswmdModel().eepInitConfigurationOrCreate.eepAddressAreaRefOrCreate.setRefTargetMdf(vMemAccMAddressAreaConfigurationReference)
                            logger.info("The EepAddressAreaRef within /ActiveEcuC/Eep/Eep_30_vMemAccMInitConfiguration/EepAddressAreaRef is set to: $vMemAccMAddressAreaConfigurationReference.")
                        }
                    }
                }
            }
        }
    }

    /**
     * The method setEepDefaultSettingsPublishedInformation is executed during the MAIN phase
     * setEepDefaultSettingsInitConfiguration will set the default settings of the Eep within the PublishedInformation container:
     * - AllowedWriteCycles
     * - EraseTime
     * - EraseUnitSize
     * - EraseValue
     * - Write- & ReadUnitSize
     * The VTTEep has to be configured and is not automatically mirrored with the Eep_30_vMemAccM module.
     */
    static void setEepDefaultSettingsPublishedInformation(OcsLogger logger, int eepSectorSize, int eepPageSize){
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if ((active_module.name == "Eep") || (active_module.name == "VTTEep")) {
                        transaction {
                            def EepPublishedInformation = active_module.bswmdModel().eepPublishedInformationOrCreate
                            EepPublishedInformation.eepAllowedWriteCyclesOrCreate.setValue(50000)
                            EepPublishedInformation.eepEraseTimeOrCreate.setValue(20000)
                            EepPublishedInformation.eepEraseUnitSizeOrCreate.ceState.setUserDefined(true)
                            EepPublishedInformation.eepEraseUnitSizeOrCreate.setValue(eepSectorSize)
                            EepPublishedInformation.eepEraseValueOrCreate.setValue(255)
                            EepPublishedInformation.eepReadUnitSizeOrCreate.setValue(1)
                            EepPublishedInformation.eepWriteUnitSizeOrCreate.ceState.setUserDefined(true)
                            EepPublishedInformation.eepWriteUnitSizeOrCreate.setValue(eepPageSize)
                            logger.info("The following Eep default settings are set within /ActiveEcuC/Eep/Eep_30_vMemAccMPublishedInformation: AllowedWriteCycles: 5000, EraseTime: 20000, EraseUnitSize: $eepSectorSize, EraseValue: 255, ReadUnitSize: 1 Byte, WriteUnitSize: $eepPageSize.")
                        }
                        transaction{
                            def EepPublishedInformation = active_module.bswmdModel().eepPublishedInformationOrCreate
                            EepPublishedInformation.eepEraseUnitSizeOrCreate.ceState.setUserDefined(false)
                            EepPublishedInformation.eepWriteUnitSizeOrCreate.ceState.setUserDefined(false)

                        }
                    }
                }
            }
        }
    }

    /**
     * The method setupEepMainFunction is executed during the MAIN phase
     * setupEepMainFunction configuration steps will trigger the creation of the Eep_MainFunction
     * - setting eepMainFunctionPeriod to 0.005s
     * - setting eepMainFunctionTriggering to FixedCycleTime
     */
    static void setupEepMainFunction(logger) {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if (active_module.name == "Eep") {
                        transaction {
                            active_module.bswmdModel().getEepGeneralOrCreate().eepMainFunctionPeriodOrCreate.setValueMdf(0.005D)
                            active_module.bswmdModel().getEepGeneralOrCreate().eepMainFunctionTriggeringOrCreate.setValueMdf("FixedCycleTime")
                            logger.info("The eepMainFunctionPeriod is set to 0.005s and the eepMainFunctionTriggering is set to FixedCycleTime within container /ActiveEcuC/Eep/Eep_30_vMemAccMGeneral.")
                        }
                    }
                }
            }
        }
    }
}
