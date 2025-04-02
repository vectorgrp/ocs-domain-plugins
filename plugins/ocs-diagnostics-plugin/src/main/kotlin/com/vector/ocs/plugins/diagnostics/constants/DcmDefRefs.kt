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
/*!        \file  DcmDefRefs.kt
 *        \brief  Class defining Dcm def refs for different releases.
 *
 *      \details
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.diagnostics.constants

import com.vector.ocs.lib.shared.PluginsCommon

abstract class DcmDefRefs {
    /** R31 */
    open var DCM: String = "/MICROSAR/Dcm"
    open var DCM_CONFIG_SET: String = ""
    open var DCM_DSP_COMCONTROL: String = ""
    open var DCM_DSP_COM_CONTROL_ALL_CHANNELS: String = ""
    open var DCM_DSL: String = ""
    open var DCM_DSL_PROTOCOL: String = ""
    open var DCM_DSL_BUFFER: String = ""
    open var DCM_DSL_BUFFER_SIZE: String = ""
    open var DCM_DSL_PROTOCOLROW: String = ""
    open var DCM_DEM_CLIENT_REF: String = ""
    open var DCM_DSL_TX_BUFFER_ID: String = ""
    open var DCM_DSL_RX_BUFFER_ID: String = ""
    open var DCM_DSL_PROTOCOL_SID_TABLE: String = ""
    open var DCM_DSL_PROTOCOL_MAXIMUM_RESPONSE_SIZE = ""
    open var DCM_DSD: String = ""
    open var DCM_DSD_SERVICE_TABLE: String = ""

    init {
        (this).apply {
            DCM_CONFIG_SET = "$DCM/DcmConfigSet"
            DCM_DSP_COMCONTROL = "$DCM_CONFIG_SET/DcmDsp/DcmDspComControl"
            DCM_DSP_COM_CONTROL_ALL_CHANNELS = "$DCM_DSP_COMCONTROL/DcmDspComControlAllChannel"
            DCM_DSL = "$DCM_CONFIG_SET/DcmDsl"
            DCM_DSL_PROTOCOL = "$DCM_DSL/DcmDslProtocol"
            DCM_DSL_BUFFER = "$DCM_DSL/DcmDslBuffer"
            DCM_DSL_BUFFER_SIZE = "$DCM_DSL_BUFFER/DcmDslBufferSize"
            DCM_DSL_PROTOCOLROW = "$DCM_DSL_PROTOCOL/DcmDslProtocolRow"
            DCM_DEM_CLIENT_REF = "$DCM_DSL_PROTOCOLROW/DcmDemClientRef"
            DCM_DSL_TX_BUFFER_ID = "$DCM_DSL_PROTOCOLROW/DcmDslProtocolTxBufferID"
            DCM_DSL_RX_BUFFER_ID = "$DCM_DSL_PROTOCOLROW/DcmDslProtocolRxBufferID"
            DCM_DSL_PROTOCOL_SID_TABLE = "$DCM_DSL_PROTOCOLROW/DcmDslProtocolSIDTable"
            DCM_DSL_PROTOCOL_MAXIMUM_RESPONSE_SIZE = "$DCM_DSL_PROTOCOLROW/DcmDslProtocolMaximumResponseSize"
            DCM_DSD = "$DCM_CONFIG_SET/DcmDsd"
            DCM_DSD_SERVICE_TABLE = "$DCM_CONFIG_SET/DcmDsd/DcmDsdServiceTable"
        }
    }

    open class DcmDefRefsR31 : DcmDefRefs() {
        init {

        }
    }

    object DcmDefRefConstantsFactory {
        fun getConstants(): DcmDefRefs {
            val minorVersion = PluginsCommon.Cfg5MinorVersion()
            return when {
                minorVersion.toInt() >= 28 -> DcmDefRefsR31()
                else -> throw IllegalArgumentException("Unknown version: $minorVersion")
            }
        }
    }
}
