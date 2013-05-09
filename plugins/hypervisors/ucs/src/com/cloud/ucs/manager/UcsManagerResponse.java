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
//
package com.cloud.ucs.manager;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.EntityReference;

import org.apache.cloudstack.api.BaseResponse;
import org.apache.serializer.Param;

import com.cloud.ucs.database.UcsManagerVO;
import com.google.gson.annotations.SerializedName;
@EntityReference(value=UcsManagerVO.class)
public class UcsManagerResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the ID of the ucs manager")
    private String id;

    @SerializedName(ApiConstants.NAME) @Param(description="the name of ucs manager")
    private String name;

    @SerializedName(ApiConstants.URL) @Param(description="the url of ucs manager")
    private String url;

    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the zone ID of ucs manager")
    private String zoneId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }
}
