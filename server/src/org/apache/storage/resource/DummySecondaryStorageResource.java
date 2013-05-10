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
package org.apache.storage.resource;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.agent.api.Answer;
import org.apache.agent.api.CheckHealthAnswer;
import org.apache.agent.api.CheckHealthCommand;
import org.apache.agent.api.Command;
import org.apache.agent.api.GetStorageStatsAnswer;
import org.apache.agent.api.GetStorageStatsCommand;
import org.apache.agent.api.PingCommand;
import org.apache.agent.api.PingStorageCommand;
import org.apache.agent.api.ReadyAnswer;
import org.apache.agent.api.ReadyCommand;
import org.apache.agent.api.StartupCommand;
import org.apache.agent.api.StartupStorageCommand;
import org.apache.agent.api.storage.DownloadAnswer;
import org.apache.agent.api.storage.DownloadCommand;
import org.apache.agent.api.storage.DownloadProgressCommand;
import org.apache.host.Host;
import org.apache.host.Host.Type;
import org.apache.log4j.Logger;
import org.apache.resource.ServerResource;
import org.apache.resource.ServerResourceBase;
import org.apache.storage.Storage;
import org.apache.storage.VMTemplateVO;
import org.apache.storage.Storage.StoragePoolType;
import org.apache.storage.dao.VMTemplateDao;
import org.apache.storage.template.TemplateConstants;
import org.apache.storage.template.TemplateInfo;



public class DummySecondaryStorageResource extends ServerResourceBase implements ServerResource {
    private static final Logger s_logger = Logger.getLogger(DummySecondaryStorageResource.class);

    String _dc;
    String _pod;
    String _guid;
    String _dummyPath;
    @Inject VMTemplateDao _tmpltDao;
    private boolean _useServiceVm;

    public DummySecondaryStorageResource() {
        setUseServiceVm(true);
    }
    
    public DummySecondaryStorageResource(boolean useServiceVM) {
        setUseServiceVm(useServiceVM);
    }

    @Override
    protected String getDefaultScriptsDir() {
        return "dummy";
    }

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof DownloadProgressCommand) {
            return new DownloadAnswer(null, 100, cmd,
                    org.apache.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOADED,
                    "dummyFS",
                    "/dummy");
        } else if (cmd instanceof DownloadCommand) {
            return new DownloadAnswer(null, 100, cmd,
                    org.apache.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOADED,
                    "dummyFS",
                    "/dummy");
        } else if (cmd instanceof GetStorageStatsCommand) {
            return execute((GetStorageStatsCommand)cmd);
        } else if (cmd instanceof CheckHealthCommand) {
            return new CheckHealthAnswer((CheckHealthCommand)cmd, true);
        } else if (cmd instanceof ReadyCommand) {
            return new ReadyAnswer((ReadyCommand)cmd);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        return new PingStorageCommand(Host.Type.Storage, id, new HashMap<String, Boolean>());
    }

    @Override
    public Type getType() {
        return Host.Type.SecondaryStorage;
    }

    @Override
    public StartupCommand[] initialize() {
        final StartupStorageCommand cmd = new StartupStorageCommand("dummy",
                StoragePoolType.NetworkFilesystem, 1024*1024*1024*100L,
                new HashMap<String, TemplateInfo>());

        cmd.setResourceType(Storage.StorageResourceType.SECONDARY_STORAGE);
        cmd.setIqn(null);
        cmd.setNfsShare(_guid);

        fillNetworkInformation(cmd);
        cmd.setDataCenter(_dc);
        cmd.setPod(_pod);
        cmd.setGuid(_guid);

        cmd.setName(_guid);
        cmd.setVersion(DummySecondaryStorageResource.class.getPackage().getImplementationVersion());
        /* gather TemplateInfo in second storage */
        cmd.setTemplateInfo(getDefaultSystemVmTemplateInfo());
        cmd.getHostDetails().put("mount.parent", "dummy");
        cmd.getHostDetails().put("mount.path", "dummy");
        cmd.getHostDetails().put("orig.url", _guid);

        String tok[] = _dummyPath.split(":");
        cmd.setPrivateIpAddress(tok[0]);
        return new StartupCommand [] {cmd};
    }

    protected GetStorageStatsAnswer execute(GetStorageStatsCommand cmd) {
        long size = 1024*1024*1024*100L;
        return new GetStorageStatsAnswer(cmd, 0, size);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        _guid = (String)params.get("guid");
        if (_guid == null) {
            throw new ConfigurationException("Unable to find the guid");
        }

        _dc = (String)params.get("zone");
        if (_dc == null) {
            throw new ConfigurationException("Unable to find the zone");
        }
        _pod = (String)params.get("pod");

        _dummyPath = (String)params.get("mount.path");
        if (_dummyPath == null) {
            throw new ConfigurationException("Unable to find mount.path");
        }

        return true;
    }

    public void setUseServiceVm(boolean _useServiceVm) {
        this._useServiceVm = _useServiceVm;
    }

    public boolean useServiceVm() {
        return _useServiceVm;
    }

    public Map<String, TemplateInfo> getDefaultSystemVmTemplateInfo() {	        
        List<VMTemplateVO> tmplts = _tmpltDao.listAllSystemVMTemplates();
        Map<String, TemplateInfo> tmpltInfo = new HashMap<String, TemplateInfo>();
        if (tmplts != null) {
            for (VMTemplateVO tmplt : tmplts) {
                TemplateInfo routingInfo = new TemplateInfo(tmplt.getUniqueName(), TemplateConstants.DEFAULT_SYSTEM_VM_TEMPLATE_PATH + tmplt.getId() + File.separator, false, false);
                tmpltInfo.put(tmplt.getUniqueName(), routingInfo);
            }
        }
        return tmpltInfo;
    }

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setConfigParams(Map<String, Object> params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, Object> getConfigParams() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getRunLevel() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setRunLevel(int level) {
		// TODO Auto-generated method stub
		
	}
}
