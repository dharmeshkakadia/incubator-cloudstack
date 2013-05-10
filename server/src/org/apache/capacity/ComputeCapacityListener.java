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
package org.apache.capacity;

import java.util.List;

import org.apache.agent.Listener;
import org.apache.agent.api.AgentControlAnswer;
import org.apache.agent.api.AgentControlCommand;
import org.apache.agent.api.Answer;
import org.apache.agent.api.Command;
import org.apache.agent.api.StartupCommand;
import org.apache.agent.api.StartupRoutingCommand;
import org.apache.capacity.dao.CapacityDao;
import org.apache.exception.ConnectionException;
import org.apache.host.HostVO;
import org.apache.host.Status;
import org.apache.log4j.Logger;
import org.apache.utils.db.SearchCriteria;



public class ComputeCapacityListener implements Listener {
    private static final Logger s_logger = Logger.getLogger(ComputeCapacityListener.class);
    CapacityDao _capacityDao;   
    CapacityManager _capacityMgr;
    float _cpuOverProvisioningFactor = 1.0f;


    public ComputeCapacityListener(CapacityDao _capacityDao,
    		CapacityManager _capacityMgr
           ) {
        super();
        this._capacityDao = _capacityDao;
        this._capacityMgr = _capacityMgr;
    }


    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        return false;
    }


    @Override
    public boolean processCommands(long agentId, long seq, Command[] commands) {
        return false;
    }


    @Override
    public AgentControlAnswer processControlCommand(long agentId,
            AgentControlCommand cmd) {

        return null;
    }


    @Override
    public void processConnect(HostVO server, StartupCommand startup, boolean forRebalance) throws ConnectionException {
        if (!(startup instanceof StartupRoutingCommand)) {
            return;
        }
        _capacityMgr.updateCapacityForHost(server);        
    }




    @Override
    public boolean processDisconnect(long agentId, Status state) {
        return false;
    }


    @Override
    public boolean isRecurring() {
        return false;
    }


    @Override
    public int getTimeout() {
        return 0;
    }


    @Override
    public boolean processTimeout(long agentId, long seq) {
        return false;
    }
    
}
