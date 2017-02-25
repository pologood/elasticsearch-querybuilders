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

import org.codelibs.elasticsearch.action.ActionRequestValidationException;
import org.codelibs.elasticsearch.action.support.master.AcknowledgedRequest;
import org.codelibs.elasticsearch.common.io.stream.StreamInput;
import org.codelibs.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

import static org.codelibs.elasticsearch.action.ValidateActions.addValidationError;

public class DeleteStoredScriptRequest extends AcknowledgedRequest<DeleteStoredScriptRequest> {

    private String id;
    private String scriptLang;

    DeleteStoredScriptRequest() {
    }

    public DeleteStoredScriptRequest(String scriptLang, String id) {
        this.scriptLang = scriptLang;
        this.id = id;
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if (id == null) {
            validationException = addValidationError("id is missing", validationException);
        } else if (id.contains("#")) {
            validationException = addValidationError("id can't contain: '#'", validationException);
        }
        if (scriptLang == null) {
            validationException = addValidationError("lang is missing", validationException);
        } else if (scriptLang.contains("#")) {
            validationException = addValidationError("lang can't contain: '#'", validationException);
        }
        return validationException;
    }

    public String scriptLang() {
        return scriptLang;
    }

    public DeleteStoredScriptRequest scriptLang(String type) {
        this.scriptLang = type;
        return this;
    }

    public String id() {
        return id;
    }

    public DeleteStoredScriptRequest id(String id) {
        this.id = id;
        return this;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        scriptLang = in.readString();
        id = in.readString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(scriptLang);
        out.writeString(id);
    }

    @Override
    public String toString() {
        return "delete script {[" + scriptLang + "][" + id + "]}";
    }
}
