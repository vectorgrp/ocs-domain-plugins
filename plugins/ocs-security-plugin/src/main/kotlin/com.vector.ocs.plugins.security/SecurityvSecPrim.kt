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
/*!        \file  SecurityvSecPrim.kt
 *        \brief  The vSecPrim module addresses configuration possibilities in the vSecPrim.
 *
 *      \details  -
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.security

import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.model.pai.api.transaction
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.HelperLib.getContainer
import com.vector.ocs.lib.shared.HelperLib.getModule
import com.vector.ocs.lib.shared.HelperLib.setParam

private val project = ScriptApi.getActiveProject()
private val vSecPrimCfg = project.getModule("/MICROSAR/vSecPrim")

/**
 * Enables the algo algorithm
 * @param algo The algorithm which will be activated
 * @param logger Logger for logging messages
 */
internal fun enableMacAlgo(algo: String, logger: OcsLogger) {
    val vSecPrimMacCfg = vSecPrimCfg.getContainer("vSecPrimSymmetricAlgorithms/vSecPrimMac")
    project.transaction {
        when (algo) {
            "Cmac" -> vSecPrimMacCfg?.setParam("vSecPrimCmacEnabled", true)
            "Gmac" -> vSecPrimMacCfg?.setParam("vSecPrimGMacEnabled", true)
            "RMD160" -> vSecPrimMacCfg?.setParam("vSecPrimHMacRMD160Enabled", true)
            "SHA1" -> vSecPrimMacCfg?.setParam("vSecPrimHMacSHA1Enabled", true)
            "SHA2_256" -> vSecPrimMacCfg?.setParam("vSecPrimHMacSHA2_256Enabled", true)
            "SHA2_384" -> vSecPrimMacCfg?.setParam("vSecPrimHMacSHA2_384Enabled", true)
            "Poly1305" -> vSecPrimMacCfg?.setParam("vSecPrimPoly1305Enabled", true)
            "SipHash" -> vSecPrimMacCfg?.setParam("vSecPrimSipHashEnabled", true)
            else -> {
                logger.info("No valid Mac algorithm chosen. Please use only valid Mac algorithms.")
            }
        }
    }
    logger.info("$algo enabled.")
}
