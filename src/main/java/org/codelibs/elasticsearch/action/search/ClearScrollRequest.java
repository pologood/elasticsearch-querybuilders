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

import org.codelibs.elasticsearch.action.ActionRequest;
import org.codelibs.elasticsearch.action.ActionRequestValidationException;
import org.codelibs.elasticsearch.common.io.stream.StreamInput;
import org.codelibs.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.codelibs.elasticsearch.action.ValidateActions.addValidationError;

/**
 */
public class ClearScrollRequest extends ActionRequest {

    private List<String> scrollIds;

    public List<String> getScrollIds() {
        return scrollIds;
    }

    public void setScrollIds(List<String> scrollIds) {
        this.scrollIds = scrollIds;
    }

    public void addScrollId(String scrollId) {
        if (scrollIds == null) {
            scrollIds = new ArrayList<>();
        }
        scrollIds.add(scrollId);
    }

    public List<String> scrollIds() {
        return scrollIds;
    }

    public void scrollIds(List<String> scrollIds) {
        this.scrollIds = scrollIds;
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if (scrollIds == null || scrollIds.isEmpty()) {
            validationException = addValidationError("no scroll ids specified", validationException);
        }
        return validationException;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        scrollIds = Arrays.asList(in.readStringArray());
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        if (scrollIds == null) {
            out.writeVInt(0);
        } else {
            out.writeStringArray(scrollIds.toArray(new String[scrollIds.size()]));
        }
    }

}
