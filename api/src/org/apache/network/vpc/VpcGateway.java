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

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

public interface VpcGateway extends Identity, ControlledEntity, InternalIdentity {
    public enum Type {
        Private,
        Public,
        Vpn
    }

    public enum State {
        Creating,
        Ready,
        Deleting
    }

    /**
     * @return
     */
    String getIp4Address();

    /**
     * @return
     */
    Type getType();

    /**
     * @return
     */
    Long getVpcId();

    /**
     * @return
     */
    long getZoneId();

    /**
     * @return
     */
    Long getNetworkId();

    /**
     * @return
     */
    String getGateway();

    /**
     * @return
     */
    String getNetmask();

    /**
     * @return
     */
    String getVlanTag();

    /**
     * @return
     */
    State getState();
    /**
     * @return
     */
    boolean getSourceNat();
}
