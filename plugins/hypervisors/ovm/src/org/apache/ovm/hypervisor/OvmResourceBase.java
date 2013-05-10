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
package org.apache.ovm.hypervisor;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.ConfigurationException;

import org.apache.agent.IAgentControl;
import org.apache.agent.api.Answer;
import org.apache.agent.api.AttachIsoCommand;
import org.apache.agent.api.AttachVolumeAnswer;
import org.apache.agent.api.AttachVolumeCommand;
import org.apache.agent.api.CheckNetworkAnswer;
import org.apache.agent.api.CheckNetworkCommand;
import org.apache.agent.api.CheckVirtualMachineAnswer;
import org.apache.agent.api.CheckVirtualMachineCommand;
import org.apache.agent.api.CleanupNetworkRulesCmd;
import org.apache.agent.api.Command;
import org.apache.agent.api.CreatePrivateTemplateFromVolumeCommand;
import org.apache.agent.api.CreateStoragePoolCommand;
import org.apache.agent.api.DeleteStoragePoolCommand;
import org.apache.agent.api.FenceAnswer;
import org.apache.agent.api.FenceCommand;
import org.apache.agent.api.GetHostStatsAnswer;
import org.apache.agent.api.GetHostStatsCommand;
import org.apache.agent.api.GetStorageStatsAnswer;
import org.apache.agent.api.GetStorageStatsCommand;
import org.apache.agent.api.GetVmStatsAnswer;
import org.apache.agent.api.GetVmStatsCommand;
import org.apache.agent.api.GetVncPortAnswer;
import org.apache.agent.api.GetVncPortCommand;
import org.apache.agent.api.HostStatsEntry;
import org.apache.agent.api.MaintainAnswer;
import org.apache.agent.api.MaintainCommand;
import org.apache.agent.api.MigrateAnswer;
import org.apache.agent.api.MigrateCommand;
import org.apache.agent.api.ModifyStoragePoolAnswer;
import org.apache.agent.api.ModifyStoragePoolCommand;
import org.apache.agent.api.PingCommand;
import org.apache.agent.api.PingRoutingCommand;
import org.apache.agent.api.PingTestCommand;
import org.apache.agent.api.PrepareForMigrationAnswer;
import org.apache.agent.api.PrepareForMigrationCommand;
import org.apache.agent.api.PrepareOCFS2NodesCommand;
import org.apache.agent.api.ReadyAnswer;
import org.apache.agent.api.ReadyCommand;
import org.apache.agent.api.RebootAnswer;
import org.apache.agent.api.RebootCommand;
import org.apache.agent.api.SecurityGroupRuleAnswer;
import org.apache.agent.api.SecurityGroupRulesCmd;
import org.apache.agent.api.StartAnswer;
import org.apache.agent.api.StartCommand;
import org.apache.agent.api.StartupCommand;
import org.apache.agent.api.StartupRoutingCommand;
import org.apache.agent.api.StopAnswer;
import org.apache.agent.api.StopCommand;
import org.apache.agent.api.VmStatsEntry;
import org.apache.agent.api.storage.CopyVolumeAnswer;
import org.apache.agent.api.storage.CopyVolumeCommand;
import org.apache.agent.api.storage.CreateAnswer;
import org.apache.agent.api.storage.CreateCommand;
import org.apache.agent.api.storage.CreatePrivateTemplateAnswer;
import org.apache.agent.api.storage.DestroyCommand;
import org.apache.agent.api.storage.PrimaryStorageDownloadAnswer;
import org.apache.agent.api.storage.PrimaryStorageDownloadCommand;
import org.apache.agent.api.to.NicTO;
import org.apache.agent.api.to.StorageFilerTO;
import org.apache.agent.api.to.VirtualMachineTO;
import org.apache.agent.api.to.VolumeTO;
import org.apache.host.Host.Type;
import org.apache.hypervisor.Hypervisor.HypervisorType;
import org.apache.log4j.Logger;
import org.apache.network.PhysicalNetworkSetupInfo;
import org.apache.network.Networks.BroadcastDomainType;
import org.apache.network.Networks.TrafficType;
import org.apache.ovm.object.Connection;
import org.apache.ovm.object.OvmBridge;
import org.apache.ovm.object.OvmDisk;
import org.apache.ovm.object.OvmHost;
import org.apache.ovm.object.OvmSecurityGroup;
import org.apache.ovm.object.OvmStoragePool;
import org.apache.ovm.object.OvmVif;
import org.apache.ovm.object.OvmVlan;
import org.apache.ovm.object.OvmVm;
import org.apache.ovm.object.OvmVolume;
import org.apache.resource.ServerResource;
import org.apache.resource.hypervisor.HypervisorResource;
import org.apache.storage.Volume;
import org.apache.storage.Storage.ImageFormat;
import org.apache.storage.Storage.StoragePoolType;
import org.apache.storage.template.TemplateInfo;
import org.apache.template.VirtualMachineTemplate.BootloaderType;
import org.apache.utils.Pair;
import org.apache.utils.Ternary;
import org.apache.utils.exception.CloudRuntimeException;
import org.apache.utils.script.Script;
import org.apache.utils.ssh.SSHCmdHelper;
import org.apache.vm.DiskProfile;
import org.apache.vm.VirtualMachine;
import org.apache.vm.VirtualMachine.State;
import org.apache.xmlrpc.XmlRpcException;

import com.trilead.ssh2.SCPClient;
import com.xensource.xenapi.Types.XenAPIException;

public class OvmResourceBase implements ServerResource, HypervisorResource {
	private static final Logger s_logger = Logger.getLogger(OvmResourceBase.class);
	String _name;
	Long _zoneId;
	Long _podId;
	Long _clusterId;
	String _ip;
	String _username;
	String _password;
	String _guid;
	String _agentUserName = "oracle";
	String _agentPassword;
	Connection _conn;
    String _privateNetworkName;
    String _publicNetworkName;
    String _guestNetworkName;
    boolean _canBridgeFirewall;
    static boolean _isHeartBeat = false;
    List<String> _bridges = null;
	protected HashMap<String, State> _vms = new HashMap<String, State>(50);
	static HashMap<String, State> _stateMaps;
	private final Map<String, Pair<Long, Long>> _vmNetworkStats= new ConcurrentHashMap<String, Pair<Long, Long>>();
	private static String _ovsAgentPath = "/opt/ovs-agent-latest";
	
	static {
		_stateMaps = new HashMap<String, State>();
		_stateMaps.put("RUNNING", State.Running);
		_stateMaps.put("DOWN", State.Stopped);
		_stateMaps.put("ERROR", State.Error);
		_stateMaps.put("SUSPEND", State.Stopped);
	}
	
	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		_name = name;
		
		try {
			_zoneId = Long.parseLong((String) params.get("zone"));
			_podId = Long.parseLong((String) params.get("pod"));
			_clusterId = Long.parseLong((String) params.get("cluster"));
			_ip = (String) params.get("ip");
			_username = (String) params.get("username");
			_password = (String) params.get("password");
			_guid = (String) params.get("guid");
	        _privateNetworkName = (String) params.get("private.network.device");
	        _publicNetworkName = (String) params.get("public.network.device");
	        _guestNetworkName = (String)params.get("guest.network.device");
	        _agentUserName = (String)params.get("agentusername");
	        _agentPassword = (String)params.get("agentpassword");
		} catch (Exception e) {
			s_logger.debug("Configure " + _name + " failed", e);
			throw new ConfigurationException("Configure " + _name + " failed, " + e.toString());
		}
		
		if (_podId == null) {
            throw new ConfigurationException("Unable to get the pod");
        }

        if (_ip == null) {
            throw new ConfigurationException("Unable to get the host address");
        }

        if (_username == null) {
            throw new ConfigurationException("Unable to get the username");
        }

        if (_password == null) {
            throw new ConfigurationException("Unable to get the password");
        }

        if (_guid == null) {
            throw new ConfigurationException("Unable to get the guid");
        }
        
        if (_agentUserName == null) {
        	throw new ConfigurationException("Unable to get agent user name");
        }
        
        if (_agentPassword == null) {
        	throw new ConfigurationException("Unable to get agent password");
        }                
        
		try {
			setupServer();
		} catch (Exception e) {
			s_logger.debug("Setup server failed, ip " + _ip, e);
			throw new ConfigurationException("Unable to setup server");
		}
		
        _conn = new Connection(_ip, _agentUserName, _agentPassword);
        try {
			OvmHost.registerAsMaster(_conn);
			OvmHost.registerAsVmServer(_conn);
			_bridges = OvmBridge.getAllBridges(_conn);
		} catch (XmlRpcException e) {
			s_logger.debug("Get bridges failed", e);
			throw new ConfigurationException("Cannot get bridges on host " + _ip + "," + e.getMessage());
		}
		
		if (_privateNetworkName != null && !_bridges.contains(_privateNetworkName)) {
			throw new ConfigurationException("Cannot find bridge " + _privateNetworkName + " on host " + _ip + ", all bridges are:" + _bridges);
		}
		
		if (_publicNetworkName != null && !_bridges.contains(_publicNetworkName)) {
			throw new ConfigurationException("Cannot find bridge " + _publicNetworkName + " on host " + _ip + ", all bridges are:" + _bridges);
		}
		
		if (_guestNetworkName != null && !_bridges.contains(_guestNetworkName)) {
			throw new ConfigurationException("Cannot find bridge " + _guestNetworkName + " on host " + _ip + ", all bridges are:" + _bridges);
		}
        
		/* set to false so each time ModifyStoragePoolCommand will re-setup heartbeat*/
		_isHeartBeat = false;
		
		/*
		try {
			_canBridgeFirewall = canBridgeFirewall();
		} catch (XmlRpcException e) {
			s_logger.error("Failed to detect whether the host supports security groups.", e);
			_canBridgeFirewall = false;
		}
		*/
		
		_canBridgeFirewall = false;
		
		s_logger.debug(_canBridgeFirewall ? "OVM host supports security groups." : "OVM host doesn't support security groups.");
		
		return true;
	}

	@Override
	public boolean start() {
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}

	@Override
	public String getName() {
		return _name;
	}

	@Override
	public Type getType() {
		return Type.Routing;
	}

	protected void fillHostInfo(StartupRoutingCommand cmd) {
		try {
			OvmHost.Details hostDetails = OvmHost.getDetails(_conn);
			
			cmd.setName(hostDetails.name);
			cmd.setSpeed(hostDetails.cpuSpeed);
			cmd.setCpus(hostDetails.cpuNum);
			cmd.setMemory(hostDetails.freeMemory);
			cmd.setDom0MinMemory(hostDetails.dom0Memory);
			cmd.setGuid(_guid);
			cmd.setDataCenter(_zoneId.toString());
			cmd.setPod(_podId.toString());
			cmd.setCluster(_clusterId.toString());
			cmd.setVersion(OvmResourceBase.class.getPackage().getImplementationVersion());
			cmd.setHypervisorType(HypervisorType.Ovm);
			//TODO: introudce PIF
			cmd.setPrivateIpAddress(_ip);
			cmd.setStorageIpAddress(_ip);
			
			String defaultBridge = OvmBridge.getBridgeByIp(_conn, _ip);
			if (_publicNetworkName == null) {
				_publicNetworkName = defaultBridge;
			}
			if (_privateNetworkName == null) {
				_privateNetworkName = _publicNetworkName;
			}
			if (_guestNetworkName == null) {
				_guestNetworkName = _privateNetworkName;
			}
			Map<String, String> d = cmd.getHostDetails();
			d.put("public.network.device", _publicNetworkName);
			d.put("private.network.device", _privateNetworkName);
			d.put("guest.network.device", _guestNetworkName);
			cmd.setHostDetails(d);
			
			s_logger.debug(String.format("Add a OVM host(%s)", hostDetails.toJson()));
		} catch (XmlRpcException e) {
			s_logger.debug("XML RPC Exception" + e.getMessage(), e);
			throw new CloudRuntimeException("XML RPC Exception" + e.getMessage(), e);
		}
	}
	
	protected void setupServer() throws IOException {
		com.trilead.ssh2.Connection sshConnection = new com.trilead.ssh2.Connection(_ip, 22);
        sshConnection.connect(null, 60000, 60000);
        if (!sshConnection.authenticateWithPassword(_username, _password)) {
            throw new CloudRuntimeException("Unable to authenticate");
        }
        SCPClient scp = new SCPClient(sshConnection);
        
        String configScriptName = "scripts/vm/hypervisor/ovm/configureOvm.sh";
        String configScriptPath = Script.findScript("" , configScriptName);
        if (configScriptPath == null) {
        	throw new CloudRuntimeException("Unable to find " + configScriptName);
        }
        scp.put(configScriptPath, "/usr/bin/", "0755");
        
		if (!SSHCmdHelper.sshExecuteCmd(sshConnection, "sh /usr/bin/configureOvm.sh preSetup")) {
			throw new CloudRuntimeException("Execute configureOvm.sh preSetup failed on " + _ip);
		}
		
        File tmp = new File(configScriptPath);
        File scriptDir = new File(tmp.getParent());
        File [] scripts = scriptDir.listFiles();
        for (int i=0; i<scripts.length; i++) {
        	File script = scripts[i];
        	if (script.getName().equals("configureOvm.sh")) {
        		continue;
        	}
        	
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Copying " + script.getPath() + " to " + _ovsAgentPath + " on " + _ip + " with permission 0644");
            }
            scp.put(script.getPath(), _ovsAgentPath, "0644");
        }
        
		sshConnection = SSHCmdHelper.acquireAuthorizedConnection(_ip, _username, _password);
		if (sshConnection == null) {
			throw new CloudRuntimeException(
					String.format("Cannot connect to ovm host(IP=%1$s, username=%2$s, password=%3$s", _ip, _username, _password));
		}

		if (!SSHCmdHelper.sshExecuteCmd(sshConnection, "sh /usr/bin/configureOvm.sh postSetup")) {
			throw new CloudRuntimeException("Execute configureOvm.sh postSetup failed on " + _ip);
		}
        
	}
	
	@Override
	public StartupCommand[] initialize() {
		try {
			StartupRoutingCommand cmd = new StartupRoutingCommand();
			fillHostInfo(cmd);
	        Map<String, State> changes = null;
	        synchronized (_vms) {
	            _vms.clear();
	            changes = sync();
	        }
			cmd.setStateChanges(changes);
			cmd.setCaps("hvm");
			return new StartupCommand[] {cmd};
		} catch (Exception e) {
			s_logger.debug("Ovm resource initializes failed", e);
			return null;
		}
	}
	
	@Override
	public PingCommand getCurrentStatus(long id) {
		try {
			OvmHost.ping(_conn);
			HashMap<String, State> newStates = sync();
			return new PingRoutingCommand(getType(), id, newStates);
		} catch (XmlRpcException e) {
			s_logger.debug("Check agent status failed", e);
			return null;
		}
	}

	protected ReadyAnswer execute(ReadyCommand cmd) {
		try {
			OvmHost.Details d = OvmHost.getDetails(_conn);
			//TODO: cleanup halted vm
			if (d.masterIp.equalsIgnoreCase(_ip)) {
				return new ReadyAnswer(cmd);
			} else {
				s_logger.debug("Master IP changes to " + d.masterIp + ", it should be " + _ip);
				return new ReadyAnswer(cmd, "I am not the master server");
			}
		} catch (XmlRpcException e) {
			s_logger.debug("XML RPC Exception" + e.getMessage(), e);
			throw new CloudRuntimeException("XML RPC Exception" + e.getMessage(), e);
		}
		
	}
	
	protected void createNfsSr(StorageFilerTO pool) throws XmlRpcException {
		String mountPoint = String.format("%1$s:%2$s", pool.getHost(), pool.getPath());
		OvmStoragePool.Details d = new OvmStoragePool.Details();
		d.path = mountPoint;
		d.type = OvmStoragePool.NFS;
		d.uuid = pool.getUuid();
		OvmStoragePool.create(_conn, d);
		s_logger.debug(String.format("Created SR (mount point:%1$s)", mountPoint));
	}
	
	protected void createOCFS2Sr(StorageFilerTO pool)  throws XmlRpcException {
        OvmStoragePool.Details d = new OvmStoragePool.Details();
        d.path = pool.getPath();
        d.type = OvmStoragePool.OCFS2;
        d.uuid = pool.getUuid();
        OvmStoragePool.create(_conn, d);
        s_logger.debug(String.format("Created SR (mount point:%1$s)", d.path));
	}
	
	private void setupHeartBeat(String poolUuid) {
		try {
			if (!_isHeartBeat) {
				OvmHost.setupHeartBeat(_conn, poolUuid, _ip);
				_isHeartBeat = true;
			}
		} catch (Exception e) {
			s_logger.debug("setup heart beat for " + _ip + " failed", e);
			_isHeartBeat = false;
		}
	}
	
	protected Answer execute(ModifyStoragePoolCommand cmd) {
		StorageFilerTO pool = cmd.getPool();
		try {
			if (pool.getType() == StoragePoolType.NetworkFilesystem) {
				createNfsSr(pool);
			} else if (pool.getType() == StoragePoolType.OCFS2) {
			    createOCFS2Sr(pool);
			} else {
	            return new Answer(cmd, false, "The pool type: " + pool.getType().name() + " is not supported.");
	        }
			
			setupHeartBeat(pool.getUuid());
			OvmStoragePool.Details d = OvmStoragePool.getDetailsByUuid(_conn, pool.getUuid());
			Map<String, TemplateInfo> tInfo = new HashMap<String, TemplateInfo>();
			ModifyStoragePoolAnswer answer = new ModifyStoragePoolAnswer(cmd, d.totalSpace, d.freeSpace, tInfo);
			return answer;
		} catch (Exception e) {
			s_logger.debug("ModifyStoragePoolCommand failed", e);
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	protected Answer execute(CreateStoragePoolCommand cmd) {
		return new Answer(cmd, true, "success");
	}
	
	protected PrimaryStorageDownloadAnswer execute(final PrimaryStorageDownloadCommand cmd) {
		try {
			URI uri = new URI(cmd.getUrl());
			String secondaryStoragePath = uri.getHost() + ":" + uri.getPath();
			Pair<String, Long> res = OvmStoragePool.downloadTemplate(_conn, cmd.getPoolUuid(), secondaryStoragePath); 
			return new PrimaryStorageDownloadAnswer(res.first(), res.second());
		} catch (Exception e) {
			s_logger.debug("PrimaryStorageDownloadCommand failed", e);
			return new PrimaryStorageDownloadAnswer(e.getMessage());
		}
	}
	
	protected CreateAnswer execute(CreateCommand cmd) {
		StorageFilerTO primaryStorage = cmd.getPool();
		DiskProfile disk = cmd.getDiskCharacteristics();
		
		try {
			OvmVolume.Details vol = null;
			if (cmd.getTemplateUrl() != null) {
				vol = OvmVolume.createFromTemplate(_conn, primaryStorage.getUuid(),  cmd.getTemplateUrl());
			} else {
				vol = OvmVolume.createDataDsik(_conn, primaryStorage.getUuid(), Long.toString(disk.getSize()), disk.getType() == Volume.Type.ROOT);
			}
			
			VolumeTO volume = new VolumeTO(cmd.getVolumeId(), disk.getType(),
					primaryStorage.getType(), primaryStorage.getUuid(), primaryStorage.getPath(),
					vol.name, vol.path, vol.size, null);
			return new CreateAnswer(cmd, volume);
		} catch (Exception e) {
			s_logger.debug("CreateCommand failed", e);
			return new CreateAnswer(cmd, e.getMessage());
		}
	}
	
	protected void applySpecToVm(OvmVm.Details vm, VirtualMachineTO spec) {
		vm.name = spec.getName();
		vm.memory = spec.getMinRam();
		vm.cpuNum = spec.getCpus();
		vm.uuid = UUID.nameUUIDFromBytes(spec.getName().getBytes()).toString();
		if (spec.getBootloader() == BootloaderType.CD) {
			vm.bootDev = OvmVm.CD;
			vm.type = OvmVm.HVM;
		} else {
			vm.bootDev = OvmVm.HDD;
			String osType = OvmHelper.getOvmGuestType(spec.getOs());
			if (OvmHelper.ORACLE_LINUX.equals(osType)) {
				vm.type = OvmVm.PV;
			} else if (OvmHelper.WINDOWS.equals(osType) || OvmHelper.ORACLE_SOLARIS.equals(osType)) {
				vm.type = OvmVm.HVM;
			} else {
				throw new CloudRuntimeException(spec.getOs() + " is not supported" + ",Oracle VM only supports Oracle Linux and Windows");
			}
		}
	}
	
	protected void createVbds(OvmVm.Details vm, VirtualMachineTO spec) throws URISyntaxException {
		for (VolumeTO volume : spec.getDisks()) {
			if (volume.getType() == Volume.Type.ROOT) {
				OvmDisk.Details root = new OvmDisk.Details();
				root.path = volume.getPath();
				root.type = OvmDisk.WRITE;
				root.isIso = false;
				vm.rootDisk = root;
			} else if (volume.getType() == Volume.Type.ISO) {
				if (volume.getPath() != null) {
					OvmDisk.Details iso = new OvmDisk.Details();
					URI path = new URI(volume.getPath());
					iso.path = path.getHost() + ":" + path.getPath();
					iso.type = OvmDisk.READ;
					iso.isIso = true;
					vm.disks.add(iso);
				}
			} else if (volume.getType() == Volume.Type.DATADISK){
				OvmDisk.Details data = new OvmDisk.Details();
				data.path = volume.getPath();
				data.type = OvmDisk.SHAREDWRITE;
				data.isIso = false;
				vm.disks.add(data);
			} else {
				throw new CloudRuntimeException("Unknow volume type: " + volume.getType());
			}
		}
	}
	
	private String createVlanBridge(String networkName, String vlanId) throws XmlRpcException {
       	OvmBridge.Details brdetails = OvmBridge.getDetails(_conn, networkName);
    	if (brdetails.attach.equalsIgnoreCase("null")) {
    		throw new CloudRuntimeException("Bridge " + networkName + " has no PIF");
    	}
    	
    	OvmVlan.Details vdetails = new OvmVlan.Details();
    	vdetails.pif = brdetails.attach;
    	vdetails.vid = Integer.parseInt(vlanId);
    	brdetails.name = "vlan" + vlanId;
    	brdetails.attach = "null";
    	OvmBridge.createVlanBridge(_conn, brdetails, vdetails);
    	return brdetails.name;
	}
	
	
	//TODO: complete all network support
	protected String getNetwork(NicTO nic) throws XmlRpcException {
        String vlanId = null;
        String bridgeName = null;
        if (nic.getBroadcastType() == BroadcastDomainType.Vlan) {
            URI broadcastUri = nic.getBroadcastUri();
            vlanId = broadcastUri.getHost();
        }
        
        if (nic.getType() == TrafficType.Guest) {
            if (nic.getBroadcastType() == BroadcastDomainType.Vlan && !vlanId.equalsIgnoreCase("untagged")){
            	bridgeName = createVlanBridge(_guestNetworkName, vlanId);
            } else {
            	bridgeName = _guestNetworkName;
            }
        } else if (nic.getType() == TrafficType.Control) {
        	throw new CloudRuntimeException("local link network is not supported");
        } else if (nic.getType() == TrafficType.Public) {
        	throw new CloudRuntimeException("public network for system vm is not supported");
        } else if (nic.getType() == TrafficType.Management) {
        	bridgeName = _privateNetworkName;
        } else {
        	throw new CloudRuntimeException("Unkonw network traffic type:" + nic.getType());
        }
        
        return bridgeName;
	}
	
	protected OvmVif.Details createVif(NicTO nic) throws XmlRpcException {
		OvmVif.Details vif = new OvmVif.Details();
		vif.mac = nic.getMac();
		vif.bridge = getNetwork(nic);
		return vif;
	}
	
	protected void createVifs(OvmVm.Details vm, VirtualMachineTO spec) throws CloudRuntimeException, XmlRpcException {
		NicTO[] nics = spec.getNics();
		List<OvmVif.Details> vifs = new ArrayList<OvmVif.Details>(nics.length);
		for (NicTO nic : nics) {
			OvmVif.Details vif = createVif(nic);
			vifs.add(nic.getDeviceId(), vif);
		}
		vm.vifs.addAll(vifs);
	}
	
	protected void startVm(OvmVm.Details vm) throws XmlRpcException {
		OvmVm.create(_conn, vm);
	}
	
	
	private void cleanupNetwork(List<OvmVif.Details> vifs) throws XmlRpcException {
		for (OvmVif.Details vif : vifs) {
			if (vif.bridge.startsWith("vlan")) {
				OvmBridge.deleteVlanBridge(_conn, vif.bridge);
			}
		}
	}
	
	protected void cleanup(OvmVm.Details vm) {
		try {
			cleanupNetwork(vm.vifs);
		} catch (XmlRpcException e) {
			s_logger.debug("Clean up network for " + vm.name + " failed", e);
		}
		_vmNetworkStats.remove(vm.name);
	}
	
	@Override
	public synchronized StartAnswer execute(StartCommand cmd) {
		VirtualMachineTO vmSpec = cmd.getVirtualMachine();
		String vmName = vmSpec.getName();
		State state = State.Stopped;
		OvmVm.Details vmDetails = null;
		try {
			synchronized (_vms) {
				_vms.put(vmName, State.Starting);
			}
			
			vmDetails = new OvmVm.Details();
			applySpecToVm(vmDetails, vmSpec);
			createVbds(vmDetails, vmSpec);
			createVifs(vmDetails, vmSpec);
			startVm(vmDetails);
			
			// Add security group rules
			NicTO[] nics = vmSpec.getNics();
            for (NicTO nic : nics) {
                if (nic.isSecurityGroupEnabled()) {
                    if (vmSpec.getType().equals(VirtualMachine.Type.User)) {
                        defaultNetworkRulesForUserVm(vmName, vmSpec.getId(), nic);
                    }
                }
            }
			
			state = State.Running;
			return new StartAnswer(cmd);
		} catch (Exception e ) {
			s_logger.debug("Start vm " + vmName + " failed", e);
			cleanup(vmDetails);
			return new StartAnswer(cmd, e.getMessage());
		} finally {
			synchronized (_vms) {
				//FIXME: where to come to Stopped???
				if (state != State.Stopped) {
					_vms.put(vmName, state);
				} else {
					_vms.remove(vmName);
				}
			}
		}
	}
	
	protected Answer execute(GetHostStatsCommand cmd) {
		try {
			Map<String, String> res = OvmHost.getPerformanceStats(_conn, _publicNetworkName);
			Double cpuUtil = Double.parseDouble(res.get("cpuUtil"));
			Double rxBytes = Double.parseDouble(res.get("rxBytes"));
			Double txBytes = Double.parseDouble(res.get("txBytes"));
			Double totalMemory = Double.parseDouble(res.get("totalMemory"));
			Double freeMemory = Double.parseDouble(res.get("freeMemory"));
			HostStatsEntry hostStats = new HostStatsEntry(cmd.getHostId(), cpuUtil, rxBytes,
					txBytes, "host", totalMemory, freeMemory, 0, 0);
			return new GetHostStatsAnswer(cmd, hostStats);
		} catch (Exception e) {
			s_logger.debug("Get host stats of " + cmd.getHostName() + " failed", e);
			return new Answer(cmd, false, e.getMessage());
		}
		
	}
	
	@Override
	public StopAnswer execute(StopCommand cmd) {
		String vmName = cmd.getVmName();
		State state = null;
        synchronized(_vms) {
            state = _vms.get(vmName);
            _vms.put(vmName, State.Stopping);
        }
        
        try {
        	OvmVm.Details vm = null;
        	try {
        		vm = OvmVm.getDetails(_conn, vmName);   
        	} catch (XmlRpcException e) {
        		s_logger.debug("Unable to get details of vm: " + vmName + ", treating it as stopped", e);
        		return new StopAnswer(cmd, "success", 0, true);
        	}
        	
        	deleteAllNetworkRulesForVm(vmName);    
        	OvmVm.stop(_conn, vmName);
        	cleanup(vm);
        	
        	state = State.Stopped;
        	return new StopAnswer(cmd, "success", 0, true);
        } catch (Exception e) {
        	s_logger.debug("Stop " + vmName + "failed", e);
        	return new StopAnswer(cmd, e.getMessage(), false);
        } finally {
        	synchronized(_vms) {
                if (state != null) {
                    _vms.put(vmName, state);
                } else {
                    _vms.remove(vmName);
                }
            }
        }
	}
	
	@Override
	public RebootAnswer execute(RebootCommand cmd) {
		String vmName = cmd.getVmName();
		synchronized(_vms) {
    		_vms.put(vmName, State.Starting);
    	}
		
		try {
			Map<String, String> res = OvmVm.reboot(_conn, vmName);
			Integer vncPort = Integer.parseInt(res.get("vncPort"));
			return new RebootAnswer(cmd, null, vncPort);
		} catch (Exception e) {
			s_logger.debug("Reboot " + vmName + " failed", e);
			return new RebootAnswer(cmd, e.getMessage(), false);
		} finally {
    		synchronized(_vms) {
    			_vms.put(cmd.getVmName(), State.Running);
    		}
		}
	}
	
	private State toState(String vmName, String s) {
		State state = _stateMaps.get(s);
		if (state == null) {
			s_logger.debug("Unkown state " + s + " for " + vmName);
			state = State.Unknown;
		}
		return state;
	}
	
	protected HashMap<String, State> getAllVms() throws XmlRpcException {
		final HashMap<String, State> vmStates = new HashMap<String, State>();
		Map<String, String> vms = OvmHost.getAllVms(_conn);
		for (final Map.Entry<String, String> entry : vms.entrySet()) {
        	State state = toState(entry.getKey(), entry.getValue());
        	vmStates.put(entry.getKey(), state);
        }
		return vmStates;
	}
	
	protected HashMap<String, State> sync() {
        HashMap<String, State> newStates;
        HashMap<String, State> oldStates = null;
        
        try {
			final HashMap<String, State> changes = new HashMap<String, State>();
			newStates = getAllVms();
			if (newStates == null) {
				s_logger.debug("Unable to get the vm states so no state sync at this point.");
				return null;
			}

			synchronized (_vms) {
				oldStates = new HashMap<String, State>(_vms.size());
				oldStates.putAll(_vms);

				for (final Map.Entry<String, State> entry : newStates
						.entrySet()) {
					final String vm = entry.getKey();

					State newState = entry.getValue();
					final State oldState = oldStates.remove(vm);

                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace("VM " + vm + ": ovm has state " + newState + " and we have state " + (oldState != null ? oldState.toString() : "null"));
					}

					/*
					 * TODO: what is migrating ??? if
					 * (vm.startsWith("migrating")) {
					 * s_logger.debug("Migrating from xen detected.  Skipping");
					 * continue; }
					 */

					if (oldState == null) {
						_vms.put(vm, newState);
						s_logger.debug("Detecting a new state but couldn't find a old state so adding it to the changes: "
								+ vm);
						changes.put(vm, newState);
					} else if (oldState == State.Starting) {
						if (newState == State.Running) {
							_vms.put(vm, newState);
						} else if (newState == State.Stopped) {
							s_logger.debug("Ignoring vm " + vm
									+ " because of a lag in starting the vm.");
						}
					} else if (oldState == State.Migrating) {
						if (newState == State.Running) {
							s_logger.debug("Detected that an migrating VM is now running: "
									+ vm);
							_vms.put(vm, newState);
						}
					} else if (oldState == State.Stopping) {
						if (newState == State.Stopped) {
							_vms.put(vm, newState);
						} else if (newState == State.Running) {
							s_logger.debug("Ignoring vm " + vm
									+ " because of a lag in stopping the vm. ");
						}
					} else if (oldState != newState) {
						_vms.put(vm, newState);
						if (newState == State.Stopped) {
							//TODO: need anything here?
						}
						changes.put(vm, newState);
					}
				}

                for (final Map.Entry<String, State> entry : oldStates.entrySet()) {
                    final String vm = entry.getKey();
                    final State oldState = entry.getValue();

                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace("VM " + vm + " is now missing from ovm server so reporting stopped");
                    }

                    if (oldState == State.Stopping) {
                        s_logger.debug("Ignoring VM " + vm + " in transition state stopping.");
                        _vms.remove(vm);
                    } else if (oldState == State.Starting) {
                        s_logger.debug("Ignoring VM " + vm + " in transition state starting.");
                    } else if (oldState == State.Stopped) {
                        _vms.remove(vm);
                    } else if (oldState == State.Migrating) {
                        s_logger.debug("Ignoring VM " + vm + " in migrating state.");
                    } else {
                        _vms.remove(vm);
                        State state = State.Stopped;
                        // TODO: need anything here???
                        changes.put(entry.getKey(), state);
                    }
                }
            }

            return changes;
        } catch (Exception e) {
        	s_logger.debug("Ovm full sync failed", e);
        	return null;
        }
	}
	
	protected GetStorageStatsAnswer execute(final GetStorageStatsCommand cmd) {
		try {
			OvmStoragePool.Details d = OvmStoragePool.getDetailsByUuid(_conn, cmd.getStorageId());
			return new GetStorageStatsAnswer(cmd, d.totalSpace, d.usedSpace);
		} catch (Exception e) {
			s_logger.debug("GetStorageStatsCommand on pool " + cmd.getStorageId() + " failed", e);
			return new GetStorageStatsAnswer(cmd, e.getMessage());
		}
	}
	
	private VmStatsEntry getVmStat(String vmName) throws XmlRpcException {
		Map<String, String> vmStat = OvmVm.getVmStats(_conn, vmName);
		int nvcpus = Integer.parseInt(vmStat.get("cpuNum"));
		float cpuUtil = Float.parseFloat(vmStat.get("cpuUtil"));
		long rxBytes = Long.parseLong(vmStat.get("rxBytes"));
		long txBytes = Long.parseLong(vmStat.get("txBytes"));
		Pair<Long, Long> oldNetworkStat = _vmNetworkStats.get(vmName);
		
		long rx = rxBytes;
		long tx = txBytes;
		if (oldNetworkStat != null) {
			rx -= oldNetworkStat.first();
			tx -= oldNetworkStat.second();
			oldNetworkStat.set(rxBytes, txBytes);
		} else {
			oldNetworkStat = new Pair<Long, Long>(rx, tx);
		}
		_vmNetworkStats.put(vmName, oldNetworkStat);
		
		VmStatsEntry e = new VmStatsEntry();
		e.setCPUUtilization(cpuUtil);
		e.setNumCPUs(nvcpus);
		e.setNetworkReadKBs(rx);
		e.setNetworkWriteKBs(tx);
		e.setEntityType("vm");
		return e;
	}
	
	protected GetVmStatsAnswer execute(GetVmStatsCommand cmd) {
		List<String> vmNames = cmd.getVmNames();
		HashMap<String, VmStatsEntry> vmStatsNameMap = new HashMap<String, VmStatsEntry>();
		for (String vmName : vmNames) {
			try {
				VmStatsEntry e = getVmStat(vmName);
				vmStatsNameMap.put(vmName, e);
			} catch (XmlRpcException e) {
				s_logger.debug("Get vm stat for " + vmName + " failed", e);
				continue;
			}	
		}
		return new GetVmStatsAnswer(cmd, vmStatsNameMap);
	}
	
	protected AttachVolumeAnswer execute(AttachVolumeCommand cmd) {
		return new AttachVolumeAnswer(cmd, "You must stop " + cmd.getVmName() + " first, OVM doesn't support hotplug datadisk");
	}
	
	public Answer execute(DestroyCommand cmd) {
		 try {
			 OvmVolume.destroy(_conn, cmd.getVolume().getPoolUuid(), cmd.getVolume().getPath());
			 return new Answer(cmd, true, "Success");
		 } catch (Exception e) {
			 s_logger.debug("Destroy volume " + cmd.getVolume().getName() + " failed", e);
			 return new Answer(cmd, false, e.getMessage());
		 }
	}
	
    protected PrepareForMigrationAnswer execute(PrepareForMigrationCommand cmd) {
        VirtualMachineTO vm = cmd.getVirtualMachine();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Preparing host for migrating " + vm);
        }
        
        NicTO[] nics = vm.getNics();
        try {
            for (NicTO nic : nics) {
                getNetwork(nic);
            }
            
            synchronized (_vms) {
                _vms.put(vm.getName(), State.Migrating);
            }
            return new PrepareForMigrationAnswer(cmd);
        } catch (Exception e) {
            s_logger.warn("Catch Exception " + e.getClass().getName() + " prepare for migration failed due to " + e.toString(), e);
            return new PrepareForMigrationAnswer(cmd, e);
        }
    }
    
    protected MigrateAnswer execute(final MigrateCommand cmd) {
        final String vmName = cmd.getVmName();
        State state = null;
        
        synchronized (_vms) {
            state = _vms.get(vmName);
            _vms.put(vmName, State.Stopping);
        }
        
        try {
        	OvmVm.Details vm = OvmVm.getDetails(_conn, vmName);
        	String destIp = cmd.getDestinationIp();
        	OvmVm.migrate(_conn, vmName, destIp);
            state = State.Stopping;
            cleanup(vm);
            return new MigrateAnswer(cmd, true, "migration succeeded", null);
        } catch (Exception e) {
            String msg = "Catch Exception " + e.getClass().getName() + ": Migration failed due to " + e.toString();
            s_logger.debug(msg, e);
            return new MigrateAnswer(cmd, false, msg, null);
        } finally {
            synchronized (_vms) {
                _vms.put(vmName, state);
            }
        }
    }
    
    protected CheckVirtualMachineAnswer execute(final CheckVirtualMachineCommand cmd) {
        final String vmName = cmd.getVmName();
        try {
        	Map<String, String> res = OvmVm.register(_conn, vmName);
        	Integer vncPort = Integer.parseInt(res.get("vncPort"));
        	HashMap<String, State> states = getAllVms();
        	State vmState = states.get(vmName);
        	if (vmState == null) {
        		s_logger.warn("Check state of " + vmName + " return null in CheckVirtualMachineCommand");
        		vmState = State.Stopped;
        	}
        	
            if (vmState == State.Running) {
                synchronized (_vms) {
                    _vms.put(vmName, State.Running);
                }
            }
                
            return new CheckVirtualMachineAnswer(cmd, vmState, vncPort);
        } catch (Exception e) {
        	s_logger.debug("Check migration for " + vmName + " failed", e);
        	 return new CheckVirtualMachineAnswer(cmd, State.Stopped, null);
        }
    }
    
	protected MaintainAnswer execute(MaintainCommand cmd) {
		return new MaintainAnswer(cmd);
	}
	
	protected GetVncPortAnswer execute(GetVncPortCommand cmd) {
		try {
			Integer vncPort = OvmVm.getVncPort(_conn, cmd.getName());
			return new GetVncPortAnswer(cmd, _ip, vncPort);
		} catch (Exception e) {
			s_logger.debug("get vnc port for " + cmd.getName() + " failed", e);
			return new GetVncPortAnswer(cmd, e.getMessage());
		}
	}
	
	protected Answer execute(PingTestCommand cmd) {
		try {
			if (cmd.getComputingHostIp() != null) {
				OvmHost.pingAnotherHost(_conn, cmd.getComputingHostIp());
			} else {
				return new Answer(cmd, false, "why asks me to ping router???");
			}
			
			return new Answer(cmd, true, "success");
		} catch (Exception e) {
			s_logger.debug("Ping " + cmd.getComputingHostIp() + " failed", e);
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	protected FenceAnswer execute(FenceCommand cmd) {
		try {
			Boolean res = OvmHost.fence(_conn, cmd.getHostIp());
			return new FenceAnswer(cmd, res, res.toString());
		} catch (Exception e) {
			s_logger.debug("fence " + cmd.getHostIp() + " failed", e);
			return new FenceAnswer(cmd, false, e.getMessage());
		}
	}
	
	protected Answer execute(AttachIsoCommand cmd) {
		try {
			URI iso = new URI(cmd.getIsoPath());
			String isoPath = iso.getHost() + ":" + iso.getPath();
			OvmVm.detachOrAttachIso(_conn, cmd.getVmName(), isoPath, cmd.isAttach());
			return new Answer(cmd);
		} catch (Exception e) {
			s_logger.debug("Attach or detach ISO " + cmd.getIsoPath() + " for " + cmd.getVmName() + " attach:" + cmd.isAttach() + " failed", e);
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	private Answer execute(SecurityGroupRulesCmd cmd) {
        boolean result = false;        
        try {
        	OvmVif.Details vif = getVifFromVm(cmd.getVmName(), null);
    	    String vifDeviceName = vif.name;
            String bridgeName = vif.bridge;
        	result = addNetworkRules(cmd.getVmName(), Long.toString(cmd.getVmId()), cmd.getGuestIp(), cmd.getSignature(), String.valueOf(cmd.getSeqNum()), cmd.getGuestMac(), cmd.stringifyRules(), vifDeviceName, bridgeName);
        } catch (XmlRpcException e) {
        	s_logger.error(e);
        	result = false;
        }

        if (!result) {
            s_logger.warn("Failed to program network rules for vm " + cmd.getVmName());
            return new SecurityGroupRuleAnswer(cmd, false, "programming network rules failed");
        } else {
            s_logger.info("Programmed network rules for vm " + cmd.getVmName() + " guestIp=" + cmd.getGuestIp() + ":ingress num rules=" + cmd.getIngressRuleSet().length + ":egress num rules=" + cmd.getEgressRuleSet().length);
            return new SecurityGroupRuleAnswer(cmd);
        }	    
    }	
	
	private Answer execute(CleanupNetworkRulesCmd cmd) {		
	    boolean result = false;
	    try {
	    	result =  cleanupNetworkRules();
	    } catch (XmlRpcException e) {
	    	s_logger.error(e);
	    	result = false;
	    }
	    
	    if (result) {
	        return new Answer(cmd);
	    } else {
	        return new Answer(cmd, result, "Failed to cleanup network rules.");
	    }
    }
	
    protected boolean defaultNetworkRulesForUserVm(String vmName, Long vmId, NicTO nic) throws XmlRpcException {
        if (!_canBridgeFirewall) {
            return false;
        }
        
        OvmVif.Details vif = getVifFromVm(vmName, nic.getDeviceId());
        String ipAddress = nic.getIp();
        String macAddress = vif.mac;
        String vifName = vif.name;
        String bridgeName = vif.bridge;

        return OvmSecurityGroup.defaultNetworkRulesForUserVm(_conn, vmName, String.valueOf(vmId), ipAddress, macAddress, vifName, bridgeName);
    }
    
    protected boolean deleteAllNetworkRulesForVm(String vmName) throws XmlRpcException {
        if (!_canBridgeFirewall) {
            return false;
        }
        
        String vif = getVifFromVm(vmName, null).name;
        return OvmSecurityGroup.deleteAllNetworkRulesForVm(_conn, vmName, vif);
    }
    
    protected boolean addNetworkRules(String vmName, String vmId, String guestIp, String signature, String seqno, String vifMacAddress, String rules, String vifDeviceName, String bridgeName) throws XmlRpcException {
        if (!_canBridgeFirewall) {
            return false;
        }
                
        String newRules = rules.replace(" ", ";");        
        return OvmSecurityGroup.addNetworkRules(_conn, vmName, vmId, guestIp, signature, seqno, vifMacAddress, newRules, vifDeviceName, bridgeName);
    }
    
    protected boolean cleanupNetworkRules() throws XmlRpcException {
        if (!_canBridgeFirewall) {
            return false;
        }
        
        return OvmSecurityGroup.cleanupNetworkRules(_conn);
    }
	
	protected boolean canBridgeFirewall() throws XmlRpcException {
	    return OvmSecurityGroup.canBridgeFirewall(_conn);
	}
	
	protected OvmVif.Details getVifFromVm(String vmName, Integer deviceId) throws XmlRpcException {
		String vif = null;
        List<OvmVif.Details> vifs = null;
        
        try {
            vifs = getInterfaces(vmName);   
        } catch (XmlRpcException e) {
            s_logger.error("Failed to get VIFs for VM " + vmName, e);
           	throw e;
        }
        
        if (deviceId != null && vifs.size() > deviceId) {
        	return vifs.get(deviceId);
        } else if (deviceId == null && vifs.size() > 0) {
        	return vifs.get(0);
        } else {
        	return null;
        }
	}
	
	protected  List<OvmVif.Details> getInterfaces(String vmName) throws XmlRpcException {
	    OvmVm.Details vmDetails = OvmVm.getDetails(_conn, vmName);
	    return vmDetails.vifs;
    }
	
	protected Answer execute(PrepareOCFS2NodesCommand cmd) {
	    List<Ternary<Integer, String, String>> nodes = cmd.getNodes();
	    StringBuffer params = new StringBuffer();
	    for (Ternary<Integer, String, String> node : nodes) {
	        String param = String.format("%1$s:%2$s:%3$s", node.first(), node.second(), node.third());
	        params.append(param);
	        params.append(";");
	    }
	    
	    try {
            OvmStoragePool.prepareOCFS2Nodes(_conn, cmd.getClusterName(), params.toString());
            return new Answer(cmd, true, "Success");
        } catch (XmlRpcException e) {
            s_logger.debug("OCFS2 prepare nodes failed", e);
            return new Answer(cmd, false, e.getMessage());
        }
	}
	
	protected CreatePrivateTemplateAnswer execute(final CreatePrivateTemplateFromVolumeCommand cmd) {
        String secondaryStorageUrl = cmd.getSecondaryStorageUrl();
		String volumePath = cmd.getVolumePath();
		Long accountId = cmd.getAccountId();
		Long templateId = cmd.getTemplateId();
		int wait = cmd.getWait();
		if (wait == 0) {
			/* Defaut timeout 2 hours */
			wait = 7200;
		}
		 
		try {
			URI uri;
            uri = new URI(secondaryStorageUrl);
			String secondaryStorageMountPath = uri.getHost() + ":" + uri.getPath();
			String installPath = "template/tmpl/" + accountId + "/" + templateId;
			Map<String, String> res = OvmStoragePool.createTemplateFromVolume(_conn, secondaryStorageMountPath, installPath, volumePath, wait);
			return new CreatePrivateTemplateAnswer(cmd, true, null, res.get("installPath"), Long.valueOf(res.get("virtualSize")), Long.valueOf(res.get("physicalSize")), res.get("templateFileName"), ImageFormat.RAW);
		} catch (Exception e) {
			s_logger.debug("Create template failed", e);
			return new CreatePrivateTemplateAnswer(cmd, false, e.getMessage());
		}
	}
	
	protected CopyVolumeAnswer execute(CopyVolumeCommand cmd) {
	    String volumePath = cmd.getVolumePath();
	    String secondaryStorageURL = cmd.getSecondaryStorageURL();
	    int wait = cmd.getWait();
	    if (wait == 0) {
	    	wait = 7200;
	    }
	    
	    try {
	    	URI uri = new URI(secondaryStorageURL);
	    	String secStorageMountPath = uri.getHost() + ":" + uri.getPath();
	    	String volumeFolderOnSecStorage = "volumes/" + String.valueOf(cmd.getVolumeId());
	    	String storagePoolUuid = cmd.getPool().getUuid();
	    	Boolean toSec = cmd.toSecondaryStorage();
	    	String res = OvmStoragePool.copyVolume(_conn, secStorageMountPath, volumeFolderOnSecStorage, volumePath, storagePoolUuid, toSec, wait);
	    	return new CopyVolumeAnswer(cmd, true, null, null, res);
	    } catch (Exception e) {
	    	s_logger.debug("Copy volume failed", e);
	    	return new CopyVolumeAnswer(cmd, false, e.getMessage(), null, null);
	    }
	}
	
	protected Answer execute(DeleteStoragePoolCommand cmd) {
		try {
			OvmStoragePool.delete(_conn, cmd.getPool().getUuid());
		} catch (Exception e) {
			s_logger.debug("Delete storage pool on host " + _ip + " failed, however, we leave to user for cleanup and tell managment server it succeeded", e);
		}
		
		return new Answer(cmd);
	}
	
	protected CheckNetworkAnswer execute(CheckNetworkCommand cmd) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Checking if network name setup is done on the resource");
        }

		List<PhysicalNetworkSetupInfo> infoList = cmd.getPhysicalNetworkInfoList();

		boolean errorout = false;
		String msg = "";
		for (PhysicalNetworkSetupInfo info : infoList) {
			if (!isNetworkSetupByName(info.getGuestNetworkName())) {
				msg = "For Physical Network id:" + info.getPhysicalNetworkId() + ", Guest Network is not configured on the backend by name "
				        + info.getGuestNetworkName();
				errorout = true;
				break;
			}
			if (!isNetworkSetupByName(info.getPrivateNetworkName())) {
				msg = "For Physical Network id:" + info.getPhysicalNetworkId() + ", Private Network is not configured on the backend by name "
				        + info.getPrivateNetworkName();
				errorout = true;
				break;
			}
			if (!isNetworkSetupByName(info.getPublicNetworkName())) {
				msg = "For Physical Network id:" + info.getPhysicalNetworkId() + ", Public Network is not configured on the backend by name "
				        + info.getPublicNetworkName();
				errorout = true;
				break;
			}
		}
		
		if (errorout) {
			s_logger.error(msg);
			return new CheckNetworkAnswer(cmd, false, msg);
		} else {
			return new CheckNetworkAnswer(cmd, true, "Network Setup check by names is done");
		}
    }

    private boolean isNetworkSetupByName(String nameTag) {
        if (nameTag != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Looking for network setup by name " + nameTag);
            }
            return _bridges.contains(nameTag);
        }
        return true;
    }    
    
	@Override
	public Answer executeRequest(Command cmd) {
		Class<? extends Command> clazz = cmd.getClass();
		if (clazz == ReadyCommand.class) {
			return execute((ReadyCommand)cmd);
		} else if (clazz == CreateStoragePoolCommand.class) {
			return execute((CreateStoragePoolCommand)cmd);
		} else if (clazz == ModifyStoragePoolCommand.class) {
			return execute((ModifyStoragePoolCommand)cmd);
		} else if (clazz == PrimaryStorageDownloadCommand.class) {
			return execute((PrimaryStorageDownloadCommand)cmd);
		} else if (clazz == CreateCommand.class) {
			return execute((CreateCommand)cmd);
		} else if (clazz == GetHostStatsCommand.class) {
			return execute((GetHostStatsCommand)cmd);
		} else if (clazz == StopCommand.class) {
			return execute((StopCommand)cmd);
		} else if (clazz == RebootCommand.class) {
			return execute((RebootCommand)cmd);
		} else if (clazz == GetStorageStatsCommand.class) { 
			return execute((GetStorageStatsCommand)cmd);
		} else if (clazz == GetVmStatsCommand.class) {
			return execute((GetVmStatsCommand)cmd);
		} else if (clazz == AttachVolumeCommand.class) {
			return execute((AttachVolumeCommand)cmd);
		} else if (clazz == DestroyCommand.class) {
			return execute((DestroyCommand)cmd);
		} else if (clazz == PrepareForMigrationCommand.class) {
			return execute((PrepareForMigrationCommand)cmd);
		} else if (clazz == MigrateCommand.class) {
			return execute((MigrateCommand)cmd);
		} else if (clazz == CheckVirtualMachineCommand.class) {
			return execute((CheckVirtualMachineCommand)cmd);
		} else if (clazz == MaintainCommand.class) {
			return execute((MaintainCommand)cmd);
		} else if (clazz == StartCommand.class) {
			return execute((StartCommand)cmd);
		} else if (clazz == GetVncPortCommand.class) {
			return execute((GetVncPortCommand)cmd);
		} else if (clazz == PingTestCommand.class) {
			return execute((PingTestCommand)cmd);
		} else if (clazz == FenceCommand.class) {
			return execute((FenceCommand)cmd);
		} else if (clazz == AttachIsoCommand.class) {
			return execute((AttachIsoCommand)cmd);
		} else if (clazz == SecurityGroupRulesCmd.class) {
		    return execute((SecurityGroupRulesCmd) cmd);
		} else if (clazz == CleanupNetworkRulesCmd.class) {
		    return execute((CleanupNetworkRulesCmd) cmd);
		} else if (clazz == PrepareOCFS2NodesCommand.class) {
		    return execute((PrepareOCFS2NodesCommand)cmd);
		} else if (clazz == CreatePrivateTemplateFromVolumeCommand.class) {
			return execute((CreatePrivateTemplateFromVolumeCommand)cmd);
		} else if (clazz == CopyVolumeCommand.class) {
			return execute((CopyVolumeCommand)cmd);
		} else if (clazz == DeleteStoragePoolCommand.class) {
			return execute((DeleteStoragePoolCommand)cmd);
		} else if (clazz == CheckNetworkCommand.class) {
			return execute((CheckNetworkCommand)cmd);
		} else {
			return Answer.createUnsupportedCommandAnswer(cmd);
		}
	}

	@Override
	public void disconnected() {
		// TODO Auto-generated method stub

	}

	@Override
	public IAgentControl getAgentControl() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAgentControl(IAgentControl agentControl) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setConfigParams(Map<String, Object> params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, Object> getConfigParams() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getRunLevel() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setRunLevel(int level) {
		// TODO Auto-generated method stub
		
	}

}
