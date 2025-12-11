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
/*!        \file  ComMDefRefs.kt
 *        \brief  Class defining ComM def refs for different releases.
 *
 *      \details
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.diagnostics.constants

import com.vector.ocs.lib.shared.PluginsCommon

/**
 * ComM def refs
 */
abstract class ComMDefRefs {

    open val COMM: String = "/MICROSAR/ComM"
    open var COMM_CONFIG_SET: String = ""
    open var COMM_CHANNEL: String = ""
    open var COMM_MAIN_FUNCTION_PERIOD = ""

    init {
        (this).apply {
            COMM_CONFIG_SET = "$COMM/ComMConfigSet"
            COMM_CHANNEL = "$COMM_CONFIG_SET/ComMChannel"
            COMM_MAIN_FUNCTION_PERIOD = "$COMM_CHANNEL/ComMMainFunctionPeriod"
        }
    }

    open class ComMDefRefsR35 : ComMDefRefs() {
        init {

        }
    }

    object ComMDefRefConstantsFactory {
        fun getConstants(): ComMDefRefs {
            return ComMDefRefsR35()
        }
    }
}
