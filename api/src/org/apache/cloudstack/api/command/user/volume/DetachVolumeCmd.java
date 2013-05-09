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
package org.apache.cloudstack.api.command.user.volume;

import org.apache.async.AsyncJob;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.event.EventTypes;
import org.apache.log4j.Logger;
import org.apache.storage.Volume;
import org.apache.user.Account;
import org.apache.user.UserContext;
import org.apache.uservm.UserVm;


@APICommand(name = "detachVolume", description="Detaches a disk volume from a virtual machine.", responseObject=VolumeResponse.class)
public class DetachVolumeCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DetachVolumeCmd.class.getName());
    private static final String s_name = "detachvolumeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=VolumeResponse.class,
            description="the ID of the disk volume")
    private Long id;

    @Parameter(name=ApiConstants.DEVICE_ID, type=CommandType.LONG, description="the device ID on the virtual machine where volume is detached from")
    private Long deviceId;

    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.UUID, entityType=UserVmResponse.class,
            description="the ID of the virtual machine where the volume is detached from")
    private Long virtualMachineId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return "volume";
    }

    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.Volume;
    }

    public Long getInstanceId() {
        return getId();
    }

    @Override
    public long getEntityOwnerId() {
        Long volumeId = getId();
        if (volumeId != null) {
            Volume volume = _responseGenerator.findVolumeById(volumeId);
            if (volume != null) {
                return volume.getAccountId();
            }
        } else if (getVirtualMachineId() != null) {
            UserVm vm = _responseGenerator.findUserVmById(getVirtualMachineId());
            if (vm != null) {
                return vm.getAccountId();
            }
        }

        // invalid id, parent this command to SYSTEM so ERROR events are tracked
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VOLUME_DETACH;
    }

    @Override
    public String getEventDescription() {
        StringBuilder sb = new StringBuilder();
        if (id != null) {
            sb.append(": " + id);
        } else if ((deviceId != null) && (virtualMachineId != null)) {
            sb.append(" with device id: " + deviceId + " from vm: " + virtualMachineId);
        } else {
            sb.append(" <error:  either volume id or deviceId/vmId need to be specified>");
        }
        return  "detaching volume" + sb.toString();
    }

    @Override
    public void execute(){
        UserContext.current().setEventDetails("Volume Id: "+getId()+" VmId: "+getVirtualMachineId());
        Volume result = _volumeService.detachVolumeFromVM(this);
        if (result != null){
            VolumeResponse response = _responseGenerator.createVolumeResponse(result);
            response.setResponseName("volume");
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to detach volume");
        }
    }
}
