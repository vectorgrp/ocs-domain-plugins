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
/*!        \file  vMemAccM.groovy
 *        \brief  consists of vMemAccM specific configurations, actions and mdf model
 *
 *      \details  Module Activation of
 *                - vMemAccM
 *                - vMemAccM specific configurations
 **********************************************************************************************************************/
package com.vector.ocs.plugins.nvmemory
import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.model.mdf.MIObject
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIModuleConfiguration
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.PluginsCommon
/**
 * vMemAccM specific configurations, actions and mdf model
 */
class vMemAccM extends GeneralFee {
    List<MIObject> vMemAccMSubAddressAreConfigurationReference
    List<MIObject> vMemAccMAddressAreaConfigurationReference
    /**
     * The constructor vMemAccM is executed during the MAIN phase and is called when the object vmemaccm (NvMemoryImpl.groovy) is created.
     * vMemAccM will activate the corresponding modules and defines vMemAccMSubAddressAreConfigurationReference which is needed by the method setSubAddressAreaReference
     * @param logger instance of the logger
     */
    vMemAccM(OcsLogger logger) {
        PluginsCommon.ModuleActivation("/MICROSAR", "vMemAccM", logger)
        activateCompareApi()
        vMemAccMSubAddressAreConfigurationReference = ScriptApi.activeProject.mdfModel("/ActiveEcuC/vMemAccM/vMemAccMAddressAreaConfiguration/vMemAccMSubAddressAreaConfiguration")
        vMemAccMAddressAreaConfigurationReference = ScriptApi.activeProject.mdfModel("/ActiveEcuC/vMemAccM/vMemAccMAddressAreaConfiguration")
    }

    /**
     * The setAddressAreConfigurationsSettings method is executed during the MAIN phase
     * The method will set Sector Settings  within the vMemAccMAddressAreConfiguration block depending on the settings set by the user in NvMemory.json
     * @param logger instance for OCS specific logging
     * @param vMemSectorReference for setting the reference of the vMemSectorReference
     * @param flsNumberOfSectors for setting the number of sectors in the vMemAccMAddressAreConfiguration block
     * @param flsStartAddress for setting the start address in the vMemAccMAddressAreConfiguration block
     */
    void setAddressAreaConfigurationSettings(OcsLogger logger, List<MIObject> vMemSectorReference, int NumberOfSectors, long StartAddress) {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if (active_module.name == "vMemAccM") {
                        transaction {
                            active_module.bswmdModel().VMemAccMAddressAreaConfiguration.first.VMemAccMSubAddressAreaConfiguration.first.VMemAccMSectorRefOrCreate.setRefTargetMdf(vMemSectorReference)
                            active_module.bswmdModel().VMemAccMAddressAreaConfiguration.first.VMemAccMSubAddressAreaConfiguration.first.VMemAccMNumberOfSectors.setValueMdf(NumberOfSectors)
                            active_module.bswmdModel().VMemAccMAddressAreaConfiguration.first.VMemAccMSubAddressAreaConfiguration.first.VMemAccMVirtualStartAddress.setValueMdf(StartAddress)
                            logger.info("The following values of the VMemAccMSubAddressAreaConfiguration configuration block in vvMemAccM/vMemAccMAddressAreaConfiguration/ are set: Number of Sectors: $NumberOfSectors, Sector Reference: $vMemSectorReference, Start Address: $StartAddress.")
                        }
                    }
                }
            }
        }
    }

    /**
     * The activateCompareApi method is executed during the MAIN phase
     * The method will set the checkbox for the "CompareApi" within vMemAccM General to true. (Aligned to activated Compare Api Checkbox in Fls_30_vMemAccMGeneral)
     */
    void activateCompareApi() {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if (active_module.name == "vMemAccM") {
                        transaction {
                            active_module.bswmdModel().VMemAccMGeneral.VMemAccMCompareApi.setValue(true)
                        }
                    }
                }
            }
        }
    }
}
