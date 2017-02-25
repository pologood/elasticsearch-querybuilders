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

import org.codelibs.elasticsearch.action.ActionResponse;
import org.codelibs.elasticsearch.common.io.stream.StreamInput;
import org.codelibs.elasticsearch.common.io.stream.StreamOutput;
import org.codelibs.elasticsearch.common.xcontent.StatusToXContent;
import org.codelibs.elasticsearch.common.xcontent.XContentBuilder;
import org.codelibs.elasticsearch.rest.RestStatus;

import java.io.IOException;

import static org.codelibs.elasticsearch.rest.RestStatus.NOT_FOUND;
import static org.codelibs.elasticsearch.rest.RestStatus.OK;

/**
 */
public class ClearScrollResponse extends ActionResponse implements StatusToXContent {

    private boolean succeeded;
    private int numFreed;

    public ClearScrollResponse(boolean succeeded, int numFreed) {
        this.succeeded = succeeded;
        this.numFreed = numFreed;
    }

    ClearScrollResponse() {
    }

    /**
     * @return Whether the attempt to clear a scroll was successful.
     */
    public boolean isSucceeded() {
        return succeeded;
    }

    /**
     * @return The number of search contexts that were freed. If this is <code>0</code> the assumption can be made,
     * that the scroll id specified in the request did not exist. (never existed, was expired, or completely consumed)
     */
    public int getNumFreed() {
        return numFreed;
    }

    @Override
    public RestStatus status() {
        return numFreed == 0 ? NOT_FOUND : OK;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.field(Fields.SUCCEEDED, succeeded);
        builder.field(Fields.NUMFREED, numFreed);
        return builder;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        succeeded = in.readBoolean();
        numFreed = in.readVInt();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeBoolean(succeeded);
        out.writeVInt(numFreed);
    }

    static final class Fields {
        static final String SUCCEEDED = "succeeded";
        static final String NUMFREED = "num_freed";
    }

}
