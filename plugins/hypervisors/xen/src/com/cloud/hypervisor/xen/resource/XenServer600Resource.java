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
package com.cloud.hypervisor.xen.resource;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;
import org.apache.resource.ServerResource;
import org.apache.utils.exception.CloudRuntimeException;
import org.apache.utils.script.Script;



@Local(value=ServerResource.class)
public class XenServer600Resource extends XenServer56FP1Resource {
    private static final Logger s_logger = Logger.getLogger(XenServer600Resource.class);
    
    public XenServer600Resource() {
        super();
    }
    
    @Override
    protected String getGuestOsType(String stdType, boolean bootFromCD) {
        return CitrixHelper.getXenServer600GuestOsType(stdType, bootFromCD);
    }
   
    @Override
    protected List<File> getPatchFiles() {      
        List<File> files = new ArrayList<File>();
        String patch = "scripts/vm/hypervisor/xenserver/xenserver60/patch";    
        String patchfilePath = Script.findScript("" , patch);
        if ( patchfilePath == null ) {
            throw new CloudRuntimeException("Unable to find patch file " + patch);
        }
        File file = new File(patchfilePath);
        files.add(file);
        return files;
    }

}
