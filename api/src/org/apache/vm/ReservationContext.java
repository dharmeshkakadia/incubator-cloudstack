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
package org.apache.vm;

import org.apache.domain.Domain;
import org.apache.domain.PartOf;
import org.apache.user.Account;
import org.apache.user.OwnedBy;
import org.apache.user.User;
import org.apache.utils.Journal;


/**
 */
public interface ReservationContext extends PartOf, OwnedBy {
    /**
     * @return the user making the call.
     */
    User getCaller();

    /**
     * @return the account
     */
    Account getAccount();

    /**
     * @return the domain.
     */
    Domain getDomain();

    /**
     * @return the journal
     */
    Journal getJournal();

    /**
     * @return the reservation id.
     */
    String getReservationId();
}
