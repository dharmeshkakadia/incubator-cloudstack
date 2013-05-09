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
package org.apache.cloudstack.engine.cloud.entity.api;

import org.apache.cloudstack.engine.cloud.entity.api.db.VMEntityVO;
import org.apache.deploy.DeploymentPlan;
import org.apache.deploy.DeploymentPlanner.ExcludeList;
import org.apache.exception.AgentUnavailableException;
import org.apache.exception.ConcurrentOperationException;
import org.apache.exception.InsufficientCapacityException;
import org.apache.exception.OperationTimedoutException;
import org.apache.exception.ResourceUnavailableException;
import org.apache.vm.VirtualMachineProfile;


import java.util.Map;

public interface VMEntityManager {

	VMEntityVO loadVirtualMachine(String vmId);

	void saveVirtualMachine(VMEntityVO vmInstanceVO);

	String reserveVirtualMachine(VMEntityVO vmEntityVO, String plannerToUse, DeploymentPlan plan, ExcludeList exclude) throws InsufficientCapacityException, ResourceUnavailableException;

    void deployVirtualMachine(String reservationId, VMEntityVO vmEntityVO, String caller, Map<VirtualMachineProfile.Param, Object> params) throws InsufficientCapacityException, ResourceUnavailableException;

    boolean stopvirtualmachine(VMEntityVO vmEntityVO, String caller) throws ResourceUnavailableException;

    boolean destroyVirtualMachine(VMEntityVO vmEntityVO, String caller) throws AgentUnavailableException, OperationTimedoutException, ConcurrentOperationException;
}
