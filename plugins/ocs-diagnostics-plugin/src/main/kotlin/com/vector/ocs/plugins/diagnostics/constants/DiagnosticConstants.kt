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
/*!        \file  DiagnosticConstants.kt
 *        \brief  Class defining constants for Diagnostics parameters.
 *
 *      \details
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.diagnostics.constants

import groovy.transform.PackageScope

@PackageScope
class DiagnosticConstants {

    companion object {
        /** Scaling factor for setting the amount of DemPrimaryDataBlocks in relation to the demDTCClass size.
         * Will only be used if there is no diagMemBlocksScaling parameter provided in the model, then this is selected as default. */
        const val BLOCK_COUNT_SCALING = 0.2f

        /** Threshold for status PASSED in the Debouncing algorithm TIMEBASED after PrePassed report (ms) */
        const val DEBOUNCE_TIME_PASSED_THRESHOLD: Float = 0.1f

        /** Threshold for status FAILED in the Debouncing algorithm TIMEBASED after PreFailed report (ms) */
        const val DEBOUNCE_TIME_FAILED_THRESHOLD: Float = 0.1f

        /** Cfg5 version of R34 */
        const val CFG5_VERSION_R34 = 31
    }
}
