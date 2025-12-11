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
/*!        \file  EcuStateManagementModel.kt
 *        \brief  Definition of the EcuStateManagementScript model as user interface for the plugin.
 *
 *      \details
 *
 *********************************************************************************************************************/

package com.vector.ocs.plugins.ecustatemanagement

import com.vector.ocs.core.api.ModelValidationException
import com.vector.ocs.core.api.PluginModel
import com.vector.ocs.json.api.SchemaDescription
import com.vector.ocs.json.api.SchemaIntEnum
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
data class EcuStateManagementModel(
    @SchemaDescription("Enable Communication Control Auto Configuration in the comfort editor of DaVinci Configurator.")
    @EncodeDefault
    val AutoConfig_CC_Enabled: Boolean = true,
    @SchemaDescription("Particular definition of Sub-Features for every Communication channel separately. If not defined, all Sub-Features are enabled if possible.")
    @EncodeDefault
    val AutoConfig_CC_CChannels: Set<AutoConfig_CC_Channel>? = emptySet(),
    @SchemaDescription("Enable a non-TLS DoIP connection.")
    @EncodeDefault
    val AutoConfig_CC_NonTls_DoIp_Connection_Enabled: Boolean = false,
    @SchemaDescription("Enable Ecu State Handling Auto Configuration.")
    @EncodeDefault
    val AutoConfig_EcuStateHandling_Enabled: Boolean = true,
    @SchemaDescription("Particular definition of Ecu State Machine settings - if not defined, default values are used.")
    @EncodeDefault
    val AutoConfig_EcuStateMachineHandling: ECUStateMachineSettings = ECUStateMachineSettings(),
    @SchemaDescription("Configuration parameters for BswMConfigs. If more than one BswMConfig is configured "+
                                 "Multi Partitioning is enabled")
    @EncodeDefault
    val PartitionConfig: Set<PartitionConfigSettings>? = setOf(PartitionConfigSettings()),
    @SchemaDescription("Enable Service Discovery Control Auto Configuration.")
    @EncodeDefault
    val AutoConfig_ServiceDiscovery_Enabled: Boolean = true,
    @SchemaDescription("Enable using immediate processing instead of deferred processing for server services.")
    @EncodeDefault
    val AutoConfig_SdServerImmediateProcessing_Enabled: Boolean = false,
    @SchemaDescription("Enable using immediate processing instead of deferred processing for client services.")
    @EncodeDefault
    val AutoConfig_SdClientImmediateProcessing_Enabled: Boolean = false,
    @SchemaDescription("List of server services and event handlers to be disabled.")
    @EncodeDefault
    val AutoConfig_DisabledServerServices: List<ServerSettings>? = emptyList(),
    @SchemaDescription("List of client services and consumed event groups to be disabled.")
    @EncodeDefault
    val AutoConfig_DisabledClientServices: List<ClientSettings>? = emptyList(),
    @SchemaDescription("Generate PreInit and InitMemory functions.")
    @EncodeDefault
    val AutoGenerate_PreAndMemoryInitFunctions: Boolean = true
) : PluginModel {
    @Suppress("unused")
    @Required
    @SchemaIntEnum(VERSION.toLong())
    @SchemaDescription("The version of this model. Must always be '$VERSION'.")
    private val version: Int = VERSION
    init {
        if (!AutoConfig_CC_CChannels.isNullOrEmpty()) {
            AutoConfig_CC_CChannels.checkUnique("Multiple channels are defined with the same name.") { it.ChannelName }
        }
    }
}

private const val VERSION = 4

private fun <T, K> Collection<T>.checkUnique(message: String, selector: (T) -> K) {
    if (asSequence().distinctBy(selector).count() != size) throw ModelValidationException(message)
}

@Serializable
data class ServerSettings(
    @SchemaDescription("Server Service name.")
    @EncodeDefault
    val ServerService: String,
    @SchemaDescription("Enable Server Service.")
    @EncodeDefault
    val ServerService_Enabled: Boolean,
    @SchemaDescription("List of Event Handlers.")
    @EncodeDefault
    val EventHandlers: List<String>?
)

@Serializable
data class ClientSettings(
    @SchemaDescription("Client Service name.")
    @EncodeDefault
    val ClientService: String,
    @SchemaDescription("Enable Client Service.")
    @EncodeDefault
    val ClientService_Enabled: Boolean,
    @SchemaDescription("List of Consumed Event Groups.")
    @EncodeDefault
    val ConsumedEventGroup: List<String>?
)

@Serializable
data class ECUStateMachineSettings(
    @SchemaDescription("The state machine will stay in Run mode for at least this period of time (in seconds).")
    @EncodeDefault
    val SelfRunRequestTimeout: Float = 0.1f,
    @SchemaDescription("The number of users which will be created for requesting Run mode.")
    @EncodeDefault
    val NumberOfRunRequestUsers: Int = 2,
    @SchemaDescription("The number of users which will be created for requesting PostRun mode.")
    @EncodeDefault
    val NumberOfPostRunRequestUsers: Int = 2,
    @SchemaDescription("Enable Kill All Run Request Port.")
    @EncodeDefault
    val KillAllRunRequestPortEnabled: Boolean = true,
    @SchemaDescription("Enable Dem Handling.")
    @EncodeDefault
    val DemHandlingEnabled: Boolean = true,
    @SchemaDescription("Enable ComM support for selected channels.")
    @EncodeDefault
    val ComMHandlingEnabled: Boolean = true,
    @SchemaDescription("List of all ComM channels which are to be ignored by the Ecu State Machine handling. If ignored, the Communication channels have to be enabled by the Application Code. If this list is empty, then all Communication channels are enabled at startup by BswM.")
    @EncodeDefault
    val DisabledComMChannels: List<String>? = emptyList(),
    @SchemaDescription("Enable NvM Handling.")
    @EncodeDefault
    val NvMHandlingEnabled: Boolean = true,
    @SchemaDescription("NvM Write All Timeout period in seconds.")
    @EncodeDefault
    val NvMWriteAllTimeout: Float = 60f,
    @SchemaDescription("NvM Cancel Write All Timeout period in seconds.")
    @EncodeDefault
    val NvMCancelWriteAllTimeout: Float = 60f,
    @SchemaDescription("Enable Rte Mode Synchronization.")
    @EncodeDefault
    val RteModeSynchronisationEnabled: Boolean = true,
    @SchemaDescription("Enable standard callouts for each state transition.")
    @EncodeDefault
    val EnableCallouts: Boolean = true,
    @SchemaDescription("Enable mode handling of EcuM. /MICROSAR/EcuM/EcuMFlexGeneral/EcuMModeHandling will also be enabled.")
    @EncodeDefault
    val EcuMModeHandlingEnabled: Boolean = true,
    @SchemaDescription("Enable restart EthIf Switch Ports functionality.")
    @EncodeDefault
    val RestartEthIfSwitchPorts: Boolean = true
) {
    init {
        if (SelfRunRequestTimeout < 0.0) {
            throw ModelValidationException("Self Run Request Timeout should have a value >= 0.0.")
        }
        if (NumberOfRunRequestUsers < 0) {
            throw ModelValidationException("Number Of Run Request Users should have a value >= 0.")
        }
        if (NumberOfPostRunRequestUsers < 0) {
            throw ModelValidationException("Number Of PostRun Request Users should have a value >= 0.")
        }
        if (NvMWriteAllTimeout < 0.0) {
            throw ModelValidationException("NvM Write All Timeout should have a value >= 0.")
        }
        if (NvMCancelWriteAllTimeout < 0.0) {
            throw ModelValidationException("NvM Cancel Write All Timeout should have a value >= 0.")
        }
    }
}


@Serializable
data class AutoConfig_CC_Channel(
    @SchemaDescription("Channel Name or shortname of Channel. So if this string is inherited in the DaVinci Cfg5-ChannelName, then it is used. A Uniqueness-Check will test if channels are defined twice.")
    @EncodeDefault
    val ChannelName: String,
    @SchemaDescription("Enable channel.")
    @EncodeDefault
    val ChannelEnabled: Boolean,
    @SchemaDescription("Enable parameter ReInitialize TX.")
    @EncodeDefault
    val ReInitializeTx: Boolean = true,
    @SchemaDescription("Enable parameter ReInitialize RX.")
    @EncodeDefault
    val ReInitializeRx: Boolean = true,
    @SchemaDescription("Schedule table of LIN channel.")
    @EncodeDefault
    val StartSchedule: String = "NO_STARTUP_SCHEDULE",
    @SchemaDescription("Enable parameter DCM_ENABLE_RX_DISABLE_TX_NORM.")
    @EncodeDefault
    val DcmEnableRxDisableTxNorm: Boolean = true,
    @SchemaDescription("Enable parameter DCM_DISABLE_RX_ENABLE_TX_NORM.")
    @EncodeDefault
    val DcmDisableRxEnableTxNorm: Boolean = false,
    @SchemaDescription("Enable parameter DCM_DISABLE_RX_TX_NORM_NM.")
    @EncodeDefault
    val DcmDisableRxTxNormNm: Boolean = false,
    @SchemaDescription("Enable parameter DCM_DISABLE_RX_TX_NM.")
    @EncodeDefault
    val DcmDisableRxTxNm: Boolean = false,
    @SchemaDescription("Enable parameter DCM_DISABLE_RX_TX_NORMAL.")
    @EncodeDefault
    val DcmDisableRxTxNorm: Boolean = false,
    @SchemaDescription("Enable parameter DCM_ENABLE_RX_DISABLE_TX_NM.")
    @EncodeDefault
    val DcmEnableRxDisableTxNm: Boolean = false,
    @SchemaDescription("Enable parameter DCM_DISABLE_RX_ENABLE_TX_NM.")
    @EncodeDefault
    val DcmDisableRxEnableTxNm: Boolean = false,
    @SchemaDescription("Enable parameter DCM_ENABLE_RX_DISABLE_TX_NORM_NM.")
    @EncodeDefault
    val DcmEnableRxDisableTxNormNm: Boolean = false,
    @SchemaDescription("Enable parameter DCM_DISABLE_RX_ENABLE_TX_NORM_NM.")
    @EncodeDefault
    val DcmDisableRxEnableTxNormNm: Boolean = false,
    @SchemaDescription("Enable parameter NM_STATE_BUS_OFF.")
    @EncodeDefault
    val NmStateBusOff: Boolean = false,
    @SchemaDescription("Enable parameter NM_STATE_CHECK_WAKEUP.")
    @EncodeDefault
    val NmStateCheckWakeup: Boolean = false,
    @SchemaDescription("Enable parameter NM_STATE_WAIT_STARTUP.")
    @EncodeDefault
    val NmStateWaitStartup: Boolean = false,
    @SchemaDescription("Enable parameter NM_STATE_BUS_SLEEP.")
    @EncodeDefault
    val NmStateBusSleep: Boolean = false,
    @SchemaDescription("Enable parameter NM_STATE_PREPARE_BUS_SLEEP.")
    @EncodeDefault
    val NmStatePrepareBusSleep: Boolean = false,
    @SchemaDescription("Enable Nm Communication for the channel.")
    @EncodeDefault
    val EnableNmCommunication: Boolean = false,
    @SchemaDescription("List of Ipdu Groups that should not be handled by the BswM Auto Config Rules - This implies that the application code enables/disables these Ipdu Groups. If this List is empty, then all Ipdu Groups are handled by BswM Auto Config.")
    @EncodeDefault
    val DisabledIpduGroups: List<String> = emptyList(),
    @SchemaDescription("List of PNCs.")
    @EncodeDefault
    val PNCIpduGroupSettings: List<AutoConfig_CC_PncDefs>? = emptyList(),
    @SchemaDescription("List of J1939 NM nodes.")
    @EncodeDefault
    val J1939Settings: List<AutoConfig_CC_J1939Defs>? = emptyList()
)

@Serializable
data class AutoConfig_CC_PncDefs(
    @SchemaDescription("Name of the PNC.")
    @EncodeDefault
    val PncName: String,
    @SchemaDescription("Enable the handling of the PNC by BswM. Otherwise the application code will have to enable/disable the Ipdu Groups.")
    @EncodeDefault
    val enabled: Boolean = true,
    @SchemaDescription("List of Ipdu Groups within the PNC that should not be handled by the BswM Auto Config Rules - This implies that the application code enables/disables these Ipdu Groups. If this List is empty, then all Ipdu Groups are handled by BswM Auto Config.")
    @EncodeDefault
    val DisabledIpduGroups: List<String> = emptyList()
)

@Serializable
data class AutoConfig_CC_J1939Defs(
    @SchemaDescription("Name of the J1939 NM node.")
    @EncodeDefault
    val J1939NmNodeName: String,
    @SchemaDescription("Enable the handling of the J1939 Nm node by BswM. Otherwise the application code will have to enable/disable the Rm node, Dcm node and Ipdu Groups.")
    @EncodeDefault
    val enabled: Boolean = true,
    @SchemaDescription("Enable the J1939 Rm node.")
    @EncodeDefault
    val enableRmNode: Boolean = true,
    @SchemaDescription("Enable the J1939 Dcm node.")
    @EncodeDefault
    val enableDcmNode: Boolean = true,
    @SchemaDescription("List of Routing Paths within the J1939 node that should not be handled by the BswM Auto Config Rules - This implies that the application code enables/disables these Routing Paths. If this List is empty, then all Routing Paths are handled by BswM Auto Config.")
    @EncodeDefault
    val DisabledRoutingPaths: List<String> = emptyList(),
    @SchemaDescription("List of Ipdu Groups within the J1939 node that should not be handled by the BswM Auto Config Rules - This implies that the application code enables/disables these Ipdu Groups. If this List is empty, then all Ipdu Groups are handled by BswM Auto Config.")
    @EncodeDefault
    val DisabledIpduGroups: List<String> = emptyList()
)

@Serializable
data class PartitionConfigSettings(
    @SchemaDescription("Enable Automatic Initialisation of Bsw Modules while Startup.")
    @EncodeDefault
    val AutoConfig_ModuleInitialisationEnabled: Boolean = true,
    @SchemaDescription("Name of the BswMConfig container")
    @EncodeDefault
    val BswMConfigName: String = "BswMConfig",
    @SchemaDescription("Name of the referenced EcucPartition")
    @EncodeDefault
    val EcucPartitionName: String = String(),
    @SchemaDescription("Sets the init task that initializes the selected BswM instance. Only needed when " +
                                 "partitions share the same core")
    @EncodeDefault
    val InitTask: String = String(),
    @SchemaDescription("Sets the init task event that is needed for synchronization at initialization")
    @EncodeDefault
    val InitTaskEvent: String = String(),
    @SchemaDescription("Sets the parameter Execute NvM ReadAll to true or false")
    @EncodeDefault
    val ExecuteNvMReadAll: Boolean = true,
    @SchemaDescription("Sets the parameter Enable Interrupts to true or false")
    @EncodeDefault
    val EnableInterrupts: Boolean = true,
    @SchemaDescription("Exclude Bsw Init functions from default initialisation. If defined here, the " +
                                 "application has to take care of initialisation.")
    @EncodeDefault
    val AutoConfig_DisabledModuleInitialisations: Set<String>? = emptySet()
)
