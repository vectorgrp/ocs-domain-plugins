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
/*!        \file  vMem.groovy
 *        \brief  consists of vMem specific configurations, actions and mdf model
 *
 *      \details  Module Activation of
 *                - vMem
 *                - vMem specific configurations
 **********************************************************************************************************************/
package com.vector.ocs.plugins.nvmemory
import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.model.mdf.MIObject
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIModuleConfiguration
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.PluginsCommon

/**
 * vMem consists of vMem specific configurations, actions and mdf model
 */
class vMem extends GeneralFee {
    List<MIObject> vMemSectorReference
    /**
     * The constructor vMem is executed during the MAIN phase and is called when the object vmem (NvMemoryImpl.groovy) is created.
     * vMem will activate the corresponding modules and defines vMemSectorReference which is needed by the method setAddressAreaConfigurationSettings
     * @param logger instance of the logger
     */
    vMem(OcsLogger logger) {
        PluginsCommon.ModuleActivation("/MICROSAR/vMem_30_Vtt", "vMem", logger)
        PluginsCommon.ModuleActivation("/MICROSAR/VTT", "VTTvMem", logger)
        vMemSectorReference = ScriptApi.activeProject.mdfModel("/ActiveEcuC/vMem/vMem_30_Vtt_vMemInstance/vMemSectorList/vMemSector")
    }


    /**
     * The setSectorSettings method is executed during the MAIN phase
     * The method will set the Sector settings for the vMem module within the vMemSector configuration block depending on the settings set by the user in NvMemory.json
     * @param logger instance for OCS specific logging
     * @param NumberOfSectors for setting the number of sectors in the vMemSector configuration block
     * @param PageSize for setting the page size in the vMemSector configuration block
     * @param SectorSize for setting the sector size in the vMemSector configuration block
     * @param StartAddress for setting the start address in the vMemSector configuration block
     */
    static void setSectorSettings(OcsLogger logger, int NumberOfSectors, int PageSize, SectorSize, StartAddress) {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if (active_module.name == "vMem") {
                        transaction {
                            active_module.bswmdModel().VMemInstance.first.VMemSectorListOrCreate.VMemSector.first.VMemNumberOfSectorsOrCreate.setValueMdf(NumberOfSectors)
                            active_module.bswmdModel().VMemInstance.first.VMemSectorListOrCreate.VMemSector.first.VMemPageSizeOrCreate.setValueMdf(PageSize)
                            active_module.bswmdModel().VMemInstance.first.VMemSectorListOrCreate.VMemSector.first.VMemSectorSizeOrCreate.setValueMdf(SectorSize)
                            active_module.bswmdModel().VMemInstance.first.VMemSectorListOrCreate.VMemSector.first.VMemStartAddressOrCreate.setValueMdf(StartAddress)
                            logger.info("The following values of the vMemSector configuration block in vMem/vMemInstance/vMemSectorList are set: Number of Sectors: $NumberOfSectors, Page Size: $PageSize, Sector Size: $SectorSize, Start Address: $StartAddress.")
                        }
                    }
                }
            }
        }
    }
}
