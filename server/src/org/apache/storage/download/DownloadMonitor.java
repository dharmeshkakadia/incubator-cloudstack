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
package org.apache.storage.download;

import java.util.Map;

import org.apache.exception.StorageUnavailableException;
import org.apache.host.HostVO;
import org.apache.storage.VMTemplateVO;
import org.apache.storage.VolumeVO;
import org.apache.storage.Storage.ImageFormat;
import org.apache.storage.template.TemplateInfo;
import org.apache.utils.component.Manager;



/**
 * Monitor download progress of all templates across all servers
 *
 */
public interface DownloadMonitor extends Manager{
	
	public boolean downloadTemplateToStorage(VMTemplateVO template, Long zoneId);
	
	public void cancelAllDownloads(Long templateId);

	public void handleTemplateSync(HostVO host);

	public boolean copyTemplate(VMTemplateVO template, HostVO sourceServer, HostVO destServer)
			throws StorageUnavailableException;

    void handleSysTemplateDownload(HostVO hostId);

    void handleSync(Long dcId);

    void addSystemVMTemplatesToHost(HostVO host, Map<String, TemplateInfo> templateInfos);

	boolean downloadVolumeToStorage(VolumeVO volume, Long zoneId, String url, String checkSum, ImageFormat format);

	void handleVolumeSync(HostVO ssHost);	

}