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
/*!        \file  CommunicationModel.kt
 *        \brief  Definition of the CommunicationScript model as user interface for the plugin.
 *
 *      \details  -
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.communication

import com.vector.ocs.core.api.PluginModel
import com.vector.ocs.json.api.SchemaDescription
import com.vector.ocs.json.api.SchemaIntEnum

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

private const val VERSION = 2
@Serializable
data class CommunicationModel(
    @SchemaDescription("Mark these Com Signals as necessary that are used by module ComM. This will make the PNC functionality of ComM working, otherwise the PNC Signals ComM is using are not handled by module Com.")
    @EncodeDefault
    val setupComMSignalAccess: Boolean? = true,
    @SchemaDescription("Set up a fitting cycle time for the MainFunctions of module Com.")
    @EncodeDefault
    val setupComMainFunctions: Boolean? = true,
    @SchemaDescription("Insert a Cdd Module that handles all PDUs where the connection to BSW Modules is unclear and would create Generation Errors.")
    @EncodeDefault
    val stubUnconnectedPDUs: Boolean? = true,
    @SchemaDescription("Setup Default Routing Activations for each possible DoIp Diag Tester. Insert TcpIp Arp config if not present and reference it if needed.")
    @EncodeDefault
    val autoExtendEthStack: Boolean? = true,
    @SchemaDescription("Find Response PDUs to the Rx PDUs configured.")
    @EncodeDefault
    val correctXcpPdus: Boolean? = true,
    @SchemaDescription("Specify more precisely how the Com MainFunctions are to be configured.")
    @EncodeDefault
    val comMainFunctionSettings: ComMainFunctionHandling? = ComMainFunctionHandling(
        defaultMainFunctionRxCycle = 5,
        defaultMainFunctionTxCycle = 5
    )
) : PluginModel {
    @Suppress("unused")
    @Required
    @SchemaIntEnum(VERSION.toLong())
    @SchemaDescription("The version of this model. Must always be '$VERSION'.")
    private val version: Int = VERSION
}

@Serializable
data class ComMainFunctionHandling(
    @SchemaDescription("Default Cycle time of the Com_MainFunctionRx in ms.")
    @EncodeDefault
    val defaultMainFunctionRxCycle: Int? = 5,
    @SchemaDescription("Default Cycle time of the Com_MainFunctionTx in ms.")
    @EncodeDefault
    val defaultMainFunctionTxCycle: Int? = 5
)
