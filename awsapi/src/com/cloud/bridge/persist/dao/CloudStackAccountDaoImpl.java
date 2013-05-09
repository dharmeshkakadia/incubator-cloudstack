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
package com.cloud.bridge.persist.dao;

import javax.ejb.Local;

import org.apache.utils.db.GenericDaoBase;
import org.apache.utils.db.SearchBuilder;
import org.apache.utils.db.SearchCriteria;
import org.apache.utils.db.Transaction;
import org.springframework.stereotype.Component;

import com.cloud.bridge.model.CloudStackAccountVO;

@Component
@Local(value={CloudStackAccountDao.class})
public class CloudStackAccountDaoImpl extends GenericDaoBase<CloudStackAccountVO, String> implements CloudStackAccountDao {
    
    @Override
    public String getDefaultZoneId(String accountId) {
        
        SearchBuilder<CloudStackAccountVO> SearchByUUID = createSearchBuilder();
        Transaction txn = Transaction.open(Transaction.CLOUD_DB);
        try {
            txn.start();
            SearchByUUID.and("uuid", SearchByUUID.entity().getUuid(),
                    SearchCriteria.Op.EQ);
            SearchByUUID.done();
            SearchCriteria<CloudStackAccountVO> sc = SearchByUUID.create();
            sc.setParameters("uuid", accountId);
            CloudStackAccountVO account = findOneBy(sc);
            if (null != account) 
                if(null != account.getDefaultZoneId())
                    return Long.toString(account.getDefaultZoneId());
            return null;
        } finally {
            txn.commit();
            txn.close();
        }

    }
    

}
