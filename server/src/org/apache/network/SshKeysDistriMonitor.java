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

import org.apache.agent.AgentManager;
import org.apache.agent.Listener;
import org.apache.agent.api.AgentControlAnswer;
import org.apache.agent.api.AgentControlCommand;
import org.apache.agent.api.Answer;
import org.apache.agent.api.Command;
import org.apache.agent.api.ModifySshKeysCommand;
import org.apache.agent.api.StartupCommand;
import org.apache.agent.api.StartupRoutingCommand;
import org.apache.agent.manager.Commands;
import org.apache.configuration.dao.ConfigurationDao;
import org.apache.exception.AgentUnavailableException;
import org.apache.exception.ConnectionException;
import org.apache.host.HostVO;
import org.apache.host.Status;
import org.apache.host.dao.HostDao;
import org.apache.hypervisor.Hypervisor.HypervisorType;
import org.apache.log4j.Logger;



public class SshKeysDistriMonitor implements Listener {
	  private static final Logger s_logger = Logger.getLogger(SshKeysDistriMonitor.class);
	  	AgentManager _agentMgr;
		private final HostDao _hostDao;
		private ConfigurationDao _configDao;
	    public SshKeysDistriMonitor(AgentManager mgr, HostDao host, ConfigurationDao config) {
	    	this._agentMgr = mgr;
	    	_hostDao = host;
	    	_configDao = config;
	    }
	    
	    
	    @Override
	    public boolean isRecurring() {
	        return false;
	    }
	    
	    @Override
	    public synchronized boolean processAnswers(long agentId, long seq, Answer[] resp) {
	        return true;
	    }
	    
	    @Override
	    public synchronized boolean processDisconnect(long agentId, Status state) {
	    	if(s_logger.isTraceEnabled())
	    		s_logger.trace("Agent disconnected, agent id: " + agentId + ", state: " + state + ". Will notify waiters");
	    	
	    
	        return true;
	    }
	    
	    @Override
	    public void processConnect(HostVO host, StartupCommand cmd, boolean forRebalance) throws ConnectionException {
	    	if (cmd instanceof StartupRoutingCommand) {
	    		if (((StartupRoutingCommand) cmd).getHypervisorType() == HypervisorType.KVM ||
                    ((StartupRoutingCommand) cmd).getHypervisorType() == HypervisorType.XenServer ||
                    ((StartupRoutingCommand) cmd).getHypervisorType() == HypervisorType.LXC) {
	    			/*TODO: Get the private/public keys here*/
	    			
	    			String pubKey = _configDao.getValue("ssh.publickey");
	    			String prvKey = _configDao.getValue("ssh.privatekey");
	    			
	    			try {
	    				ModifySshKeysCommand cmds = new ModifySshKeysCommand(pubKey, prvKey);
		    			Commands c = new Commands(cmds);
	    				_agentMgr.send(host.getId(), c, this);
	    			} catch (AgentUnavailableException e) {
	    				s_logger.debug("Failed to send keys to agent: " + host.getId());
	    			}
	    		}
	    	}
	    }

		@Override
		public int getTimeout() {
			// TODO Auto-generated method stub
			return -1;
		}


		@Override
		public boolean processCommands(long agentId, long seq, Command[] commands) {
			// TODO Auto-generated method stub
			return false;
		}


		@Override
		public AgentControlAnswer processControlCommand(long agentId,
				AgentControlCommand cmd) {
			// TODO Auto-generated method stub
			return null;
		}


		@Override
		public boolean processTimeout(long agentId, long seq) {
			// TODO Auto-generated method stub
			return false;
		}
}
