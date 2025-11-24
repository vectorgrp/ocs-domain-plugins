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
/*!        \file  ConfigureDiagnosticsService.kt
 *        \brief  Service responsible for implementing use cases of port ConfigureDiagnosticsUseCase.
 *
 *      \details
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.diagnostics.application.service

import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.plugins.diagnostics.ComM.ComMChannel
import com.vector.ocs.plugins.diagnostics.DiagnosticsModel
import com.vector.ocs.plugins.diagnostics.EDefaultDcmBuffer
import com.vector.ocs.plugins.diagnostics.EDefaultDebouncingStrategy
import com.vector.ocs.plugins.diagnostics.application.port.`in`.ConfigureDiagnosticsUseCase
import com.vector.ocs.plugins.diagnostics.application.port.out.cfgAPI.ConfigServiceAPI
import com.vector.ocs.plugins.diagnostics.constants.DiagnosticConstants
import com.vector.ocs.plugins.diagnostics.constants.NvMDefRefs
import com.vector.ocs.plugins.diagnostics.domain.ComM.ComM
import com.vector.ocs.plugins.diagnostics.domain.Dcm.Dcm
import com.vector.ocs.plugins.diagnostics.domain.Dcm.DcmDslBuffer
import com.vector.ocs.plugins.diagnostics.domain.Dcm.DcmDslProtocolRow
import com.vector.ocs.plugins.diagnostics.domain.Dcm.DcmDspComControlAllChannel
import com.vector.ocs.plugins.diagnostics.domain.Dem.*
import com.vector.ocs.plugins.diagnostics.domain.EnumTypes.EnumTypes
import com.vector.ocs.plugins.diagnostics.domain.NvM.NvM
import com.vector.ocs.plugins.diagnostics.domain.NvM.NvMBlockDescriptor

class ConfigureDiagnosticsService(
    private val configServiceApi: ConfigServiceAPI,
    private val model: DiagnosticsModel,
    private val logger: LoggerService
) : ConfigureDiagnosticsUseCase {

    init {
        logger.infoMessage("Initialize Service and print the connected ConfigService object: $configServiceApi.")
    }

    /**
     * Configure diagnostic domain including the modules Dem, Dcm, ComM and NvM if applicable.
     *
     * @param logger OcsLogger object
     */
    override fun configureDiagnostics(logger: OcsLogger) {
        val dem = Dem()
        val dcm = Dcm()
        val comM = ComM()
        val nvM = NvM()

        configServiceApi.readDiagnosticModules(dem, dcm, comM, nvM, logger)

        processDcmConfiguration(model, dcm, dem, comM)
        processDemNvMConfiguration(model, dem, nvM)

        configServiceApi.writeDiagnosticModules(dem, dcm, nvM, logger)
    }

    /**
     * Process Dcm configuration
     *
     * @param model Diagnostic model
     * @param dcm Dcm module object
     * @param dem Dem module object
     * @param comM ComM module object
     */
    fun processDcmConfiguration(model: DiagnosticsModel, dcm: Dcm, dem: Dem, comM: ComM) {
        dcm.dcmDslProtocolRowList.forEachIndexed { index, currentProtocolRow ->

            // Set reference to DcmDsdServiceTable
            if (model.defaultDcmServiceTableAssignment == true) {
                val serviceTableRef = if (index < dcm.dcmDsdServiceTableList.size) {
                    dcm.dcmDsdServiceTableList[index]
                } else {
                    // Fallback to the first service table if index exceeds the list size
                    dcm.dcmDsdServiceTableList.first()
                }

                currentProtocolRow.dcmDsdServiceTableRef = serviceTableRef
                logger.infoMessage("Setting Service Table " + serviceTableRef.shortName + " as reference for protocol row " + currentProtocolRow.shortName + ".")
            }

            // Add DcmDslBuffer
            configureDcmDslBuffer(model, dcm, currentProtocolRow)

            // Set reference to DemClient
            if (model.setupDefaultDemClient == true) {
                if (dem.demClientList.isEmpty()) {
                    dem.demClientList.add(DemClient("DemClient"))
                }
                val currentDemClient = dem.demClientList.first()
                currentProtocolRow.addReferenceDemClient(currentDemClient)
                logger.infoMessage("Dem Client " + currentDemClient.shortName + " added to protocol row " + currentProtocolRow.shortName + ".")
            }
        }

        // Set default debounce strategy
        if (model.setupDefaultDebouncing == true) {
            setDefaultDebouncingStrategy(dem.demEventParameterList, model.defaultDebouncingStrategy)
            logger.infoMessage("No Debouncing configured for all DemEventParameters. Creating it and using " + model.defaultDebouncingStrategy.toString() + " as specified in json.")
        }

        // Set reference com control all channels with comM channels
        if (model.selectAllChannelsForControlAllChannels == true) {
            selectAllChannels(dcm.dcmDspComControlAllChannelList, comM.comMChannelList)
            logger.infoMessage("Set reference DcmDspComControlAllChannels with ComMChannels.")
        }
    }

    /**
     * Process Dem NvM configuration
     *
     * @param model Diagnostic model
     * @param dem Dem module object
     * @param nvM NvM module object
     */
    fun processDemNvMConfiguration(model: DiagnosticsModel, dem: Dem, nvM: NvM) {

        // Set up DiagMemBlocks
        if (model.setupDiagMemBlocks == true) {
            dem.demFreezeFrameRecordClassList.forEach { demFreezeFrameRecordClass ->
                demFreezeFrameRecordClass.freeFrameRecordUpdate = EnumTypes.EFrameRecordUpdate.DEM_UPDATE_RECORD_YES
            }
            logger.infoMessage("Dem present - setup the Dem Blocks.")

            // Create NvM Block Descriptors for admin and status data
            val adminBlock = NvMBlockDescriptor("DemAdminDataBlock")
            val statusBlock = NvMBlockDescriptor("DemStatusDataBlock")
            nvM.nvMBlockDescriptors.add(adminBlock)
            nvM.nvMBlockDescriptors.add(statusBlock)

            // Create Nv Ram Block Ids for admin and status
            val demAdmin = DemNvRamBlockId("Dem_AdminData")
            val demStatus = DemNvRamBlockId("Dem_StatusData")
            dem.demNvRamBlockIds.add(demAdmin)
            dem.demNvRamBlockIds.add(demStatus)

            // Get the max number of event entry primary
            val demPriBlockCnt = calculateMaxNumberOfEventEntryPrimary(model, dem)

            // Dem DTC attribute references
            val dtcClassWithoutAttributeRef = mutableListOf<DemDTCClass>()
            dem.demDTCClass.forEach { demDTCClass ->
                if (demDTCClass.demDTCAttributesRef == null) {
                    dtcClassWithoutAttributeRef.add(demDTCClass)
                }
            }

            if (dtcClassWithoutAttributeRef.isNotEmpty()) {
                if (dem.demDTCAttributes.isEmpty()) {
                    dem.demDTCAttributes.add(DemDTCAttributes("DemDTCAttributes"))
                }
                val currentDemDTCAttributesRef = dem.demDTCAttributes.first()
                currentDemDTCAttributesRef.demMemoryDestinationRef = dem.demPrimaryMemory
                dtcClassWithoutAttributeRef.forEach { dtcC ->
                    dtcC.demDTCAttributesRef = currentDemDTCAttributesRef
                }
            }

            // Configuring the admin block
            configureBlock(adminBlock, "Admin")

            // Configuring the status block
            configureBlock(statusBlock, "Status")

            // Setting references in the DemNvRamBlocks to the NvMBlockDescriptors
            demAdmin.nvRamBlockIdRef = adminBlock
            demAdmin.nvRamBlockIdType = EnumTypes.ENvRamBlockIdType.DEM_NVBLOCK_ADMIN
            demStatus.nvRamBlockIdRef = statusBlock
            demStatus.nvRamBlockIdType = EnumTypes.ENvRamBlockIdType.DEM_NVBLOCK_STATUS

            // Configuring the primary data blocks
            for (i in 0 until demPriBlockCnt.toInt()) {
                val priBlock = NvMBlockDescriptor("DemPrimaryDataBlock$i")
                nvM.nvMBlockDescriptors.add(priBlock)
                val demPriBlock = DemNvRamBlockId("Dem_PrimaryEntry$i")
                dem.demNvRamBlockIds.add(demPriBlock)
                demPriBlock.nvRamBlockIdRef = priBlock
                demPriBlock.nvRamBlockIdType = EnumTypes.ENvRamBlockIdType.DEM_NVBLOCK_PRIMARY
                demPriBlock.nvRamBlockIdIndex = i
                demPriBlock.nvRamBlockIdEventMemoryRef = dem.demPrimaryMemory
                configureBlock(priBlock, "Primary", i)
            }
        }
    }

    /**
     * Calculate max number of event entry primary objects
     *
     * @param model Diagnostic model
     * @param dem Dem module object
     * @return float number of event entry primary
     */
    fun calculateMaxNumberOfEventEntryPrimary(model: DiagnosticsModel, dem: Dem): Float {
        val demBlockCountScaling = model.diagMemBlocksScaling ?: DiagnosticConstants.BLOCK_COUNT_SCALING
        var demPriBlockCnt = dem.demDTCClass.count() * demBlockCountScaling
        if (demPriBlockCnt < model.DiagMemBlocksMin!!) {
            demPriBlockCnt = model.DiagMemBlocksMin.toFloat()
        } else if (demPriBlockCnt > model.DiagMemBlocksMax!!) {
            demPriBlockCnt = model.DiagMemBlocksMax.toFloat()
        }
        dem.demPrimaryMemory.demMaxNumberEventEntryPrimary = demPriBlockCnt.toInt()
        return demPriBlockCnt
    }

    /**
     * Configure Dcm dsl buffer
     *
     * @param model Diagnostic model
     * @param dcm Dcm module object
     * @param currentProtocolRow  current protocol row
     * @return modified Dcm module
     */
    private fun configureDcmDslBuffer(model: DiagnosticsModel, dcm: Dcm, currentProtocolRow: DcmDslProtocolRow): Dcm {
        when (model.defaultDcmBufferCreation) {
            EDefaultDcmBuffer.DISABLED -> {
                // Do nothing
            }

            EDefaultDcmBuffer.ENABLED_WITH_FIXED_SIZE -> {
                // DslProtocolMaximumResponseSize will be set to defaultDcmBufferSize while the reference to the buffer is set
                createBuffer(dcm, currentProtocolRow, model.defaultDcmBufferSize)
            }

            EDefaultDcmBuffer.ENABLED_WITH_CALC_SIZE -> {
                // DslProtocolMaximumResponseSize will be set to the calculated size while the reference to the buffer is set
                createBuffer(dcm, currentProtocolRow, 8)
            }

            else -> logger.errorMessage("${model.defaultDcmBufferCreation} has no case in configureDcmDslBuffer method.")
        }
        return dcm
    }

    /**
     * Configure NvM block
     *
     * @param nvMBlockDescriptor NvM block descriptor
     * @param name name for block data
     * @param counter increases for more entries
     * @return configured NvM Block Descriptor
     */
    fun configureBlock(nvMBlockDescriptor: NvMBlockDescriptor, name: String, counter: Int = 0): NvMBlockDescriptor {
        nvMBlockDescriptor.nvMBlockUseSetRamBlockStatus = true

        if (name == "Primary") {
            nvMBlockDescriptor.nvMRomBlockDataAddress = if (NvMDefRefs.minorVersion >= DiagnosticConstants.CFG5_VERSION_R34) "Dem_Cfg_PrimaryEventEntryInit" else "Dem_Cfg_MemoryEntryInit"
            nvMBlockDescriptor.nvMSingleBlockCallback = "Dem_NvM_JobFinished"
            nvMBlockDescriptor.nvMRamBlockDataAddress = "Dem_Cfg_PrimaryEntry_$counter"
            nvMBlockDescriptor.nvMUseInitCallback = false
        } else {
            nvMBlockDescriptor.nvMInitBlockCallback = "Dem_NvM_Init${name}Data"
            nvMBlockDescriptor.nvMSingleBlockCallback = "Dem_NvM_JobFinished"
            nvMBlockDescriptor.nvMRamBlockDataAddress = "Dem_Cfg_${name}Data"
            nvMBlockDescriptor.nvMUseInitCallback = true
        }
        nvMBlockDescriptor.nvMUseJobEndCallback = true
        nvMBlockDescriptor.nvMSelectBlockForReadAll = true
        nvMBlockDescriptor.nvMSelectBlockForWriteAll = true
        nvMBlockDescriptor.nvMResistantToChangedSoftware = true
        nvMBlockDescriptor.nvMBlockUseCrc = true
        nvMBlockDescriptor.nvMBlockCrcType = EnumTypes.ENvMBlockCrcType.NVM_CRC16
        nvMBlockDescriptor.nvMBlockManagementType = EnumTypes.ENvMBlockManagementType.NVM_BLOCK_NATIVE

        return nvMBlockDescriptor
    }

    /**
     * Create buffer
     *
     * @param dcm Dcm module
     * @param currentProtocolRow current Protocol row
     * @param bufferSize size of buffer
     */
    fun createBuffer(
        dcm: Dcm,
        currentProtocolRow: DcmDslProtocolRow,
        bufferSize: Int?
    ) {
        if (dcm.dcmDslBufferList.isEmpty()) {
            dcm.dcmDslBufferList.add(DcmDslBuffer("DcmDslBuffer", bufferSize))
        }

        val firstBuffer = dcm.dcmDslBufferList.first()
        firstBuffer.dcmDslBufferSize = bufferSize
        logger.infoMessage("Creating Dsl Buffer with default length $bufferSize Bytes, assigning it to empty protocol row buffer references.")

        // Add reference and buffer size
        currentProtocolRow.addReferenceProtocolAndDcmDslBuffer(firstBuffer)
        logger.infoMessage("${currentProtocolRow.shortName} Tx Buffer ID set to ${firstBuffer.shortName}.")
        logger.infoMessage("ResponseSize for Protocol Row " + currentProtocolRow.shortName + " set to default buffer's size: " + firstBuffer.dcmDslBufferSize.toString() + ".")
    }

    /**
     * Select all channels
     *
     * @param dcmDspComControlAllChannelList list of DcmDspComControlAllChannel objects
     * @param comMChannelList list of ComMChannel objects
     * @return modified list of DcmDspComControlAllChannel objects
     */
    fun selectAllChannels(
        dcmDspComControlAllChannelList: MutableList<DcmDspComControlAllChannel>,
        comMChannelList: MutableList<ComMChannel>
    ): MutableList<DcmDspComControlAllChannel> {
        val alreadyReferencedComMChannels = mutableListOf<String>()
        // Reference ComMChannels with ComMChannelRef
        dcmDspComControlAllChannelList.forEachIndexed { index, dcmDspComControlAllChannel ->
            if (index < comMChannelList.count()) {
                val comMChannel = comMChannelList[index]
                val currentRefComMChannel = dcmDspComControlAllChannel.dcmDspAllComMChannelRef
                if (currentRefComMChannel != null) {
                    // Index of element equals channel id
                    if (currentRefComMChannel.channelId == index) {
                        alreadyReferencedComMChannels.add(currentRefComMChannel.shortName)
                    }
                } else {
                    dcmDspComControlAllChannel.dcmDspAllComMChannelRef = comMChannel
                    alreadyReferencedComMChannels.add(comMChannel.shortName)
                }
            }
        }

        // Check if ComMChannel is already referenced. If not, create new DspComControlAllChannel
        comMChannelList.forEach { comMChannel ->
            if (!alreadyReferencedComMChannels.contains(comMChannel.shortName)) {
                val allChannelEntry = (DcmDspComControlAllChannel("DspComControlAllChannel_${comMChannel.shortName}"))
                allChannelEntry.dcmDspAllComMChannelRef = comMChannel
                dcmDspComControlAllChannelList.add(allChannelEntry)
                logger.infoMessage("ComM Channel " + comMChannel.shortName + " used for DcmComControlAllChannelEntry " + allChannelEntry.shortName + ".")
            }
        }
        return dcmDspComControlAllChannelList
    }

    /**
     * Set default debouncing strategy of DebounceAlgorithmClass
     *
     * @param demEventParameterList list of EventParameter objects
     * @param defaultDebouncingStrategy default DebounceStrategy of the EventParameter
     */
    fun setDefaultDebouncingStrategy(
        demEventParameterList: MutableList<DemEventParameter>,
        defaultDebouncingStrategy: EDefaultDebouncingStrategy?
    ) {
        for (demEventParameter in demEventParameterList) {
            logger.infoMessage("No Debouncing configured for " + demEventParameter.shortName + ". Creating it and using " + defaultDebouncingStrategy + " as specified in json.")
            when (defaultDebouncingStrategy) {
                // Configure CounterBased DebouncingStrategy
                EDefaultDebouncingStrategy.CounterBased -> {
                    val demDebounceCounterBased =
                        DemDebounceCounterBased("DemDebounceCounterBased")
                    demEventParameter.demEventClass.demDebounceAlgorithmClass.demDebounceCounterBased =
                        demDebounceCounterBased
                }
                // Configure TimeBased DebouncingStrategy
                EDefaultDebouncingStrategy.TimeBased -> {
                    val demDebounceTimeBase =
                        DemDebounceTimeBase("DemDebounceTimeBase")
                    demDebounceTimeBase.demDebounceTimeFailedThreshold =
                        DiagnosticConstants.DEBOUNCE_TIME_FAILED_THRESHOLD
                    demDebounceTimeBase.demDebounceTimePassedThreshold =
                        DiagnosticConstants.DEBOUNCE_TIME_PASSED_THRESHOLD
                    demEventParameter.demEventClass.demDebounceAlgorithmClass.demDebounceTimeBase = demDebounceTimeBase
                }
                // Configure MonitorInternal DebouncingStrategy
                EDefaultDebouncingStrategy.MonitorInternal -> {
                    val demDebounceMonitorInternal =
                        DemDebounceMonitorInternal("DemDebounceMonitorInternal")
                    demEventParameter.demEventClass.demDebounceAlgorithmClass.demDebounceMonitorInternal =
                        demDebounceMonitorInternal
                }

                else -> logger.errorMessage("$defaultDebouncingStrategy has no case in setDefaultDebouncing method.")
            }
        }
    }
}
