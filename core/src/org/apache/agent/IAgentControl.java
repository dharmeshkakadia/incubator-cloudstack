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
package org.apache.agent;


import org.apache.agent.api.AgentControlAnswer;
import org.apache.agent.api.AgentControlCommand;
import org.apache.exception.AgentControlChannelException;


public interface IAgentControl {
	void registerControlListener(IAgentControlListener listener);
	void unregisterControlListener(IAgentControlListener listener);
	
	AgentControlAnswer sendRequest(AgentControlCommand cmd, int timeoutInMilliseconds) throws AgentControlChannelException;
	void postRequest(AgentControlCommand cmd) throws AgentControlChannelException;
}
