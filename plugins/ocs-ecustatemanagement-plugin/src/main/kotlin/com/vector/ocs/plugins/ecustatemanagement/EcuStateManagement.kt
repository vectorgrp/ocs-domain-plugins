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
/*!        \file  EcuStateManagement.kt
 *        \brief  Register the EcuStateManagementScript implementation to the OCS Core.
 *
 *      \details  Define the installation instructions for the run method for the MAIN phase
 *
 *********************************************************************************************************************/

package com.vector.ocs.plugins.ecustatemanagement

import com.vector.ocs.core.api.*
import com.vector.ocs.plugins.BuildConfig

private typealias Config = ModelPluginConfig<EcuStateManagementModel>

class EcuStateManagementPlugin(config: Config) : ModelPlugin<EcuStateManagementModel>(config) {
    override val serializer = EcuStateManagementModel.serializer()
    override val version = BuildConfig.ECUSTATEMANAGEMENT_PLUGIN_VERSION

    companion object : ModelPluginInstaller<EcuStateManagementModel, Config> {
        override fun install(
            pipeline: ModelPipeline,
            block: Config.() -> Unit
        ): EcuStateManagementPlugin {
            val plugin = EcuStateManagementPlugin(Config().apply(block))
            pipeline.addAction(plugin, PipelinePhase.MAIN) { ctx ->
                EcuStateManagementScript.run(ctx.model, ctx.logger)
            }
            pipeline.addAction(plugin, PipelinePhase.CLEANUP) { ctx ->
                EcuStateManagementScript.cleanup(ctx.model, ctx.logger)
            }
            return plugin
        }
    }
}
