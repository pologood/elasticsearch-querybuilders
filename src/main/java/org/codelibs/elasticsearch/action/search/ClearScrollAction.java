/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codelibs.elasticsearch.action.search;

import org.codelibs.elasticsearch.action.Action;
import org.codelibs.elasticsearch.client.ElasticsearchClient;

/**
 */
public class ClearScrollAction extends Action<ClearScrollRequest, ClearScrollResponse, ClearScrollRequestBuilder> {

    public static final ClearScrollAction INSTANCE = new ClearScrollAction();
    public static final String NAME = "indices:data/read/scroll/clear";

    private ClearScrollAction() {
        super(NAME);
    }

    @Override
    public ClearScrollResponse newResponse() {
        return new ClearScrollResponse();
    }

    @Override
    public ClearScrollRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new ClearScrollRequestBuilder(client, this);
    }
}
