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
package org.apache.network.dao;

import java.util.List;

import javax.ejb.Local;

import org.apache.network.cisco.CiscoVnmcControllerVO;
import org.apache.utils.db.GenericDaoBase;
import org.apache.utils.db.SearchBuilder;
import org.apache.utils.db.SearchCriteria;
import org.apache.utils.db.SearchCriteria.Op;
import org.springframework.stereotype.Component;


@Component
@Local(value=CiscoVnmcDao.class)
public class CiscoVnmcDaoImpl extends GenericDaoBase<CiscoVnmcControllerVO, Long>
        implements CiscoVnmcDao {
    
    protected final SearchBuilder<CiscoVnmcControllerVO> physicalNetworkIdSearch;

    public CiscoVnmcDaoImpl() {
        physicalNetworkIdSearch = createSearchBuilder();
        physicalNetworkIdSearch.and("physicalNetworkId", physicalNetworkIdSearch.entity().getPhysicalNetworkId(), Op.EQ);
        physicalNetworkIdSearch.done();
    }

    @Override
    public List<CiscoVnmcControllerVO> listByPhysicalNetwork(long physicalNetworkId) {
        SearchCriteria<CiscoVnmcControllerVO> sc = physicalNetworkIdSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        return search(sc, null);
    }

}
