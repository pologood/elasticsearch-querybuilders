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

package org.codelibs.elasticsearch.action.admin.indices.alias;

import org.codelibs.elasticsearch.action.ActionListener;
import org.codelibs.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions;
import org.codelibs.elasticsearch.action.support.ActionFilters;
import org.codelibs.elasticsearch.action.support.master.TransportMasterNodeAction;
import org.codelibs.elasticsearch.cluster.ClusterState;
import org.codelibs.elasticsearch.cluster.ack.ClusterStateUpdateResponse;
import org.codelibs.elasticsearch.cluster.block.ClusterBlockException;
import org.codelibs.elasticsearch.cluster.block.ClusterBlockLevel;
import org.codelibs.elasticsearch.cluster.metadata.AliasAction;
import org.codelibs.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.codelibs.elasticsearch.cluster.metadata.MetaDataIndexAliasesService;
import org.codelibs.elasticsearch.cluster.service.ClusterService;
import org.codelibs.elasticsearch.common.inject.Inject;
import org.codelibs.elasticsearch.common.settings.Settings;
import org.codelibs.elasticsearch.rest.action.admin.indices.AliasesNotFoundException;
import org.codelibs.elasticsearch.threadpool.ThreadPool;
import org.codelibs.elasticsearch.transport.TransportService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.unmodifiableList;

/**
 * Add/remove aliases action
 */
public class TransportIndicesAliasesAction extends TransportMasterNodeAction<IndicesAliasesRequest, IndicesAliasesResponse> {

    private final MetaDataIndexAliasesService indexAliasesService;

    @Inject
    public TransportIndicesAliasesAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                         ThreadPool threadPool, MetaDataIndexAliasesService indexAliasesService,
                                         ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver) {
        super(settings, IndicesAliasesAction.NAME, transportService, clusterService, threadPool, actionFilters, indexNameExpressionResolver, IndicesAliasesRequest::new);
        this.indexAliasesService = indexAliasesService;
    }

    @Override
    protected String executor() {
        // we go async right away...
        return ThreadPool.Names.SAME;
    }

    @Override
    protected IndicesAliasesResponse newResponse() {
        return new IndicesAliasesResponse();
    }

    @Override
    protected ClusterBlockException checkBlock(IndicesAliasesRequest request, ClusterState state) {
        Set<String> indices = new HashSet<>();
        for (AliasActions aliasAction : request.aliasActions()) {
            for (String index : aliasAction.indices()) {
                indices.add(index);
            }
        }
        return state.blocks().indicesBlockedException(ClusterBlockLevel.METADATA_WRITE, indices.toArray(new String[indices.size()]));
    }

    @Override
    protected void masterOperation(final IndicesAliasesRequest request, final ClusterState state, final ActionListener<IndicesAliasesResponse> listener) {

        //Expand the indices names
        List<AliasActions> actions = request.aliasActions();
        List<AliasAction> finalActions = new ArrayList<>();

        // Resolve all the AliasActions into AliasAction instances and gather all the aliases
        Set<String> aliases = new HashSet<>();
        for (AliasActions action : actions) {
            String[] concreteIndices = indexNameExpressionResolver.concreteIndexNames(state, request.indicesOptions(), action.indices());
            Collections.addAll(aliases, action.aliases());
            for (String index : concreteIndices) {
                switch (action.actionType()) {
                case ADD:
                    for (String alias : action.concreteAliases(state.metaData(), index)) {
                        finalActions.add(new AliasAction.Add(index, alias, action.filter(), action.indexRouting(), action.searchRouting()));
                    }
                    break;
                case REMOVE:
                    for (String alias : action.concreteAliases(state.metaData(), index)) {
                        finalActions.add(new AliasAction.Remove(index, alias));
                    }
                    break;
                case REMOVE_INDEX:
                    finalActions.add(new AliasAction.RemoveIndex(index));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported action [" + action.actionType() + "]");
                }
            }
        }
        if (finalActions.isEmpty() && false == actions.isEmpty()) {
            throw new AliasesNotFoundException(aliases.toArray(new String[aliases.size()]));
        }
        request.aliasActions().clear();
        IndicesAliasesClusterStateUpdateRequest updateRequest = new IndicesAliasesClusterStateUpdateRequest(unmodifiableList(finalActions))
                .ackTimeout(request.timeout()).masterNodeTimeout(request.masterNodeTimeout());

        indexAliasesService.indicesAliases(updateRequest, new ActionListener<ClusterStateUpdateResponse>() {
            @Override
            public void onResponse(ClusterStateUpdateResponse response) {
                listener.onResponse(new IndicesAliasesResponse(response.isAcknowledged()));
            }

            @Override
            public void onFailure(Exception t) {
                logger.debug("failed to perform aliases", t);
                listener.onFailure(t);
            }
        });
    }
}
