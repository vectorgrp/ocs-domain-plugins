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
/*!        \file  AurixFee.groovy
 *        \brief  consists of Aurix Fee specific configurations, actions and mdf model
 *
 *      \details  Module Activation of
 *                - NvM, Crc, MemIf, Fee, Fls
 *
 *                - define FeeGeneralMdf
 **********************************************************************************************************************/
package com.vector.ocs.plugins.nvmemory
import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.model.mdf.MIObject
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.PluginsCommon
/**
 * AurixFee consists of AurixFee specific configurations and mdf model
 */
class AurixFee extends GeneralFee {
    List<MIObject> FeeGeneralMdf
    /**
     * The method init is executed during the MAIN phase and is called after the object fee (NvMemoryImpl.groovy) is created
     * init will activate the corresponding modules and define needed mdf model
     * @param logger instance of the logger
     */
    void init(OcsLogger logger){
        PluginsCommon.ModuleActivation("/MICROSAR", "NvM", logger)
        PluginsCommon.ModuleActivation("/MICROSAR", "Crc", logger)
        PluginsCommon.ModuleActivation("/MICROSAR", "MemIf", logger)
        PluginsCommon.ModuleActivation("/AURIX2G/EcucDefs", "Fee", logger)
    }

    /**
     * The method initMdfModels is executed during the MAIN and Cleanup phase and is called after the object fee (NvMemoryImpl.groovy) is created
     * mdfModels will create the mdf models for specific Fee References which are needed for other module parameters.
     */
    void initMdfModels(){
        FeeGeneralMdf = ScriptApi.activeProject.mdfModel("/AURIX2G/EcucDefs/Fee/FeeGeneral")
    }
}
