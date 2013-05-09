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
package com.cloud.api.commands.netapp;


import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.apache.server.api.response.netapp.DeleteVolumePoolCmdResponse;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.exception.ConcurrentOperationException;
import org.apache.exception.InsufficientCapacityException;
import org.apache.exception.InvalidParameterValueException;
import org.apache.exception.ResourceAllocationException;
import org.apache.exception.ResourceInUseException;
import org.apache.exception.ResourceUnavailableException;

import com.cloud.netapp.NetappManager;


@APICommand(name = "deletePool", description="Delete a pool", responseObject = DeleteVolumePoolCmdResponse.class)
public class DeleteVolumePoolCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(DeleteVolumePoolCmd.class.getName());
    private static final String s_name = "deletepoolresponse";
    
    @Parameter(name=ApiConstants.POOL_NAME, type=CommandType.STRING, required = true, description="pool name.")
	private String poolName;
    
    @Inject NetappManager netappMgr;

	@Override
	public void execute() throws ResourceUnavailableException,
			InsufficientCapacityException, ServerApiException,
			ConcurrentOperationException, ResourceAllocationException {
    	try {
			netappMgr.deletePool(poolName);
			DeleteVolumePoolCmdResponse response = new DeleteVolumePoolCmdResponse();
			response.setResponseName(getCommandName());
			this.setResponseObject(response);
		} catch (InvalidParameterValueException e) {
			throw new ServerApiException(ApiErrorCode.PARAM_ERROR, e.toString());
		} catch (ResourceInUseException e) {
			throw new ServerApiException(ApiErrorCode.RESOURCE_IN_USE_ERROR, e.toString());
		}
	}

	@Override
	public String getCommandName() {
		// TODO Auto-generated method stub
		return s_name;
	}

	@Override
	public long getEntityOwnerId() {
		// TODO Auto-generated method stub
		return 0;
	}
    
}
