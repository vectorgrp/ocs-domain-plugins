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
/*!        \file  SecurityCryptoLibCv.kt
 *        \brief  The SecurityCryptoLibCv module addresses configuration elements that belong to the CryptoLibCv
 *                module.
 *
 *      \details  -
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.security

import com.vector.cfg.automation.api.ScriptApi
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.interop.transactionApi
import com.vector.ocs.lib.shared.HelperLib.createOrGetContainer
import com.vector.ocs.lib.shared.HelperLib.delete
import com.vector.ocs.lib.shared.HelperLib.getContainer
import com.vector.ocs.lib.shared.HelperLib.getModule
import com.vector.ocs.lib.shared.HelperLib.getParameterList
import com.vector.ocs.lib.shared.HelperLib.getValueString
import com.vector.ocs.lib.shared.HelperLib.setParam

private val project = ScriptApi.getActiveProject()
private val transactionApi = project.transactionApi
private val cryptoCfg = project.getModule("/MICROSAR/Crypto_30_LibCv/Crypto")

/**
 * Creates a CryptoKey called name and references keyType
 * @param name Name of the created CryptoKey
 * @param keyType keyType which will be referenced
 * @param logger Logger for logging messages
 */
internal fun createCryptoKey(name: String, keyType: String, logger: OcsLogger) {
    transactionApi.transaction {
        val cryptoKeys = cryptoCfg.createOrGetContainer("CryptoKeys")
        val cryptoKeyCfg = cryptoKeys?.createOrGetContainer("CryptoKey", name)
        cryptoKeyCfg?.setParam("CryptoKeyTypeRef", "/ActiveEcuC/Crypto/CryptoKeyTypes/$keyType")
    }
    logger.info("Created CryptoKey $name and set Key Type Ref to $keyType.")
}

/**
 * Adds a Primitive Ref in CryptoDriverObject
 * @param name Name of the CryptoDriverObject
 * @param primitiveRef Name of the PrimitiveRef which should be added
 * @param logger Logger for logging messages
 */
internal fun addPrimitiveRef(name: String, primitiveRef: String, logger: OcsLogger) {
    transactionApi.transaction {
        val cryptoDriverObject = cryptoCfg.getContainer("CryptoDriverObjects/CryptoDriverObject", name)
        if (cryptoDriverObject?.getParameterList("CryptoPrimitiveRef")?.elementAt(0)?.getValueString() == null) {
            cryptoDriverObject?.getParameterList("CryptoPrimitiveRef")?.elementAt(0)?.delete()
        }
        cryptoDriverObject?.setParam("CryptoPrimitiveRef", "/ActiveEcuC/Crypto/CryptoPrimitives/$primitiveRef", -1)
    }
    logger.info("Created CryptoDriverObject $name and set Primitive Ref to $primitiveRef.")
}
