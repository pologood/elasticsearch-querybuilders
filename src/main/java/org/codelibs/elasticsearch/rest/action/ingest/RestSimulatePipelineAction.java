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

package org.codelibs.elasticsearch.rest.action.ingest;

import org.codelibs.elasticsearch.action.ingest.SimulatePipelineRequest;
import org.codelibs.elasticsearch.client.node.NodeClient;
import org.codelibs.elasticsearch.common.inject.Inject;
import org.codelibs.elasticsearch.common.settings.Settings;
import org.codelibs.elasticsearch.rest.BaseRestHandler;
import org.codelibs.elasticsearch.rest.RestController;
import org.codelibs.elasticsearch.rest.RestRequest;
import org.codelibs.elasticsearch.rest.action.RestActions;
import org.codelibs.elasticsearch.rest.action.RestToXContentListener;

import java.io.IOException;

public class RestSimulatePipelineAction extends BaseRestHandler {

    @Inject
    public RestSimulatePipelineAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(RestRequest.Method.POST, "/_ingest/pipeline/{id}/_simulate", this);
        controller.registerHandler(RestRequest.Method.GET, "/_ingest/pipeline/{id}/_simulate", this);
        controller.registerHandler(RestRequest.Method.POST, "/_ingest/pipeline/_simulate", this);
        controller.registerHandler(RestRequest.Method.GET, "/_ingest/pipeline/_simulate", this);
    }

    @Override
    public RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        SimulatePipelineRequest request = new SimulatePipelineRequest(restRequest.contentOrSourceParam());
        request.setId(restRequest.param("id"));
        request.setVerbose(restRequest.paramAsBoolean("verbose", false));
        return channel -> client.admin().cluster().simulatePipeline(request, new RestToXContentListener<>(channel));
    }
}
