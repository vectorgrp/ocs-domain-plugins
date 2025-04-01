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
/*!        \file  HelperFunctions.groovy
 *        \brief  consists of general HelperFunctions for the NvM Plugin
 *
 *      \details  - calculateFlsSize
 **********************************************************************************************************************/
package com.vector.ocs.plugins.nvmemory

import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIModuleConfiguration
import groovy.transform.PackageScope
/**
* HelperFunctions consists functions which are not related to special classes/modules and provide HelperFunctions, e.g. calculation of fls size
*/
@PackageScope
class HelperFunctions {
/**
 * The calculateFlsSize method is executed during the MAIN phase and will calculate the size of the Fls
 * @return flsData: fls size and start address
 */
    static calculateFlsSize() {
        /* synchronize Fls' Sector settings with the Fee Partition settings - default: all Fls Sectors used by one partition - only possible if Fls settings were already performed */
        int flsSize = 0
        long flsLowestStartAddress = 0xFFFFFFFF
        int sectorContainerCount = 0
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                activeEcuc.allModules.each { MIModuleConfiguration active_module ->
                    if (active_module.name == "Fls") {
                        transaction {
                            active_module.bswmdModel().flsConfigSetOrCreate.flsSectorListOrCreate.flsSector.each { def fls_sector ->
                                sectorContainerCount = sectorContainerCount + 1
                                flsSize = (int) (flsSize + (fls_sector.flsNumberOfSectors.value * fls_sector.flsSectorSize.value))
                                if (flsLowestStartAddress > fls_sector.flsSectorStartaddress.value) {
                                    flsLowestStartAddress = fls_sector.flsSectorStartaddress.value
                                }
                            }
                        }
                    }
                }
            }
        }
        ArrayList<Long> flsData = [flsSize, flsLowestStartAddress]
        return flsData
    }
}
