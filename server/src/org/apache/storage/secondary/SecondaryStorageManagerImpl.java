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
package org.apache.storage.secondary;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.agent.AgentManager;
import org.apache.agent.api.Answer;
import org.apache.agent.api.Command;
import org.apache.agent.api.RebootCommand;
import org.apache.agent.api.SecStorageFirewallCfgCommand;
import org.apache.agent.api.SecStorageSetupAnswer;
import org.apache.agent.api.SecStorageSetupCommand;
import org.apache.agent.api.SecStorageVMSetupCommand;
import org.apache.agent.api.StartupCommand;
import org.apache.agent.api.StartupSecondaryStorageCommand;
import org.apache.agent.api.StartupStorageCommand;
import org.apache.agent.api.StopAnswer;
import org.apache.agent.api.SecStorageSetupCommand.Certificates;
import org.apache.agent.api.check.CheckSshAnswer;
import org.apache.agent.api.check.CheckSshCommand;
import org.apache.agent.api.to.NicTO;
import org.apache.agent.api.to.VirtualMachineTO;
import org.apache.agent.manager.Commands;
import org.apache.capacity.dao.CapacityDao;
import org.apache.cluster.ClusterManager;
import org.apache.cluster.ManagementServerNode;
import org.apache.configuration.Config;
import org.apache.configuration.ZoneConfig;
import org.apache.configuration.dao.ConfigurationDao;
import org.apache.consoleproxy.ConsoleProxyManager;
import org.apache.dc.DataCenter;
import org.apache.dc.DataCenterVO;
import org.apache.dc.DataCenter.NetworkType;
import org.apache.dc.dao.DataCenterDao;
import org.apache.deploy.DataCenterDeployment;
import org.apache.deploy.DeployDestination;
import org.apache.exception.ConcurrentOperationException;
import org.apache.exception.InsufficientCapacityException;
import org.apache.exception.ResourceUnavailableException;
import org.apache.exception.StorageUnavailableException;
import org.apache.host.Host;
import org.apache.host.HostVO;
import org.apache.host.dao.HostDao;
import org.apache.hypervisor.Hypervisor.HypervisorType;
import org.apache.info.RunningHostCountInfo;
import org.apache.info.RunningHostInfoAgregator;
import org.apache.info.RunningHostInfoAgregator.ZoneHostInfo;
import org.apache.keystore.KeystoreManager;
import org.apache.log4j.Logger;
import org.apache.network.Network;
import org.apache.network.NetworkManager;
import org.apache.network.NetworkModel;
import org.apache.network.Networks.TrafficType;
import org.apache.network.dao.IPAddressDao;
import org.apache.network.dao.IPAddressVO;
import org.apache.network.dao.NetworkDao;
import org.apache.network.dao.NetworkVO;
import org.apache.network.rules.RulesManager;
import org.apache.offering.NetworkOffering;
import org.apache.offering.ServiceOffering;
import org.apache.offerings.NetworkOfferingVO;
import org.apache.offerings.dao.NetworkOfferingDao;
import org.apache.resource.ResourceManager;
import org.apache.resource.ResourceStateAdapter;
import org.apache.resource.ServerResource;
import org.apache.resource.UnableDeleteHostException;
import org.apache.service.ServiceOfferingVO;
import org.apache.service.dao.ServiceOfferingDao;
import org.apache.storage.SnapshotVO;
import org.apache.storage.Storage;
import org.apache.storage.VMTemplateHostVO;
import org.apache.storage.VMTemplateVO;
import org.apache.storage.VMTemplateStorageResourceAssoc.Status;
import org.apache.storage.dao.SnapshotDao;
import org.apache.storage.dao.StoragePoolHostDao;
import org.apache.storage.dao.VMTemplateDao;
import org.apache.storage.dao.VMTemplateHostDao;
import org.apache.storage.resource.DummySecondaryStorageResource;
import org.apache.storage.swift.SwiftManager;
import org.apache.storage.template.TemplateConstants;
import org.apache.user.Account;
import org.apache.user.AccountService;
import org.apache.user.User;
import org.apache.user.UserContext;
import org.apache.utils.DateUtil;
import org.apache.utils.NumbersUtil;
import org.apache.utils.Pair;
import org.apache.utils.component.ManagerBase;
import org.apache.utils.db.GlobalLock;
import org.apache.utils.db.SearchCriteria2;
import org.apache.utils.db.SearchCriteriaService;
import org.apache.utils.db.SearchCriteria.Op;
import org.apache.utils.events.SubscriptionMgr;
import org.apache.utils.exception.CloudRuntimeException;
import org.apache.utils.net.NetUtils;
import org.apache.vm.Nic;
import org.apache.vm.NicProfile;
import org.apache.vm.ReservationContext;
import org.apache.vm.SecondaryStorageVm;
import org.apache.vm.SecondaryStorageVmVO;
import org.apache.vm.SystemVmLoadScanHandler;
import org.apache.vm.SystemVmLoadScanner;
import org.apache.vm.VirtualMachine;
import org.apache.vm.VirtualMachineGuru;
import org.apache.vm.VirtualMachineManager;
import org.apache.vm.VirtualMachineName;
import org.apache.vm.VirtualMachineProfile;
import org.apache.vm.SystemVmLoadScanner.AfterScanAction;
import org.apache.vm.VirtualMachine.State;
import org.apache.vm.dao.SecondaryStorageVmDao;
import org.apache.vm.dao.UserVmDetailsDao;
import org.apache.vm.dao.VMInstanceDao;


//
// Possible secondary storage vm state transition cases
//		Creating -> Destroyed
//		Creating -> Stopped --> Starting -> Running
//		HA -> Stopped -> Starting -> Running
//		Migrating -> Running	(if previous state is Running before it enters into Migrating state
//		Migrating -> Stopped	(if previous state is not Running before it enters into Migrating state)
//		Running -> HA			(if agent lost connection)
//		Stopped -> Destroyed
//
//		Creating state indicates of record creating and IP address allocation are ready, it is a transient
// 		state which will soon be switching towards Running if everything goes well.
//		Stopped state indicates the readiness of being able to start (has storage and IP resources allocated)
//		Starting state can only be entered from Stopped states
//
// Starting, HA, Migrating, Creating and Running state are all counted as "Open" for available capacity calculation
// because sooner or later, it will be driven into Running state
//
@Local(value = { SecondaryStorageVmManager.class })
public class SecondaryStorageManagerImpl extends ManagerBase implements SecondaryStorageVmManager, VirtualMachineGuru<SecondaryStorageVmVO>, SystemVmLoadScanHandler<Long>, ResourceStateAdapter {
    private static final Logger s_logger = Logger.getLogger(SecondaryStorageManagerImpl.class);

    private static final int DEFAULT_CAPACITY_SCAN_INTERVAL = 30000; // 30
    // seconds
    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC = 180; // 3
    // minutes

    private static final int STARTUP_DELAY = 60000; // 60 seconds

    private String _mgmt_host;
    private int _mgmt_port = 8250;

    @Inject
    private List<SecondaryStorageVmAllocator> _ssVmAllocators;

    @Inject
    protected SecondaryStorageVmDao _secStorageVmDao;
    @Inject
    private DataCenterDao _dcDao;
    @Inject
    private VMTemplateDao _templateDao;
    @Inject
    private HostDao _hostDao;
    @Inject
    private StoragePoolHostDao _storagePoolHostDao;

    @Inject
    private VMTemplateHostDao _vmTemplateHostDao;

    @Inject
    private AgentManager _agentMgr;
    @Inject
    protected SwiftManager _swiftMgr;
    @Inject
    protected NetworkManager _networkMgr;
    @Inject
    protected NetworkModel _networkModel;
    @Inject
    protected SnapshotDao _snapshotDao;
    
    @Inject
    private ClusterManager _clusterMgr;

    private SecondaryStorageListener _listener;

    private ServiceOfferingVO _serviceOffering;

    @Inject
    protected ConfigurationDao _configDao;
    @Inject
    private ServiceOfferingDao _offeringDao;
    @Inject
    private AccountService _accountMgr;
    @Inject
    private VirtualMachineManager _itMgr;
    @Inject
    protected VMInstanceDao                       _vmDao;
    @Inject
    protected CapacityDao                         _capacityDao;
    @Inject
    UserVmDetailsDao _vmDetailsDao;
    @Inject 
    protected ResourceManager _resourceMgr;
    //@Inject			// TODO this is a very strange usage, a singleton class need to inject itself?
    protected SecondaryStorageVmManager _ssvmMgr;
    @Inject
    NetworkDao _networkDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    protected IPAddressDao _ipAddressDao = null;
    @Inject
    protected RulesManager _rulesMgr;
    
    @Inject
    KeystoreManager _keystoreMgr;
    private long _capacityScanInterval = DEFAULT_CAPACITY_SCAN_INTERVAL;
    private int _secStorageVmMtuSize;

    private String _instance;
    private boolean _useLocalStorage;
    private boolean _useSSlCopy;
    private String _httpProxy;
    private String _allowedInternalSites;
    protected long                                _nodeId                              = ManagementServerNode.getManagementServerId();

    private SystemVmLoadScanner<Long> _loadScanner;
    private Map<Long, ZoneHostInfo> _zoneHostInfoMap; // map <zone id, info about running host in zone>

    private final GlobalLock _allocLock = GlobalLock.getInternLock(getAllocLockName());

    public SecondaryStorageManagerImpl() {
    	_ssvmMgr = this;
    }
    
    @Override
    public SecondaryStorageVmVO startSecStorageVm(long secStorageVmId) {
        try {
            SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(secStorageVmId);
            Account systemAcct = _accountMgr.getSystemAccount();
            User systemUser = _accountMgr.getSystemUser();
            return _itMgr.start(secStorageVm, null, systemUser, systemAcct);
        } catch (StorageUnavailableException e) {
            s_logger.warn("Exception while trying to start secondary storage vm", e);
            return null;
        } catch (InsufficientCapacityException e) {
            s_logger.warn("Exception while trying to start secondary storage vm", e);
            return null;
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Exception while trying to start secondary storage vm", e);
            return null;
        } catch (Exception e) {
            s_logger.warn("Exception while trying to start secondary storage vm", e);
            return null;
        }
    }

    SecondaryStorageVmVO getSSVMfromHost(HostVO ssAHost) {
        if( ssAHost.getType() == Host.Type.SecondaryStorageVM ) {
            return _secStorageVmDao.findByInstanceName(ssAHost.getName());
        }
        return null;
    }
    
    @Override
    public boolean generateSetupCommand(Long ssHostId) {
        HostVO cssHost = _hostDao.findById(ssHostId);
        Long zoneId = cssHost.getDataCenterId();
        if( cssHost.getType() == Host.Type.SecondaryStorageVM ) {
    
            SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findByInstanceName(cssHost.getName());
            if (secStorageVm == null) {
                s_logger.warn("secondary storage VM " + cssHost.getName() + " doesn't exist");
                return false;
            }

            List<HostVO> ssHosts = _ssvmMgr.listSecondaryStorageHostsInOneZone(zoneId);
            for( HostVO ssHost : ssHosts ) {
                String secUrl = ssHost.getStorageUrl();
                SecStorageSetupCommand setupCmd = null;
                if (!_useSSlCopy) {
                	setupCmd = new SecStorageSetupCommand(secUrl, null);
                } else {
                	Certificates certs = _keystoreMgr.getCertificates(ConsoleProxyManager.CERTIFICATE_NAME);
                	setupCmd = new SecStorageSetupCommand(secUrl, certs);
                }
                
                Answer answer = _agentMgr.easySend(ssHostId, setupCmd);
                if (answer != null && answer.getResult()) {
                    SecStorageSetupAnswer an = (SecStorageSetupAnswer) answer;
                    ssHost.setParent(an.get_dir());
                    _hostDao.update(ssHost.getId(), ssHost);
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Successfully programmed secondary storage " + ssHost.getName() + " in secondary storage VM " + secStorageVm.getInstanceName());
                    }
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Successfully programmed secondary storage " + ssHost.getName() + " in secondary storage VM " + secStorageVm.getInstanceName());
                    }
                    return false;
                }
            }
        } else if( cssHost.getType() == Host.Type.SecondaryStorage ) {
            List<SecondaryStorageVmVO> alreadyRunning = _secStorageVmDao.getSecStorageVmListInStates(SecondaryStorageVm.Role.templateProcessor, zoneId, State.Running);
            String secUrl = cssHost.getStorageUrl();
            SecStorageSetupCommand setupCmd = new SecStorageSetupCommand(secUrl, null);
            for ( SecondaryStorageVmVO ssVm : alreadyRunning ) {
                HostVO host = _resourceMgr.findHostByName(ssVm.getInstanceName());
                Answer answer = _agentMgr.easySend(host.getId(), setupCmd);
                if (answer != null && answer.getResult()) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Successfully programmed secondary storage " + host.getName() + " in secondary storage VM " + ssVm.getInstanceName());
                    }
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Successfully programmed secondary storage " + host.getName() + " in secondary storage VM " + ssVm.getInstanceName());
                    }
                    return false;
                }               
            }
        }
        return true;
    }
    
    
    @Override 
    public boolean deleteHost(Long hostId) {
        List<SnapshotVO> snapshots = _snapshotDao.listByHostId(hostId);
        if( snapshots != null && !snapshots.isEmpty()) {
            throw new CloudRuntimeException("Can not delete this secondary storage since it contains atleast one or more snapshots ");
        }
        if (!_swiftMgr.isSwiftEnabled()) {
            List<Long> list = _templateDao.listPrivateTemplatesByHost(hostId);
            if (list != null && !list.isEmpty()) {
                throw new CloudRuntimeException("Can not delete this secondary storage since it contains private templates ");
            }
        }
        _vmTemplateHostDao.deleteByHost(hostId);
        HostVO host = _hostDao.findById(hostId);
        host.setGuid(null);
        _hostDao.update(hostId, host);
        _hostDao.remove(hostId);
        
        return true;
    }
    
    @Override
    public boolean generateVMSetupCommand(Long ssAHostId) {
        HostVO ssAHost = _hostDao.findById(ssAHostId);
        if( ssAHost.getType() != Host.Type.SecondaryStorageVM ) {
            return false;
        }
        SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findByInstanceName(ssAHost.getName());
        if (secStorageVm == null) {
            s_logger.warn("secondary storage VM " + ssAHost.getName() + " doesn't exist");
            return false;
        }
      
        SecStorageVMSetupCommand setupCmd = new SecStorageVMSetupCommand();
        if (_allowedInternalSites != null) {
            List<String> allowedCidrs = new ArrayList<String>();
            String[] cidrs = _allowedInternalSites.split(",");
            for (String cidr : cidrs) {
                if (NetUtils.isValidCIDR(cidr) || NetUtils.isValidIp(cidr) || !cidr.startsWith("0.0.0.0")) {
                    allowedCidrs.add(cidr);
                }
            }
            List<? extends Nic> nics = _networkModel.getNicsForTraffic(secStorageVm.getId(), TrafficType.Management);
            setupCmd.setAllowedInternalSites(allowedCidrs.toArray(new String[allowedCidrs.size()]));
        }
        String copyPasswd = _configDao.getValue("secstorage.copy.password");
        setupCmd.setCopyPassword(copyPasswd);
        setupCmd.setCopyUserName(TemplateConstants.DEFAULT_HTTP_AUTH_USER);
        Answer answer = _agentMgr.easySend(ssAHostId, setupCmd);
        if (answer != null && answer.getResult()) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Successfully programmed http auth into " + secStorageVm.getHostName());
            }
            return true;
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("failed to program http auth into secondary storage vm : " + secStorageVm.getHostName());
            }
            return false;
        }
    }

    @Override
    public Pair<HostVO, SecondaryStorageVmVO> assignSecStorageVm(long zoneId, Command cmd) {
        return null;
    }

    @Override
    public boolean generateFirewallConfiguration(Long ssAHostId) {
        if ( ssAHostId == null ) {
            return true;
        }
        HostVO ssAHost = _hostDao.findById(ssAHostId);
        SecondaryStorageVmVO thisSecStorageVm = _secStorageVmDao.findByInstanceName(ssAHost.getName());
        
        if (thisSecStorageVm == null) {
            s_logger.warn("secondary storage VM " + ssAHost.getName() + " doesn't exist");
            return false;
        }

        String copyPort = _useSSlCopy? "443" : Integer.toString(TemplateConstants.DEFAULT_TMPLT_COPY_PORT);
        SecStorageFirewallCfgCommand thiscpc = new SecStorageFirewallCfgCommand(true);
        thiscpc.addPortConfig(thisSecStorageVm.getPublicIpAddress(), copyPort, true, TemplateConstants.DEFAULT_TMPLT_COPY_INTF);
        
        SearchCriteriaService<HostVO, HostVO> sc = SearchCriteria2.create(HostVO.class);
        sc.addAnd(sc.getEntity().getType(), Op.EQ, Host.Type.SecondaryStorageVM);
        sc.addAnd(sc.getEntity().getStatus(), Op.IN, org.apache.host.Status.Up, org.apache.host.Status.Connecting);
        List<HostVO> ssvms = sc.list();
        for (HostVO ssvm : ssvms) {
        	if (ssvm.getId() == ssAHostId) {
        		continue;
        	}
            Answer answer = _agentMgr.easySend(ssvm.getId(), thiscpc);
            if (answer != null && answer.getResult()) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Successfully programmed firewall rules into SSVM " + ssvm.getName());
                }
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("failed to program firewall rules into secondary storage vm : " + ssvm.getName());
                }
                return false;
            }
        }
        
        SecStorageFirewallCfgCommand allSSVMIpList = new SecStorageFirewallCfgCommand(false);
        for (HostVO ssvm : ssvms) {
        	if (ssvm.getId() == ssAHostId) {
        		continue;
        	}
        	allSSVMIpList.addPortConfig(ssvm.getPublicIpAddress(), copyPort, true, TemplateConstants.DEFAULT_TMPLT_COPY_INTF);
        }
        
        Answer answer = _agentMgr.easySend(ssAHostId, allSSVMIpList);
        if (answer != null && answer.getResult()) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Successfully programmed firewall rules into " + thisSecStorageVm.getHostName());
            }
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("failed to program firewall rules into secondary storage vm : " + thisSecStorageVm.getHostName());
            }
            return false;
        }
        
        return true;

    }

    protected boolean isSecondaryStorageVmRequired(long dcId) {
        DataCenterVO dc = _dcDao.findById(dcId);
        _dcDao.loadDetails(dc);
        String ssvmReq = dc.getDetail(ZoneConfig.EnableSecStorageVm.key());
        if (ssvmReq != null) {
            return Boolean.parseBoolean(ssvmReq);
        }
        return true;
    }

    public SecondaryStorageVmVO startNew(long dataCenterId, SecondaryStorageVm.Role role) {

        if (!isSecondaryStorageVmRequired(dataCenterId)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Secondary storage vm not required in zone " + dataCenterId + " acc. to zone config");
            }
            return null;
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Assign secondary storage vm from a newly started instance for request from data center : " + dataCenterId);
        }

        Map<String, Object> context = createSecStorageVmInstance(dataCenterId, role);

        long secStorageVmId = (Long) context.get("secStorageVmId");
        if (secStorageVmId == 0) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Creating secondary storage vm instance failed, data center id : " + dataCenterId);
            }

            return null;
        }

        SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(secStorageVmId);
        // SecondaryStorageVmVO secStorageVm =
        // allocSecStorageVmStorage(dataCenterId, secStorageVmId);
        if (secStorageVm != null) {
            SubscriptionMgr.getInstance().notifySubscribers(ALERT_SUBJECT, this,
                    new SecStorageVmAlertEventArgs(SecStorageVmAlertEventArgs.SSVM_CREATED, dataCenterId, secStorageVmId, secStorageVm, null));
            return secStorageVm;
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to allocate secondary storage vm storage, remove the secondary storage vm record from DB, secondary storage vm id: " + secStorageVmId);
            }

            SubscriptionMgr.getInstance().notifySubscribers(ALERT_SUBJECT, this,
                    new SecStorageVmAlertEventArgs(SecStorageVmAlertEventArgs.SSVM_CREATE_FAILURE, dataCenterId, secStorageVmId, null, "Unable to allocate storage"));
        }
        return null;
    }

    protected Map<String, Object> createSecStorageVmInstance(long dataCenterId, SecondaryStorageVm.Role role) {
        HostVO secHost = findSecondaryStorageHost(dataCenterId);
        if (secHost == null) {
            String msg = "No secondary storage available in zone " + dataCenterId + ", cannot create secondary storage vm";
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }

        long id = _secStorageVmDao.getNextInSequence(Long.class, "id");
        String name = VirtualMachineName.getSystemVmName(id, _instance, "s").intern();
        Account systemAcct = _accountMgr.getSystemAccount();

        DataCenterDeployment plan = new DataCenterDeployment(dataCenterId);
        DataCenter dc = _dcDao.findById(plan.getDataCenterId());
        
        TrafficType defaultTrafficType = TrafficType.Public;
        if (dc.getNetworkType() == NetworkType.Basic || dc.isSecurityGroupEnabled()) {
        	defaultTrafficType = TrafficType.Guest;
        }
        
        List<NetworkVO> defaultNetworks = _networkDao.listByZoneAndTrafficType(dataCenterId, defaultTrafficType);
        
        //api should never allow this situation to happen
        if (defaultNetworks.size() != 1) {
        	throw new CloudRuntimeException("Found " + defaultNetworks.size() + " networks of type " + defaultTrafficType + " when expect to find 1");
        }
        
        NetworkVO defaultNetwork = defaultNetworks.get(0);

        List<? extends NetworkOffering> offerings = _networkModel.getSystemAccountNetworkOfferings(NetworkOfferingVO.SystemControlNetwork, NetworkOfferingVO.SystemManagementNetwork, NetworkOfferingVO.SystemStorageNetwork);
        List<Pair<NetworkVO, NicProfile>> networks = new ArrayList<Pair<NetworkVO, NicProfile>>(offerings.size() + 1);
        NicProfile defaultNic = new NicProfile();
        defaultNic.setDefaultNic(true);
        defaultNic.setDeviceId(2);
        try {
        	networks.add(new Pair<NetworkVO, NicProfile>(_networkMgr.setupNetwork(systemAcct, _networkOfferingDao.findById(defaultNetwork.getNetworkOfferingId()), plan, null, null, false).get(0), defaultNic));
            for (NetworkOffering offering : offerings) {
                networks.add(new Pair<NetworkVO, NicProfile>(_networkMgr.setupNetwork(systemAcct, offering, plan, null, null, false).get(0), null));
            }
        } catch (ConcurrentOperationException e) {
            s_logger.info("Unable to setup due to concurrent operation. " + e);
            return new HashMap<String, Object>();
        }

        HypervisorType hypeType = _resourceMgr.getAvailableHypervisor(dataCenterId);
        
        VMTemplateVO template = _templateDao.findSystemVMTemplate(dataCenterId, hypeType);
        if (template == null) {
            s_logger.debug("Can't find a template to start");
            throw new CloudRuntimeException("Insufficient capacity exception");
        }

        SecondaryStorageVmVO secStorageVm = new SecondaryStorageVmVO(id, _serviceOffering.getId(), name, template.getId(), template.getHypervisorType(), template.getGuestOSId(), dataCenterId,
                systemAcct.getDomainId(), systemAcct.getId(), role, _serviceOffering.getOfferHA());
        try {
            secStorageVm = _itMgr.allocate(secStorageVm, template, _serviceOffering, networks, plan, null, systemAcct);
        } catch (InsufficientCapacityException e) {
            s_logger.warn("InsufficientCapacity", e);
            throw new CloudRuntimeException("Insufficient capacity exception", e);
        }

        Map<String, Object> context = new HashMap<String, Object>();
        context.put("secStorageVmId", secStorageVm.getId());
        return context;
    }

    private SecondaryStorageVmAllocator getCurrentAllocator() {

        // for now, only one adapter is supported
    	if(_ssVmAllocators.size() > 0)
    		return _ssVmAllocators.get(0);
    	
        return null;
    }

    protected String connect(String ipAddress, int port) {
        return null;
    }

    public SecondaryStorageVmVO assignSecStorageVmFromRunningPool(long dataCenterId, SecondaryStorageVm.Role role) {

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Assign  secondary storage vm from running pool for request from data center : " + dataCenterId);
        }

        SecondaryStorageVmAllocator allocator = getCurrentAllocator();
        assert (allocator != null);
        List<SecondaryStorageVmVO> runningList = _secStorageVmDao.getSecStorageVmListInStates(role, dataCenterId, State.Running);
        if (runningList != null && runningList.size() > 0) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Running secondary storage vm pool size : " + runningList.size());
                for (SecondaryStorageVmVO secStorageVm : runningList) {
                    s_logger.trace("Running secStorageVm instance : " + secStorageVm.getHostName());
                }
            }

            Map<Long, Integer> loadInfo = new HashMap<Long, Integer>();

            return allocator.allocSecondaryStorageVm(runningList, loadInfo, dataCenterId);
        } else {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Empty running secStorageVm pool for now in data center : " + dataCenterId);
            }
        }
        return null;
    }

    public SecondaryStorageVmVO assignSecStorageVmFromStoppedPool(long dataCenterId, SecondaryStorageVm.Role role) {
        List<SecondaryStorageVmVO> l = _secStorageVmDao.getSecStorageVmListInStates(role, dataCenterId, State.Starting, State.Stopped, State.Migrating);
        if (l != null && l.size() > 0) {
            return l.get(0);
        }

        return null;
    }

    private void allocCapacity(long dataCenterId, SecondaryStorageVm.Role role) {
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Allocate secondary storage vm standby capacity for data center : " + dataCenterId);
        }

        if (!isSecondaryStorageVmRequired(dataCenterId)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Secondary storage vm not required in zone " + dataCenterId + " acc. to zone config");
            }
            return;
        }
        
        
        boolean secStorageVmFromStoppedPool = false;
        SecondaryStorageVmVO secStorageVm = assignSecStorageVmFromStoppedPool(dataCenterId, role);
        if (secStorageVm == null) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("No stopped secondary storage vm is available, need to allocate a new secondary storage vm");
            }

            if (_allocLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
                try {
                    secStorageVm = startNew(dataCenterId, role);
                } finally {
                    _allocLock.unlock();
                }
            } else {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Unable to acquire synchronization lock to allocate secStorageVm resource for standby capacity, wait for next scan");
                }
                return;
            }
        } else {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Found a stopped secondary storage vm, bring it up to running pool. secStorageVm vm id : " + secStorageVm.getId());
            }
            secStorageVmFromStoppedPool = true;
        }

        if (secStorageVm != null) {
            long secStorageVmId = secStorageVm.getId();
            GlobalLock secStorageVmLock = GlobalLock.getInternLock(getSecStorageVmLockName(secStorageVmId));
            try {
                if (secStorageVmLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
                    try {
                        secStorageVm = startSecStorageVm(secStorageVmId);
                    } finally {
                        secStorageVmLock.unlock();
                    }
                } else {
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Unable to acquire synchronization lock to start secStorageVm for standby capacity, secStorageVm vm id : " + secStorageVm.getId());
                    }
                    return;
                }
            } finally {
                secStorageVmLock.releaseRef();
            }

            if (secStorageVm == null) {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Unable to start secondary storage vm for standby capacity, secStorageVm vm Id : " + secStorageVmId + ", will recycle it and start a new one");
                }

                if (secStorageVmFromStoppedPool) {
                    destroySecStorageVm(secStorageVmId);
                }
            } else {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Secondary storage vm " + secStorageVm.getHostName() + " is started");
                }
            }
        }
    }

    public boolean isZoneReady(Map<Long, ZoneHostInfo> zoneHostInfoMap, long dataCenterId) {
        ZoneHostInfo zoneHostInfo = zoneHostInfoMap.get(dataCenterId);
        if (zoneHostInfo != null && (zoneHostInfo.getFlags() & RunningHostInfoAgregator.ZoneHostInfo.ROUTING_HOST_MASK) != 0) {
            VMTemplateVO template = _templateDao.findSystemVMTemplate(dataCenterId);
            HostVO secHost = _ssvmMgr.findSecondaryStorageHost(dataCenterId);
            if (secHost == null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("No secondary storage available in zone " + dataCenterId + ", wait until it is ready to launch secondary storage vm");
                }
                return false;
            }

            boolean templateReady = false;
            if (template != null) {
                VMTemplateHostVO templateHostRef = _vmTemplateHostDao.findByHostTemplate(secHost.getId(), template.getId());
                templateReady = (templateHostRef != null) && (templateHostRef.getDownloadState() == Status.DOWNLOADED);
            }

            if (templateReady) {

                List<Pair<Long, Integer>> l = _storagePoolHostDao.getDatacenterStoragePoolHostInfo(dataCenterId, !_useLocalStorage);
                if (l != null && l.size() > 0 && l.get(0).second().intValue() > 0) {

                    return true;
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Primary storage is not ready, wait until it is ready to launch secondary storage vm. dcId: " + dataCenterId + " system.vm.use.local.storage: " + _useLocalStorage +
                        		"If you want to use local storage to start ssvm, need to set system.vm.use.local.storage to true");
                    }
                }
            } else {
                if (s_logger.isDebugEnabled()) {
                    if (template == null) {
                        s_logger.debug("Zone host is ready, but secondary storage vm template does not exist");
                    } else {
                        s_logger.debug("Zone host is ready, but secondary storage vm template: " + template.getId() + " is not ready on secondary storage: " + secHost.getId());
                    }
                }
            }
        }
        return false;
    }

    private synchronized Map<Long, ZoneHostInfo> getZoneHostInfo() {
        Date cutTime = DateUtil.currentGMTTime();
        List<RunningHostCountInfo> l = _hostDao.getRunningHostCounts(new Date(cutTime.getTime() - _clusterMgr.getHeartbeatThreshold()));

        RunningHostInfoAgregator aggregator = new RunningHostInfoAgregator();
        if (l.size() > 0) {
            for (RunningHostCountInfo countInfo : l) {
                aggregator.aggregate(countInfo);
            }
        }

        return aggregator.getZoneHostInfoMap();
    }


    @Override
    public boolean start() {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Start secondary storage vm manager");
        }

        return true;
    }

    @Override
    public boolean stop() {
        _loadScanner.stop();
        _allocLock.releaseRef();
        _resourceMgr.unregisterResourceStateAdapter(this.getClass().getSimpleName());
        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Start configuring secondary storage vm manager : " + name);
        }

        Map<String, String> configs = _configDao.getConfiguration("management-server", params);
        
        _secStorageVmMtuSize = NumbersUtil.parseInt(configs.get("secstorage.vm.mtu.size"), DEFAULT_SS_VM_MTUSIZE);
        String useServiceVM = _configDao.getValue("secondary.storage.vm");
        boolean _useServiceVM = false;
        if ("true".equalsIgnoreCase(useServiceVM)) {
            _useServiceVM = true;
        }

        String sslcopy = _configDao.getValue("secstorage.encrypt.copy");
        if ("true".equalsIgnoreCase(sslcopy)) {
            _useSSlCopy = true;
        }

        _allowedInternalSites = _configDao.getValue("secstorage.allowed.internal.sites");

        String value = configs.get("secstorage.capacityscan.interval");
        _capacityScanInterval = NumbersUtil.parseLong(value, DEFAULT_CAPACITY_SCAN_INTERVAL);

        _instance = configs.get("instance.name");
        if (_instance == null) {
            _instance = "DEFAULT";
        }

        Map<String, String> agentMgrConfigs = _configDao.getConfiguration("AgentManager", params);
        _mgmt_host = agentMgrConfigs.get("host");
        if (_mgmt_host == null) {
            s_logger.warn("Critical warning! Please configure your management server host address right after you have started your management server and then restart it, otherwise you won't have access to secondary storage");
        }

        value = agentMgrConfigs.get("port");
        _mgmt_port = NumbersUtil.parseInt(value, 8250);

        _listener = new SecondaryStorageListener(this, _agentMgr);
        _agentMgr.registerForHostEvents(_listener, true, false, true);

        _itMgr.registerGuru(VirtualMachine.Type.SecondaryStorageVm, this);
        
        //check if there is a default service offering configured
        String ssvmSrvcOffIdStr = configs.get(Config.SecondaryStorageServiceOffering.key());
        if (ssvmSrvcOffIdStr != null) {
            Long ssvmSrvcOffId = Long.parseLong(ssvmSrvcOffIdStr);
            _serviceOffering = _offeringDao.findById(ssvmSrvcOffId);
            if (_serviceOffering == null || !_serviceOffering.getSystemUse()) {
                String msg = "Can't find system service offering id=" + ssvmSrvcOffId + " for secondary storage vm";
                s_logger.error(msg);
                throw new ConfigurationException(msg);
            }
        } else {
        	int ramSize = NumbersUtil.parseInt(_configDao.getValue("ssvm.ram.size"), DEFAULT_SS_VM_RAMSIZE);
        	int cpuFreq = NumbersUtil.parseInt(_configDao.getValue("ssvm.cpu.mhz"), DEFAULT_SS_VM_CPUMHZ);
            _useLocalStorage = Boolean.parseBoolean(configs.get(Config.SystemVMUseLocalStorage.key()));
            _serviceOffering = new ServiceOfferingVO("System Offering For Secondary Storage VM", 1, ramSize, cpuFreq, null, null, false, null, _useLocalStorage, true, null, true, VirtualMachine.Type.SecondaryStorageVm, true);
            _serviceOffering.setUniqueName(ServiceOffering.ssvmDefaultOffUniqueName);
            _serviceOffering = _offeringDao.persistSystemServiceOffering(_serviceOffering);
            
            // this can sometimes happen, if DB is manually or programmatically manipulated
            if(_serviceOffering == null) {
                String msg = "Data integrity problem : System Offering For Secondary Storage VM has been removed?";
                s_logger.error(msg);
                throw new ConfigurationException(msg);
            }
        }

        if (_useServiceVM) {
            _loadScanner = new SystemVmLoadScanner<Long>(this);
            _loadScanner.initScan(STARTUP_DELAY, _capacityScanInterval);
        }
        
        _httpProxy = configs.get(Config.SecStorageProxy.key());
        if (_httpProxy != null) {
        	boolean valid = true;
        	String errMsg = null;
        	try {
				URI uri = new URI(_httpProxy);
				if (!"http".equalsIgnoreCase(uri.getScheme())) {
					errMsg = "Only support http proxy";
					valid = false;
				} else if (uri.getHost() == null) {
					errMsg = "host can not be null";
					valid = false;
				} else if (uri.getPort() == -1) {
					_httpProxy = _httpProxy + ":3128";
				}
			} catch (URISyntaxException e) {
				errMsg = e.toString();
			} finally {
				if (!valid) {
					s_logger.debug("ssvm http proxy " + _httpProxy + " is invalid: " + errMsg);
					throw new ConfigurationException("ssvm http proxy " + _httpProxy +  "is invalid: " + errMsg);
				}
			}
        }
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Secondary storage vm Manager is configured.");
        }
        _resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        return true;
    }

    @Override
    public Long convertToId(String vmName) {
        if (!VirtualMachineName.isValidSystemVmName(vmName, _instance, "s")) {
            return null;
        }
        return VirtualMachineName.getSystemVmId(vmName);
    }

    @Override
    public boolean stopSecStorageVm(long secStorageVmId) {
        SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(secStorageVmId);
        if (secStorageVm == null) {
            String msg = "Stopping secondary storage vm failed: secondary storage vm " + secStorageVmId + " no longer exists";
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(msg);
            }
            return false;
        }
        try {
            if (secStorageVm.getHostId() != null) {
                GlobalLock secStorageVmLock = GlobalLock.getInternLock(getSecStorageVmLockName(secStorageVm.getId()));
                try {
                    if (secStorageVmLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
                        try {
                            boolean result = _itMgr.stop(secStorageVm, _accountMgr.getSystemUser(), _accountMgr.getSystemAccount());
                            if (result) {
                            }

                            return result;
                        } finally {
                            secStorageVmLock.unlock();
                        }
                    } else {
                        String msg = "Unable to acquire secondary storage vm lock : " + secStorageVm.toString();
                        s_logger.debug(msg);
                        return false;
                    }
                } finally {
                    secStorageVmLock.releaseRef();
                }
            }

            // vm was already stopped, return true
            return true;
        } catch (ResourceUnavailableException e) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Stopping secondary storage vm " + secStorageVm.getHostName() + " faled : exception " + e.toString());
            }
            return false;
        }
    }

    @Override
    public boolean rebootSecStorageVm(long secStorageVmId) {
        final SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(secStorageVmId);

        if (secStorageVm == null || secStorageVm.getState() == State.Destroyed) {
            return false;
        }

        if (secStorageVm.getState() == State.Running && secStorageVm.getHostId() != null) {
            final RebootCommand cmd = new RebootCommand(secStorageVm.getInstanceName());
            final Answer answer = _agentMgr.easySend(secStorageVm.getHostId(), cmd);

            if (answer != null && answer.getResult()) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Successfully reboot secondary storage vm " + secStorageVm.getHostName());
                }

                SubscriptionMgr.getInstance().notifySubscribers(ALERT_SUBJECT, this,
                        new SecStorageVmAlertEventArgs(SecStorageVmAlertEventArgs.SSVM_REBOOTED, secStorageVm.getDataCenterId(), secStorageVm.getId(), secStorageVm, null));

                return true;
            } else {
                String msg = "Rebooting Secondary Storage VM failed - " + secStorageVm.getHostName();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(msg);
                }
                return false;
            }
        } else {
            return startSecStorageVm(secStorageVmId) != null;
        }
    }

    @Override
    public boolean destroySecStorageVm(long vmId) {
        SecondaryStorageVmVO ssvm = _secStorageVmDao.findById(vmId);

        try {
            boolean result = _itMgr.expunge(ssvm, _accountMgr.getSystemUser(), _accountMgr.getSystemAccount());
            if (result) {
                HostVO host = _hostDao.findByTypeNameAndZoneId(ssvm.getDataCenterId(), ssvm.getHostName(), 
                        Host.Type.SecondaryStorageVM);
                if (host != null) {
                    s_logger.debug("Removing host entry for ssvm id=" + vmId);
                    result = result && _hostDao.remove(host.getId());
                }
            }
            
            return result;
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to expunge " + ssvm, e);
            return false;
        }
    }

    @Override
    public void onAgentConnect(Long dcId, StartupCommand cmd) {
    }

    private String getAllocLockName() {
        // to improve security, it may be better to return a unique mashed
        // name(for example MD5 hashed)
        return "secStorageVm.alloc";
    }

    private String getSecStorageVmLockName(long id) {
        return "secStorageVm." + id;
    }

    @Override
    public SecondaryStorageVmVO findByName(String name) {
        if (!VirtualMachineName.isValidSecStorageVmName(name, null)) {
            return null;
        }
        return findById(VirtualMachineName.getSystemVmId(name));
    }

    @Override
    public SecondaryStorageVmVO findById(long id) {
        return _secStorageVmDao.findById(id);
    }

    @Override
    public SecondaryStorageVmVO persist(SecondaryStorageVmVO vm) {
        return _secStorageVmDao.persist(vm);
    }

    @Override
    public boolean finalizeVirtualMachineProfile(VirtualMachineProfile<SecondaryStorageVmVO> profile, DeployDestination dest, ReservationContext context) {

    	SecondaryStorageVmVO vm = profile.getVirtualMachine();
        Map<String, String> details = _vmDetailsDao.findDetails(vm.getId());
        vm.setDetails(details);
    	
        HostVO secHost = _ssvmMgr.findSecondaryStorageHost(dest.getDataCenter().getId());
        assert (secHost != null);

        StringBuilder buf = profile.getBootArgsBuilder();
        buf.append(" template=domP type=secstorage");
        buf.append(" host=").append(_mgmt_host);
        buf.append(" port=").append(_mgmt_port);
        buf.append(" name=").append(profile.getVirtualMachine().getHostName());

        buf.append(" zone=").append(dest.getDataCenter().getId());
        buf.append(" pod=").append(dest.getPod().getId());

        buf.append(" guid=").append(profile.getVirtualMachine().getHostName());

        if (_configDao.isPremium()) {
            if (profile.getHypervisorType() == HypervisorType.Hyperv) {
            	s_logger.debug("Hyperv hypervisor configured, telling the ssvm to load the CifsSecondaryStorageResource");
                buf.append(" resource=org.apache.storage.resource.CifsSecondaryStorageResource");
            } else if (profile.getHypervisorType() == HypervisorType.VMware) {
            	s_logger.debug("VmWare hypervisor configured, telling the ssvm to load the PremiumSecondaryStorageResource");
            	buf.append(" resource=org.apache.storage.resource.PremiumSecondaryStorageResource");
            } else {
            	s_logger.debug("Telling the ssvm to load the NfsSecondaryStorageResource");
                buf.append(" resource=org.apache.cloudstack.storage.resource.NfsSecondaryStorageResource");
            }
        } else {
            buf.append(" resource=org.apache.cloudstack.storage.resource.NfsSecondaryStorageResource");
        }
        buf.append(" instance=SecStorage");
        buf.append(" sslcopy=").append(Boolean.toString(_useSSlCopy));
        buf.append(" role=").append(profile.getVirtualMachine().getRole().toString());
        buf.append(" mtu=").append(_secStorageVmMtuSize);

        boolean externalDhcp = false;
        String externalDhcpStr = _configDao.getValue("direct.attach.network.externalIpAllocator.enabled");
        if (externalDhcpStr != null && externalDhcpStr.equalsIgnoreCase("true")) {
            externalDhcp = true;
        }
        
        if (Boolean.valueOf(_configDao.getValue("system.vm.random.password"))) {
        	buf.append(" vmpassword=").append(_configDao.getValue("system.vm.password"));
        }

        for (NicProfile nic : profile.getNics()) {
            int deviceId = nic.getDeviceId();
            if (nic.getIp4Address() == null) {
                buf.append(" eth").append(deviceId).append("mask=").append("0.0.0.0");
                buf.append(" eth").append(deviceId).append("ip=").append("0.0.0.0");
            } else {
                buf.append(" eth").append(deviceId).append("ip=").append(nic.getIp4Address());
                buf.append(" eth").append(deviceId).append("mask=").append(nic.getNetmask());
            }

            if (nic.isDefaultNic()) {
                buf.append(" gateway=").append(nic.getGateway());
            }
            if (nic.getTrafficType() == TrafficType.Management) {
                String mgmt_cidr = _configDao.getValue(Config.ManagementNetwork.key());
                if (NetUtils.isValidCIDR(mgmt_cidr)) {
                    buf.append(" mgmtcidr=").append(mgmt_cidr);
                }
                buf.append(" localgw=").append(dest.getPod().getGateway());
                buf.append(" private.network.device=").append("eth").append(deviceId);
            } else if (nic.getTrafficType() == TrafficType.Public) {
                buf.append(" public.network.device=").append("eth").append(deviceId);
            } else if (nic.getTrafficType() == TrafficType.Storage) {
            	buf.append(" storageip=").append(nic.getIp4Address());
            	buf.append(" storagenetmask=").append(nic.getNetmask());
            	buf.append(" storagegateway=").append(nic.getGateway());
            }
        }

        /* External DHCP mode */
        if (externalDhcp) {
            buf.append(" bootproto=dhcp");
        }

        DataCenterVO dc = _dcDao.findById(profile.getVirtualMachine().getDataCenterId());
        buf.append(" internaldns1=").append(dc.getInternalDns1());
        if (dc.getInternalDns2() != null) {
            buf.append(" internaldns2=").append(dc.getInternalDns2());
        }
        buf.append(" dns1=").append(dc.getDns1());
        if (dc.getDns2() != null) {
            buf.append(" dns2=").append(dc.getDns2());
        }

        String bootArgs = buf.toString();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Boot Args for " + profile + ": " + bootArgs);
        }

        return true;
    }

    @Override
    public boolean finalizeDeployment(Commands cmds, VirtualMachineProfile<SecondaryStorageVmVO> profile, DeployDestination dest, ReservationContext context) {

        finalizeCommandsOnStart(cmds, profile);

        SecondaryStorageVmVO secVm = profile.getVirtualMachine();
        DataCenter dc = dest.getDataCenter();
        List<NicProfile> nics = profile.getNics();
        for (NicProfile nic : nics) {
            if ((nic.getTrafficType() == TrafficType.Public && dc.getNetworkType() == NetworkType.Advanced)
                    || (nic.getTrafficType() == TrafficType.Guest && (dc.getNetworkType() == NetworkType.Basic || dc.isSecurityGroupEnabled()))) {
                secVm.setPublicIpAddress(nic.getIp4Address());
                secVm.setPublicNetmask(nic.getNetmask());
                secVm.setPublicMacAddress(nic.getMacAddress());
            } else if (nic.getTrafficType() == TrafficType.Management) {
                secVm.setPrivateIpAddress(nic.getIp4Address());
                secVm.setPrivateMacAddress(nic.getMacAddress());
            }
        }
        _secStorageVmDao.update(secVm.getId(), secVm);
        return true;
    }

    @Override
    public boolean finalizeCommandsOnStart(Commands cmds, VirtualMachineProfile<SecondaryStorageVmVO> profile) {

        NicProfile managementNic = null;
        NicProfile controlNic = null;
        for (NicProfile nic : profile.getNics()) {
            if (nic.getTrafficType() == TrafficType.Management) {
                managementNic = nic;
            } else if (nic.getTrafficType() == TrafficType.Control && nic.getIp4Address() != null) {
                controlNic = nic;
            }
        }

        if (controlNic == null) {
            if (managementNic == null) {
                s_logger.error("Management network doesn't exist for the secondaryStorageVm " + profile.getVirtualMachine());
                return false;
            }
            controlNic = managementNic;
        }

        CheckSshCommand check = new CheckSshCommand(profile.getInstanceName(), controlNic.getIp4Address(), 3922);
        cmds.addCommand("checkSsh", check);

        return true;
    }

    @Override
    public boolean finalizeStart(VirtualMachineProfile<SecondaryStorageVmVO> profile, long hostId, Commands cmds, ReservationContext context) {
        CheckSshAnswer answer = (CheckSshAnswer) cmds.getAnswer("checkSsh");
        if (!answer.getResult()) {
            s_logger.warn("Unable to ssh to the VM: " + answer.getDetails());
            return false;
        }

        try {
            //get system ip and create static nat rule for the vm in case of basic networking with EIP/ELB
            _rulesMgr.getSystemIpAndEnableStaticNatForVm(profile.getVirtualMachine(), false);
            IPAddressVO ipaddr = _ipAddressDao.findByAssociatedVmId(profile.getVirtualMachine().getId());
            if (ipaddr != null && ipaddr.getSystem()) {
                SecondaryStorageVmVO secVm = profile.getVirtualMachine();
                // override SSVM guest IP with EIP, so that download url's with be prepared with EIP
                secVm.setPublicIpAddress(ipaddr.getAddress().addr());
                _secStorageVmDao.update(secVm.getId(), secVm);
            }
        } catch (Exception ex) {
            s_logger.warn("Failed to get system ip and enable static nat for the vm " + profile.getVirtualMachine() + " due to exception ", ex);
            return false;
        }

        return true;
    }

    @Override
    public void finalizeStop(VirtualMachineProfile<SecondaryStorageVmVO> profile, StopAnswer answer) {
        //release elastic IP here
        IPAddressVO ip = _ipAddressDao.findByAssociatedVmId(profile.getId());
        if (ip != null && ip.getSystem()) {
            UserContext ctx = UserContext.current();
            try {
                _rulesMgr.disableStaticNat(ip.getId(), ctx.getCaller(), ctx.getCallerUserId(), true);
            } catch (Exception ex) {
                s_logger.warn("Failed to disable static nat and release system ip " + ip + " as a part of vm " + profile.getVirtualMachine() + " stop due to exception ", ex);
            }
        }
    }

    @Override
    public void finalizeExpunge(SecondaryStorageVmVO vm) {
        vm.setPublicIpAddress(null);
        vm.setPublicMacAddress(null);
        vm.setPublicNetmask(null);
        _secStorageVmDao.update(vm.getId(), vm);
    }

    @Override
    public String getScanHandlerName() {
        return "secstorage";
    }

    @Override
    public boolean canScan() {
        return true;
    }

    @Override
    public void onScanStart() {
        _zoneHostInfoMap = getZoneHostInfo();
    }

    @Override
    public Long[] getScannablePools() {
        List<DataCenterVO> zones = _dcDao.listEnabledZones();

        Long[] dcIdList = new Long[zones.size()];
        int i = 0;
        for (DataCenterVO dc : zones) {
            dcIdList[i++] = dc.getId();
        }

        return dcIdList;
    }

    @Override
    public boolean isPoolReadyForScan(Long pool) {
        // pool is at zone basis
        long dataCenterId = pool.longValue();

        if (!isZoneReady(_zoneHostInfoMap, dataCenterId)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Zone " + dataCenterId + " is not ready to launch secondary storage VM yet");
            }
            return false;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Zone " + dataCenterId + " is ready to launch secondary storage VM");
        }
        return true;
    }

    @Override
    public Pair<AfterScanAction, Object> scanPool(Long pool) {
        long dataCenterId = pool.longValue();

        List<SecondaryStorageVmVO> ssVms = _secStorageVmDao.getSecStorageVmListInStates(SecondaryStorageVm.Role.templateProcessor, dataCenterId, State.Running, State.Migrating,
                State.Starting,  State.Stopped, State.Stopping );
        int vmSize = (ssVms == null)? 0 : ssVms.size();
        List<HostVO> ssHosts = _ssvmMgr.listSecondaryStorageHostsInOneZone(dataCenterId);        
        int hostSize = (ssHosts == null)? 0 : ssHosts.size();
        if ( hostSize > vmSize ) {
            s_logger.info("No secondary storage vms found in datacenter id=" + dataCenterId + ", starting a new one");
            return new Pair<AfterScanAction, Object>(AfterScanAction.expand, SecondaryStorageVm.Role.templateProcessor);
        }

        return new Pair<AfterScanAction, Object>(AfterScanAction.nop, SecondaryStorageVm.Role.templateProcessor);
    }

    @Override
    public void expandPool(Long pool, Object actionArgs) {
        long dataCenterId = pool.longValue();
        allocCapacity(dataCenterId, (SecondaryStorageVm.Role) actionArgs);
    }

    @Override
    public void shrinkPool(Long pool, Object actionArgs) {
    }

    @Override
    public void onScanEnd() {
    }

	@Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
		/* Called when Secondary Storage VM connected */
		StartupCommand firstCmd = cmd[0];
	    if (!(firstCmd instanceof StartupSecondaryStorageCommand)) {
	    	return null;
	    }
	    
		host.setType( org.apache.host.Host.Type.SecondaryStorageVM);
		return host;	
    }

	@Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details,
            List<String> hostTags) {
		/* Called when add secondary storage on UI */
		StartupCommand firstCmd = startup[0];
		if (!(firstCmd instanceof StartupStorageCommand)) {
			return null;
		}

		org.apache.host.Host.Type type = null;
		StartupStorageCommand ssCmd = ((StartupStorageCommand) firstCmd);
		if (ssCmd.getHostType() == Host.Type.SecondaryStorageCmdExecutor) {
			type = ssCmd.getHostType();
		} else {
			if (ssCmd.getResourceType() == Storage.StorageResourceType.SECONDARY_STORAGE) {
				type = Host.Type.SecondaryStorage;
				if (resource != null && resource instanceof DummySecondaryStorageResource) {
					host.setResource(null);
				}
			} else if (ssCmd.getResourceType() == Storage.StorageResourceType.LOCAL_SECONDARY_STORAGE) {
				type = Host.Type.LocalSecondaryStorage;
			} else {
				type = Host.Type.Storage;
			}

			final Map<String, String> hostDetails = ssCmd.getHostDetails();
			if (hostDetails != null) {
				if (details != null) {
					details.putAll(hostDetails);
				} else {
					details = hostDetails;
				}
			}

			host.setDetails(details);
			host.setParent(ssCmd.getParent());
			host.setTotalSize(ssCmd.getTotalSize());
			host.setHypervisorType(HypervisorType.None);
			host.setType(type);
			if (ssCmd.getNfsShare() != null) {
				host.setStorageUrl(ssCmd.getNfsShare());
			}
		}
		
		return host;
    }

	@Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        if (host.getType() == Host.Type.SecondaryStorage) {
            deleteHost(host.getId());
            return new DeleteHostAnswer(false);
        }
        return null;
    }

	@Override
    public HostVO findSecondaryStorageHost(long dcId) {
		SearchCriteriaService<HostVO, HostVO> sc = SearchCriteria2.create(HostVO.class);
	    sc.addAnd(sc.getEntity().getType(), Op.EQ, Host.Type.SecondaryStorage);
	    sc.addAnd(sc.getEntity().getDataCenterId(), Op.EQ, dcId);
	    List<HostVO> storageHosts = sc.list();
	    if (storageHosts == null || storageHosts.size() < 1) {
            return null;
        } else {
            Collections.shuffle(storageHosts);
            return storageHosts.get(0);
        }
    }

	@Override
    public List<HostVO> listSecondaryStorageHostsInAllZones() {
		SearchCriteriaService<HostVO, HostVO> sc = SearchCriteria2.create(HostVO.class);
	    sc.addAnd(sc.getEntity().getType(), Op.EQ, Host.Type.SecondaryStorage);
	    return sc.list();
    }

	@Override
    public List<HostVO> listSecondaryStorageHostsInOneZone(long dataCenterId) {
		SearchCriteriaService<HostVO, HostVO> sc = SearchCriteria2.create(HostVO.class);
		sc.addAnd(sc.getEntity().getDataCenterId(), Op.EQ, dataCenterId);
		sc.addAnd(sc.getEntity().getType(), Op.EQ, Host.Type.SecondaryStorage);
	    return sc.list();
    }

	@Override
    public List<HostVO> listLocalSecondaryStorageHostsInOneZone(long dataCenterId) {
		SearchCriteriaService<HostVO, HostVO> sc = SearchCriteria2.create(HostVO.class);
		sc.addAnd(sc.getEntity().getDataCenterId(), Op.EQ, dataCenterId);
		sc.addAnd(sc.getEntity().getType(), Op.EQ, Host.Type.LocalSecondaryStorage);
	    return sc.list();
    }

	@Override
    public List<HostVO> listAllTypesSecondaryStorageHostsInOneZone(long dataCenterId) {
		SearchCriteriaService<HostVO, HostVO> sc = SearchCriteria2.create(HostVO.class);
		sc.addAnd(sc.getEntity().getDataCenterId(), Op.EQ, dataCenterId);
		sc.addAnd(sc.getEntity().getType(), Op.IN, Host.Type.LocalSecondaryStorage, Host.Type.SecondaryStorage);
	    return sc.list();
    }

	@Override
    public List<HostVO> listUpAndConnectingSecondaryStorageVmHost(Long dcId) {
		SearchCriteriaService<HostVO, HostVO> sc = SearchCriteria2.create(HostVO.class);
        if (dcId != null) {
            sc.addAnd(sc.getEntity().getDataCenterId(), Op.EQ, dcId);
        }
		sc.addAnd(sc.getEntity().getStatus(), Op.IN, org.apache.host.Status.Up, org.apache.host.Status.Connecting);
		sc.addAnd(sc.getEntity().getType(), Op.EQ, Host.Type.SecondaryStorageVM);
	    return sc.list();
    }

	@Override
    public HostVO pickSsvmHost(HostVO ssHost) {
        if( ssHost.getType() == Host.Type.LocalSecondaryStorage ) {
            return  ssHost;
        } else if ( ssHost.getType() == Host.Type.SecondaryStorage) {
            Long dcId = ssHost.getDataCenterId();
            List<HostVO> ssAHosts = listUpAndConnectingSecondaryStorageVmHost(dcId);
            if (ssAHosts == null || ssAHosts.isEmpty() ) {
                return null;
            }
            Collections.shuffle(ssAHosts);
            return ssAHosts.get(0);
        }
        return null;
    }
	
    @Override
    public boolean plugNic(Network network, NicTO nic, VirtualMachineTO vm,
            ReservationContext context, DeployDestination dest) throws ConcurrentOperationException, ResourceUnavailableException,
            InsufficientCapacityException {
        //not supported
        throw new UnsupportedOperationException("Plug nic is not supported for vm of type " + vm.getType());
    }


    @Override
    public boolean unplugNic(Network network, NicTO nic, VirtualMachineTO vm,
            ReservationContext context, DeployDestination dest) throws ConcurrentOperationException, ResourceUnavailableException {
        //not supported
        throw new UnsupportedOperationException("Unplug nic is not supported for vm of type " + vm.getType());
    }

	@Override
	public void prepareStop(VirtualMachineProfile<SecondaryStorageVmVO> profile) {
		
	}
}
