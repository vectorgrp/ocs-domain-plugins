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
/*!        \file  SecurityCryIf.kt
 *        \brief  The CryIf module contains functions for the configuration of the CryIf.
 *
 *      \details  -
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.security

import com.vector.ocs.lib.shared.HelperLib.createOrGetContainer
import com.vector.ocs.lib.shared.HelperLib.getContainerList
import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.model.pai.api.transaction
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.HelperLib.getModule
import com.vector.ocs.lib.shared.HelperLib.getParam
import com.vector.ocs.lib.shared.HelperLib.getValueString
import com.vector.ocs.lib.shared.HelperLib.setParam

private val project = ScriptApi.getActiveProject()
private val cryIfCfg = project.getModule("/MICROSAR/CryIf")

/**
 * Creates a CryIfChannel and references the Crypto_30_LibCv object as Driver Object Ref
 * @param logger Logger for logging messages
 */
internal fun createCryIfChannel(logger: OcsLogger) {
    project.transaction {
        val cryIfChannelCfg = cryIfCfg.createOrGetContainer("CryIfChannel")
        cryIfChannelCfg?.setParam("CryIfDriverObjectRef", "/ActiveEcuC/Crypto/CryptoDriverObjects/Crypto_30_LibCv")
    }
    logger.info("Created CryIfChannel and set Driver Object Ref to Crypto_30_LibCv.")
}

/**
 * Creates a CryIfCryptoModule and references the Crypto module in Crypto Module Ref
 * @param logger Logger for logging messages
 */
internal fun createCryIfCryptoModule(logger: OcsLogger) {
    project.transaction {
        val cryIfCryptoModuleCfg = cryIfCfg.createOrGetContainer("CryIfCryptoModule")
        cryIfCryptoModuleCfg?.setParam("CryIfCryptoModuleRef", "/ActiveEcuC/Crypto")
        cryIfCryptoModuleCfg?.setParam("CryIfSupportsKeyElementCopyPartial", true)
    }
    logger.info("Created CryIfCryptoModule and set CryptoModuleRef to Crypto and Supports Key Element Copy Partial to true.")
}

/**
 * Creates a CryIfKey and references the CryptoKey called name
 * @param name Name of the CryIfKey
 * @param cryptoRef Name of the CryptoKey which will be referenced
 * @param logger Logger for logging messages
 */
internal fun createCryIfKey(name: String, cryptoRef: String, logger: OcsLogger) {
    var flag = true
    project.transaction() {
        cryIfCfg.getContainerList("CryIfKey").forEach { cryIfKey ->
            if (cryIfKey.getParam("CryIfKeyRef")?.getValueString() == null) {
                cryIfKey.name = name
                cryIfKey.setParam("CryIfKeyRef", "/ActiveEcuC/Crypto/CryptoKeys/$cryptoRef")
                flag = false
                return@forEach
            }
        }
        if (flag) {
            val cryIfKeyCfg = cryIfCfg.createOrGetContainer("CryIfKey", name)
            cryIfKeyCfg?.setParam("CryIfKeyRef", "/ActiveEcuC/Crypto/CryptoKeys/$cryptoRef")
        }
    }
    logger.info("Created CryIfKey $name and set Key Ref to $cryptoRef.")
}
