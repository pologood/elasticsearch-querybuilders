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

package org.codelibs.elasticsearch.rest.action.cat;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import org.codelibs.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.codelibs.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.codelibs.elasticsearch.client.node.NodeClient;
import org.codelibs.elasticsearch.cluster.metadata.IndexTemplateMetaData;
import org.codelibs.elasticsearch.cluster.metadata.MetaData;
import org.codelibs.elasticsearch.common.Table;
import org.codelibs.elasticsearch.common.inject.Inject;
import org.codelibs.elasticsearch.common.regex.Regex;
import org.codelibs.elasticsearch.common.settings.Settings;
import org.codelibs.elasticsearch.rest.RestController;
import org.codelibs.elasticsearch.rest.RestRequest;
import org.codelibs.elasticsearch.rest.RestResponse;
import org.codelibs.elasticsearch.rest.action.RestResponseListener;

import static org.codelibs.elasticsearch.rest.RestRequest.Method.GET;

public class RestTemplatesAction extends AbstractCatAction {
    @Inject
    public RestTemplatesAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(GET, "/_cat/templates", this);
        controller.registerHandler(GET, "/_cat/templates/{name}", this);
    }

    @Override
    protected void documentation(StringBuilder sb) {
        sb.append("/_cat/templates\n");
    }

    @Override
    protected RestChannelConsumer doCatRequest(final RestRequest request, NodeClient client) {
        final String matchPattern = request.hasParam("name") ? request.param("name") : null;
        final ClusterStateRequest clusterStateRequest = new ClusterStateRequest();
        clusterStateRequest.clear().metaData(true);
        clusterStateRequest.local(request.paramAsBoolean("local", clusterStateRequest.local()));
        clusterStateRequest.masterNodeTimeout(request.paramAsTime("master_timeout", clusterStateRequest.masterNodeTimeout()));

        return channel -> client.admin().cluster().state(clusterStateRequest, new RestResponseListener<ClusterStateResponse>(channel) {
            @Override
            public RestResponse buildResponse(ClusterStateResponse clusterStateResponse) throws Exception {
                return RestTable.buildResponse(buildTable(request, clusterStateResponse, matchPattern), channel);
            }
        });
    }

    @Override
    protected Table getTableWithHeader(RestRequest request) {
        Table table = new Table();
        table.startHeaders();
        table.addCell("name", "alias:n;desc:template name");
        table.addCell("template", "alias:t;desc:template pattern string");
        table.addCell("order", "alias:o;desc:template application order number");
        table.addCell("version", "alias:v;desc:version");
        table.endHeaders();
        return table;
    }

    private Table buildTable(RestRequest request, ClusterStateResponse clusterStateResponse, String patternString) {
        Table table = getTableWithHeader(request);
        MetaData metadata = clusterStateResponse.getState().metaData();
        for (ObjectObjectCursor<String, IndexTemplateMetaData> entry : metadata.templates()) {
            IndexTemplateMetaData indexData = entry.value;
            if (patternString == null || Regex.simpleMatch(patternString, indexData.name())) {
                table.startRow();
                table.addCell(indexData.name());
                table.addCell(indexData.getTemplate());
                table.addCell(indexData.getOrder());
                table.addCell(indexData.getVersion());
                table.endRow();
            }
        }
        return table;
    }
}
