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

package org.codelibs.elasticsearch.rest.action.admin.indices;

import org.codelibs.elasticsearch.ExceptionsHelper;
import org.codelibs.elasticsearch.action.ActionListener;
import org.codelibs.elasticsearch.action.admin.indices.alias.exists.AliasesExistResponse;
import org.codelibs.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.codelibs.elasticsearch.action.support.IndicesOptions;
import org.codelibs.elasticsearch.client.node.NodeClient;
import org.codelibs.elasticsearch.common.Strings;
import org.codelibs.elasticsearch.common.bytes.BytesArray;
import org.codelibs.elasticsearch.common.inject.Inject;
import org.codelibs.elasticsearch.common.settings.Settings;
import org.codelibs.elasticsearch.rest.BaseRestHandler;
import org.codelibs.elasticsearch.rest.BytesRestResponse;
import org.codelibs.elasticsearch.rest.RestController;
import org.codelibs.elasticsearch.rest.RestRequest;

import java.io.IOException;

import static org.codelibs.elasticsearch.rest.RestRequest.Method.HEAD;
import static org.codelibs.elasticsearch.rest.RestStatus.NOT_FOUND;
import static org.codelibs.elasticsearch.rest.RestStatus.OK;

public class RestAliasesExistAction extends BaseRestHandler {

    @Inject
    public RestAliasesExistAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(HEAD, "/_alias/{name}", this);
        controller.registerHandler(HEAD, "/{index}/_alias/{name}", this);
        controller.registerHandler(HEAD, "/{index}/_alias", this);
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        String[] aliases = request.paramAsStringArray("name", Strings.EMPTY_ARRAY);
        final String[] indices = Strings.splitStringByCommaToArray(request.param("index"));
        GetAliasesRequest getAliasesRequest = new GetAliasesRequest(aliases);
        getAliasesRequest.indices(indices);
        getAliasesRequest.indicesOptions(IndicesOptions.fromRequest(request, getAliasesRequest.indicesOptions()));
        getAliasesRequest.local(request.paramAsBoolean("local", getAliasesRequest.local()));

        return channel -> client.admin().indices().aliasesExist(getAliasesRequest, new ActionListener<AliasesExistResponse>() {

            @Override
            public void onResponse(AliasesExistResponse response) {
                try {
                    if (response.isExists()) {
                        channel.sendResponse(new BytesRestResponse(OK, BytesRestResponse.TEXT_CONTENT_TYPE, BytesArray.EMPTY));
                    } else {
                        channel.sendResponse(new BytesRestResponse(NOT_FOUND, BytesRestResponse.TEXT_CONTENT_TYPE, BytesArray.EMPTY));
                    }
                } catch (Exception e) {
                    onFailure(e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                try {
                    channel.sendResponse(
                        new BytesRestResponse(ExceptionsHelper.status(e), BytesRestResponse.TEXT_CONTENT_TYPE, BytesArray.EMPTY));
                } catch (Exception inner) {
                    inner.addSuppressed(e);
                    logger.error("Failed to send failure response", inner);
                }
            }

        });
    }
}
