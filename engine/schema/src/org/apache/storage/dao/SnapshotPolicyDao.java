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
package org.apache.storage.dao;

import java.util.List;

import org.apache.storage.SnapshotPolicyVO;
import org.apache.utils.Pair;
import org.apache.utils.DateUtil.IntervalType;
import org.apache.utils.db.Filter;
import org.apache.utils.db.GenericDao;


/*
 * Data Access Object for snapshot_policy table
 */
public interface SnapshotPolicyDao extends GenericDao<SnapshotPolicyVO, Long> {
	List<SnapshotPolicyVO> listByVolumeId(long volumeId);
	List<SnapshotPolicyVO> listByVolumeId(long volumeId, Filter filter);
    Pair<List<SnapshotPolicyVO>, Integer> listAndCountByVolumeId(long volumeId);
    Pair<List<SnapshotPolicyVO>, Integer> listAndCountByVolumeId(long volumeId, Filter filter);
	SnapshotPolicyVO findOneByVolumeInterval(long volumeId, IntervalType intvType);
    List<SnapshotPolicyVO> listActivePolicies();
    SnapshotPolicyVO findOneByVolume(long volumeId);
}
