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
/*!        \file  SecuritySecOC.kt
 *        \brief  The SecuritySecOC module addresses configuration elements that belong to the SecOC module.
 *
 *      \details  -
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.security

import com.vector.cfg.automation.api.ScriptApi
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.interop.transactionApi
import com.vector.ocs.lib.shared.HelperLib.getContainer
import com.vector.ocs.lib.shared.HelperLib.getContainerList
import com.vector.ocs.lib.shared.HelperLib.getModule
import com.vector.ocs.lib.shared.HelperLib.setParam

private val project = ScriptApi.getActiveProject()
private val transactionApi = project.transactionApi
private val secOCCfg = project.getModule("/MICROSAR/SecOC")

/**
 * Sets the Rx Auth Service Config Ref for SecOC Rx Pdus
 * @param logger Logger for logging messages
 */
internal fun setRxAuthServiceConfigRef(logger: OcsLogger) {
    transactionApi.transaction {
        secOCCfg.getContainerList("SecOCRxPduProcessing").forEachIndexed { index, secOCPdu ->
            createCsmJob("CsmJob_CmacVerify_$index", "CsmKey_Cmac", "CsmPrimitive_CmacVerify", "CsmQueue_CmacVerify", logger)
            secOCPdu.setParam("SecOCRxAuthServiceConfigRef", "/ActiveEcuC/Csm/CsmJobs/CsmJob_CmacVerify_$index")
            logger.info("Set Rx Auth Service Config Ref to CsmJob_CmacVerify_$index.")
        }
    }
}

/**
 * Sets the Tx Auth Service Config Ref for SecOC Tx Pdus
 * @param logger Logger for logging messages
 */
internal fun setTxAuthServiceConfigRef(logger: OcsLogger) {
    transactionApi.transaction {
        secOCCfg.getContainerList("SecOCTxPduProcessing").forEachIndexed { index, secOCPdu ->
            createCsmJob("CsmJob_CmacGenerate_$index", "CsmKey_Cmac", "CsmPrimitive_CmacGenerate", "CsmQueue_CmacGenerate", logger)
            secOCPdu.setParam("SecOCTxAuthServiceConfigRef", "/ActiveEcuC/Csm/CsmJobs/CsmJob_CmacGenerate_$index")
            logger.info("Set Tx Auth Service Config Ref to CsmJob_CmacGenerate_$index.")
        }
    }
}

/**
 * Sets the Query Freshness Value attribute
 * @param queryFreshnessValue value of the Query Freshness Value
 * @param logger Logger for logging messages
 */
internal fun setQueryFreshnessValue(queryFreshnessValue: String, logger: OcsLogger) {
    val secOCGeneral = secOCCfg.getContainer("SecOCGeneral")
    transactionApi.transaction {
        if (queryFreshnessValue == "RTE" || queryFreshnessValue == "CFUNC") {
            secOCGeneral?.setParam("SecOCQueryFreshnessValue", queryFreshnessValue)
            logger.info("Set Query Freshness Value to $queryFreshnessValue.")
        } else {
            logger.error("Value $queryFreshnessValue not applicable for attribute Query Freshness Value.")
        }
    }
}
