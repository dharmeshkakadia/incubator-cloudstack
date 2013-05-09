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

import javax.ejb.Local;

import org.apache.storage.GuestOSVO;
import org.apache.storage.VMTemplateHostVO;
import org.apache.utils.db.GenericDaoBase;
import org.apache.utils.db.SearchBuilder;
import org.apache.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;


@Component
@Local (value={GuestOSDao.class})
public class GuestOSDaoImpl extends GenericDaoBase<GuestOSVO, Long> implements GuestOSDao {
    
    
    protected final SearchBuilder<GuestOSVO> Search;
    
	protected GuestOSDaoImpl() {
        Search = createSearchBuilder();
        Search.and("display_name", Search.entity().getDisplayName(), SearchCriteria.Op.EQ);
        Search.done();
	}
	
   @Override
    public GuestOSVO listByDisplayName(String displayName) {
        SearchCriteria<GuestOSVO> sc = Search.create();
        sc.setParameters("display_name", displayName);
        return findOneBy(sc);
    }

}
