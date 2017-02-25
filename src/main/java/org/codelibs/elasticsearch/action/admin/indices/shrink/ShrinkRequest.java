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
package org.codelibs.elasticsearch.action.admin.indices.shrink;

import org.codelibs.elasticsearch.action.ActionRequestValidationException;
import org.codelibs.elasticsearch.action.IndicesRequest;
import org.codelibs.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.codelibs.elasticsearch.action.support.ActiveShardCount;
import org.codelibs.elasticsearch.action.support.IndicesOptions;
import org.codelibs.elasticsearch.action.support.master.AcknowledgedRequest;
import org.codelibs.elasticsearch.common.ParseField;
import org.codelibs.elasticsearch.common.ParseFieldMatcherSupplier;
import org.codelibs.elasticsearch.common.io.stream.StreamInput;
import org.codelibs.elasticsearch.common.io.stream.StreamOutput;
import org.codelibs.elasticsearch.common.xcontent.ObjectParser;

import java.io.IOException;
import java.util.Objects;

import static org.codelibs.elasticsearch.action.ValidateActions.addValidationError;

/**
 * Request class to shrink an index into a single shard
 */
public class ShrinkRequest extends AcknowledgedRequest<ShrinkRequest> implements IndicesRequest {

    public static final ObjectParser<ShrinkRequest, ParseFieldMatcherSupplier> PARSER =
        new ObjectParser<>("shrink_request", null);
    static {
        PARSER.declareField((parser, request, parseFieldMatcherSupplier) ->
                request.getShrinkIndexRequest().settings(parser.map()),
            new ParseField("settings"), ObjectParser.ValueType.OBJECT);
        PARSER.declareField((parser, request, parseFieldMatcherSupplier) ->
                request.getShrinkIndexRequest().aliases(parser.map()),
            new ParseField("aliases"), ObjectParser.ValueType.OBJECT);
    }

    private CreateIndexRequest shrinkIndexRequest;
    private String sourceIndex;

    ShrinkRequest() {}

    public ShrinkRequest(String targetIndex, String sourceindex) {
        this.shrinkIndexRequest = new CreateIndexRequest(targetIndex);
        this.sourceIndex = sourceindex;
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = shrinkIndexRequest == null ? null : shrinkIndexRequest.validate();
        if (sourceIndex == null) {
            validationException = addValidationError("source index is missing", validationException);
        }
        if (shrinkIndexRequest == null) {
            validationException = addValidationError("shrink index request is missing", validationException);
        }
        return validationException;
    }

    public void setSourceIndex(String index) {
        this.sourceIndex = index;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        shrinkIndexRequest = new CreateIndexRequest();
        shrinkIndexRequest.readFrom(in);
        sourceIndex = in.readString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        shrinkIndexRequest.writeTo(out);
        out.writeString(sourceIndex);
    }

    @Override
    public String[] indices() {
        return new String[] {sourceIndex};
    }

    @Override
    public IndicesOptions indicesOptions() {
        return IndicesOptions.lenientExpandOpen();
    }

    public void setShrinkIndex(CreateIndexRequest shrinkIndexRequest) {
        this.shrinkIndexRequest = Objects.requireNonNull(shrinkIndexRequest, "shrink index request must not be null");
    }

    /**
     * Returns the {@link CreateIndexRequest} for the shrink index
     */
    public CreateIndexRequest getShrinkIndexRequest() {
        return shrinkIndexRequest;
    }

    /**
     * Returns the source index name
     */
    public String getSourceIndex() {
        return sourceIndex;
    }

    /**
     * Sets the number of shard copies that should be active for creation of the
     * new shrunken index to return. Defaults to {@link ActiveShardCount#DEFAULT}, which will
     * wait for one shard copy (the primary) to become active. Set this value to
     * {@link ActiveShardCount#ALL} to wait for all shards (primary and all replicas) to be active
     * before returning. Otherwise, use {@link ActiveShardCount#from(int)} to set this value to any
     * non-negative integer, up to the number of copies per shard (number of replicas + 1),
     * to wait for the desired amount of shard copies to become active before returning.
     * Index creation will only wait up until the timeout value for the number of shard copies
     * to be active before returning.  Check {@link ShrinkResponse#isShardsAcked()} to
     * determine if the requisite shard copies were all started before returning or timing out.
     *
     * @param waitForActiveShards number of active shard copies to wait on
     */
    public void setWaitForActiveShards(ActiveShardCount waitForActiveShards) {
        this.getShrinkIndexRequest().waitForActiveShards(waitForActiveShards);
    }

    /**
     * A shortcut for {@link #setWaitForActiveShards(ActiveShardCount)} where the numerical
     * shard count is passed in, instead of having to first call {@link ActiveShardCount#from(int)}
     * to get the ActiveShardCount.
     */
    public void setWaitForActiveShards(final int waitForActiveShards) {
        setWaitForActiveShards(ActiveShardCount.from(waitForActiveShards));
    }
}
