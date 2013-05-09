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
package org.apache.network.vpc;

import java.util.List;

import org.apache.exception.ResourceUnavailableException;
import org.apache.network.firewall.NetworkACLService;
import org.apache.network.rules.FirewallRule;
import org.apache.user.Account;



public interface NetworkACLManager extends NetworkACLService{
    
    /**
     * @param networkId
     * @param userId
     * @param caller
     * @return
     * @throws ResourceUnavailableException
     */
    boolean revokeAllNetworkACLsForNetwork(long networkId, long userId, Account caller) throws ResourceUnavailableException;
    
    List<? extends FirewallRule> listNetworkACLs(long guestNtwkId);

}
