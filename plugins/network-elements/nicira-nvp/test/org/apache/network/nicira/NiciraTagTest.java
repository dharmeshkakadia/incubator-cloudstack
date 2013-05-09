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
package org.apache.network.nicira;

import org.apache.network.nicira.NiciraNvpTag;
import org.junit.Test;
import static org.junit.Assert.*;

public class NiciraTagTest {
    @Test
    public void testCreateTag() {
        NiciraNvpTag tag = new NiciraNvpTag("scope","tag");
        assertEquals("scope part set", "scope", tag.getScope());
        assertEquals("tag part set", "tag", tag.getTag());
    }

    @Test
    public void testCreateLongTag() {
        NiciraNvpTag tag = new NiciraNvpTag("scope","verylongtagthatshouldattheminimumexceedthefortycharacterlenght");
        assertEquals("scope part set", "scope", tag.getScope());
        assertEquals("tag part set", "verylongtagthatshouldattheminimumexceedt", tag.getTag());
    }

    @Test
    public void testSetTag() {
        NiciraNvpTag tag = new NiciraNvpTag();
        tag.setScope("scope");
        tag.setTag("tag");
        assertEquals("scope part set", "scope", tag.getScope());
        assertEquals("tag part set", "tag", tag.getTag());
    }

    @Test
    public void testSetLongTag() {
        NiciraNvpTag tag = new NiciraNvpTag();
        tag.setScope("scope");
        tag.setTag("verylongtagthatshouldattheminimumexceedthefortycharacterlenght");
        assertEquals("scope part set", "scope", tag.getScope());
        assertEquals("tag part set", "verylongtagthatshouldattheminimumexceedt", tag.getTag());
    }
}