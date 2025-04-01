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
 *  ------------------------------------------------------------------------------------------------------------------*/
/*!        \file  NvMemorySolvingActions.groovy
 *        \brief  The NvMemorySolvingActions addresses solving actions to achieve an error free configured NvM stack.
 *
 *      \details  Depending on the used MemIf lower modules, NvMemorySolvingActions will trigger following Solving Actions:
 *                - FEE00150
 *                - VTTFls54010
 *                - NVM01000
 *                - FEE01053
 *                - vSet54000
 **********************************************************************************************************************/
package com.vector.ocs.plugins.nvmemory

import com.vector.cfg.automation.api.ScriptApi
import com.vector.cfg.automation.model.ecuc.microsar.vset.VSet
import com.vector.cfg.automation.model.ecuc.microsar.vset.vsetbswinitialization.vsetinitfunction.VSetInitFunction
import com.vector.cfg.automation.model.ecuc.microsar.vtt.vtteep.VTTEep
import com.vector.cfg.automation.model.ecuc.microsar.vtt.vttfls.VTTFls
import com.vector.cfg.automation.model.ecuc.microsar.vtt.vttvset.VTTvSet
import com.vector.cfg.consistency.ui.ISolvingActionUI
import com.vector.cfg.consistency.ui.IValidationResultUI
import com.vector.cfg.model.mdf.commoncore.autosar.MIReferrable
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.lib.shared.PluginsCommon

/**
 * NvMemorySolvingActions adds actions to the pipeline phase CLEANUP.
 * CLEANUP executes Solving Actions
 *
 * Supported MemIf lower layers: Vector classic Fee, Vector FeeFlexNor, Infineon Fee
 *
 * When the plugin is executed, it is expected that a project is loaded and active.
 */
class NvMemorySolvingActions {
    /**
     * The method solveNvMemory is called during the Cleanup phase
     * It will trigger solving actions to achieve an error free NvM Stack Configuration
     * @param logger instance for OCS specific logging
     */
    static void solveNvMemory(OcsLogger logger) {
        ScriptApi.activeProject { prj ->
            ScriptApi.scriptCode {
                validation {
                    validationResults.each { IValidationResultUI validation_result ->
                        if ((validation_result.id.id == 150) && validation_result.id.origin == "FEE") {
                            validation_result.solvingActions.each { ISolvingActionUI solving_action ->
                                if (validation_result.active) {
                                    solving_action.solve()
                                }
                            }
                        }
                    }
                    validationResults.each { IValidationResultUI validation_result ->
                        if ((validation_result.id.id == 54010) && (validation_result.id.origin == "VTTFls")) {
                            validation_result.solvingActions.each { ISolvingActionUI solving_action ->
                                if (validation_result.active) {
                                    solving_action.solve()
                                }
                            }
                        }
                    }
                    validationResults.each { IValidationResultUI validation_result ->
                        if ((validation_result.id.id == 1000) && (validation_result.id.origin == "NVM")) {
                            validation_result.solvingActions.each { ISolvingActionUI solving_action ->
                                if (validation_result.active) {
                                    solving_action.solve()
                                }
                            }
                        }
                    }
                    validationResults.each { IValidationResultUI validation_result ->
                        if ((validation_result.id.id == 1053) && (validation_result.id.origin == "Fee")) {
                            validation_result.solvingActions.each { ISolvingActionUI solving_action ->
                                if (validation_result.active) {
                                    solving_action.solve()
                                }
                            }
                        }
                    }
                }
                //implementation related do ValidationId 54000vSet which can not be solved yet via triggering the Solving Action
                transaction {
                    if (((PluginsCommon.ConfigPresent("/MICROSAR/vSet"))) && ((PluginsCommon.ConfigPresent("/MICROSAR/VTT/VTTFls")))) {
                        VSet myvSet = bswmdModel(VSet.DefRef).single
                        VTTFls myVTTFls = bswmdModel(VTTFls.DefRef).single
                        if ((PluginsCommon.ConfigPresent("/MICROSAR/Fls")) && (myvSet.getVSetBswInitializationOrNull().getVSetInitFunction().exists("Fls_Init"))) {
                            VSetInitFunction vSetFlsMicrosar = myvSet.getVSetBswInitializationOrNull().getVSetInitFunction().byName("Fls_Init")
                            vSetFlsMicrosar.getVSetModuleRef().setRefTargetMdf(myVTTFls as MIReferrable)
                        } else if ((PluginsCommon.ConfigPresent("/AURIX2G/EcucDefs/Fls")) && (myvSet.getVSetBswInitializationOrNull().getVSetInitFunction().exists("Fls_17_Dmu_Init"))) {
                            VSetInitFunction vSetFlsInfineon = myvSet.getVSetBswInitializationOrNull().getVSetInitFunction().byName("Fls_17_Dmu_Init")
                            vSetFlsInfineon.getVSetModuleRef().setRefTargetMdf(myVTTFls as MIReferrable)
                        }
                    } else if (((PluginsCommon.ConfigPresent("/MICROSAR/VTT/VTTvSet"))) && ((PluginsCommon.ConfigPresent("/MICROSAR/VTT/VTTEep")))) {
                        VTTvSet myVTTvSet = bswmdModel(VTTvSet.DefRef).single
                        VTTEep myVTTEep = bswmdModel(VTTEep.DefRef).single
                        if ((PluginsCommon.ConfigPresent("/MICROSAR/Eep_30_vMemAccM")) && (myVTTvSet.getVSetBswInitializationOrNull().getVSetInitFunction().exists("Eep_30_vMemAccM_Init"))) {
                            def vSetEepMicrosar = myVTTvSet.getVSetBswInitializationOrNull().getVSetInitFunction().byName("Eep_30_vMemAccM_Init")
                            vSetEepMicrosar.getVSetModuleRef().setRefTargetMdf(myVTTEep as MIReferrable)
                        } else if ((PluginsCommon.ConfigPresent("/MICROSAR/Eep")) && (myVTTvSet.getVSetBswInitializationOrNull().getVSetInitFunction().exists("Eep_Init"))) {
                            def vSetEepMicrosarvMemAccM = myVTTvSet.getVSetBswInitializationOrNull().getVSetInitFunction().byName("Eep_Init")
                            vSetEepMicrosarvMemAccM.getVSetModuleRef().setRefTargetMdf(myVTTEep as MIReferrable)
                        }
                    }
                }
            }
            PluginsCommon.modelSynchronization(prj, logger)
        }
    }
}
