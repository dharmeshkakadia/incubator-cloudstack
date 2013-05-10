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

import org.apache.network.VirtualRouterProvider.VirtualRouterProviderType;
import org.apache.network.element.VirtualRouterProviderVO;
import org.apache.utils.db.DB;
import org.apache.utils.db.GenericDaoBase;
import org.apache.utils.db.SearchBuilder;
import org.apache.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;


@Component
@Local(value=VirtualRouterProviderDao.class) @DB(txn=false)
public class VirtualRouterProviderDaoImpl extends GenericDaoBase<VirtualRouterProviderVO, Long> implements VirtualRouterProviderDao {
    final SearchBuilder<VirtualRouterProviderVO> AllFieldsSearch;
    
    public VirtualRouterProviderDaoImpl() {
        super();
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("nsp_id", AllFieldsSearch.entity().getNspId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("uuid", AllFieldsSearch.entity().getUuid(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("type", AllFieldsSearch.entity().getType(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("enabled", AllFieldsSearch.entity().isEnabled(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }

    @Override
    public VirtualRouterProviderVO findByNspIdAndType(long nspId, VirtualRouterProviderType type) {
        SearchCriteria<VirtualRouterProviderVO> sc = AllFieldsSearch.create();
        sc.setParameters("nsp_id", nspId);
        sc.setParameters("type", type);
        return findOneBy(sc);
    }
    
    @Override
    public List<VirtualRouterProviderVO> listByEnabledAndType(boolean enabled, VirtualRouterProviderType type) {
        SearchCriteria<VirtualRouterProviderVO> sc = AllFieldsSearch.create();
        sc.setParameters("enabled", enabled);
        sc.setParameters("type", type);
        return listBy(sc);
    }
    
    @Override
    public VirtualRouterProviderVO findByIdAndEnabledAndType(long id, boolean enabled, VirtualRouterProviderType type) {
        SearchCriteria<VirtualRouterProviderVO> sc = AllFieldsSearch.create();
        sc.setParameters("id", id);
        sc.setParameters("enabled", enabled);
        sc.setParameters("type", type);
        return findOneBy(sc);
    }

    @Override
    public List<VirtualRouterProviderVO> listByType(VirtualRouterProviderType type) {
        SearchCriteria<VirtualRouterProviderVO> sc = AllFieldsSearch.create();
        sc.setParameters("type", type);
        return listBy(sc);
    }
}
