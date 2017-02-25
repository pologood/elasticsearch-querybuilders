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

package org.codelibs.elasticsearch.action.admin.cluster.storedscripts;

import org.codelibs.elasticsearch.action.ActionListener;
import org.codelibs.elasticsearch.action.support.ActionFilters;
import org.codelibs.elasticsearch.action.support.master.TransportMasterNodeAction;
import org.codelibs.elasticsearch.cluster.ClusterState;
import org.codelibs.elasticsearch.cluster.block.ClusterBlockException;
import org.codelibs.elasticsearch.cluster.block.ClusterBlockLevel;
import org.codelibs.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.codelibs.elasticsearch.cluster.service.ClusterService;
import org.codelibs.elasticsearch.common.inject.Inject;
import org.codelibs.elasticsearch.common.settings.Settings;
import org.codelibs.elasticsearch.script.ScriptService;
import org.codelibs.elasticsearch.threadpool.ThreadPool;
import org.codelibs.elasticsearch.transport.TransportService;

public class TransportPutStoredScriptAction extends TransportMasterNodeAction<PutStoredScriptRequest, PutStoredScriptResponse> {

    private final ScriptService scriptService;

    @Inject
    public TransportPutStoredScriptAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                          ThreadPool threadPool, ActionFilters actionFilters,
                                          IndexNameExpressionResolver indexNameExpressionResolver, ScriptService scriptService) {
        super(settings, PutStoredScriptAction.NAME, transportService, clusterService, threadPool, actionFilters,
                indexNameExpressionResolver, PutStoredScriptRequest::new);
        this.scriptService = scriptService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.SAME;
    }

    @Override
    protected PutStoredScriptResponse newResponse() {
        return new PutStoredScriptResponse();
    }

    @Override
    protected void masterOperation(PutStoredScriptRequest request, ClusterState state,
                                   ActionListener<PutStoredScriptResponse> listener) throws Exception {
        scriptService.storeScript(clusterService, request, listener);
    }

    @Override
    protected ClusterBlockException checkBlock(PutStoredScriptRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
    }

}
