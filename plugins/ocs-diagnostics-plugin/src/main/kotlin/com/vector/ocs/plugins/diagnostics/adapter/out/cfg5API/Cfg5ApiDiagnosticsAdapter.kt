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
/*!        \file  Cfg5ApiDiagnosticsAdapter.kt
 *        \brief  Adapter that defines functions to read from or write to containers in Diagnostic related modules.
 *
 *      \details  Contains implementation of the read and write functions for Diagnostic related modules - Dcm, Dem, Nvm
 *                and ComM.
 *
 *********************************************************************************************************************/
package com.vector.ocs.plugins.diagnostics.adapter.out.cfg5API

import com.vector.ocs.lib.shared.Cfg5Client
import com.vector.cfg.automation.model.ecuc.microsar.dem.demgeneral.demfreezeframerecordclass.demfreezeframerecordupdate.EDemFreezeFrameRecordUpdate
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIContainer
import com.vector.ocs.core.api.OcsLogger
import com.vector.ocs.interop.derived
import com.vector.ocs.plugins.diagnostics.ComM.ComMChannel
import com.vector.ocs.lib.shared.Cfg5Client.createContainer
import com.vector.ocs.lib.shared.Cfg5Client.createContainerUnderModule
import com.vector.ocs.lib.shared.Cfg5Client.createOrGetChildContainer
import com.vector.ocs.lib.shared.Cfg5Client.deleteContainer
import com.vector.ocs.lib.shared.Cfg5Client.getChildContainer
import com.vector.ocs.lib.shared.Cfg5Client.getChildContainerList
import com.vector.ocs.lib.shared.Cfg5Client.setParameter
import com.vector.ocs.lib.shared.HelperLib.getValueInteger
import com.vector.ocs.plugins.diagnostics.application.port.out.cfgAPI.ConfigServiceAPI
import com.vector.ocs.plugins.diagnostics.domain.ComM.ComM
import com.vector.ocs.plugins.diagnostics.domain.Dcm.*
import com.vector.ocs.plugins.diagnostics.domain.Dem.*
import com.vector.ocs.plugins.diagnostics.domain.EnumTypes.EnumTypes
import com.vector.ocs.plugins.diagnostics.domain.NvM.NvM
import com.vector.ocs.plugins.diagnostics.domain.NvM.NvMBlockDescriptor
import com.vector.ocs.plugins.diagnostics.constants.DcmDefRefs.DcmDefRefConstantsFactory
import com.vector.ocs.plugins.diagnostics.constants.DemDefRefs.DemDefRefConstantsFactory
import com.vector.ocs.plugins.diagnostics.constants.NvMDefRefs.NvMDefRefConstantsFactory
import com.vector.ocs.plugins.diagnostics.constants.ComMDefRefs.ComMDefRefConstantsFactory

class Cfg5ApiDiagnosticsAdapter : ConfigServiceAPI {

    private val demDefRefs = DemDefRefConstantsFactory.getConstants() // Get Dem MDF-DefRefs depending on release
    private val dcmDefRefs = DcmDefRefConstantsFactory.getConstants() // Get Dcm MDF-DefRefs depending on release
    private val comMDefRefs = ComMDefRefConstantsFactory.getConstants() // Get ComM MDF-DefRefs depending on release
    private val nvMDefRefs = NvMDefRefConstantsFactory.getConstants() // Get NvM MDF-DefRefs depending on release

    /**
     * Write all containers and parameters which are necessary for diagnostics
     * Add all the functions which should write into the DaVinci Configurator Classic (CFG5)
     *
     * @param dem Dem module
     * @param dcm Dcm module
     * @param nvM NvM module
     * @param logger OcsLogger object
     */
    override fun writeDiagnosticModules(dem: Dem, dcm: Dcm, nvM: NvM, logger: OcsLogger) {

        // Call all use cases of the diagnostic models as functions
        writeComControlAllChannel(dcm.dcmDspComControlAllChannelList, logger)
        writeDcmDslProtocolRows(dcm.dcmDslProtocolRowList, logger)
        writeDcmDslBuffers(dcm.dcmDslBufferList, logger)
        writeDebouncingStrategy(dem.demEventParameterList, logger)
        writeDemClients(dem.demClientList, logger)
        writeDemFreezeRecordClass(dem.demFreezeFrameRecordClassList, logger)
        writeNvMBlockDescriptors(nvM.nvMBlockDescriptors, logger)
        writeNvMRamBlockIds(dem.demNvRamBlockIds, logger)
        writeDemDTCClass(dem.demDTCClass, logger)
        writeDemPrimaryMemory(dem.demPrimaryMemory, logger)
    }

    /**
     * Read containers and parameters which are necessary for the business logic
     *
     * @param dem Dem module
     * @param dcm Dcm module
     * @param comM ComM module
     * @param nvM NvM module
     * @param logger OcsLogger object
     */
    override fun readDiagnosticModules(dem: Dem, dcm: Dcm, comM: ComM, nvM: NvM, logger: OcsLogger) {
        readComMChannels(comM, logger)
        readDemEventParameters(dem, logger)
        readDcmDslProtocolRows(dcm, logger)
        readDcmDsdServiceTableList(dcm, logger)
        readDcmDslBufferList(dcm, logger)
        readDcmDspComControlAllChannels(dcm, logger)
        readDemClientList(dem, logger)
        readDemFreezeFrameRecordClassList(dem, logger)
        readDemDTCClassList(dem, logger)
        readDemPrimaryMemory(dem, logger)
        readDemDTCAttributesList(dem, logger)
        readNvMBlockDescriptorList(nvM, logger)
        readNvRamBlockIdList(dem, logger)
    }

    // Region Write Functions

    /**
     * Write Com control all channels
     *
     * @param dcmDspComControlAllChannelList list of DcmDspComControlAllChannels
     * @param logger OcsLogger object
     */
    private fun writeComControlAllChannel(
        dcmDspComControlAllChannelList: MutableList<DcmDspComControlAllChannel>,
        logger: OcsLogger
    ) {
        val comControlAllChannelShortName = "DcmDspComControlAllChannel"
        val comControlAllChannelDefRef = createDefRef(dcmDefRefs.DCM_DSP_COMCONTROL, comControlAllChannelShortName)
        val currentParameterDefRef = createDefRef(comControlAllChannelDefRef, "DcmDspAllComMChannelRef")

        dcmDspComControlAllChannelList.forEach { dcmDspComControlAllChannel ->
            val currentComMDefRefs = dcmDspComControlAllChannel.dcmDspAllComMChannelRef?.let {
                // Short name path needed for reference
                createShortNamePath(
                    comMDefRefs.COMM_CONFIG_SET,
                    it.shortName
                )
            }
            createContainer(
                dcmDefRefs.DCM_DSP_COMCONTROL,
                comControlAllChannelDefRef,
                dcmDspComControlAllChannel.shortName,
                logger
            )
            if (currentComMDefRefs != null) {
                setParameter(
                    dcmDspComControlAllChannel.shortName,
                    currentParameterDefRef,
                    currentComMDefRefs,
                    logger
                )
            }
        }
    }

    /**
     * Write debouncing strategy
     *
     * @param demEventParameterList list of DemEventParameters
     * @param logger OcsLogger object
     */
    private fun writeDebouncingStrategy(demEventParameterList: MutableList<DemEventParameter>, logger: OcsLogger) {

        for (demEventParameter in demEventParameterList) {
            var isDerived = false
            val algorithmClassContainer =
                Cfg5Client.getContainer(demDefRefs.DEM_EVENT_PARAMETER, demEventParameter.shortName, logger)
                    ?.getChildContainer(demDefRefs.DEM_EVENT_CLASS, demEventParameter.demEventClass.shortName)
                    ?.createOrGetChildContainer(
                        demDefRefs.DEM_DEBOUNCE_ALGORITHM_CLASS,
                        demEventParameter.demEventClass.demDebounceAlgorithmClass.shortName
                    )
            // Check if the algorithm class for the debounce strategy has any child container (counter based, monitor internal or time based)
            val childContainerList = mutableListOf<MIContainer>()
            childContainerList.addAll(
                algorithmClassContainer?.getChildContainerList(demDefRefs.DEM_DEBOUNCE_COUNTER_BASED) ?: emptyList()
            )
            childContainerList.addAll(
                algorithmClassContainer?.getChildContainerList(demDefRefs.DEM_DEBOUNCE_MONITOR_INTERNAL) ?: emptyList()
            )
            childContainerList.addAll(
                algorithmClassContainer?.getChildContainerList(demDefRefs.DEM_DEBOUNCE_TIME_BASE) ?: emptyList()
            )
            if (childContainerList.size > 0) {
                // Delete all children if they are not derived
                childContainerList.forEach {
                    if (!it.derived.isDerived) {
                        deleteContainer(it)
                    } else {
                        isDerived = true
                    }
                }
            }
            if (!isDerived) {
                if (demEventParameter.demEventClass.demDebounceAlgorithmClass.demDebounceCounterBased != null) {
                    algorithmClassContainer?.createOrGetChildContainer(
                        demDefRefs.DEM_DEBOUNCE_COUNTER_BASED,
                        "DemDebounceCounterBased"
                    )
                } else if (demEventParameter.demEventClass.demDebounceAlgorithmClass.demDebounceMonitorInternal != null) {
                    algorithmClassContainer?.createOrGetChildContainer(
                        demDefRefs.DEM_DEBOUNCE_MONITOR_INTERNAL,
                        "DemDebounceMonitorInternal"
                    )
                } else if (demEventParameter.demEventClass.demDebounceAlgorithmClass.demDebounceTimeBase != null) {
                    algorithmClassContainer?.createOrGetChildContainer(
                        demDefRefs.DEM_DEBOUNCE_TIME_BASE,
                        "DemDebounceTimeBase"
                    )
                    setParameter(
                        "DemDebounceTimeBase",
                        demDefRefs.DEM_DEBOUNCE_TIME_FAILED_THRESHOLD,
                        demEventParameter.demEventClass.demDebounceAlgorithmClass.demDebounceTimeBase!!.demDebounceTimeFailedThreshold,
                        logger
                    )
                    setParameter(
                        "DemDebounceTimeBase",
                        demDefRefs.DEM_DEBOUNCE_TIME_PASSED_THRESHOLD,
                        demEventParameter.demEventClass.demDebounceAlgorithmClass.demDebounceTimeBase!!.demDebounceTimePassedThreshold,
                        logger
                    )
                }
            }
        }
    }

    /**
     * Write Dem clients
     *
     * @param demClientList list of DemClients
     * @param logger OcsLogger object
     */
    private fun writeDemClients(demClientList: MutableList<DemClient>, logger: OcsLogger) {
        for (demClient in demClientList) {
            val demClientListCfg = Cfg5Client.getListOfContainer(demDefRefs.DEM_CLIENT, logger)
            if (!demClientListCfg.any { miContainer -> miContainer.name == demClient.shortName }) {
                createContainer(
                    demDefRefs.DEM_GENERAL,
                    demDefRefs.DEM_CLIENT,
                    demClient.shortName,
                    logger
                )
            }
        }
    }

    /**
     * Write Dcm dsl buffers
     *
     * @param dcmDslBufferList list of DcmDslBuffers
     * @param logger OcsLogger object
     */
    override fun writeDcmDslBuffers(dcmDslBufferList: MutableList<DcmDslBuffer>, logger: OcsLogger) {
        for (dcmDslBuffer in dcmDslBufferList) {
            val dcmDslBufferListCfg = Cfg5Client.getListOfContainer(dcmDefRefs.DCM_DSL_BUFFER, logger)
            if (!dcmDslBufferListCfg.any { miContainer -> miContainer.name == dcmDslBuffer.shortName }) {
                createContainer(
                    dcmDefRefs.DCM_DSL,
                    dcmDefRefs.DCM_DSL_BUFFER,
                    dcmDslBuffer.shortName,
                    logger
                )
            }
            dcmDslBuffer.dcmDslBufferSize?.let {
                setParameter(
                    dcmDslBuffer.shortName,
                    dcmDefRefs.DCM_DSL_BUFFER_SIZE,
                    it,
                    logger
                )
            }
        }
    }

    /**
     * Write Dcm dsl protocol rows
     *
     * @param dcmDslProtocolRowList list of DcmDslProtocolRows
     * @param logger OcsLogger object
     */
    private fun writeDcmDslProtocolRows(dcmDslProtocolRowList: MutableList<DcmDslProtocolRow>, logger: OcsLogger) {
        for (dcmDslProtocolRow in dcmDslProtocolRowList) {

            val currentServiceTableDefRefs = dcmDslProtocolRow.dcmDsdServiceTableRef?.let {
                // Short name path needed for reference
                createShortNamePath(
                    dcmDefRefs.DCM_DSD,
                    it.shortName
                )
            }
            createContainer(
                dcmDefRefs.DCM_DSL_PROTOCOL,
                dcmDefRefs.DCM_DSL_PROTOCOLROW,
                dcmDslProtocolRow.shortName,
                logger
            )
            if (currentServiceTableDefRefs != null) {
                setParameter(
                    dcmDslProtocolRow.shortName,
                    dcmDefRefs.DCM_DSL_PROTOCOL_SID_TABLE,
                    currentServiceTableDefRefs,
                    logger
                )
            }
            val currentDslProtocolTxBufferIdDefRefs = dcmDslProtocolRow.dcmDslProtocolTxBufferID?.let {
                createShortNamePath(dcmDefRefs.DCM_DSL, it.shortName)
            }
            val currentDcmDemClientRef = dcmDslProtocolRow.demClientRef?.let {
                createShortNamePath(demDefRefs.DEM_GENERAL, it.shortName)
            }
            if (currentDcmDemClientRef != null) {
                setParameter(
                    dcmDslProtocolRow.shortName,
                    dcmDefRefs.DCM_DEM_CLIENT_REF,
                    currentDcmDemClientRef,
                    logger
                )
            }
            if (currentDslProtocolTxBufferIdDefRefs != null) {
                setParameter(
                    dcmDslProtocolRow.shortName,
                    dcmDefRefs.DCM_DSL_TX_BUFFER_ID,
                    currentDslProtocolTxBufferIdDefRefs,
                    logger
                )
            }
            val currentDslProtocolRxBufferIdDefRefs = dcmDslProtocolRow.dcmDslProtocolRxBufferID?.let {
                createShortNamePath(dcmDefRefs.DCM_DSL, it.shortName)
            }
            if (currentDslProtocolRxBufferIdDefRefs != null) {
                setParameter(
                    dcmDslProtocolRow.shortName,
                    dcmDefRefs.DCM_DSL_RX_BUFFER_ID,
                    currentDslProtocolRxBufferIdDefRefs,
                    logger
                )
            }
            val currentMaxRespSizeDefRefs = dcmDslProtocolRow.dcmDslProtocolMaximumResponseSize
            if (currentMaxRespSizeDefRefs != null) {
                setParameter(
                    dcmDslProtocolRow.shortName,
                    dcmDefRefs.DCM_DSL_PROTOCOL_MAXIMUM_RESPONSE_SIZE,
                    currentMaxRespSizeDefRefs,
                    logger
                )
            }
        }
    }

    /**
     * Write Dem freeze record class
     *
     * @param demFreezeFrameRecordClassList list of DemFreezeFrameRecordClass
     * @param logger OcsLogger object
     */
    private fun writeDemFreezeRecordClass(
        demFreezeFrameRecordClassList: MutableList<DemFreezeFrameRecordClass>,
        logger: OcsLogger
    ) {
        for (demFreezeFrameRecordClass in demFreezeFrameRecordClassList) {
            var currentRecordUpdate: EDemFreezeFrameRecordUpdate? = null
            if (demFreezeFrameRecordClass.freeFrameRecordUpdate == EnumTypes.EFrameRecordUpdate.DEM_UPDATE_RECORD_NO) {
                currentRecordUpdate = EDemFreezeFrameRecordUpdate.DEM_UPDATE_RECORD_NO
            } else if (demFreezeFrameRecordClass.freeFrameRecordUpdate == EnumTypes.EFrameRecordUpdate.DEM_UPDATE_RECORD_YES) {
                currentRecordUpdate = EDemFreezeFrameRecordUpdate.DEM_UPDATE_RECORD_YES
            }
            setParameter(
                demFreezeFrameRecordClass.shortName,
                demDefRefs.DEM_FREEZE_FRAME_RECORD_UPDATE,
                currentRecordUpdate.toString(),
                logger
            )
        }
    }

    /**
     * Write NvM block descriptors
     *
     * @param nvMBlockDescriptors list of NvMBlockDescriptors
     * @param logger OcsLogger object
     */
    private fun writeNvMBlockDescriptors(nvMBlockDescriptors: MutableList<NvMBlockDescriptor>, logger: OcsLogger) {
        val currentBlockDescriptorList = Cfg5Client.getListOfContainer(nvMDefRefs.NVM_BLOCK_DESCRIPTOR, logger)
        for (nvMBlockDescriptor in nvMBlockDescriptors) {
            if (!currentBlockDescriptorList.any { x -> x.name == nvMBlockDescriptor.shortName }) {
                val containerDefRef = createDefRef(nvMDefRefs.NVM, "NvMBlockDescriptor")
                createContainerUnderModule(
                    containerDefRef,
                    nvMBlockDescriptor.shortName,
                    logger
                )
            }
            // Set NvM Block Use Set Ram Block Status
            nvMBlockDescriptor.nvMBlockUseSetRamBlockStatus?.let {
                setParameter(
                    nvMBlockDescriptor.shortName,
                    nvMDefRefs.NVM_BLOCK_USE_SET_RAM_BLOCK_STATUS,
                    it,
                    logger
                )
            }
            // Set NvM Use Job End Callback
            nvMBlockDescriptor.nvMUseJobEndCallback?.let {
                setParameter(
                    nvMBlockDescriptor.shortName,
                    nvMDefRefs.NVM_USE_JOBEND_CALLBACK,
                    it,
                    logger
                )
            }
            // Set NvM Use Init Callback
            nvMBlockDescriptor.nvMUseInitCallback?.let {
                setParameter(
                    nvMBlockDescriptor.shortName,
                    nvMDefRefs.NVM_USE_INIT_CALLBACK,
                    nvMBlockDescriptor.nvMUseInitCallback.toString(),
                    logger
                )
            }
            // Set NvM Init Block Callback
            nvMBlockDescriptor.nvMInitBlockCallback?.let {
                setParameter(
                    nvMBlockDescriptor.shortName,
                    nvMDefRefs.NVM_INIT_BLOCK_CALLBACK,
                    it,
                    logger
                )
            }
            // Set NvM Single Block Callback
            nvMBlockDescriptor.nvMSingleBlockCallback?.let {
                setParameter(
                    nvMBlockDescriptor.shortName,
                    nvMDefRefs.NVM_SINGLE_BLOCK_CALLBACK,
                    it,
                    logger
                )
            }
            // Set NvM Ram Block Data Address
            nvMBlockDescriptor.nvMRamBlockDataAddress?.let {
                setParameter(
                    nvMBlockDescriptor.shortName,
                    nvMDefRefs.NVM_RAM_BLOCK_DATA_ADDRESS,
                    nvMBlockDescriptor.nvMRamBlockDataAddress.toString(),
                    logger
                )
            }
            // Set NvM Select Block For Read All
            nvMBlockDescriptor.nvMSelectBlockForReadAll?.let {
                setParameter(
                    nvMBlockDescriptor.shortName,
                    nvMDefRefs.NVM_SELECT_BLOCK_FOR_READ_ALL,
                    nvMBlockDescriptor.nvMSelectBlockForReadAll.toString(),
                    logger
                )
            }
            // Set NvM Select Block For Write All
            nvMBlockDescriptor.nvMSelectBlockForWriteAll?.let {
                setParameter(
                    nvMBlockDescriptor.shortName,
                    nvMDefRefs.NVM_SELECT_BLOCK_FOR_WRITE_ALL,
                    nvMBlockDescriptor.nvMSelectBlockForWriteAll.toString(),
                    logger
                )
            }
            // Set NvM Resistant To Changed Software
            nvMBlockDescriptor.nvMResistantToChangedSoftware?.let {
                setParameter(
                    nvMBlockDescriptor.shortName,
                    nvMDefRefs.NVM_RESISTANT_TO_CHANGED_SOFTWARE,
                    nvMBlockDescriptor.nvMResistantToChangedSoftware.toString(),
                    logger
                )
            }
            // Set NvM Block Use Crc
            nvMBlockDescriptor.nvMBlockUseCrc?.let {
                setParameter(
                    nvMBlockDescriptor.shortName,
                    nvMDefRefs.NVM_BLOCK_USE_CRC,
                    nvMBlockDescriptor.nvMBlockUseCrc.toString(),
                    logger
                )
            }
            // Set NvM Rom Block Data Address
            nvMBlockDescriptor.nvMRomBlockDataAddress?.let {
                setParameter(
                    nvMBlockDescriptor.shortName,
                    nvMDefRefs.NVM_ROM_BLOCK_DATA_ADDRESS,
                    nvMBlockDescriptor.nvMRomBlockDataAddress.toString(),
                    logger
                )
            }
            // Set NvM Block Crc Type
            nvMBlockDescriptor.nvMBlockCrcType?.let {
                setParameter(
                    nvMBlockDescriptor.shortName,
                    nvMDefRefs.NVM_BLOCK_CRC_TYPE,
                    nvMBlockDescriptor.nvMBlockCrcType.toString(),
                    logger
                )
            }
            // Set NvM Block Management Type
            nvMBlockDescriptor.nvMBlockManagementType?.let {
                setParameter(
                    nvMBlockDescriptor.shortName,
                    nvMDefRefs.NVM_BLOCK_MANAGEMENT_TYPE,
                    nvMBlockDescriptor.nvMBlockManagementType.toString(),
                    logger
                )
            }
        }
    }

    /**
     * Write NvM ram block ids
     *
     * @param demNvRamBlockIds list of DemNvRamBlockIds
     * @param logger OcsLogger object
     */
    private fun writeNvMRamBlockIds(demNvRamBlockIds: MutableList<DemNvRamBlockId>, logger: OcsLogger) {
        val currentDemNvmRamBlockIdListCfg = Cfg5Client.getListOfContainer(demDefRefs.DEM_NV_RAM_BLOCK_ID, logger)
        for (demNvRamBlockId in demNvRamBlockIds) {
            if (!currentDemNvmRamBlockIdListCfg.any { miContainer -> miContainer.name == demNvRamBlockId.shortName }) {
                createContainer(
                    demDefRefs.DEM_GENERAL,
                    demDefRefs.DEM_NV_RAM_BLOCK_ID,
                    demNvRamBlockId.shortName,
                    logger
                )
            }
            demNvRamBlockId.nvRamBlockIdRef?.let {
                setParameter(
                    demNvRamBlockId.shortName,
                    demDefRefs.DEM_NV_RAM_BLOCK_ID_REF,
                    createShortNamePath(nvMDefRefs.NVM, it.shortName),
                    logger
                )
            }
            demNvRamBlockId.nvRamBlockIdType?.let {
                setParameter(
                    demNvRamBlockId.shortName,
                    demDefRefs.DEM_NV_RAM_BLOCK_ID_TYPE,
                    it.toString(),
                    logger
                )
            }
            demNvRamBlockId.nvRamBlockIdIndex?.let {
                setParameter(
                    demNvRamBlockId.shortName,
                    demDefRefs.DEM_NV_RAM_BLOCK_ID_INDEX,
                    it.toString(),
                    logger
                )
            }
            demNvRamBlockId.nvRamBlockIdEventMemoryRef?.let {
                val currentEventMemoryShortnamePath = createShortNamePath(
                    demDefRefs.DEM_EVENT_MEMORY_SET,
                    demNvRamBlockId.nvRamBlockIdEventMemoryRef!!.shortName
                )
                setParameter(
                    demNvRamBlockId.shortName,
                    demDefRefs.DEM_NV_RAM_BLOCK_ID_EVENT_MEMORY_REF,
                    currentEventMemoryShortnamePath,
                    logger
                )
            }
        }
    }

    /**
     * Write Dem DTC class
     *
     * @param demDTCClassList list of DemDTCClass
     * @param logger OcsLogger object
     */
    private fun writeDemDTCClass(demDTCClassList: MutableList<DemDTCClass>, logger: OcsLogger) {
        for (demDTCClass in demDTCClassList) {
            demDTCClass.demDTCAttributesRef?.let {
                setParameter(
                    demDTCClass.shortName,
                    demDefRefs.DEM_DTC_ATTRIBUTES_REF,
                    createShortNamePath(demDefRefs.DEM_CONFIG_SET, it.shortName),
                    logger
                )
            }
        }
    }

    /**
     * Write Dem primary memory
     *
     * @param demPrimaryMemory Dem primary memory parameter
     * @param logger OcsLogger object
     */
    private fun writeDemPrimaryMemory(demPrimaryMemory: DemPrimaryMemory, logger: OcsLogger) {
        setParameter(
            demPrimaryMemory.shortName,
            demDefRefs.DEM_MAX_NUMBER_EVENT_ENTRY_PRIMARY,
            demPrimaryMemory.demMaxNumberEventEntryPrimary.toString(),
            logger
        )
    }
    // End region

    // Region Read Functions

    /**
     * Read ComM channels
     *
     * @param comM ComM module
     * @param logger OcsLogger object
     */
    private fun readComMChannels(comM: ComM, logger: OcsLogger) {
        // Update the ComMChannels in domain objects
        // Get all available channels
        val comMChannelsCfg = Cfg5Client.getListOfContainer(comMDefRefs.COMM_CHANNEL, logger)
        comM.comMChannelList = mutableListOf()
        // For each channel create a domain object and add it to the list (needed shortname and main function period)
        comMChannelsCfg.forEach {
            comM.comMChannelList.add(
                ComMChannel(
                    it.name,
                    Cfg5Client.getParameter(it.name, comMDefRefs.COMM_MAIN_FUNCTION_PERIOD, logger)
                )
            )
        }
    }

    /**
     * Read Dem event parameters
     *
     * @param dem Dem module
     * @param logger OcsLogger object
     */
    private fun readDemEventParameters(dem: Dem, logger: OcsLogger) {
        val demEventParameterListCfg = Cfg5Client.getListOfContainer(demDefRefs.DEM_EVENT_PARAMETER, logger)
        dem.demEventParameterList = mutableListOf()
        demEventParameterListCfg.forEach { miContainer ->
            dem.demEventParameterList.add(DemEventParameter(miContainer.name))
        }
    }

    /**
     * Read Dcm dsl protocol rows
     *
     * @param dcm Dcm module
     * @param logger OcsLogger object
     */
    override fun readDcmDslProtocolRows(dcm: Dcm, logger: OcsLogger) {
        val dcmDslProtocolRowsCfg = Cfg5Client.getListOfContainer(dcmDefRefs.DCM_DSL_PROTOCOLROW, logger)
        dcm.dcmDslProtocolRowList = mutableListOf()
        dcmDslProtocolRowsCfg.forEach { miContainer ->
            dcm.dcmDslProtocolRowList.add(DcmDslProtocolRow(miContainer.name))
        }
    }

    /**
     * Read Dcm dsd service table list
     *
     * @param dcm Dcm module
     * @param logger OcsLogger object
     */
    private fun readDcmDsdServiceTableList(dcm: Dcm, logger: OcsLogger) {
        val dcmDsdServiceTableListCfg = Cfg5Client.getListOfContainer(dcmDefRefs.DCM_DSD_SERVICE_TABLE, logger)
        dcm.dcmDsdServiceTableList = mutableListOf()
        dcmDsdServiceTableListCfg.forEach { miContainer ->
            dcm.dcmDsdServiceTableList.add(DcmDsdServiceTable(miContainer.name))
        }
    }

    /**
     * Read Dcm dsl buffer list
     *
     * @param dcm Dcm module
     */
    override fun readDcmDslBufferList(dcm: Dcm, logger: OcsLogger) {
        val dcmDslBufferListCfg = Cfg5Client.getListOfContainer(dcmDefRefs.DCM_DSL_BUFFER, logger)
        dcm.dcmDslBufferList = mutableListOf()
        dcmDslBufferListCfg.forEach { miContainer ->
            val dcmDslBufferSize = miContainer.parameter.first().getValueInteger()!!.toInt()
            dcm.dcmDslBufferList.add(DcmDslBuffer(miContainer.name, dcmDslBufferSize))
            if (dcm.dcmDslProtocolRowList.size > 0) {
                dcm.dcmDslProtocolRowList.forEach {
                    if (it.dcmDslProtocolTxBufferID?.shortName?.contains(miContainer.name) == true) {
                        it.dcmDslProtocolMaximumResponseSize = dcmDslBufferSize
                    }
                }
            }
        }
    }

    /**
     * Read Dcm dsp com control all channels
     *
     * @param dcm Dcm module
     * @param logger OcsLogger object
     */
    private fun readDcmDspComControlAllChannels(dcm: Dcm, logger: OcsLogger) {
        val dcmDspComControlAllChannelsCfg =
            Cfg5Client.getListOfContainer(dcmDefRefs.DCM_DSP_COM_CONTROL_ALL_CHANNELS, logger)
        dcm.dcmDspComControlAllChannelList = mutableListOf()
        dcmDspComControlAllChannelsCfg.forEach { miContainer ->
            dcm.dcmDspComControlAllChannelList.add(DcmDspComControlAllChannel(miContainer.name))
        }
    }

    /**
     * Read Dem client list
     *
     * @param dem Dem module
     * @param logger OcsLogger object
     */
    private fun readDemClientList(dem: Dem, logger: OcsLogger) {
        val demClientListCfg = Cfg5Client.getListOfContainer(demDefRefs.DEM_CLIENT, logger)
        dem.demClientList = mutableListOf()
        dem.demClientList.clear()
        demClientListCfg.forEach { miContainer ->
            dem.demClientList.add(DemClient(miContainer.name))
        }
    }

    /**
     * Read Dem freeze frame record class list
     *
     * @param dem Dem module
     * @param logger OcsLogger object
     */
    private fun readDemFreezeFrameRecordClassList(dem: Dem, logger: OcsLogger) {
        val demFreezeFrameRecordClassListCfg =
            Cfg5Client.getListOfContainer(demDefRefs.DEM_FREEZE_FRAME_RECORD_CLASS, logger)
        dem.demFreezeFrameRecordClassList = mutableListOf()
        demFreezeFrameRecordClassListCfg.forEach { miContainer ->
            dem.demFreezeFrameRecordClassList.add(DemFreezeFrameRecordClass(miContainer.name))
        }
    }

    /**
     * Read NvM block descriptor list
     *
     * @param nvM NvM module
     * @param logger OcsLogger object
     * @return modified list of NvMBlockDescriptors
     */
    private fun readNvMBlockDescriptorList(nvM: NvM, logger: OcsLogger): MutableList<NvMBlockDescriptor> {
        val nvMBlockDescriptorListCfg = Cfg5Client.getListOfContainer(nvMDefRefs.NVM_BLOCK_DESCRIPTOR, logger)
        nvM.nvMBlockDescriptors = mutableListOf()
        nvMBlockDescriptorListCfg.forEach { miContainer ->
            nvM.nvMBlockDescriptors.add(NvMBlockDescriptor(miContainer.name))
        }
        return nvM.nvMBlockDescriptors
    }

    /**
     * Read NvRam block id list
     *
     * @param dem Dem module
     * @param logger OcsLogger object
     */
    private fun readNvRamBlockIdList(dem: Dem, logger: OcsLogger) {
        val demNvRamBlockIdListCfg = Cfg5Client.getListOfContainer(demDefRefs.DEM_NV_RAM_BLOCK_ID, logger)
        dem.demNvRamBlockIds = mutableListOf()
        demNvRamBlockIdListCfg.forEach { miContainer ->
            dem.demNvRamBlockIds.add(DemNvRamBlockId(miContainer.name))
        }
    }

    /**
     * Read Dem DTC class list
     *
     * @param dem Dem module
     * @param logger OcsLogger object
     */
    private fun readDemDTCClassList(dem: Dem, logger: OcsLogger) {
        val demDTCClassListCfg = Cfg5Client.getListOfContainer(demDefRefs.DEM_DTC_CLASS, logger)
        dem.demDTCClass = mutableListOf()
        demDTCClassListCfg.forEach { miContainer ->
            dem.demDTCClass.add(DemDTCClass(miContainer.name))
        }
    }

    /**
     * Read Dem primary memory
     *
     * @param dem Dem module
     * @param logger OcsLogger object
     */
    private fun readDemPrimaryMemory(dem: Dem, logger: OcsLogger) {
        val demPrimaryMemoryCfg = Cfg5Client.getContainer(demDefRefs.DEM_PRIMARY_MEMORY, "PrimaryMemory", logger)
        if (demPrimaryMemoryCfg != null) {
            dem.demPrimaryMemory = DemPrimaryMemory(demPrimaryMemoryCfg.name)
        } else {
            dem.demPrimaryMemory = DemPrimaryMemory("PrimaryMemory")
        }
    }

    /**
     * Read dem DTC attributes list
     *
     * @param dem Dem module
     * @param logger OcsLogger object
     */
    private fun readDemDTCAttributesList(dem: Dem, logger: OcsLogger) {
        val demDTCAttributesListCfg = Cfg5Client.getListOfContainer(demDefRefs.DEM_DTC_ATTRIBUTES, logger)
        dem.demDTCAttributes = mutableListOf()
        demDTCAttributesListCfg.forEach { miContainer ->
            dem.demDTCAttributes.add(DemDTCAttributes(miContainer.name))
        }
    }
    // End region

    // Region Helper functions

    /**
     * Create def ref
     *
     * @param parentDefRef defRef of the parent container
     * @param shortName short name of the element
     * @return shortName with parentDefRef
     */
    private fun createDefRef(parentDefRef: String, shortName: String): String {
        return "$parentDefRef/$shortName"
    }

    /**
     * Create short name path
     *
     * @param parentDefRef defRef of the parent container
     * @param shortName short name of the element
     * @return shortName path
     */
    private fun createShortNamePath(parentDefRef: String, shortName: String): String {
        return "${parentDefRef.replace("/MICROSAR", "/ActiveEcuC")}/$shortName"
    }


    /**
     * Validate modules
     *
     * @param modules modules to be validated
     */
    override fun validate(vararg modules: String) {
        Cfg5Client.validate(*modules)
    }

    /**
     * Trigger solving actions
     *
     * @param validationOrigin namespace of the validation message
     * @param validationId id of the validation message
     * @param logger OcsLogger object
     */
    override fun solve(validationOrigin: String, validationId: Int, logger: OcsLogger) {
        Cfg5Client.solve(validationOrigin, validationId, logger)
    }
    // End region
}
