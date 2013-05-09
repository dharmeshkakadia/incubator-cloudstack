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
package org.apache.network.lb;

import org.apache.cloudstack.api.command.user.loadbalancer.CreateLoadBalancerRuleCmd;
import org.apache.exception.NetworkRuleConflictException;
import org.apache.exception.ResourceUnavailableException;
import org.apache.network.Network;
import org.apache.network.lb.LoadBalancingRulesService;
import org.apache.network.lb.LoadBalancingRule.LbDestination;
import org.apache.network.lb.LoadBalancingRule.LbHealthCheckPolicy;
import org.apache.network.lb.LoadBalancingRule.LbStickinessPolicy;
import org.apache.network.rules.FirewallRule;
import org.apache.network.rules.LbStickinessMethod;
import org.apache.network.rules.LoadBalancer;
import org.apache.user.Account;

import java.util.List;

public interface LoadBalancingRulesManager extends LoadBalancingRulesService {

    LoadBalancer createLoadBalancer(CreateLoadBalancerRuleCmd lb, boolean openFirewall) throws NetworkRuleConflictException;

    boolean removeAllLoadBalanacersForIp(long ipId, Account caller, long callerUserId);
    boolean removeAllLoadBalanacersForNetwork(long networkId, Account caller, long callerUserId);
    List<LbDestination> getExistingDestinations(long lbId);
    List<LbStickinessPolicy> getStickinessPolicies(long lbId);
    List<LbStickinessMethod> getStickinessMethods(long networkid);
    List<LbHealthCheckPolicy> getHealthCheckPolicies(long lbId);

    /**
     * Remove vm from all load balancers
     * @param vmId
     * @return true if removal is successful
     */
    boolean removeVmFromLoadBalancers(long vmId);
    boolean applyRules(Network network, FirewallRule.Purpose purpose, List<? extends FirewallRule> rules) throws ResourceUnavailableException ;
    boolean applyLoadBalancersForNetwork(long networkId) throws ResourceUnavailableException;
    String getLBCapability(long networkid, String capabilityName);
    boolean configureLbAutoScaleVmGroup(long vmGroupid, String currentState) throws ResourceUnavailableException;
    boolean revokeLoadBalancersForNetwork(long networkId) throws ResourceUnavailableException;
}
