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
package org.apache.network;


import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.api.command.admin.network.DedicateGuestVlanRangeCmd;
import org.apache.cloudstack.api.command.admin.network.ListDedicatedGuestVlanRangesCmd;
import org.apache.cloudstack.api.command.admin.usage.ListTrafficTypeImplementorsCmd;
import org.apache.cloudstack.api.command.user.network.CreateNetworkCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworksCmd;
import org.apache.cloudstack.api.command.user.network.RestartNetworkCmd;
import org.apache.cloudstack.api.command.user.vm.ListNicsCmd;
import org.apache.dc.DataCenter;
import org.apache.dc.Pod;
import org.apache.dc.Vlan.VlanType;
import org.apache.deploy.DataCenterDeployment;
import org.apache.deploy.DeployDestination;
import org.apache.deploy.DeploymentPlan;
import org.apache.exception.*;
import org.apache.network.GuestVlan;
import org.apache.network.IpAddress;
import org.apache.network.Network;
import org.apache.network.NetworkManager;
import org.apache.network.NetworkProfile;
import org.apache.network.NetworkRuleApplier;
import org.apache.network.NetworkService;
import org.apache.network.PhysicalNetwork;
import org.apache.network.PhysicalNetworkServiceProvider;
import org.apache.network.PhysicalNetworkTrafficType;
import org.apache.network.PublicIpAddress;
import org.apache.network.Network.Provider;
import org.apache.network.Network.Service;
import org.apache.network.Networks.TrafficType;
import org.apache.network.addr.PublicIp;
import org.apache.network.dao.IPAddressVO;
import org.apache.network.dao.NetworkVO;
import org.apache.network.element.LoadBalancingServiceProvider;
import org.apache.network.element.StaticNatServiceProvider;
import org.apache.network.element.UserDataServiceProvider;
import org.apache.network.guru.NetworkGuru;
import org.apache.network.rules.FirewallRule;
import org.apache.network.rules.StaticNat;
import org.apache.network.rules.FirewallRule.Purpose;
import org.apache.network.rules.FirewallRule.State;
import org.apache.offering.NetworkOffering;
import org.apache.offerings.NetworkOfferingVO;
import org.apache.user.Account;
import org.apache.user.User;
import org.apache.utils.Pair;
import org.apache.utils.component.ManagerBase;
import org.apache.vm.*;
import org.apache.vm.VirtualMachine.Type;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import javax.naming.ConfigurationException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Local(value = { NetworkManager.class, NetworkService.class })
public class MockNetworkManagerImpl extends ManagerBase implements NetworkManager, NetworkService {

    @Override
    public List<? extends Network> getIsolatedNetworksOwnedByAccountInZone(long zoneId, Account owner) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IPAddressVO associateIPToGuestNetwork(long ipId, long networkId, boolean releaseOnFailure) throws ResourceAllocationException, InsufficientAddressCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean releaseIpAddress(long ipAddressId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Network createGuestNetwork(CreateNetworkCmd cmd) throws InsufficientCapacityException, ConcurrentOperationException, ResourceAllocationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<? extends Network> searchForNetworks(ListNetworksCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean deleteNetwork(long networkId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getActiveNicsInNetwork(long networkId) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Network getNetwork(long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Network getNetwork(String networkUuid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IpAddress getIp(long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NetworkProfile convertNetworkToNetworkProfile(long networkId) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public boolean disassociatePublicIpAddress(long id, long userId, Account caller) {
        // TODO Auto-generated method stub
        return false;
    }



    @Override
    public List<NetworkVO> setupNetwork(Account owner, NetworkOffering offering, DeploymentPlan plan, String name, String displayText, boolean isDefault)
            throws ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NetworkVO> setupNetwork(Account owner, NetworkOffering offering, Network predefined, DeploymentPlan plan, String name, String displayText, boolean errorIfAlreadySetup, Long domainId,
            ACLType aclType, Boolean subdomainAccess, Long vpcId) throws ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }



    @Override
    public void allocate(VirtualMachineProfile<? extends VMInstanceVO> vm, List<Pair<NetworkVO, NicProfile>> networks) throws InsufficientCapacityException, ConcurrentOperationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void prepare(VirtualMachineProfile<? extends VMInstanceVO> profile, DeployDestination dest, ReservationContext context) throws InsufficientCapacityException, ConcurrentOperationException,
    ResourceUnavailableException {
        // TODO Auto-generated method stub

    }

    @Override
    public void release(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, boolean forced) {
        // TODO Auto-generated method stub

    }

    @Override
    public void cleanupNics(VirtualMachineProfile<? extends VMInstanceVO> vm) {
        // TODO Auto-generated method stub

    }

    @Override
    public void expungeNics(VirtualMachineProfile<? extends VMInstanceVO> vm) {
        // TODO Auto-generated method stub

    }


    @Override
    public List<NicProfile> getNicProfiles(VirtualMachine vm) {
        // TODO Auto-generated method stub
        return null;
    }





    @Override
    public Pair<NetworkGuru, NetworkVO> implementNetwork(long networkId, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException,
    InsufficientCapacityException {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public <T extends VMInstanceVO> void prepareNicForMigration(VirtualMachineProfile<T> vm, DeployDestination dest) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean destroyNetwork(long networkId, ReservationContext context) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Network createGuestNetwork(long networkOfferingId, String name, String displayText, String gateway, String cidr, String vlanId, String networkDomain, Account owner, Long domainId,
            PhysicalNetwork physicalNetwork, long zoneId, ACLType aclType, Boolean subdomainAccess, Long vpcId, String gatewayv6, String cidrv6) throws ConcurrentOperationException, InsufficientCapacityException, ResourceAllocationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean associateIpAddressListToAccount(long userId, long accountId, long zoneId, Long vlanId, Network networkToAssociateWith) throws InsufficientCapacityException,
    ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }





    @Override
    public boolean applyIpAssociations(Network network, boolean continueOnError) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }



    @Override
    public boolean startNetwork(long networkId, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public IPAddressVO markIpAsUnavailable(long addrId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String acquireGuestIpAddress(Network network, String requestedIp) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public boolean applyStaticNats(List<? extends StaticNat> staticNats, boolean continueOnError) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }



    public Map<Service, Set<Provider>> getNetworkOfferingServiceProvidersMap(long networkOfferingId) {
        return null;
    }





    @Override
    public PhysicalNetwork createPhysicalNetwork(Long zoneId, String vnetRange, String networkSpeed, List<String> isolationMethods, String broadcastDomainRange, Long domainId, List<String> tags, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pair<List<? extends PhysicalNetwork>, Integer> searchPhysicalNetworks(Long id, Long zoneId, String keyword, Long startIndex, Long pageSize, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PhysicalNetwork updatePhysicalNetwork(Long id, String networkSpeed, List<String> tags, String newVnetRangeString, String state, String removeVlan) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean deletePhysicalNetwork(Long id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public GuestVlan dedicateGuestVlanRange(DedicateGuestVlanRangeCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pair<List<? extends GuestVlan>, Integer> listDedicatedGuestVlanRanges(ListDedicatedGuestVlanRangesCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean releaseDedicatedGuestVlanRange(Long dedicatedGuestVlanRangeId) {
        // TODO Auto-generated method stub
        return true;
    }

        @Override
    public List<? extends Service> listNetworkServices(String providerName) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public PhysicalNetworkServiceProvider addProviderToPhysicalNetwork(Long physicalNetworkId, String providerName, Long destinationPhysicalNetworkId, List<String> enabledServices) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pair<List<? extends PhysicalNetworkServiceProvider>, Integer> listNetworkServiceProviders(Long physicalNetworkId, String name, String state, Long startIndex, Long pageSize) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean deleteNetworkServiceProvider(Long id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public PhysicalNetwork getPhysicalNetwork(Long physicalNetworkId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PhysicalNetwork getCreatedPhysicalNetwork(Long physicalNetworkId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PhysicalNetworkServiceProvider getPhysicalNetworkServiceProvider(Long providerId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PhysicalNetworkServiceProvider getCreatedPhysicalNetworkServiceProvider(Long providerId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long findPhysicalNetworkId(long zoneId, String tag, TrafficType trafficType) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public PhysicalNetworkTrafficType getPhysicalNetworkTrafficType(Long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PhysicalNetworkTrafficType updatePhysicalNetworkTrafficType(Long id, String xenLabel, String kvmLabel, String vmwareLabel) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean deletePhysicalNetworkTrafficType(Long id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Pair<List<? extends PhysicalNetworkTrafficType>, Integer> listTrafficTypes(Long physicalNetworkId) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public Network getExclusiveGuestNetwork(long zoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserDataServiceProvider getPasswordResetProvider(Network network) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserDataServiceProvider getSSHKeyResetProvider(Network network) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PhysicalNetworkServiceProvider updateNetworkServiceProvider(Long id, String state, List<String> enabledServices) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PhysicalNetworkTrafficType addTrafficTypeToPhysicalNetwork(Long physicalNetworkId, String trafficType, String xenLabel, String kvmLabel, String vmwareLabel, String simulatorLabel, String vlan) {
        // TODO Auto-generated method stub
        return null;
    }


    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#applyRules(java.util.List, org.apache.network.rules.FirewallRule.Purpose, org.apache.network.NetworkRuleApplier, boolean)
     */
    @Override
    public boolean applyRules(List<? extends FirewallRule> rules, Purpose purpose, NetworkRuleApplier applier,
            boolean continueOnError) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#applyIpAssociations(org.apache.network.Network, boolean, boolean, java.util.List)
     */
    @Override
    public boolean applyIpAssociations(Network network, boolean rulesRevoked, boolean continueOnError,
            List<? extends PublicIpAddress> publicIps) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#cleanupIpResources(long, long, org.apache.user.Account)
     */
    @Override
    public boolean cleanupIpResources(long addrId, long userId, Account caller) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#restartNetwork(java.lang.Long, org.apache.user.Account, org.apache.user.User, boolean)
     */
    @Override
    public boolean restartNetwork(Long networkId, Account callerAccount, User callerUser, boolean cleanup)
            throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#shutdownNetworkElementsAndResources(org.apache.vm.ReservationContext, boolean, org.apache.network.NetworkVO)
     */
    @Override
    public boolean shutdownNetworkElementsAndResources(ReservationContext context, boolean b, NetworkVO network) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#implementNetworkElementsAndResources(org.apache.deploy.DeployDestination, org.apache.vm.ReservationContext, org.apache.network.NetworkVO, org.apache.offerings.NetworkOfferingVO)
     */
    @Override
    public void implementNetworkElementsAndResources(DeployDestination dest, ReservationContext context,
            NetworkVO network, NetworkOfferingVO findById) throws ConcurrentOperationException,
            InsufficientAddressCapacityException, ResourceUnavailableException, InsufficientCapacityException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#allocateIp(org.apache.user.Account, boolean, org.apache.user.Account, org.apache.dc.DataCenter)
     */
    @Override
    public IpAddress allocateIp(Account ipOwner, boolean isSystem, Account caller, long callerId, DataCenter zone)
            throws ConcurrentOperationException, ResourceAllocationException, InsufficientAddressCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#finalizeServicesAndProvidersForNetwork(org.apache.offering.NetworkOffering, java.lang.Long)
     */
    @Override
    public Map<String, String> finalizeServicesAndProvidersForNetwork(NetworkOffering offering, Long physicalNetworkId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#getNicProfileForVm(org.apache.network.Network, org.apache.vm.NicProfile, org.apache.vm.VirtualMachine)
     */
    protected NicProfile getNicProfileForVm(Network network, NicProfile requested, VirtualMachine vm) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#getOnePhysicalNetworkByZoneAndTrafficType(long, org.apache.network.Networks.TrafficType)
     */
    protected PhysicalNetwork getOnePhysicalNetworkByZoneAndTrafficType(long zoneId, TrafficType trafficType) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#getZoneNetworkDomain(long)
     */
    protected String getZoneNetworkDomain(long zoneId) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#getNicInNetworkIncludingRemoved(long, long)
     */
    protected Nic getNicInNetworkIncludingRemoved(long vmId, long networkId) {
        return null;
    }


    /* (non-Javadoc)
     * @see org.apache.network.NetworkService#restartNetwork(org.apache.api.commands.RestartNetworkCmd, boolean)
     */
    @Override
    public boolean restartNetwork(RestartNetworkCmd cmd, boolean cleanup) throws ConcurrentOperationException,
    ResourceUnavailableException, InsufficientCapacityException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkService#updateGuestNetwork(long, java.lang.String, java.lang.String, org.apache.user.Account, org.apache.user.User, java.lang.String, java.lang.Long, java.lang.Boolean)
     */
    @Override
    public Network updateGuestNetwork(long networkId, String name, String displayText, Account callerAccount,
            User callerUser, String domainSuffix, Long networkOfferingId, Boolean changeCidr, String guestVmCidr) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkService#listTrafficTypeImplementor(org.apache.api.commands.ListTrafficTypeImplementorsCmd)
     */
    @Override
    public List<Pair<TrafficType, String>> listTrafficTypeImplementor(ListTrafficTypeImplementorsCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkService#getIsolatedNetworksWithSourceNATOwnedByAccountInZone(long, org.apache.user.Account)
     */
    @Override
    public List<? extends Network> getIsolatedNetworksWithSourceNATOwnedByAccountInZone(long zoneId, Account owner) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkService#associateIPToNetwork(long, long)
     */
    @Override
    public IpAddress associateIPToNetwork(long ipId, long networkId) throws InsufficientAddressCapacityException,
    ResourceAllocationException, ResourceUnavailableException, ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkService#createPrivateNetwork(java.lang.String, java.lang.String, long, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, long, java.lang.Long)
     */
    @Override
    public Network createPrivateNetwork(String networkName, String displayText, long physicalNetworkId, String vlan,
                                        String startIp, String endIP, String gateway, String netmask, long networkOwnerId, Long vpcId, Boolean sourceNat)
                    throws ResourceAllocationException, ConcurrentOperationException, InsufficientCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#assignPublicIpAddress(long, java.lang.Long, org.apache.user.Account, org.apache.dc.Vlan.VlanType, java.lang.Long, java.lang.String, boolean)
     */
    @Override
    public PublicIp assignPublicIpAddress(long dcId, Long podId, Account owner, VlanType type, Long networkId,
            String requestedIp, boolean isSystem) throws InsufficientAddressCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#shutdownNetwork(long, org.apache.vm.ReservationContext, boolean)
     */
    @Override
    public boolean shutdownNetwork(long networkId, ReservationContext context, boolean cleanupElements) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#reallocate(org.apache.vm.VirtualMachineProfile, org.apache.deploy.DataCenterDeployment)
     */
    @Override
    public boolean reallocate(VirtualMachineProfile<? extends VMInstanceVO> vm, DataCenterDeployment dest)
            throws InsufficientCapacityException, ConcurrentOperationException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#assignSystemIp(long, org.apache.user.Account, boolean, boolean)
     */
    @Override
    public IpAddress assignSystemIp(long networkId, Account owner, boolean forElasticLb, boolean forElasticIp)
            throws InsufficientAddressCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#handleSystemIpRelease(org.apache.network.IpAddress)
     */
    @Override
    public boolean handleSystemIpRelease(IpAddress ip) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#allocateDirectIp(org.apache.vm.NicProfile, org.apache.dc.DataCenter, org.apache.vm.VirtualMachineProfile, org.apache.network.Network, java.lang.String)
     */
    @Override
    public void allocateDirectIp(NicProfile nic, DataCenter dc, VirtualMachineProfile<? extends VirtualMachine> vm,
            Network network, String requestedIpv4, String requestedIpv6) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#assignSourceNatIpAddressToGuestNetwork(org.apache.user.Account, org.apache.network.Network)
     */
    @Override
    public PublicIp assignSourceNatIpAddressToGuestNetwork(Account owner, Network guestNetwork)
            throws InsufficientAddressCapacityException, ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#allocateNic(org.apache.vm.NicProfile, org.apache.network.Network, java.lang.Boolean, int, org.apache.vm.VirtualMachineProfile)
     */
    @Override
    public Pair<NicProfile, Integer> allocateNic(NicProfile requested, Network network, Boolean isDefaultNic,
            int deviceId, VirtualMachineProfile<? extends VMInstanceVO> vm)
                    throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException,
                    ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#prepareNic(org.apache.vm.VirtualMachineProfile, org.apache.deploy.DeployDestination, org.apache.vm.ReservationContext, long, org.apache.network.NetworkVO)
     */
    @Override
    public NicProfile prepareNic(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, DeployDestination dest,
            ReservationContext context, long nicId, NetworkVO network)
                    throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException,
                    ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#removeNic(org.apache.vm.VirtualMachineProfile, org.apache.vm.Nic)
     */
    @Override
    public void removeNic(VirtualMachineProfile<? extends VMInstanceVO> vm, Nic nic) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#setupDns(org.apache.network.Network, org.apache.network.Network.Provider)
     */
    @Override
    public boolean setupDns(Network network, Provider provider) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#releaseNic(org.apache.vm.VirtualMachineProfile, org.apache.vm.Nic)
     */
    @Override
    public void releaseNic(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, Nic nic)
            throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#createNicForVm(org.apache.network.Network, org.apache.vm.NicProfile, org.apache.vm.ReservationContext, org.apache.vm.VirtualMachineProfileImpl, boolean)
     */
    @Override
    public NicProfile createNicForVm(Network network, NicProfile requested, ReservationContext context,
            VirtualMachineProfile<? extends VMInstanceVO> vmProfile, boolean prepare)
                    throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException,
                    ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#assignVpnGatewayIpAddress(long, org.apache.user.Account, long)
     */
    @Override
    public PublicIp assignVpnGatewayIpAddress(long dcId, Account owner, long vpcId)
            throws InsufficientAddressCapacityException, ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#markPublicIpAsAllocated(org.apache.network.IPAddressVO)
     */
    @Override
    public void markPublicIpAsAllocated(IPAddressVO addr) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#assignDedicateIpAddress(org.apache.user.Account, java.lang.Long, java.lang.Long, long, boolean)
     */
    @Override
    public PublicIp assignDedicateIpAddress(Account owner, Long guestNtwkId, Long vpcId, long dcId, boolean isSourceNat)
            throws ConcurrentOperationException, InsufficientAddressCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkManager#getNetworkLockTimeout()
     */
    @Override
    public int getNetworkLockTimeout() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isNetworkInlineMode(Network network) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public StaticNatServiceProvider getStaticNatProviderForNetwork(Network network) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Provider> getProvidersForServiceInNetwork(Network network,
            Service service) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getRuleCountForIp(Long addressId, Purpose purpose, State state) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public LoadBalancingServiceProvider getLoadBalancingProviderForNetwork(Network network) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.network.NetworkService#allocateIP(org.apache.user.Account, boolean, long)
     */
    @Override
    public IpAddress allocateIP(Account ipOwner, long zoneId, Long networkId) throws ResourceAllocationException,
    InsufficientAddressCapacityException, ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isSecondaryIpSetForNic(long nicId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean releaseSecondaryIpFromNic(long ipAddressId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public NicSecondaryIp allocateSecondaryGuestIP(Account account, long zoneId,
            Long nicId, Long networkId, String ipaddress) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String allocateGuestIP(Account ipOwner, boolean isSystem,
            long zoneId, Long networkId, String requestedIp)
            throws InsufficientAddressCapacityException {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public List<? extends Nic> listVmNics(Long vmId, Long nicId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<? extends Nic> listNics(ListNicsCmd listNicsCmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String allocatePublicIpForGuestNic(Long networkId, DataCenter dc,
            Pod pod, Account caller, String requestedIp)
            throws InsufficientAddressCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean removeVmSecondaryIpsOfNic(long nicId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public NicVO savePlaceholderNic(Network network, String ip4Address, Type vmType) {
        // TODO Auto-generated method stub
        return null;
    }
}
