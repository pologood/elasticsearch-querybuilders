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

package org.codelibs.elasticsearch.rest.action;

import org.codelibs.elasticsearch.common.xcontent.ToXContent;
import org.codelibs.elasticsearch.common.xcontent.XContentBuilder;
import org.codelibs.elasticsearch.rest.BytesRestResponse;
import org.codelibs.elasticsearch.rest.RestChannel;
import org.codelibs.elasticsearch.rest.RestResponse;
import org.codelibs.elasticsearch.rest.RestStatus;

/**
 * A REST based action listener that assumes the response is of type {@link ToXContent} and automatically
 * builds an XContent based response (wrapping the toXContent in startObject/endObject).
 */
public class RestToXContentListener<Response extends ToXContent> extends RestResponseListener<Response> {

    public RestToXContentListener(RestChannel channel) {
        super(channel);
    }

    @Override
    public final RestResponse buildResponse(Response response) throws Exception {
        return buildResponse(response, channel.newBuilder());
    }

    public final RestResponse buildResponse(Response response, XContentBuilder builder) throws Exception {
        if (wrapInObject()) {
            builder.startObject();
        }
        response.toXContent(builder, channel.request());
        if (wrapInObject()) {
            builder.endObject();
        }
        return new BytesRestResponse(getStatus(response), builder);
    }

    protected boolean wrapInObject() {
        //Ideally, the toXContent method starts with startObject and ends with endObject.
        //In practice, we have many places where toXContent produces a json fragment that's not valid by itself. We will
        //migrate those step by step, so that we never have to start objects here, and we can remove this method.
        return true;
    }

    protected RestStatus getStatus(Response response) {
        return RestStatus.OK;
    }
}