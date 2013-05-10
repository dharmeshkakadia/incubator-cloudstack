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

package org.apache.api.commands;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.apache.network.CiscoNexusVSMDevice;
import org.apache.network.element.CiscoNexusVSMElementService;
import org.apache.user.Account;

import org.apache.api.response.CiscoNexusVSMResponse;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.event.EventTypes;
import org.apache.exception.ConcurrentOperationException;
import org.apache.exception.InsufficientCapacityException;
import org.apache.exception.ResourceAllocationException;
import org.apache.exception.ResourceUnavailableException;


@APICommand(name = "disableCiscoNexusVSM", responseObject=CiscoNexusVSMResponse.class, description="disable a Cisco Nexus VSM device")
public class DisableCiscoNexusVSMCmd extends BaseAsyncCmd {

    public static final Logger s_logger = Logger.getLogger(DisableCiscoNexusVSMCmd.class.getName());
    private static final String s_name = "disablecisconexusvsmresponse";
    @Inject CiscoNexusVSMElementService _ciscoNexusVSMService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType = CiscoNexusVSMResponse.class,
            required=true, description="Id of the Cisco Nexus 1000v VSM device to be deleted")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getCiscoNexusVSMDeviceId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
    	CiscoNexusVSMDevice result = _ciscoNexusVSMService.disableCiscoNexusVSM(this);
        if (result != null) {
        	CiscoNexusVSMResponse response = _ciscoNexusVSMService.createCiscoNexusVSMDetailedResponse(result);
        	response.setResponseName(getCommandName());
        	this.setResponseObject(response);
        } else {
        	throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to disable Cisco Nexus VSM device");
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventDescription() {
    	return "Disabling a Cisco Nexus VSM device";
    }

    @Override
    public String getEventType() {
    	return EventTypes.EVENT_EXTERNAL_SWITCH_MGMT_DEVICE_DISABLE;
    }    
}
