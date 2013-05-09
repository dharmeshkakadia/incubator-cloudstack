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
package org.apache.network.guru;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.configuration.ConfigurationManager;
import org.apache.dc.DataCenter;
import org.apache.dc.DataCenter.NetworkType;
import org.apache.deploy.DeployDestination;
import org.apache.deploy.DeploymentPlan;
import org.apache.exception.InsufficientAddressCapacityException;
import org.apache.exception.InsufficientVirtualNetworkCapcityException;
import org.apache.exception.InvalidParameterValueException;
import org.apache.log4j.Logger;
import org.apache.network.Network;
import org.apache.network.NetworkModel;
import org.apache.network.NetworkProfile;
import org.apache.network.Network.GuestType;
import org.apache.network.Network.State;
import org.apache.network.Networks.AddressFormat;
import org.apache.network.Networks.BroadcastDomainType;
import org.apache.network.Networks.IsolationType;
import org.apache.network.Networks.Mode;
import org.apache.network.Networks.TrafficType;
import org.apache.network.dao.NetworkVO;
import org.apache.network.guru.NetworkGuru;
import org.apache.network.vpc.PrivateIpAddress;
import org.apache.network.vpc.PrivateIpVO;
import org.apache.network.vpc.dao.PrivateIpDao;
import org.apache.offering.NetworkOffering;
import org.apache.user.Account;
import org.apache.utils.component.AdapterBase;
import org.apache.utils.exception.CloudRuntimeException;
import org.apache.utils.net.NetUtils;
import org.apache.vm.NicProfile;
import org.apache.vm.ReservationContext;
import org.apache.vm.VirtualMachine;
import org.apache.vm.VirtualMachineProfile;
import org.apache.vm.Nic.ReservationStrategy;


@Local(value = NetworkGuru.class)
public class PrivateNetworkGuru extends AdapterBase implements NetworkGuru {
    private static final Logger s_logger = Logger.getLogger(PrivateNetworkGuru.class);
    @Inject
    protected ConfigurationManager _configMgr;
    @Inject
    protected PrivateIpDao _privateIpDao;
    @Inject
    protected NetworkModel _networkMgr;
    
    private static final TrafficType[] _trafficTypes = {TrafficType.Guest};

    protected PrivateNetworkGuru() {
        super();
    }

    @Override
    public boolean isMyTrafficType(TrafficType type) {
        for (TrafficType t : _trafficTypes) {
            if (t == type) {
                return true;
            }
        }
        return false;
    }

    @Override
    public TrafficType[] getSupportedTrafficType() {
        return _trafficTypes;
    }

    protected boolean canHandle(NetworkOffering offering, DataCenter dc) {
        // This guru handles only system Guest network
        if (dc.getNetworkType() == NetworkType.Advanced && isMyTrafficType(offering.getTrafficType()) 
                && offering.getGuestType() == Network.GuestType.Isolated && offering.isSystemOnly()) {
            return true;
        } else {
            s_logger.trace("We only take care of system Guest networks of type   " + GuestType.Isolated + " in zone of type "
                    + NetworkType.Advanced);
            return false;
        }
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {
        DataCenter dc = _configMgr.getZone(plan.getDataCenterId());
        if (!canHandle(offering, dc)) {
            return null;
        }

        NetworkVO network = new NetworkVO(offering.getTrafficType(), Mode.Static, BroadcastDomainType.Vlan, offering.getId(),
                State.Allocated, plan.getDataCenterId(), plan.getPhysicalNetworkId());
        if (userSpecified != null) {
            if ((userSpecified.getCidr() == null && userSpecified.getGateway() != null) || 
                    (userSpecified.getCidr() != null && userSpecified.getGateway() == null)) {
                throw new InvalidParameterValueException("cidr and gateway must be specified together.");
            }

            if (userSpecified.getCidr() != null) {
                network.setCidr(userSpecified.getCidr());
                network.setGateway(userSpecified.getGateway());
            } else {
                throw new InvalidParameterValueException("Can't design network " + network + "; netmask/gateway must be passed in");
            }

            if (offering.getSpecifyVlan()) {
                network.setBroadcastUri(userSpecified.getBroadcastUri());
                network.setState(State.Setup);
            }
        } else {
            throw new CloudRuntimeException("Can't design network " + network + "; netmask/gateway must be passed in");
           
        }

        return network;
    }

    @Override
    public void deallocate(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Deallocate network: networkId: " + nic.getNetworkId() + ", ip: " + nic.getIp4Address());
        }
        
        PrivateIpVO ip = _privateIpDao.findByIpAndSourceNetworkId(nic.getNetworkId(), nic.getIp4Address());
        if (ip != null) {
            _privateIpDao.releaseIpAddress(nic.getIp4Address(), nic.getNetworkId());
        }
        nic.deallocate();
    }
    
    
    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination dest, 
            ReservationContext context) throws InsufficientVirtualNetworkCapcityException {
        
        return network;
    }

    @Override
    public NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm)
            throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException {
        DataCenter dc = _configMgr.getZone(network.getDataCenterId());
        NetworkOffering offering = _configMgr.getNetworkOffering(network.getNetworkOfferingId());
        if (!canHandle(offering, dc)) {
            return null;
        }
        
        if (nic == null) {
            nic = new NicProfile(ReservationStrategy.Create, null, null, null, null);
        }
        
        getIp(nic, dc, network);

        if (nic.getIp4Address() == null) {
            nic.setStrategy(ReservationStrategy.Start);
        } else {
            nic.setStrategy(ReservationStrategy.Create);
        }

        return nic;
    }
    
    
    protected void getIp(NicProfile nic, DataCenter dc, Network network)
            throws InsufficientVirtualNetworkCapcityException,InsufficientAddressCapacityException {
        if (nic.getIp4Address() == null) {
            PrivateIpVO ipVO = _privateIpDao.allocateIpAddress(network.getDataCenterId(), network.getId(), null);
            String vlanTag = network.getBroadcastUri().getHost();
            String netmask = NetUtils.getCidrNetmask(network.getCidr());
            PrivateIpAddress ip = new PrivateIpAddress(ipVO, vlanTag, network.getGateway(), netmask, 
                    NetUtils.long2Mac(NetUtils.createSequenceBasedMacAddress(ipVO.getMacAddress())));

            nic.setIp4Address(ip.getIpAddress());
            nic.setGateway(ip.getGateway());
            nic.setNetmask(ip.getNetmask());
            nic.setIsolationUri(IsolationType.Vlan.toUri(ip.getVlanTag()));
            nic.setBroadcastUri(IsolationType.Vlan.toUri(ip.getVlanTag()));
            nic.setBroadcastType(BroadcastDomainType.Vlan);
            nic.setFormat(AddressFormat.Ip4);
            nic.setReservationId(String.valueOf(ip.getVlanTag()));
            nic.setMacAddress(ip.getMacAddress());
        }

        nic.setDns1(dc.getDns1());
        nic.setDns2(dc.getDns2());
    }
    

    @Override
    public void updateNicProfile(NicProfile profile, Network network) {
        DataCenter dc = _configMgr.getZone(network.getDataCenterId());
        if (profile != null) {
            profile.setDns1(dc.getDns1());
            profile.setDns2(dc.getDns2());
        }
    }

    @Override
    public void reserve(NicProfile nic, Network network, VirtualMachineProfile<? extends VirtualMachine> vm,
            DeployDestination dest, ReservationContext context)
            throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException {
        if (nic.getIp4Address() == null) {
            getIp(nic, _configMgr.getZone(network.getDataCenterId()), network);
            nic.setStrategy(ReservationStrategy.Create);
        }
    }

    @Override
    public boolean release(NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, String reservationId) {
        return true;
    }

    @Override
    public void shutdown(NetworkProfile profile, NetworkOffering offering) {
        
    }

    @Override
    public boolean trash(Network network, NetworkOffering offering, Account owner) {
        return true;
    }

    @Override
    public void updateNetworkProfile(NetworkProfile networkProfile) {
        DataCenter dc = _configMgr.getZone(networkProfile.getDataCenterId());
        networkProfile.setDns1(dc.getDns1());
        networkProfile.setDns2(dc.getDns2());
    }
}
