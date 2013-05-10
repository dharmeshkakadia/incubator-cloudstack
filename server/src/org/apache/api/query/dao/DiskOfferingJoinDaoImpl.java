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
package org.apache.api.query.dao;

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;
import org.apache.offering.DiskOffering;
import org.apache.offering.ServiceOffering;
import org.apache.storage.DiskOfferingVO;
import org.apache.storage.DiskOfferingVO.Type;
import org.apache.utils.db.Attribute;
import org.apache.utils.db.Filter;
import org.apache.utils.db.GenericDaoBase;
import org.apache.utils.db.SearchBuilder;
import org.apache.utils.db.SearchCriteria;
import org.apache.utils.db.SearchCriteria.Op;

import org.apache.api.query.vo.DiskOfferingJoinVO;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.springframework.stereotype.Component;

@Component
@Local(value={DiskOfferingJoinDao.class})
public class DiskOfferingJoinDaoImpl extends GenericDaoBase<DiskOfferingJoinVO, Long> implements DiskOfferingJoinDao {
    public static final Logger s_logger = Logger.getLogger(DiskOfferingJoinDaoImpl.class);


    private SearchBuilder<DiskOfferingJoinVO> dofIdSearch;
    private final Attribute _typeAttr;

     protected DiskOfferingJoinDaoImpl() {

        dofIdSearch = createSearchBuilder();
        dofIdSearch.and("id", dofIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        dofIdSearch.done();

        _typeAttr = _allAttributes.get("type");

        this._count = "select count(distinct id) from disk_offering_view WHERE ";
    }



    @Override
    public DiskOfferingResponse newDiskOfferingResponse(DiskOfferingJoinVO offering) {

        DiskOfferingResponse diskOfferingResponse = new DiskOfferingResponse();
        diskOfferingResponse.setId(offering.getUuid());
        diskOfferingResponse.setName(offering.getName());
        diskOfferingResponse.setDisplayText(offering.getDisplayText());
        diskOfferingResponse.setCreated(offering.getCreated());
        diskOfferingResponse.setDiskSize(offering.getDiskSize() / (1024 * 1024 * 1024));

                diskOfferingResponse.setDomain(offering.getDomainName());
                diskOfferingResponse.setDomainId(offering.getDomainUuid());

        diskOfferingResponse.setTags(offering.getTags());
        diskOfferingResponse.setCustomized(offering.isCustomized());
        diskOfferingResponse.setStorageType(offering.isUseLocalStorage() ? ServiceOffering.StorageType.local.toString() : ServiceOffering.StorageType.shared.toString());
        diskOfferingResponse.setObjectName("diskoffering");
        return diskOfferingResponse;
    }


    @Override
    public DiskOfferingJoinVO newDiskOfferingView(DiskOffering offering) {
        SearchCriteria<DiskOfferingJoinVO> sc = dofIdSearch.create();
        sc.setParameters("id", offering.getId());
        List<DiskOfferingJoinVO> offerings = searchIncludingRemoved(sc, null, null, false);
        assert offerings != null && offerings.size() == 1 : "No disk offering found for offering id " + offering.getId();
        return offerings.get(0);
    }

    @Override
    public List<DiskOfferingJoinVO> searchIncludingRemoved(SearchCriteria<DiskOfferingJoinVO> sc, final Filter filter, final Boolean lock, final boolean cache) {
        sc.addAnd(_typeAttr, Op.EQ, Type.Disk);
        return super.searchIncludingRemoved(sc, filter, lock, cache);
    }

    @Override
    public <K> List<K> customSearchIncludingRemoved(SearchCriteria<K> sc, final Filter filter) {
        sc.addAnd(_typeAttr, Op.EQ, Type.Disk);
        return super.customSearchIncludingRemoved(sc, filter);
    }
}
