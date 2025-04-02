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
/*!        \file  ServiceTest.kt
 *        \brief  Unit tests to verify the functionality of Diagnostics plugin services.
 *
 *      \details
 *
 *********************************************************************************************************************/
import com.vector.ocs.plugins.diagnostics.ComM.ComMChannel
import com.vector.ocs.plugins.diagnostics.DiagnosticsModel
import com.vector.ocs.plugins.diagnostics.EDefaultDebouncingStrategy
import com.vector.ocs.plugins.diagnostics.application.port.out.cfgAPI.ConfigServiceAPI
import com.vector.ocs.plugins.diagnostics.application.service.ConfigureDiagnosticsService
import com.vector.ocs.plugins.diagnostics.application.service.LoggerService
import com.vector.ocs.plugins.diagnostics.domain.ComM.ComM
import com.vector.ocs.plugins.diagnostics.domain.Dcm.Dcm
import com.vector.ocs.plugins.diagnostics.domain.Dcm.DcmDsdServiceTable
import com.vector.ocs.plugins.diagnostics.domain.Dcm.DcmDslProtocolRow
import com.vector.ocs.plugins.diagnostics.domain.Dcm.DcmDspComControlAllChannel
import com.vector.ocs.plugins.diagnostics.domain.Dem.*
import com.vector.ocs.plugins.diagnostics.domain.EnumTypes.EnumTypes
import com.vector.ocs.plugins.diagnostics.domain.NvM.NvM
import com.vector.ocs.plugins.diagnostics.domain.NvM.NvMBlockDescriptor
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ServiceTest {
    // Mock outbound port
    private val cfg5ServiceApi = mockk<ConfigServiceAPI>()
    private val diagnosticsModel = mockk<DiagnosticsModel>()
    private val logger = mockk<LoggerService>(relaxed = true)

    // Create instance of service
    private val configureDiagnosticsService = ConfigureDiagnosticsService(cfg5ServiceApi, diagnosticsModel, logger)

    /**
     * Tests if the dcmDsdServiceTableRef of dcmDslProtocolRowList is set to the correct dcmDsdServiceTable.
     * Tests if the demClientRef of dcmDslProtocolRowList is set to the correct demClient.
     * setDefaultDebouncingStrategy, selectAllChannels called in ProcessDcmConfiguration are tested separately.
     */
    @Test
    fun testProcessDcmConfiguration() {
        val dcm = Dcm()
        val dem = Dem()
        val comM = ComM()

        // Initialize dcm
        dcm.dcmDslProtocolRowList = mutableListOf(
            DcmDslProtocolRow("DcmDslProtocolRow1"),
            DcmDslProtocolRow("DcmDslProtocolRow2")
        )
        dcm.dcmDsdServiceTableList = mutableListOf(
            DcmDsdServiceTable("DcmDsdServiceTable1"),
            DcmDsdServiceTable("DcmDsdServiceTable2")
        )

        // No need for initialisation
        dcm.dcmDspComControlAllChannelList = mutableListOf()
        dcm.dcmDslBufferList = mutableListOf()
        dem.demClientList = mutableListOf()
        dem.demEventParameterList = mutableListOf()
        comM.comMChannelList = mutableListOf()

        // Initialize model
        val model = DiagnosticsModel(
            defaultDcmServiceTableAssignment = true,
            setupDefaultDemClient = true
        )

        configureDiagnosticsService.processDcmConfiguration(model, dcm, dem, comM)

        // Verify DcmDsdServiceTable references
        assertEquals(dcm.dcmDsdServiceTableList[0], dcm.dcmDslProtocolRowList[0].dcmDsdServiceTableRef)
        assertEquals(dcm.dcmDsdServiceTableList[1], dcm.dcmDslProtocolRowList[1].dcmDsdServiceTableRef)

        // Verify DemClient references
        assertEquals(dcm.dcmDslProtocolRowList[0].demClientRef, dem.demClientList[0])
        assertEquals(dcm.dcmDslProtocolRowList[1].demClientRef, dem.demClientList[0])
    }

    /**
     * Tests if the DemFreezeFrameRecordClass list is updated correctly.
     * Tests if the short names and block IDs of Admin and Status blocks are set correctly.
     * Tests if the DemDTCAttributes references are configured correctly.
     * Tests if the primary data blocks are configured correctly.
     */
    @Test
    fun testProcessDemNvMConfiguration() {
        val nvm = NvM()
        val dem = Dem()

        // Initialize nvm
        nvm.nvMBlockDescriptors = mutableListOf()

        // Initialize dem
        dem.demFreezeFrameRecordClassList = mutableListOf(
            DemFreezeFrameRecordClass("DemFreezeFrameRecordClass0"),
            DemFreezeFrameRecordClass("DemFreezeFrameRecordClass1"),
            DemFreezeFrameRecordClass("DemFreezeFrameRecordClass2")
        )
        dem.demDTCClass = mutableListOf(
            DemDTCClass("DemDTCClass1"),
            DemDTCClass("DemDTCClass2"),
            DemDTCClass("DemDTCClass3")
        )
        dem.demPrimaryMemory = DemPrimaryMemory("PrimaryMemory")
        dem.demDTCAttributes = mutableListOf()
        dem.demNvRamBlockIds = mutableListOf()

        // Initialize model
        val model = DiagnosticsModel(
            setupDiagMemBlocks = true
        )

        configureDiagnosticsService.processDemNvMConfiguration(model, dem, nvm)

        // Verify DemFreezeFrameRecordClass updates
        dem.demFreezeFrameRecordClassList.forEach {
            assertEquals(EnumTypes.EFrameRecordUpdate.DEM_UPDATE_RECORD_YES, it.freeFrameRecordUpdate)
        }

        // Verify short names of Admin and Status blocks
        assertEquals("DemAdminDataBlock", nvm.nvMBlockDescriptors[0].shortName)
        assertEquals("DemStatusDataBlock", nvm.nvMBlockDescriptors[1].shortName)

        // Verify demNvRamBlockIds of Admin and Status blocks
        assertEquals("Dem_AdminData", dem.demNvRamBlockIds[0].shortName)
        assertEquals("Dem_StatusData", dem.demNvRamBlockIds[1].shortName)

        // Verify DemDTCAttributes references
        assertEquals(1, dem.demDTCAttributes.size)
        val demDTCAttributes = dem.demDTCAttributes.first()
        assertEquals(dem.demPrimaryMemory, demDTCAttributes.demMemoryDestinationRef)
        dem.demDTCClass.forEach {
            assertEquals(demDTCAttributes, it.demDTCAttributesRef)
        }

        // Verify primary data blocks
        val demPriBlockCnt = 8

        for (i in 0 until demPriBlockCnt) {
            val priBlock = nvm.nvMBlockDescriptors[i + 2] // Offset by 2 for admin and status blocks
            assertEquals("DemPrimaryDataBlock$i", priBlock.shortName)
            val demPriBlock = dem.demNvRamBlockIds[i + 2] // Offset by 2 for admin and status blocks
            assertEquals("Dem_PrimaryEntry$i", demPriBlock.shortName)
            assertEquals(priBlock, demPriBlock.nvRamBlockIdRef)
            assertEquals(EnumTypes.ENvRamBlockIdType.DEM_NVBLOCK_PRIMARY, demPriBlock.nvRamBlockIdType)
            assertEquals(i, demPriBlock.nvRamBlockIdIndex)
            assertEquals(dem.demPrimaryMemory, demPriBlock.nvRamBlockIdEventMemoryRef)
        }
    }

    /**
     * Tests if the correct default debouncing strategy is set for the dem event parameters.
     */
    @Test
    fun testSetDefaultDebouncingStrategy() {

        // Test business logic here for each of the three strategies
        for (debounceStrategy in enumValues<EDefaultDebouncingStrategy>()) {
            // Create return list for mocked read function
            val demEventParameterList = mutableListOf(
                DemEventParameter("DemEventParameter1"),
                DemEventParameter("DemEventParameter2"),
                DemEventParameter("DemEventParameter3")
            )

            // Call function under test with specified debounce strategy
            configureDiagnosticsService.setDefaultDebouncingStrategy(demEventParameterList, debounceStrategy)

            /* Check if the correct strategy is set for every dem event parameter -> specific child object should
             not be null */
            when (debounceStrategy) {
                EDefaultDebouncingStrategy.CounterBased -> {
                    assertNotNull(demEventParameterList[0].demEventClass.demDebounceAlgorithmClass.demDebounceCounterBased)
                    assertNotNull(demEventParameterList[1].demEventClass.demDebounceAlgorithmClass.demDebounceCounterBased)
                    assertNotNull(demEventParameterList[2].demEventClass.demDebounceAlgorithmClass.demDebounceCounterBased)

                    assertNull(demEventParameterList[0].demEventClass.demDebounceAlgorithmClass.demDebounceMonitorInternal)
                    assertNull(demEventParameterList[1].demEventClass.demDebounceAlgorithmClass.demDebounceMonitorInternal)
                    assertNull(demEventParameterList[2].demEventClass.demDebounceAlgorithmClass.demDebounceMonitorInternal)

                    assertNull(demEventParameterList[0].demEventClass.demDebounceAlgorithmClass.demDebounceTimeBase)
                    assertNull(demEventParameterList[1].demEventClass.demDebounceAlgorithmClass.demDebounceTimeBase)
                    assertNull(demEventParameterList[2].demEventClass.demDebounceAlgorithmClass.demDebounceTimeBase)
                }

                EDefaultDebouncingStrategy.TimeBased -> {
                    assertNull(demEventParameterList[0].demEventClass.demDebounceAlgorithmClass.demDebounceCounterBased)
                    assertNull(demEventParameterList[1].demEventClass.demDebounceAlgorithmClass.demDebounceCounterBased)
                    assertNull(demEventParameterList[2].demEventClass.demDebounceAlgorithmClass.demDebounceCounterBased)

                    assertNull(demEventParameterList[0].demEventClass.demDebounceAlgorithmClass.demDebounceMonitorInternal)
                    assertNull(demEventParameterList[1].demEventClass.demDebounceAlgorithmClass.demDebounceMonitorInternal)
                    assertNull(demEventParameterList[2].demEventClass.demDebounceAlgorithmClass.demDebounceMonitorInternal)

                    assertNotNull(demEventParameterList[0].demEventClass.demDebounceAlgorithmClass.demDebounceTimeBase)
                    assertNotNull(demEventParameterList[1].demEventClass.demDebounceAlgorithmClass.demDebounceTimeBase)
                    assertNotNull(demEventParameterList[2].demEventClass.demDebounceAlgorithmClass.demDebounceTimeBase)
                }

                EDefaultDebouncingStrategy.MonitorInternal -> {
                    assertNull(demEventParameterList[0].demEventClass.demDebounceAlgorithmClass.demDebounceCounterBased)
                    assertNull(demEventParameterList[1].demEventClass.demDebounceAlgorithmClass.demDebounceCounterBased)
                    assertNull(demEventParameterList[2].demEventClass.demDebounceAlgorithmClass.demDebounceCounterBased)

                    assertNotNull(demEventParameterList[0].demEventClass.demDebounceAlgorithmClass.demDebounceMonitorInternal)
                    assertNotNull(demEventParameterList[1].demEventClass.demDebounceAlgorithmClass.demDebounceMonitorInternal)
                    assertNotNull(demEventParameterList[2].demEventClass.demDebounceAlgorithmClass.demDebounceMonitorInternal)

                    assertNull(demEventParameterList[0].demEventClass.demDebounceAlgorithmClass.demDebounceTimeBase)
                    assertNull(demEventParameterList[1].demEventClass.demDebounceAlgorithmClass.demDebounceTimeBase)
                    assertNull(demEventParameterList[2].demEventClass.demDebounceAlgorithmClass.demDebounceTimeBase)
                }
            }
        }
    }

    /**
     * Tests the use case when the configured DcmDspComControlAllChannels are more than the available ComMChannels.
     */
    @Test
    fun testSelectAllChannelsWithMoreComControlChannels() {
        val dcmDspComControlAllChannelList = mutableListOf(
            DcmDspComControlAllChannel("DcmDspComControlAllChannel0"),
            DcmDspComControlAllChannel("DcmDspComControlAllChannel1"),
            DcmDspComControlAllChannel("DcmDspComControlAllChannel2"),
            DcmDspComControlAllChannel("DcmDspComControlAllChannel3")
        )

        val comMChannelList = mutableListOf(
            ComMChannel("ComMChannel0"),
            ComMChannel("ComMChannel1"),
            ComMChannel("ComMChannel2")
        )
        val newDcmDspComControlAllChannelList =
            configureDiagnosticsService.selectAllChannels(dcmDspComControlAllChannelList, comMChannelList)
        val countDcmDspComControlAllChannel = newDcmDspComControlAllChannelList.count()
        var counter = 0
        for (dcmDspComControlAllChannel in dcmDspComControlAllChannelList) {
            if (counter < 3) {
                assertNotNull(dcmDspComControlAllChannel.dcmDspAllComMChannelRef)
            } else {
                assertNull(dcmDspComControlAllChannel.dcmDspAllComMChannelRef)
            }
            counter++
        }

        val countComMChannel = comMChannelList.count()
        assertEquals(7, countDcmDspComControlAllChannel + countComMChannel)
    }

    /**
     * Tests the use case when the available ComMChannels are more than the configured DcmDspComControlAllChannels.
     */
    @Test
    fun testSelectAllChannelsWithMoreComMChannels() {
        val dcmDspComControlAllChannelList = mutableListOf(
            DcmDspComControlAllChannel("DcmDspComControlAllChannel0"),
            DcmDspComControlAllChannel("DcmDspComControlAllChannel1"),
            DcmDspComControlAllChannel("DcmDspComControlAllChannel2")
        )

        val comMChannelList = mutableListOf(
            ComMChannel("ComMChannel0"),
            ComMChannel("ComMChannel1"),
            ComMChannel("ComMChannel2"),
            ComMChannel("ComMChannel3")
        )
        val newDcmDspComControlAllChannelList =
            configureDiagnosticsService.selectAllChannels(dcmDspComControlAllChannelList, comMChannelList)
        val countDcmDspComControlAllChannel = newDcmDspComControlAllChannelList.count()

        for (dcmDspComControlAllChannel in dcmDspComControlAllChannelList) {
            assertNotNull(dcmDspComControlAllChannel.dcmDspAllComMChannelRef)
        }

        val countComMChannel = comMChannelList.count()
        assertEquals(8, countDcmDspComControlAllChannel + countComMChannel)
    }

    /**
     * Tests if the parameters of the NvM block descriptors are configured correctly.
     */
    @Test
    fun testConfigureBlock() {
        val primaryBlock = NvMBlockDescriptor("DemPrimaryDataBlock")
        primaryBlock.nvMRomBlockDataAddress = "Dem_Cfg_MemoryEntryInit"
        primaryBlock.nvMSingleBlockCallback = "Dem_NvM_JobFinished"
        primaryBlock.nvMRamBlockDataAddress = "Dem_Cfg_PrimaryEntry_0"
        primaryBlock.nvMUseInitCallback = false
        primaryBlock.nvMUseJobEndCallback = true
        primaryBlock.nvMSelectBlockForReadAll = true
        primaryBlock.nvMSelectBlockForWriteAll = true
        primaryBlock.nvMResistantToChangedSoftware = true
        primaryBlock.nvMBlockUseCrc = true
        primaryBlock.nvMBlockCrcType = EnumTypes.ENvMBlockCrcType.NVM_CRC16
        primaryBlock.nvMBlockManagementType = EnumTypes.ENvMBlockManagementType.NVM_BLOCK_NATIVE

        val adminBlock = NvMBlockDescriptor("AdminBlock")
        adminBlock.nvMInitBlockCallback = "Dem_NvM_InitAdminData"
        adminBlock.nvMSingleBlockCallback = "Dem_NvM_JobFinished"
        adminBlock.nvMRamBlockDataAddress = "Dem_Cfg_AdminData"
        adminBlock.nvMUseInitCallback = true
        adminBlock.nvMUseJobEndCallback = true
        adminBlock.nvMSelectBlockForReadAll = true
        adminBlock.nvMSelectBlockForWriteAll = true
        adminBlock.nvMResistantToChangedSoftware = true
        adminBlock.nvMBlockUseCrc = true
        adminBlock.nvMBlockCrcType = EnumTypes.ENvMBlockCrcType.NVM_CRC16
        adminBlock.nvMBlockManagementType = EnumTypes.ENvMBlockManagementType.NVM_BLOCK_NATIVE

        val statusBlock = NvMBlockDescriptor("StatusBlock")
        statusBlock.nvMInitBlockCallback = "Dem_NvM_InitStatusData"
        statusBlock.nvMSingleBlockCallback = "Dem_NvM_JobFinished"
        statusBlock.nvMRamBlockDataAddress = "Dem_Cfg_StatusData"
        statusBlock.nvMUseInitCallback = true
        statusBlock.nvMUseJobEndCallback = true
        statusBlock.nvMSelectBlockForReadAll = true
        statusBlock.nvMSelectBlockForWriteAll = true
        statusBlock.nvMResistantToChangedSoftware = true
        statusBlock.nvMBlockUseCrc = true
        statusBlock.nvMBlockCrcType = EnumTypes.ENvMBlockCrcType.NVM_CRC16
        statusBlock.nvMBlockManagementType = EnumTypes.ENvMBlockManagementType.NVM_BLOCK_NATIVE

        val blocks = mapOf("Admin" to adminBlock, "Status" to statusBlock, "Primary" to primaryBlock)

        for (block in blocks) {
            val blockForTest = NvMBlockDescriptor("NvMBlockDescriptor")
            val configuredNvMBlockDescriptor = configureDiagnosticsService.configureBlock(blockForTest, block.key)

            assertEquals(configuredNvMBlockDescriptor.nvMInitBlockCallback, block.value.nvMInitBlockCallback)
            assertEquals(configuredNvMBlockDescriptor.nvMSingleBlockCallback, block.value.nvMSingleBlockCallback)
            assertEquals(configuredNvMBlockDescriptor.nvMRamBlockDataAddress, block.value.nvMRamBlockDataAddress)
            assertEquals(configuredNvMBlockDescriptor.nvMUseInitCallback, block.value.nvMUseInitCallback)
            assertEquals(configuredNvMBlockDescriptor.nvMUseJobEndCallback, block.value.nvMUseJobEndCallback)
            assertEquals(
                configuredNvMBlockDescriptor.nvMSelectBlockForReadAll,
                block.value.nvMSelectBlockForReadAll
            )
            assertEquals(
                configuredNvMBlockDescriptor.nvMSelectBlockForWriteAll,
                block.value.nvMSelectBlockForWriteAll
            )
            assertEquals(
                configuredNvMBlockDescriptor.nvMResistantToChangedSoftware,
                block.value.nvMResistantToChangedSoftware
            )
            assertEquals(configuredNvMBlockDescriptor.nvMBlockUseCrc, block.value.nvMBlockUseCrc)
            assertEquals(configuredNvMBlockDescriptor.nvMBlockCrcType, block.value.nvMBlockCrcType)
            assertEquals(configuredNvMBlockDescriptor.nvMBlockManagementType, block.value.nvMBlockManagementType)
        }
    }

    /**
     * Tests if the DcmDslBuffer is created with the correct default buffer size.
     * Tests if the maximum response size of the dcm dsl protocol row is equal to the default buffer size.
     */
    @Test
    fun testCreateBuffer() {
        val defaultBufferSizeList = mutableListOf(0, 1, 13, 300, 1020, 4096)
        val dcm = Dcm()
        dcm.dcmDslBufferList = mutableListOf()
        val dcmDslProtocolRow = DcmDslProtocolRow("DcmDslProtocolRow")

        for (defaultBufferSize in defaultBufferSizeList) {
            configureDiagnosticsService.createBuffer(dcm, dcmDslProtocolRow, defaultBufferSize)
            assertEquals(dcmDslProtocolRow.dcmDslProtocolMaximumResponseSize, defaultBufferSize)
            assertEquals(dcm.dcmDslBufferList.first().dcmDslBufferSize, defaultBufferSize)
        }
    }

    /**
     * Tests if the max number of event entry primary objects is calculated correctly.
     */
    @Test
    fun testCalculateMaxNumberOfEventEntryPrimary() {
        val dem = Dem()
        dem.demDTCClass = mutableListOf()
        dem.demPrimaryMemory = DemPrimaryMemory("DemPrimaryMemory")
        val diagMemBlocksMinList = mutableListOf(0, 1, 125, 254, 255)
        val diagMemBlocksMaxList = mutableListOf(255, 254, 125, 1, 0)
        val diagMemBlocksScalingList = mutableListOf(0.1f, 0.2f, 2f, 5f, 20f, 255f)

        dem.demDTCClass =
            mutableListOf(DemDTCClass("DtcClass0"), DemDTCClass("DtcClass1"), DemDTCClass("DtcClass2"))

        for (diagMemBlocksMin in diagMemBlocksMinList) {
            for (diagMemBlocksMax in diagMemBlocksMaxList) {
                for (diagMemBlockScaling in diagMemBlocksScalingList) {
                    val mockModel = mockk<DiagnosticsModel>()
                    every { mockModel.DiagMemBlocksMin } returns diagMemBlocksMin
                    every { mockModel.DiagMemBlocksMax } returns diagMemBlocksMax
                    every { mockModel.diagMemBlocksScaling } returns diagMemBlockScaling

                    val result = configureDiagnosticsService.calculateMaxNumberOfEventEntryPrimary(mockModel, dem)
                    var counter = dem.demDTCClass.count() * diagMemBlockScaling
                    if (counter < mockModel.DiagMemBlocksMin!!) {
                        counter = diagMemBlocksMin.toFloat()
                    } else if (counter > mockModel.DiagMemBlocksMax!!) {
                        counter = diagMemBlocksMax.toFloat()
                    }

                    assertEquals(result, counter)
                }
            }
        }
    }
}
