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

import org.apache.exception.InsufficientAddressCapacityException;
import org.apache.network.UserIpv6Address;
import org.apache.user.Account;
import org.apache.utils.component.Manager;


public interface Ipv6AddressManager extends Manager {
    public UserIpv6Address assignDirectIp6Address(long dcId, Account owner, Long networkId, String requestedIp6) throws InsufficientAddressCapacityException;

	public void revokeDirectIpv6Address(long networkId, String ip6Address);
}
