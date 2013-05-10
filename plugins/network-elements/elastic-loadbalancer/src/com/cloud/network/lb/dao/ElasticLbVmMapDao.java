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
package com.cloud.network.lb.dao;

import java.util.List;

import org.apache.network.dao.LoadBalancerVO;
import org.apache.utils.db.GenericDao;
import org.apache.vm.DomainRouterVO;

import com.cloud.network.ElasticLbVmMapVO;

public interface ElasticLbVmMapDao extends GenericDao<ElasticLbVmMapVO, Long> {
    ElasticLbVmMapVO findOneByLbIdAndElbVmId(long lbId, long elbVmId);
    ElasticLbVmMapVO findOneByIpIdAndElbVmId(long ipId, long elbVmId);
    ElasticLbVmMapVO findOneByIp(long ipId);

    List<ElasticLbVmMapVO> listByElbVmId(long elbVmId);
    List<ElasticLbVmMapVO> listByLbId(long lbId);
    int deleteLB(long lbId);
    List<DomainRouterVO> listUnusedElbVms();
    List<LoadBalancerVO> listLbsForElbVm(long elbVmId);
	
}
