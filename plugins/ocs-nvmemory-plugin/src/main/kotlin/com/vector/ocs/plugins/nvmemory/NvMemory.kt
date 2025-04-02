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
/*!        \file  NvMemory.kt
 *        \brief  Define the installation instructions for the methods:
 *                - run method for the MAIN phase
 *                - cleanup method for the CLEANUP phase
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.nvmemory

import com.vector.ocs.core.api.*
import com.vector.ocs.plugins.BuildConfig

private typealias Config = ModelPluginConfig<NvMemoryModel>

class NvMemoryPlugin(config: Config) : ModelPlugin<NvMemoryModel>(config) {
    override val serializer = NvMemoryModel.serializer()
    override val version = BuildConfig.NVMEMORY_PLUGIN_VERSION

    companion object : ModelPluginInstaller<NvMemoryModel, Config> {
        override fun install(pipeline: ModelPipeline, block: Config.() -> Unit): NvMemoryPlugin {
            val plugin = NvMemoryPlugin(Config().apply(block))
            pipeline.addAction(plugin, PipelinePhase.MAIN, 0.0f) { ctx ->
                NvMemoryImpl.runNvMemoryConfig(ctx.model, ctx.logger)
            }
            pipeline.addAction(plugin, PipelinePhase.CLEANUP, 0.0f) { ctx ->
                NvMemoryImpl.cleanupNvMemoryConfig(ctx.model, ctx.logger)
            }
            return plugin
        }
    }
}
