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
/*!       \file  CommunicationConstants.groovy
 *        \brief  Store general constants the are relevant for the communication plugin.
 *
 *        \details  -
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.communication

/**
 * Provide common constant definitions for the Communication plugin.
 */
class CommunicationConstants {
    /** Provide the DefRef of the Com module */
    final static String COM_DEFREF = "/MICROSAR/Com"
    /** Provide the DefRef of the ComM module */
    final static String COMM_DEFREF = "/MICROSAR/ComM"
    /** Provide the DefRef of the DoIP module */
    final static String DOIP_DEFREF = "/MICROSAR/DoIP"
    /** Provide the DefRef of the TcpIp module */
    final static String TCPIP_DEFREF = "/MICROSAR/TcpIp"
    /** Provide the DefRef of the SoAd module */
    final static String SOAD_DEFREF = "/MICROSAR/SoAd"
    /** Provide the DefRef of the Xcp module */
    final static String XCP_DEFREF = "/MICROSAR/Xcp"
    /** Provide the DefRef of the CanIf module */
    final static String CANIF_DEFREF = "/MICROSAR/CanIf"
    /** Provide the DefRef of the FrIf module */
    final static String FRIF_DEFREF = "/MICROSAR/FrIf"
    /** Provide the DefRef of the LinIf module */
    final static String LINIF_DEFREF = "/MICROSAR/LinIf"
    /** Provide the DefRef of the SecOC module */
    final static String SECOC_DEFREF = "/MICROSAR/SecOC"
    /** Provide the DefRef of the SecOC module */
    final static String IPDUM_DEFREF = "/MICROSAR/IpduM"
}
