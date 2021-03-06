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

package org.codelibs.elasticsearch.search.aggregations.bucket.significant;

import org.codelibs.elasticsearch.common.io.stream.StreamInput;
import org.codelibs.elasticsearch.common.io.stream.StreamOutput;
import org.codelibs.elasticsearch.search.DocValueFormat;
import org.codelibs.elasticsearch.search.aggregations.bucket.significant.heuristics.SignificanceHeuristic;
import org.codelibs.elasticsearch.search.aggregations.pipeline.PipelineAggregator;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class InternalMappedSignificantTerms<
            A extends InternalMappedSignificantTerms<A, B>,
            B extends InternalSignificantTerms.Bucket<B>>
        extends InternalSignificantTerms<A, B> {

    protected final DocValueFormat format;
    protected final long subsetSize;
    protected final long supersetSize;
    protected final SignificanceHeuristic significanceHeuristic;
    protected final List<B> buckets;
    protected Map<String, B> bucketMap;

    protected InternalMappedSignificantTerms(String name, int requiredSize, long minDocCount, List<PipelineAggregator> pipelineAggregators,
            Map<String, Object> metaData, DocValueFormat format, long subsetSize, long supersetSize,
            SignificanceHeuristic significanceHeuristic, List<B> buckets) {
        super(name, requiredSize, minDocCount, pipelineAggregators, metaData);
        this.format = format;
        this.buckets = buckets;
        this.subsetSize = subsetSize;
        this.supersetSize = supersetSize;
        this.significanceHeuristic = significanceHeuristic;
    }

    protected InternalMappedSignificantTerms(StreamInput in, Bucket.Reader<B> bucketReader) throws IOException {
        super(in);
        format = in.readNamedWriteable(DocValueFormat.class);
        subsetSize = in.readVLong();
        supersetSize = in.readVLong();
        significanceHeuristic = in.readNamedWriteable(SignificanceHeuristic.class);
        buckets = in.readList(stream -> bucketReader.read(stream, subsetSize, supersetSize, format));
    }

    @Override
    protected final void writeTermTypeInfoTo(StreamOutput out) throws IOException {
        out.writeNamedWriteable(format);
        out.writeVLong(subsetSize);
        out.writeVLong(supersetSize);
        out.writeNamedWriteable(significanceHeuristic);
        out.writeList(buckets);
    }

    @Override
    protected List<B> getBucketsInternal() {
        return buckets;
    }

    @Override
    public B getBucketByKey(String term) {
        if (bucketMap == null) {
            bucketMap = buckets.stream().collect(Collectors.toMap(InternalSignificantTerms.Bucket::getKeyAsString, Function.identity()));
        }
        return bucketMap.get(term);
    }

    @Override
    protected long getSubsetSize() {
        return subsetSize;
    }

    @Override
    protected long getSupersetSize() {
        return supersetSize;
    }

    @Override
    protected SignificanceHeuristic getSignificanceHeuristic() {
        return significanceHeuristic;
    }
}
