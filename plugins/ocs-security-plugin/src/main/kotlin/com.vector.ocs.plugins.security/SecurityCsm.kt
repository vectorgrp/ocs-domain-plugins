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
/*!        \file  SecurityCsm.kt
 *        \brief  The SecurityCsm module addresses configuration elements that belong to the Csm module.
 *
 *      \details  -
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.security

import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.model.pai.api.transaction
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.HelperLib.createOrGetContainer
import com.vector.ocs.lib.shared.HelperLib.delete
import com.vector.ocs.lib.shared.HelperLib.getContainer
import com.vector.ocs.lib.shared.HelperLib.getContainerList
import com.vector.ocs.lib.shared.HelperLib.getModule
import com.vector.ocs.lib.shared.HelperLib.setParam

private val project = ScriptApi.getActiveProject()
private val csmCfg = project.getModule("/MICROSAR/Csm")

/**
 * Creates a Csm Job. Additionally, a Csm Key, Csm Primitive, Csm Queue and Csm MainFunction will be created.
 * If these containers are already present in the configuration, no new one will be created.
 * @param csmJobName Name of the Csm Job which will be created
 * @param csmKey Name of the Csm key which will be referenced
 * @param csmPrimitive Name of the Csm Primitive which will be referenced
 * @param csmQueue Name of the Csm Queue which will be referenced
 * @param logger Logger for logging messages
 */
internal fun createCsmJob(csmJobName: String, csmKey: String, csmPrimitive: String, csmQueue: String, logger: OcsLogger) {
    project.transaction {
        val csmJobsCfg = csmCfg.createOrGetContainer("CsmJobs")
        /* Delete automatically created CsmJob (only when CsmJobs container was not yet created) */
        csmJobsCfg?.getContainerList("CsmJob")?.forEach {
            if (it.name == "CsmJob") {
                it.delete()
            }
        }
        val csmJobCfg = csmJobsCfg?.createOrGetContainer("CsmJob", csmJobName)
        csmJobCfg?.setParam("CsmJobKeyRef", "/ActiveEcuC/Csm/CsmKeys/$csmKey")
        csmJobCfg?.setParam("CsmJobPrimitiveRef", "/ActiveEcuC/Csm/$csmPrimitive")
        csmJobCfg?.setParam("CsmJobQueueRef", "/ActiveEcuC/Csm/CsmQueues/$csmQueue")
    }
    logger.info("Created CsmJob $csmJobName, Job Key $csmKey, Primitive Ref $csmPrimitive and Job Queue $csmQueue.")
}

/**
 * Creates the Csm Primitive. If the Csm Primitive is MacGenerate or MacVerify a default config will be performed.
 * @param name Name of the Primitive
 * @param primitive Type of the Primitive
 * @param logger Logger for logging messages
 */
internal fun createCsmPrimitive(name: String, primitive: String, logger: OcsLogger) {
    project.transaction {
        val csmPrimitivesCfg = csmCfg.createOrGetContainer("CsmPrimitives", name)
        val csmPrimitive = csmPrimitivesCfg?.createOrGetContainer("Csm$primitive")
        val csmPrimitiveCfg = csmPrimitive?.getContainer("Csm" + primitive + "Config")
        when (primitive) {
            "MacGenerate" -> {
                csmPrimitiveCfg?.setParam("CsmMacGenerateAlgorithmFamily", "CRYPTO_ALGOFAM_AES")
                csmPrimitiveCfg?.setParam("CsmMacGenerateAlgorithmMode", "CRYPTO_ALGOMODE_CMAC")
                csmPrimitiveCfg?.setParam("CsmMacGenerateProcessing", "CSM_SYNCHRONOUS")
                csmPrimitiveCfg?.setParam("CsmMacGenerateResultLength", 2)
            }
            "MacVerify" -> {
                csmPrimitiveCfg?.setParam("CsmMacVerifyAlgorithmFamily", "CRYPTO_ALGOFAM_AES")
                csmPrimitiveCfg?.setParam("CsmMacVerifyAlgorithmMode", "CRYPTO_ALGOMODE_CMAC")
                csmPrimitiveCfg?.setParam("CsmMacVerifyCompareLength", 2)
                csmPrimitiveCfg?.setParam("CsmMacVerifyProcessing", "CSM_SYNCHRONOUS")
            }
            else -> {
                logger.info("Only MacGenerate and MacVerify will be configured automatically.")
            }
        }
        /* Set PrimitiveConfig name to the same name as the Primitive */
        csmPrimitiveCfg?.name = name
        logger.info("Created CsmPrimitive $name.")
    }
}

/**
 * Creates a Csm Queue. Will reference the CryIfChannel and the given Csm MainFunction
 * @param name Name of the Queue
 * @param csmMainFunction Name of the Csm MainFunction which will be referenced
 * @param logger Logger for logging messages
 */
internal fun createCsmQueue(name: String, csmMainFunction: String, logger: OcsLogger) {
    project.transaction {
        val csmQueuesCfg = csmCfg.createOrGetContainer("CsmQueues")
        /* Delete automatically created CsmJob (only when CsmJobs container was not yet created) */
        csmQueuesCfg?.getContainerList("CsmQueue")?.forEach {
            if (it.name == "CsmQueue") {
                it.delete()
            }
        }
        val csmQueueCfg = csmQueuesCfg?.createOrGetContainer("CsmQueue", name)
        csmQueueCfg?.setParam("CsmChannelRef", "/ActiveEcuC/CryIf/CryIfChannel")
        csmQueueCfg?.setParam("CsmQueueMainFunctionRef", "/ActiveEcuC/Csm/$csmMainFunction")
    }
    logger.info("Created Csm Queue $name, set Channel Ref to CryIfChannel and set Csm Main Function Ref to $csmMainFunction.")
}

/**
 * Creates a Csm Key. The given CryIfKey will be referenced
 * @param name Name of the Key
 * @param cryIfKeyRef Name of the CryIf Key which will be referenced
 * @param logger Logger for logging messages
 */
internal fun createCsmKey(name: String, cryIfKeyRef: String, logger: OcsLogger) {
    project.transaction {
        val csmKeysCfg = csmCfg.createOrGetContainer("CsmKeys")
        val csmKeyCfg = csmKeysCfg?.createOrGetContainer("CsmKey", name)
        csmKeyCfg?.setParam("CsmKeyRef", "/ActiveEcuC/CryIf/$cryIfKeyRef")
    }
    logger.info("Created CsmKey $name and set Key Ref to $cryIfKeyRef.")
}

/**
 * Creates a Csm Main Function. The period can be given with the parameter mainFunctionPeriod
 * @param name Name of the MainFunction
 * @param mainFunctionPeriod Period of the MainFunction
 * @param logger Logger for logging messages
 */
internal fun createCsmMainFunction(name: String, mainFunctionPeriod: Long, logger: OcsLogger) {
    project.transaction {
        val csmMainFunctionCfg = csmCfg.createOrGetContainer("CsmMainFunction", name)
        csmMainFunctionCfg?.setParam("CsmMainFunctionPeriod", mainFunctionPeriod)
    }
    logger.info("Created CsmMainFunction $mainFunctionPeriod and set MainFunction Period to $mainFunctionPeriod.")
}

/**
 * Creates a Csm Main Function. No Main Function Period wil be set
 * @param name Name of the MainFunction
 * @param logger Logger for logging messages
 */
internal fun createCsmMainFunction(name: String, logger: OcsLogger) {
    project.transaction {
        csmCfg.createOrGetContainer("CsmMainFunction", name)
    }
    logger.info("Created CsmMainFunction $name.")
}

/**
 * Sets the CustomIncludeFile in CsmGeneral
 * @param includeFile Name of the Custom Include File
 * @param logger Logger for logging messages
 */
internal fun createCsmCustomIncludeFile(includeFile: String, logger: OcsLogger) {
    project.transaction {
        val csmGeneralCfg = csmCfg.getContainer("CsmGeneral")
        csmGeneralCfg?.setParam("CsmCustomIncludeFiles", includeFile, -1)
    }
    logger.info("Set CustomIncludeFile $includeFile in CsmGeneral.")
}
