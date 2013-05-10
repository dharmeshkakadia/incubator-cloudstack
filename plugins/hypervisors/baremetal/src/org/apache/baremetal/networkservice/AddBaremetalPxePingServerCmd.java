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
// 
// Automatically generated by addcopyright.py at 01/29/2013
package org.apache.baremetal.networkservice;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.Parameter;

@APICommand(name="addBaremetalPxePingServer", description="add a baremetal ping pxe server", responseObject = BaremetalPxePingResponse.class)
public class AddBaremetalPxePingServerCmd extends AddBaremetalPxeCmd {

    @Parameter(name=ApiConstants.PING_STORAGE_SERVER_IP, type=CommandType.STRING, required = true, description="PING storage server ip")
    private String pingStorageServerIp;
    
    @Parameter(name=ApiConstants.PING_DIR, type=CommandType.STRING, required = true, description="Root directory on PING storage server")
    private String pingDir;
    
    @Parameter(name=ApiConstants.TFTP_DIR, type=CommandType.STRING, required = true, description="Tftp root directory of PXE server")
    private String tftpDir;
    
    @Parameter(name=ApiConstants.PING_CIFS_USERNAME, type=CommandType.STRING, description="Username of PING storage server")
    private String pingStorageServerUserName;
    
    @Parameter(name=ApiConstants.PING_CIFS_PASSWORD, type=CommandType.STRING, description="Password of PING storage server")
    private String pingStorageServerPassword;

    public String getPingStorageServerIp() {
        return pingStorageServerIp;
    }

    public void setPingStorageServerIp(String pingStorageServerIp) {
        this.pingStorageServerIp = pingStorageServerIp;
    }

    public String getPingDir() {
        return pingDir;
    }

    public void setPingDir(String pingDir) {
        this.pingDir = pingDir;
    }

    public String getTftpDir() {
        return tftpDir;
    }

    public void setTftpDir(String tftpDir) {
        this.tftpDir = tftpDir;
    }

    public String getPingStorageServerUserName() {
        return pingStorageServerUserName;
    }

    public void setPingStorageServerUserName(String pingStorageServerUserName) {
        this.pingStorageServerUserName = pingStorageServerUserName;
    }

    public String getPingStorageServerPassword() {
        return pingStorageServerPassword;
    }

    public void setPingStorageServerPassword(String pingStorageServerPassword) {
        this.pingStorageServerPassword = pingStorageServerPassword;
    }
}
