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
/*!        \file  SecurityModel.kt
 *        \brief  Definition of the security model as user interface for the plugin.
 *
 *      \details  -
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.security

import com.vector.ocs.core.api.PluginModel
import com.vector.ocs.json.api.SchemaDescription
import com.vector.ocs.json.api.SchemaIntEnum
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

private const val VERSION = 2

@Serializable
data class SecurityModel(
    /**
     * Creates all stated CsmJobs. All necessary configuration steps in the crypto stack will be performed.
     * To have a full configuration the CsmJob name needs to include the CryptoKeyType name and the Csm Primitive name.
     */
    @SchemaDescription(
        "Creates all stated CsmJobs. All necessary configuration steps in the crypto stack will be performed." +
                  "To have a full configuration the CsmJob name needs to include the CryptoKeyType name and the Csm Primitive name."
    )
    @EncodeDefault
    val csmJobs: List<String> = emptyList(),
    /**
     * Creates a Csm MainFunction.
     */
    @SchemaDescription("Creates a Csm MainFunction.")
    @EncodeDefault
    val csmMainFunction: List<String> = listOf("CsmMainFunction"),
    /**
     * Activates the configuration of SecOC. A CmacVerify CsmJob is created for each Rx SecOC Pdu and a CmacGenerate CsmJob is created for each Tx SecOC Pdu.
     */
    @SchemaDescription("Activates the configuration of SecOC. A CmacVerify CsmJob is created for each Rx SecOC Pdu and a CmacGenerate CsmJob is created for each Tx SecOC Pdu.")
    @EncodeDefault
    val configureSecOC: Boolean? = true
) : PluginModel {
    @Suppress("unused")
    @Required
    @SchemaIntEnum(VERSION.toLong())
    @SchemaDescription("The version of this model. Must always be '$VERSION'.")
    private val version: Int = VERSION
}
