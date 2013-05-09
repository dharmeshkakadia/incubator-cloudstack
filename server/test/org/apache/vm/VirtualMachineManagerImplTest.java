// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.vm;


import org.apache.agent.AgentManager;
import org.apache.agent.api.Answer;
import org.apache.agent.api.CheckVirtualMachineAnswer;
import org.apache.agent.api.CheckVirtualMachineCommand;
import org.apache.agent.api.MigrateWithStorageAnswer;
import org.apache.agent.api.MigrateWithStorageCommand;
import org.apache.agent.api.MigrateWithStorageCompleteAnswer;
import org.apache.agent.api.MigrateWithStorageCompleteCommand;
import org.apache.agent.api.MigrateWithStorageReceiveAnswer;
import org.apache.agent.api.MigrateWithStorageReceiveCommand;
import org.apache.agent.api.MigrateWithStorageSendAnswer;
import org.apache.agent.api.MigrateWithStorageSendCommand;
import org.apache.agent.api.PrepareForMigrationAnswer;
import org.apache.agent.api.PrepareForMigrationCommand;
import org.apache.agent.api.ScaleVmAnswer;
import org.apache.agent.api.ScaleVmCommand;
import org.apache.capacity.CapacityManager;
import org.apache.cloudstack.api.command.user.vm.RestoreVMCmd;
import org.apache.cloudstack.api.command.user.vm.ScaleVMCmd;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.configuration.ConfigurationManager;
import org.apache.configuration.dao.ConfigurationDao;
import org.apache.dc.dao.ClusterDao;
import org.apache.dc.dao.DataCenterDao;
import org.apache.dc.dao.HostPodDao;
import org.apache.deploy.DeployDestination;
import org.apache.exception.ConcurrentOperationException;
import org.apache.exception.ManagementServerException;
import org.apache.exception.OperationTimedoutException;
import org.apache.exception.ResourceUnavailableException;
import org.apache.exception.VirtualMachineMigrationException;
import org.apache.host.HostVO;
import org.apache.host.dao.HostDao;
import org.apache.hypervisor.HypervisorGuru;
import org.apache.hypervisor.HypervisorGuruManager;
import org.apache.hypervisor.Hypervisor.HypervisorType;
import org.apache.network.NetworkManager;
import org.apache.service.ServiceOfferingVO;
import org.apache.storage.DiskOfferingVO;
import org.apache.storage.StoragePoolHostVO;
import org.apache.storage.VMTemplateVO;
import org.apache.storage.VolumeManager;
import org.apache.storage.VolumeVO;
import org.apache.storage.dao.DiskOfferingDao;
import org.apache.storage.dao.StoragePoolHostDao;
import org.apache.storage.dao.VMTemplateDao;
import org.apache.storage.dao.VolumeDao;
import org.apache.user.*;
import org.apache.user.dao.AccountDao;
import org.apache.user.dao.UserDao;
import org.apache.utils.Pair;
import org.apache.utils.exception.CloudRuntimeException;
import org.apache.vm.ItWorkDao;
import org.apache.vm.ItWorkVO;
import org.apache.vm.UserVmManagerImpl;
import org.apache.vm.UserVmVO;
import org.apache.vm.VMInstanceVO;
import org.apache.vm.VirtualMachine;
import org.apache.vm.VirtualMachineGuru;
import org.apache.vm.VirtualMachineManagerImpl;
import org.apache.vm.VirtualMachine.Event;
import org.apache.vm.VirtualMachine.State;
import org.apache.vm.dao.UserVmDao;
import org.apache.vm.dao.VMInstanceDao;
import org.apache.vm.snapshot.VMSnapshotManager;

import org.junit.Test;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public class VirtualMachineManagerImplTest {

        @Spy VirtualMachineManagerImpl _vmMgr = new VirtualMachineManagerImpl();
        @Mock
        VolumeManager _storageMgr;
        @Mock
        Account _account;
        @Mock
        AccountManager _accountMgr;
        @Mock
        ConfigurationManager _configMgr;
        @Mock
        CapacityManager _capacityMgr;
        @Mock
        AgentManager _agentMgr;
        @Mock
        AccountDao _accountDao;
        @Mock
        ConfigurationDao _configDao;
        @Mock
        HostDao _hostDao;
        @Mock
        UserDao _userDao;
        @Mock
        UserVmDao _vmDao;
        @Mock
        ItWorkDao _workDao;
        @Mock
        VMInstanceDao _vmInstanceDao;
        @Mock
        VMTemplateDao _templateDao;
        @Mock
        VolumeDao _volsDao;
        @Mock
        RestoreVMCmd _restoreVMCmd;
        @Mock
        AccountVO _accountMock;
        @Mock
        UserVO _userMock;
        @Mock
        UserVmVO _vmMock;
        @Mock
        VMInstanceVO _vmInstance;
        @Mock
        HostVO _host;
        @Mock
        VMTemplateVO _templateMock;
        @Mock
        VolumeVO _volumeMock;
        @Mock
        List<VolumeVO> _rootVols;
        @Mock
        ItWorkVO _work;

        @Mock ClusterDao _clusterDao;
        @Mock HostPodDao _podDao;
        @Mock DataCenterDao _dcDao;
        @Mock DiskOfferingDao _diskOfferingDao;
        @Mock PrimaryDataStoreDao _storagePoolDao;
        @Mock StoragePoolHostDao _poolHostDao;
        @Mock NetworkManager _networkMgr;
        @Mock HypervisorGuruManager _hvGuruMgr;
        @Mock VMSnapshotManager _vmSnapshotMgr;

        // Mock objects for vm migration with storage test.
        @Mock DiskOfferingVO _diskOfferingMock;
        @Mock StoragePoolVO _srcStoragePoolMock;
        @Mock StoragePoolVO _destStoragePoolMock;
        @Mock HostVO _srcHostMock;
        @Mock HostVO _destHostMock;
        @Mock Map<VolumeVO, StoragePoolVO> _volumeToPoolMock;

        @Before
        public void setup(){
            MockitoAnnotations.initMocks(this);

            _vmMgr._templateDao = _templateDao;
            _vmMgr._volsDao = _volsDao;
            _vmMgr.volumeMgr = _storageMgr;
            _vmMgr._accountDao = _accountDao;
            _vmMgr._userDao = _userDao;
            _vmMgr._accountMgr = _accountMgr;
            _vmMgr._configMgr = _configMgr;
            _vmMgr._capacityMgr = _capacityMgr;
            _vmMgr._hostDao = _hostDao;
            _vmMgr._nodeId = 1L;
            _vmMgr._workDao = _workDao;
            _vmMgr._agentMgr = _agentMgr;
            _vmMgr._podDao = _podDao;
            _vmMgr._clusterDao = _clusterDao;
            _vmMgr._dcDao = _dcDao;
            _vmMgr._diskOfferingDao = _diskOfferingDao;
            _vmMgr._storagePoolDao = _storagePoolDao;
            _vmMgr._poolHostDao= _poolHostDao;
            _vmMgr._networkMgr = _networkMgr;
            _vmMgr._hvGuruMgr = _hvGuruMgr;
            _vmMgr._vmSnapshotMgr = _vmSnapshotMgr;
            _vmMgr._vmDao = _vmInstanceDao;

            when(_vmMock.getId()).thenReturn(314l);
            when(_vmInstance.getId()).thenReturn(1L);
            when(_vmInstance.getServiceOfferingId()).thenReturn(2L);
            when(_vmInstance.getInstanceName()).thenReturn("myVm");
            when(_vmInstance.getHostId()).thenReturn(2L);
            when(_vmInstance.getType()).thenReturn(VirtualMachine.Type.User);
            when(_host.getId()).thenReturn(1L);
            when(_hostDao.findById(anyLong())).thenReturn(null);
            when(_configMgr.getServiceOffering(anyLong())).thenReturn(getSvcoffering(512));
            when(_workDao.persist(_work)).thenReturn(_work);
            when(_workDao.update("1", _work)).thenReturn(true);
            when(_work.getId()).thenReturn("1");
            doNothing().when(_work).setStep(ItWorkVO.Step.Done);
            //doNothing().when(_volsDao).detachVolume(anyLong());
            //when(_work.setStep(ItWorkVO.Step.Done)).thenReturn("1");

        }


    @Test(expected=CloudRuntimeException.class)
    public void testScaleVM1()  throws Exception {


        DeployDestination dest = new DeployDestination(null, null, null, _host);
        long l = 1L;

        when(_vmInstanceDao.findById(anyLong())).thenReturn(_vmInstance);
        _vmMgr.migrateForScale(_vmInstance, l, dest, l);

    }

    @Test (expected=CloudRuntimeException.class)
    public void testScaleVM2()  throws Exception {

        DeployDestination dest = new DeployDestination(null, null, null, _host);
        long l = 1L;

        when(_vmInstanceDao.findById(anyLong())).thenReturn(_vmInstance);
        ServiceOfferingVO newServiceOffering = getSvcoffering(512);
        ScaleVmCommand reconfigureCmd = new ScaleVmCommand("myVmName", newServiceOffering.getCpu(),
                newServiceOffering.getSpeed(), newServiceOffering.getRamSize(), newServiceOffering.getRamSize(), newServiceOffering.getLimitCpuUse());
        Answer answer = new ScaleVmAnswer(reconfigureCmd, true, "details");
        when(_agentMgr.send(2l, reconfigureCmd)).thenReturn(null);
        _vmMgr.reConfigureVm(_vmInstance, getSvcoffering(256), false);

    }

    @Test (expected=CloudRuntimeException.class)
    public void testScaleVM3()  throws Exception {

        /*VirtualMachineProfile<VMInstanceVO> profile = new VirtualMachineProfileImpl<VMInstanceVO>(vm);

        Long srcHostId = vm.getHostId();
        Long oldSvcOfferingId = vm.getServiceOfferingId();
        if (srcHostId == null) {
            throw new CloudRuntimeException("Unable to scale the vm because it doesn't have a host id");
        }*/

        when(_vmInstance.getHostId()).thenReturn(null);
        when(_vmInstanceDao.findById(anyLong())).thenReturn(_vmInstance);
        _vmMgr.findHostAndMigrate(VirtualMachine.Type.User, _vmInstance, 2l);

    }


    private ServiceOfferingVO getSvcoffering(int ramSize){

        long id  = 4L;
        String name = "name";
        String displayText = "displayText";
        int cpu = 1;
        //int ramSize = 256;
        int speed = 128;

        boolean ha = false;
        boolean useLocalStorage = false;

        ServiceOfferingVO serviceOffering = new ServiceOfferingVO(name, cpu, ramSize, speed, null, null, ha, displayText, useLocalStorage, false, null, false, null, false);
        return serviceOffering;
    }

    private void initializeMockConfigForMigratingVmWithVolumes() throws OperationTimedoutException,
        ResourceUnavailableException {

        // Mock the source and destination hosts.
        when(_srcHostMock.getId()).thenReturn(5L);
        when(_destHostMock.getId()).thenReturn(6L);
        when(_hostDao.findById(5L)).thenReturn(_srcHostMock);
        when(_hostDao.findById(6L)).thenReturn(_destHostMock);

        // Mock the vm being migrated.
        when(_vmMock.getId()).thenReturn(1L);
        when(_vmMock.getHypervisorType()).thenReturn(HypervisorType.XenServer);
        when(_vmMock.getState()).thenReturn(State.Running).thenReturn(State.Running).thenReturn(State.Migrating)
            .thenReturn(State.Migrating);
        when(_vmMock.getHostId()).thenReturn(5L);
        when(_vmInstance.getId()).thenReturn(1L);
        when(_vmInstance.getServiceOfferingId()).thenReturn(2L);
        when(_vmInstance.getInstanceName()).thenReturn("myVm");
        when(_vmInstance.getHostId()).thenReturn(5L);
        when(_vmInstance.getType()).thenReturn(VirtualMachine.Type.User);
        when(_vmInstance.getState()).thenReturn(State.Running).thenReturn(State.Running).thenReturn(State.Migrating)
            .thenReturn(State.Migrating);

        // Mock the work item.
        when(_workDao.persist(any(ItWorkVO.class))).thenReturn(_work);
        when(_workDao.update("1", _work)).thenReturn(true);
        when(_work.getId()).thenReturn("1");
        doNothing().when(_work).setStep(ItWorkVO.Step.Done);

        // Mock the vm guru and the user vm object that gets returned.
        _vmMgr._vmGurus = new HashMap<VirtualMachine.Type, VirtualMachineGuru<? extends VMInstanceVO>>();
        UserVmManagerImpl userVmManager = mock(UserVmManagerImpl.class);
        _vmMgr.registerGuru(VirtualMachine.Type.User, userVmManager);
        when(userVmManager.findById(anyLong())).thenReturn(_vmMock);

        // Mock the iteration over all the volumes of an instance.
        Iterator<VolumeVO> volumeIterator = mock(Iterator.class);
        when(_volsDao.findUsableVolumesForInstance(anyLong())).thenReturn(_rootVols);
        when(_rootVols.iterator()).thenReturn(volumeIterator);
        when(volumeIterator.hasNext()).thenReturn(true, false);
        when(volumeIterator.next()).thenReturn(_volumeMock);

        // Mock the disk offering and pool objects for a volume.
        when(_volumeMock.getDiskOfferingId()).thenReturn(5L);
        when(_volumeMock.getPoolId()).thenReturn(200L);
        when(_diskOfferingDao.findById(anyLong())).thenReturn(_diskOfferingMock);
        when(_storagePoolDao.findById(anyLong())).thenReturn(_srcStoragePoolMock);

        // Mock the volume to pool mapping.
        when(_volumeToPoolMock.get(_volumeMock)).thenReturn(_destStoragePoolMock);
        when(_destStoragePoolMock.getId()).thenReturn(201L);
        when(_srcStoragePoolMock.getId()).thenReturn(200L);
        when(_destStoragePoolMock.isLocal()).thenReturn(false);
        when(_diskOfferingMock.getUseLocalStorage()).thenReturn(false);
        when(_poolHostDao.findByPoolHost(anyLong(), anyLong())).thenReturn(mock(StoragePoolHostVO.class));

        // Mock hypervisor guru.
        HypervisorGuru guruMock = mock(HypervisorGuru.class);
        when(_hvGuruMgr.getGuru(HypervisorType.XenServer)).thenReturn(guruMock);

        when(_srcHostMock.getClusterId()).thenReturn(3L);
        when(_destHostMock.getClusterId()).thenReturn(3L);

        // Mock the commands and answers to the agent.
        PrepareForMigrationAnswer prepAnswerMock = mock(PrepareForMigrationAnswer.class);
        when(prepAnswerMock.getResult()).thenReturn(true);
        when(_agentMgr.send(anyLong(), isA(PrepareForMigrationCommand.class))).thenReturn(prepAnswerMock);

        MigrateWithStorageAnswer migAnswerMock = mock(MigrateWithStorageAnswer.class);
        when(migAnswerMock.getResult()).thenReturn(true);
        when(_agentMgr.send(anyLong(), isA(MigrateWithStorageCommand.class))).thenReturn(migAnswerMock);

        MigrateWithStorageReceiveAnswer migRecAnswerMock = mock(MigrateWithStorageReceiveAnswer.class);
        when(migRecAnswerMock.getResult()).thenReturn(true);
        when(_agentMgr.send(anyLong(), isA(MigrateWithStorageReceiveCommand.class))).thenReturn(migRecAnswerMock);

        MigrateWithStorageSendAnswer migSendAnswerMock = mock(MigrateWithStorageSendAnswer.class);
        when(migSendAnswerMock.getResult()).thenReturn(true);
        when(_agentMgr.send(anyLong(), isA(MigrateWithStorageSendCommand.class))).thenReturn(migSendAnswerMock);

        MigrateWithStorageCompleteAnswer migCompleteAnswerMock = mock(MigrateWithStorageCompleteAnswer.class);
        when(migCompleteAnswerMock.getResult()).thenReturn(true);
        when(_agentMgr.send(anyLong(), isA(MigrateWithStorageCompleteCommand.class))).thenReturn(migCompleteAnswerMock);

        CheckVirtualMachineAnswer checkVmAnswerMock = mock(CheckVirtualMachineAnswer.class);
        when(checkVmAnswerMock.getResult()).thenReturn(true);
        when(checkVmAnswerMock.getState()).thenReturn(State.Running);
        when(_agentMgr.send(anyLong(), isA(CheckVirtualMachineCommand.class))).thenReturn(checkVmAnswerMock);

        // Mock the state transitions of vm.
        Pair<Long, Long> opaqueMock = new Pair<Long, Long> (_vmMock.getHostId(), _destHostMock.getId());
        when(_vmSnapshotMgr.hasActiveVMSnapshotTasks(anyLong())).thenReturn(false);
        when(_vmInstanceDao.updateState(State.Running, Event.MigrationRequested, State.Migrating, _vmMock, opaqueMock))
            .thenReturn(true);
        when(_vmInstanceDao.updateState(State.Migrating, Event.OperationSucceeded, State.Running, _vmMock, opaqueMock))
            .thenReturn(true);
    }

    // Check migration of a vm with its volumes within a cluster.
    @Test
    public void testMigrateWithVolumeWithinCluster() throws ResourceUnavailableException, ConcurrentOperationException,
        ManagementServerException, VirtualMachineMigrationException, OperationTimedoutException {

        initializeMockConfigForMigratingVmWithVolumes();
        when(_srcHostMock.getClusterId()).thenReturn(3L);
        when(_destHostMock.getClusterId()).thenReturn(3L);

        _vmMgr.migrateWithStorage(_vmInstance, _srcHostMock.getId(), _destHostMock.getId(), _volumeToPoolMock);
    }

    // Check migration of a vm with its volumes across a cluster.
    @Test
    public void testMigrateWithVolumeAcrossCluster() throws ResourceUnavailableException, ConcurrentOperationException,
        ManagementServerException, VirtualMachineMigrationException, OperationTimedoutException {

        initializeMockConfigForMigratingVmWithVolumes();
        when(_srcHostMock.getClusterId()).thenReturn(3L);
        when(_destHostMock.getClusterId()).thenReturn(4L);

        _vmMgr.migrateWithStorage(_vmInstance, _srcHostMock.getId(), _destHostMock.getId(), _volumeToPoolMock);
    }

    // Check migration of a vm fails when src and destination pool are not of same type; that is, one is shared and
    // other is local.
    @Test(expected=CloudRuntimeException.class)
    public void testMigrateWithVolumeFail1() throws ResourceUnavailableException, ConcurrentOperationException,
        ManagementServerException, VirtualMachineMigrationException, OperationTimedoutException {

        initializeMockConfigForMigratingVmWithVolumes();
        when(_srcHostMock.getClusterId()).thenReturn(3L);
        when(_destHostMock.getClusterId()).thenReturn(3L);

        when(_destStoragePoolMock.isLocal()).thenReturn(true);
        when(_diskOfferingMock.getUseLocalStorage()).thenReturn(false);

        _vmMgr.migrateWithStorage(_vmInstance, _srcHostMock.getId(), _destHostMock.getId(), _volumeToPoolMock);
    }

    // Check migration of a vm fails when vm is not in Running state.
    @Test(expected=ConcurrentOperationException.class)
    public void testMigrateWithVolumeFail2() throws ResourceUnavailableException, ConcurrentOperationException,
        ManagementServerException, VirtualMachineMigrationException, OperationTimedoutException {

        initializeMockConfigForMigratingVmWithVolumes();
        when(_srcHostMock.getClusterId()).thenReturn(3L);
        when(_destHostMock.getClusterId()).thenReturn(3L);

        when(_vmMock.getState()).thenReturn(State.Stopped);

        _vmMgr.migrateWithStorage(_vmInstance, _srcHostMock.getId(), _destHostMock.getId(), _volumeToPoolMock);
    }
}
