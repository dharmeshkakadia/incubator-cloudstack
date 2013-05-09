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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.agent.api.VmStatsEntry;
import org.apache.api.query.vo.UserVmJoinVO;
import org.apache.exception.ConcurrentOperationException;
import org.apache.exception.InsufficientCapacityException;
import org.apache.exception.ResourceUnavailableException;
import org.apache.projects.Project.ListProjectResourcesCriteria;
import org.apache.server.Criteria;
import org.apache.user.Account;
import org.apache.uservm.UserVm;
import org.apache.utils.Pair;
import org.apache.vm.InstanceGroupVO;
import org.apache.vm.UserVmService;
import org.apache.vm.UserVmVO;
import org.apache.vm.VirtualMachineProfile;


/**
 *
 * 
 */
public interface UserVmManager extends VirtualMachineGuru<UserVmVO>, UserVmService{

	static final int MAX_USER_DATA_LENGTH_BYTES = 2048;
    /**
     * @param hostId get all of the virtual machines that belong to one host.
     * @return collection of VirtualMachine.
     */
    List<? extends UserVm> getVirtualMachines(long hostId);
    
    /**
     * @param vmId id of the virtual machine.
     * @return VirtualMachine
     */
    UserVmVO getVirtualMachine(long vmId);

    /**
     * Stops the virtual machine
     * @param userId the id of the user performing the action
     * @param vmId
     * @return true if stopped; false if problems.
     */
    boolean stopVirtualMachine(long userId, long vmId);

    /**
     * Obtains statistics for a list of host or VMs; CPU and network utilization
     * @param host ID
     * @param host name
     * @param list of VM IDs or host id
     * @return GetVmStatsAnswer
     */
    HashMap<Long, VmStatsEntry> getVirtualMachineStatistics(long hostId, String hostName, List<Long> vmIds);
    
    boolean deleteVmGroup(long groupId);

    boolean addInstanceToGroup(long userVmId, String group);

    InstanceGroupVO getGroupForVm(long vmId);
    
    void removeInstanceFromInstanceGroup(long vmId);

	boolean expunge(UserVmVO vm, long callerUserId, Account caller);
	
    /**
     * Obtains a list of virtual machines by the specified search criteria.
     * Can search by: "userId", "name", "state", "dataCenterId", "podId", "hostId"
     * @param c
     * @param caller TODO
     * @param domainId TODO
     * @param isRecursive TODO
     * @param permittedAccounts TODO
     * @param listAll TODO
     * @param listProjectResourcesCriteria TODO
     * @param tags TODO
     * @return List of UserVMs + count
     */
    Pair<List<UserVmJoinVO>, Integer> searchForUserVMs(Criteria c, Account caller, Long domainId, boolean isRecursive, List<Long> permittedAccounts, boolean listAll, ListProjectResourcesCriteria listProjectResourcesCriteria, Map<String, String> tags);

    Pair<UserVmVO, Map<VirtualMachineProfile.Param, Object>> startVirtualMachine(long vmId, Long hostId, Map<VirtualMachineProfile.Param, Object> additionalParams) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException;

}
