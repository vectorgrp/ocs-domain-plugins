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
/*!        \file  SecurityImpl.kt
 *        \brief  The security plugin addresses configuration elements that belong to the security cluster.
 *
 *      \details  -
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.security

import com.vector.cfg.automation.api.ScriptApi
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.interop.transactionApi
import com.vector.ocs.interop.validationApi
import com.vector.ocs.lib.shared.HelperLib.getContainerList
import com.vector.ocs.lib.shared.HelperLib.getModule
import com.vector.ocs.lib.shared.PluginsCommon

private val project = ScriptApi.getActiveProject()
private val transactionApi = project.transactionApi
private val validationApi = project.validationApi

internal class SecurityConfig {
    companion object {
        /**
         * The runConfiguration method is executed during the MAIN phase and will address the configuration related to the Crypto Stack.
         * @param model input for the processing of the configuration elements
         * @param logger logger instance for OCS specific logging
         */
        fun runConfiguration(model: SecurityModel, logger: OcsLogger) {
            /* Configure Csm and Crypto_30_LibCv */
            if (PluginsCommon.DefRefPresent(project, "/MICROSAR/Crypto_30_LibCv/Crypto", logger) &&
                PluginsCommon.DefRefPresent(project, "/MICROSAR/Csm", logger))
            {
                /* Configure Crypto */
                if (!PluginsCommon.ConfigPresent("/MICROSAR/Crypto_30_LibCv/Crypto")) {
                    PluginsCommon.ModuleActivation("/MICROSAR/Crypto_30_LibCv", "Crypto", logger)
                }
                addPrimitiveRef("Crypto_30_LibCv", "AesCmacGenerate", logger)
                addPrimitiveRef("Crypto_30_LibCv", "AesCmacVerify", logger)
                createCryptoKey("CryptoKey_Cmac", "Mac", logger)

                /* Configure Csm */
                if (!PluginsCommon.ConfigPresent("/MICROSAR/Csm")) {
                    PluginsCommon.ModuleActivation("/MICROSAR", "Csm", logger)
                }
                createCsmCustomIncludeFile("Crypto_30_LibCv_Custom.h", logger)
                model.csmMainFunction.forEach { csmMainFunction ->
                    createCsmMainFunction(csmMainFunction, logger)
                }
                createCsmKey("CsmKey_Cmac", "CryIfKey_Cmac", logger)
            }

            /* Configure CryIf */
            if (PluginsCommon.DefRefPresent(project, "/MICROSAR/CryIf", logger)) {
                if (!PluginsCommon.ConfigPresent("/MICROSAR/CryIf")) {
                    PluginsCommon.ModuleActivation("/MICROSAR", "CryIf", logger)
                }
                createCryIfChannel(logger)
                createCryIfCryptoModule(logger)
                createCryIfKey("CryIfKey_Cmac", "CryptoKey_Cmac", logger)
            }

            /* Configure vSecPrim */
            if (PluginsCommon.DefRefPresent(project, "/MICROSAR/vSecPrim", logger)) {
                if (!PluginsCommon.ConfigPresent("/MICROSAR/vSecPrim")) {
                    PluginsCommon.ModuleActivation("/MICROSAR", "vSecPrim", logger)
                }
                enableMacAlgo("Cmac", logger)
            }

            /* Configure SecOC and create corresponding CsmJobs */
            if (model.configureSecOC == true && PluginsCommon.ConfigPresent("/MICROSAR/SecOC")) {
                createCsmQueue("CsmQueue_CmacGenerate", "CsmMainFunction", logger)
                createCsmQueue("CsmQueue_CmacVerify", "CsmMainFunction", logger)
                /* Create CsmPrimitive for Tx Pdus */
                createCsmPrimitive("CsmPrimitive_CmacGenerate", "MacGenerate", logger)
                /* Create CsmPrimitive for Rx Pdus */
                createCsmPrimitive("CsmPrimitive_CmacVerify", "MacVerify", logger)
                /* Configuration of SecOC module */
                setRxAuthServiceConfigRef(logger)
                setTxAuthServiceConfigRef(logger)
                setQueryFreshnessValue("RTE", logger)
            }

            if (model.csmJobs.isNotEmpty()) {
                /* create Csm jobs */
                for (i in 0 until model.csmJobs.size) {
                    val cryptoKeyTypeName: String = findKeyTypeName(model.csmJobs[i])
                    val csmPrimitive: String = findPrimitiveName(model.csmJobs[i])

                    if (cryptoKeyTypeName == "") {
                        logger.error("KeyType of CryptoKey cannot be determined. Please check CsmJob naming of ${model.csmJobs[i]}.")
                    } else if (csmPrimitive == "") {
                        logger.error("CsmPrimitive cannot be determined. Please check CsmJob naming of ${model.csmJobs[i]}.")
                    } else {
                        createCryptoKey("CryptoKey_$cryptoKeyTypeName", cryptoKeyTypeName, logger)
                        createCryIfKey("CryIfKey_$cryptoKeyTypeName", "CryptoKey_$cryptoKeyTypeName", logger)

                        createCsmJob(model.csmJobs[i], "CsmKey_$cryptoKeyTypeName","CsmPrimitive_$csmPrimitive","CsmQueue_$csmPrimitive", logger)
                        createCsmKey("CsmKey_$cryptoKeyTypeName", "CryIfKey_$cryptoKeyTypeName", logger)
                        createCsmPrimitive("CsmPrimitive_$csmPrimitive", csmPrimitive, logger)
                        createCsmQueue("CsmQueue_$csmPrimitive", "CsmMainFunction", logger)
                    }
                }
            }
        }
        /**
         * Cleanup function. Called in the Cleanup Phase to set Dest Pdu Data Provision for SecOC Pdus and du a full Swc description build
         */
        fun cleanup() {
            validationApi.validation.validationResults.forEach { validationResult ->
                if((validationResult.id.id == 10510) && (validationResult.id.origin == "PDUR")) {
                    validationResult.solvingActions.forEach { sa ->
                        if (sa.description.contains("PDUR_DIRECT") && validationResult.isActive) {
                            sa.solve()
                        }
                    }
                }
                if ((validationResult.id.id == 94990) && (validationResult.id.origin == "SECOC")) {
                    validationResult.solvingActions.forEach { sa ->
                        if (sa.description.contains("Full Swc description build from the Ecu configuration") &&
                                validationResult.isActive) {
                            sa.solve()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Finds the corresponding cryptoKeyType
 * @param name String which needs to have the wanted CryptoKeyType included
 * @return Returns the found CryptoKeyType name
 */
private fun findKeyTypeName(name: String) : String {
    val cryptoCfg =  project.getModule("/MICROSAR/Crypto_30_LibCv/Crypto")
    var cryptoKeyTypeName = ""
    transactionApi.transaction {
        val cryptoKeyTypeList = cryptoCfg.getContainerList("CryptoKeyTypes/CryptoKeyType")
        cryptoKeyTypeList.forEach {
            if (name.contains(it.name, ignoreCase = true)) {
                cryptoKeyTypeName = it.name
            }
        }
    }
    return cryptoKeyTypeName
}

/**
 * Finds the primitiveName in the CsmJob Name
 * @param name CsmJob name
 * @return Returns the found primitive name
 */
private fun findPrimitiveName(name: String) : String {
    val primitives: Array<String> = arrayOf("AEADDecrypt", "AEADEncrypt", "Decrypt", "Encrypt", "Hash", "MacGenerate", "MacVerify",
                                            "RandomGenerate", "SecureCounter", "SignatureGenerate", "SignatureVerify", "JobKeySetValid",
                                            "JobKeyExchangeCalcPubVal", "JobKeyExchangeCalcSecret", "JobKeyDerive", "JobRandomSeed",
                                            "JobKeyGenerate", "JobCertificateParse", "JobCertificateVerify", "JobKeySetInvalid")
    var primitiveName = ""
    for (i in primitives.indices) {
        if (name.contains(primitives[i], ignoreCase = true)) {
            primitiveName = primitives[i]
        }
    }
    return primitiveName
}
