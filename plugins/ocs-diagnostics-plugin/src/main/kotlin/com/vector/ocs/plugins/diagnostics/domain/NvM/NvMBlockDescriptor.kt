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
/*!        \file  NvMBlockDescriptor.kt
 *        \brief  Domain that defines the entities of NvMBlockDescriptor required for Diagnostics.
 *
 *      \details
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.diagnostics.domain.NvM

import com.vector.ocs.plugins.diagnostics.domain.EnumTypes.EnumTypes

class NvMBlockDescriptor(var shortName: String) {
    var nvMBlockUseSetRamBlockStatus: Boolean? = null
    var nvMInitBlockCallback: String? = null
    var nvMSingleBlockCallback: String? = null
    var nvMRamBlockDataAddress: String? = null
    var nvMUseInitCallback: Boolean? = null
    var nvMUseJobEndCallback: Boolean? = null
    var nvMSelectBlockForReadAll: Boolean? = null
    var nvMSelectBlockForWriteAll: Boolean? = null
    var nvMResistantToChangedSoftware: Boolean? = null
    var nvMBlockCrcType: EnumTypes.ENvMBlockCrcType? = null
    var nvMBlockManagementType: EnumTypes.ENvMBlockManagementType? = null
    var nvMRomBlockDataAddress: String? = null
    var nvMBlockUseCrc: Boolean? = null
}
