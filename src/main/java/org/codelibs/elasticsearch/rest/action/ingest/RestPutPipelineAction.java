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

import org.codelibs.elasticsearch.action.ingest.PutPipelineRequest;
import org.codelibs.elasticsearch.client.node.NodeClient;
import org.codelibs.elasticsearch.common.inject.Inject;
import org.codelibs.elasticsearch.common.settings.Settings;
import org.codelibs.elasticsearch.rest.BaseRestHandler;
import org.codelibs.elasticsearch.rest.RestController;
import org.codelibs.elasticsearch.rest.RestRequest;
import org.codelibs.elasticsearch.rest.action.AcknowledgedRestListener;
import org.codelibs.elasticsearch.rest.action.RestActions;

import java.io.IOException;


public class RestPutPipelineAction extends BaseRestHandler {

    @Inject
    public RestPutPipelineAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(RestRequest.Method.PUT, "/_ingest/pipeline/{id}", this);
    }

    @Override
    public RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        PutPipelineRequest request = new PutPipelineRequest(restRequest.param("id"), restRequest.contentOrSourceParam());
        request.masterNodeTimeout(restRequest.paramAsTime("master_timeout", request.masterNodeTimeout()));
        request.timeout(restRequest.paramAsTime("timeout", request.timeout()));
        return channel -> client.admin().cluster().putPipeline(request, new AcknowledgedRestListener<>(channel));
    }

}
