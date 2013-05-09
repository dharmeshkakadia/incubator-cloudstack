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
package org.apache.agent.manager;

import java.util.HashMap;
import java.util.Map;

import org.apache.agent.api.Answer;
import org.apache.agent.api.BumpUpPriorityCommand;
import org.apache.agent.api.CheckRouterAnswer;
import org.apache.agent.api.CheckRouterCommand;
import org.apache.agent.api.CheckVirtualMachineCommand;
import org.apache.agent.api.CleanupNetworkRulesCmd;
import org.apache.agent.api.GetDomRVersionAnswer;
import org.apache.agent.api.GetDomRVersionCmd;
import org.apache.agent.api.GetVmStatsCommand;
import org.apache.agent.api.GetVncPortCommand;
import org.apache.agent.api.MigrateAnswer;
import org.apache.agent.api.MigrateCommand;
import org.apache.agent.api.NetworkUsageCommand;
import org.apache.agent.api.PrepareForMigrationAnswer;
import org.apache.agent.api.PrepareForMigrationCommand;
import org.apache.agent.api.RebootCommand;
import org.apache.agent.api.SecurityGroupRuleAnswer;
import org.apache.agent.api.SecurityGroupRulesCmd;
import org.apache.agent.api.StartCommand;
import org.apache.agent.api.StopCommand;
import org.apache.agent.api.check.CheckSshAnswer;
import org.apache.agent.api.check.CheckSshCommand;
import org.apache.agent.api.proxy.CheckConsoleProxyLoadCommand;
import org.apache.agent.api.proxy.WatchConsoleProxyLoadCommand;
import org.apache.agent.api.routing.DhcpEntryCommand;
import org.apache.agent.api.routing.IpAssocCommand;
import org.apache.agent.api.routing.LoadBalancerConfigCommand;
import org.apache.agent.api.routing.SavePasswordCommand;
import org.apache.agent.api.routing.SetFirewallRulesCommand;
import org.apache.agent.api.routing.SetPortForwardingRulesCommand;
import org.apache.agent.api.routing.SetStaticNatRulesCommand;
import org.apache.agent.api.routing.VmDataCommand;
import org.apache.simulator.MockVMVO;
import org.apache.utils.Pair;
import org.apache.utils.component.Manager;
import org.apache.vm.VirtualMachine.State;


public interface MockVmManager extends Manager {
    public Answer stopVM(StopCommand cmd);
	public Answer rebootVM(RebootCommand cmd);

    public Answer checkVmState(CheckVirtualMachineCommand cmd);
    public Map<String, State> getVmStates(String hostGuid);
    public Answer getVncPort(GetVncPortCommand cmd);

	Answer startVM(StartCommand cmd, SimulatorInfo info);

	Answer getVmStats(GetVmStatsCommand cmd);
    public CheckSshAnswer checkSshCommand(CheckSshCommand cmd);

    Answer SetStaticNatRules(SetStaticNatRulesCommand cmd);

    Answer SetPortForwardingRules(SetPortForwardingRulesCommand cmd);

    Answer SetFirewallRules(SetFirewallRulesCommand cmd);

    Answer getNetworkUsage(NetworkUsageCommand cmd);

    Answer IpAssoc(IpAssocCommand cmd);

    Answer LoadBalancerConfig(LoadBalancerConfigCommand cmd);

    Answer AddDhcpEntry(DhcpEntryCommand cmd);

    Answer setVmData(VmDataCommand cmd);
    Answer CleanupNetworkRules(CleanupNetworkRulesCmd cmd, SimulatorInfo info);

    Answer CheckConsoleProxyLoad(CheckConsoleProxyLoadCommand cmd);
    Answer WatchConsoleProxyLoad(WatchConsoleProxyLoadCommand cmd);

    Answer SavePassword(SavePasswordCommand cmd);
    HashMap<String, Pair<Long, Long>> syncNetworkGroups(SimulatorInfo info);
    SecurityGroupRuleAnswer AddSecurityGroupRules(SecurityGroupRulesCmd cmd, SimulatorInfo info);
	MigrateAnswer Migrate(MigrateCommand cmd, SimulatorInfo info);
    PrepareForMigrationAnswer prepareForMigrate(PrepareForMigrationCommand cmd);
	GetDomRVersionAnswer getDomRVersion(GetDomRVersionCmd cmd);
	Map<String, MockVMVO> getVms(String hostGuid);

    CheckRouterAnswer checkRouter(CheckRouterCommand cmd);

    Answer bumpPriority(BumpUpPriorityCommand cmd);
}
