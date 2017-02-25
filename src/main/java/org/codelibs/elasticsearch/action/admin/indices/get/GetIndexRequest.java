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

package org.codelibs.elasticsearch.action.admin.indices.get;

import org.codelibs.elasticsearch.action.ActionRequestValidationException;
import org.codelibs.elasticsearch.action.support.master.info.ClusterInfoRequest;
import org.codelibs.elasticsearch.common.io.stream.StreamInput;
import org.codelibs.elasticsearch.common.io.stream.StreamOutput;
import org.codelibs.elasticsearch.common.util.ArrayUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * A request to delete an index. Best created with {@link org.codelibs.elasticsearch.client.Requests#deleteIndexRequest(String)}.
 */
public class GetIndexRequest extends ClusterInfoRequest<GetIndexRequest> {

    public static enum Feature {
        ALIASES((byte) 0, "_aliases", "_alias"),
        MAPPINGS((byte) 1, "_mappings", "_mapping"),
        SETTINGS((byte) 2, "_settings");

        private static final Feature[] FEATURES = new Feature[Feature.values().length];

        static {
            for (Feature feature : Feature.values()) {
                assert feature.id() < FEATURES.length && feature.id() >= 0;
                FEATURES[feature.id] = feature;
            }
        }

        private final List<String> validNames;
        private final String preferredName;
        private final byte id;

        private Feature(byte id, String... validNames) {
            assert validNames != null && validNames.length > 0;
            this.id = id;
            this.validNames = Arrays.asList(validNames);
            this.preferredName = validNames[0];
        }

        public byte id() {
            return id;
        }

        public String preferredName() {
            return preferredName;
        }

        public boolean validName(String name) {
            return this.validNames.contains(name);
        }

        public static Feature fromName(String name) {
            for (Feature feature : Feature.values()) {
                if (feature.validName(name)) {
                    return feature;
                }
            }
            throw new IllegalArgumentException("No endpoint or operation is available at [" + name + "]");
        }

        public static Feature fromId(byte id) {
            if (id < 0 || id >= FEATURES.length) {
                throw new IllegalArgumentException("No mapping for id [" + id + "]");
            }
            return FEATURES[id];
        }

        public static Feature[] convertToFeatures(String... featureNames) {
            Feature[] features = new Feature[featureNames.length];
            for (int i = 0; i < featureNames.length; i++) {
                features[i] = Feature.fromName(featureNames[i]);
            }
            return features;
        }
    }

    private static final Feature[] DEFAULT_FEATURES = new Feature[] { Feature.ALIASES, Feature.MAPPINGS, Feature.SETTINGS };
    private Feature[] features = DEFAULT_FEATURES;
    private boolean humanReadable = false;

    public GetIndexRequest features(Feature... features) {
        if (features == null) {
            throw new IllegalArgumentException("features cannot be null");
        } else {
            this.features = features;
        }
        return this;
    }

    public GetIndexRequest addFeatures(Feature... features) {
        if (this.features == DEFAULT_FEATURES) {
            return features(features);
        } else {
            return features(ArrayUtils.concat(features(), features, Feature.class));
        }
    }

    public Feature[] features() {
        return features;
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    public GetIndexRequest humanReadable(boolean humanReadable) {
        this.humanReadable = humanReadable;
        return this;
    }

    public boolean humanReadable() {
        return humanReadable;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        int size = in.readVInt();
        features = new Feature[size];
        for (int i = 0; i < size; i++) {
            features[i] = Feature.fromId(in.readByte());
        }
        humanReadable = in.readBoolean();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVInt(features.length);
        for (Feature feature : features) {
            out.writeByte(feature.id);
        }
        out.writeBoolean(humanReadable);
    }

}
