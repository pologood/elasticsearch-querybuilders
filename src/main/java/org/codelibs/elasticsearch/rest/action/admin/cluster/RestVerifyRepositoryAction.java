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

package org.codelibs.elasticsearch.rest.action.admin.cluster;

import org.codelibs.elasticsearch.action.admin.cluster.repositories.verify.VerifyRepositoryRequest;
import org.codelibs.elasticsearch.client.node.NodeClient;
import org.codelibs.elasticsearch.common.inject.Inject;
import org.codelibs.elasticsearch.common.settings.Settings;
import org.codelibs.elasticsearch.rest.BaseRestHandler;
import org.codelibs.elasticsearch.rest.RestController;
import org.codelibs.elasticsearch.rest.RestRequest;
import org.codelibs.elasticsearch.rest.action.RestToXContentListener;

import java.io.IOException;

import static org.codelibs.elasticsearch.client.Requests.verifyRepositoryRequest;
import static org.codelibs.elasticsearch.rest.RestRequest.Method.POST;

public class RestVerifyRepositoryAction extends BaseRestHandler {

    @Inject
    public RestVerifyRepositoryAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(POST, "/_snapshot/{repository}/_verify", this);
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        VerifyRepositoryRequest verifyRepositoryRequest = verifyRepositoryRequest(request.param("repository"));
        verifyRepositoryRequest.masterNodeTimeout(request.paramAsTime("master_timeout", verifyRepositoryRequest.masterNodeTimeout()));
        verifyRepositoryRequest.timeout(request.paramAsTime("timeout", verifyRepositoryRequest.timeout()));
        return channel -> client.admin().cluster().verifyRepository(verifyRepositoryRequest, new RestToXContentListener<>(channel));
    }
}