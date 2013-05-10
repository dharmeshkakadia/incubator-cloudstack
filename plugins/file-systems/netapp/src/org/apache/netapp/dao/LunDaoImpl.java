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
package org.apache.netapp.dao;

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;
import org.apache.netapp.LunVO;
import org.apache.netapp.NetappVolumeVO;
import org.apache.netapp.PoolVO;
import org.apache.utils.db.Filter;
import org.apache.utils.db.GenericDaoBase;
import org.apache.utils.db.SearchBuilder;
import org.apache.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;


@Component
@Local(value={LunDao.class})
public class LunDaoImpl extends GenericDaoBase<LunVO, Long> implements LunDao {
    private static final Logger s_logger = Logger.getLogger(PoolDaoImpl.class);
		
    protected final SearchBuilder<LunVO> LunSearch;    
    protected final SearchBuilder<LunVO> LunNameSearch;    
    	    
	protected LunDaoImpl() {
        
        LunSearch = createSearchBuilder();
        LunSearch.and("volumeId", LunSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        LunSearch.done();

        LunNameSearch = createSearchBuilder();
        LunNameSearch.and("name", LunNameSearch.entity().getLunName(), SearchCriteria.Op.EQ);
        LunNameSearch.done();
        
	}

	@Override
    public List<LunVO> listLunsByVolId(Long volId) {
		Filter searchFilter = new Filter(LunVO.class, "id", Boolean.TRUE, Long.valueOf(0), Long.valueOf(10000));
		
        SearchCriteria sc = LunSearch.create();
        sc.setParameters("volumeId", volId);
        List<LunVO> lunList = listBy(sc,searchFilter);
        
        return lunList;
    }


	@Override
    public LunVO findByName(String name) {
        SearchCriteria sc = LunNameSearch.create();
        sc.setParameters("name", name);
        return findOneBy(sc);
    }
}
