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
/*!        \file  MicrosarNvM.groovy
 *        \brief  consists of Microsar NvM specific configurations
 *
 *      \details  configuration of
 *                - activate NvM Dem_Cbk.h
 **********************************************************************************************************************/
package com.vector.ocs.plugins.nvmemory

import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.automation.model.ecuc.microsar.nvm.NvM
import com.vector.cfg.gen.core.bswmdmodel.GIModuleConfiguration
import com.vector.cfg.model.access.DefRef
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIModuleConfiguration
import com.vector.ocs.lib.shared.PluginsCommon

/**
 * NvM consists of NvM specific configurations
 */
class MicrosarNvM {
/**
 * The method createCbkHeaderFile is executed during the MAIN phase and is called after the object fee/fls (NvMemoryImpl.groovy) are created
 * createCbkHeaderFile() will create Dem_Cbk.h
 */
    static void createCbkHeaderFile() {
        /* Include callback header containing declaration of RAM Block Data variables of Nvm block descriptors */
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                if ((PluginsCommon.ConfigPresent("/MICROSAR/Dem")) && (PluginsCommon.ConfigPresent("/MICROSAR/NvM"))) {
                    NvM nvmCfg = bswmdModel(NvM.DefRef).single
                    transaction {
                        def CbkIncludeList = nvmCfg.nvMCommonVendorParamsOrCreate.getNvMCfgCbkIncludeList()
                        if (!CbkIncludeList.any { it.value.contains("Dem_Cbk.h") }) {
                            nvmCfg.nvMCommonVendorParamsOrCreate.nvMCfgCbkIncludeList.createAndAdd().value = "Dem_Cbk.h"
                        }
                    }
                }
            }
        }
    }
}
