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


package org.codelibs.elasticsearch.search.aggregations.bucket.significant.heuristics;


import org.codelibs.elasticsearch.ElasticsearchParseException;
import org.codelibs.elasticsearch.common.io.stream.StreamInput;
import org.codelibs.elasticsearch.common.io.stream.StreamOutput;
import org.codelibs.elasticsearch.common.xcontent.XContentBuilder;
import org.codelibs.elasticsearch.common.xcontent.XContentParser;
import org.codelibs.elasticsearch.index.query.QueryParseContext;
import org.codelibs.elasticsearch.index.query.QueryShardException;
import org.codelibs.elasticsearch.script.ExecutableScript;
import org.codelibs.elasticsearch.script.Script;
import org.codelibs.elasticsearch.script.ScriptContext;
import org.codelibs.elasticsearch.search.aggregations.InternalAggregation;
import org.codelibs.elasticsearch.search.internal.SearchContext;

import java.io.IOException;
import java.util.Objects;

public class ScriptHeuristic extends SignificanceHeuristic {
    public static final String NAME = "script_heuristic";

    private final Script script;

    // This class holds an executable form of the script with private variables ready for execution
    // on a single search thread.
    static class ExecutableScriptHeuristic extends ScriptHeuristic {
        private final LongAccessor subsetSizeHolder;
        private final LongAccessor supersetSizeHolder;
        private final LongAccessor subsetDfHolder;
        private final LongAccessor supersetDfHolder;
        private final ExecutableScript executableScript;

        ExecutableScriptHeuristic(Script script, ExecutableScript executableScript){
            super(script);
            subsetSizeHolder = new LongAccessor();
            supersetSizeHolder = new LongAccessor();
            subsetDfHolder = new LongAccessor();
            supersetDfHolder = new LongAccessor();
            this.executableScript = executableScript;
            executableScript.setNextVar("_subset_freq", subsetDfHolder);
            executableScript.setNextVar("_subset_size", subsetSizeHolder);
            executableScript.setNextVar("_superset_freq", supersetDfHolder);
            executableScript.setNextVar("_superset_size", supersetSizeHolder);
        }

        @Override
        public double getScore(long subsetFreq, long subsetSize, long supersetFreq, long supersetSize) {
            subsetSizeHolder.value = subsetSize;
            supersetSizeHolder.value = supersetSize;
            subsetDfHolder.value = subsetFreq;
            supersetDfHolder.value = supersetFreq;
            return ((Number) executableScript.run()).doubleValue();
       }
    }

    public ScriptHeuristic(Script script) {
        this.script = script;
    }

    /**
     * Read from a stream.
     */
    public ScriptHeuristic(StreamInput in) throws IOException {
        this(new Script(in));
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        script.writeTo(out);
    }

    @Override
    public SignificanceHeuristic rewrite(InternalAggregation.ReduceContext context) {
        throw new UnsupportedOperationException("querybuilders does not support this operation.");
    }

    @Override
    public SignificanceHeuristic rewrite(SearchContext context) {
        throw new UnsupportedOperationException("querybuilders does not support this operation.");
    }

    /**
     * Calculates score with a script
     *
     * @param subsetFreq   The frequency of the term in the selected sample
     * @param subsetSize   The size of the selected sample (typically number of docs)
     * @param supersetFreq The frequency of the term in the superset from which the sample was taken
     * @param supersetSize The size of the superset from which the sample was taken  (typically number of docs)
     * @return a "significance" score
     */
    @Override
    public double getScore(long subsetFreq, long subsetSize, long supersetFreq, long supersetSize) {
        throw new UnsupportedOperationException("This scoring heuristic must have 'rewrite' called on it to provide a version ready for use");
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params builderParams) throws IOException {
        builder.startObject(NAME);
        builder.field(Script.SCRIPT_PARSE_FIELD.getPreferredName());
        script.toXContent(builder, builderParams);
        builder.endObject();
        return builder;
    }

    @Override
    public int hashCode() {
        return Objects.hash(script);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ScriptHeuristic other = (ScriptHeuristic) obj;
        return Objects.equals(script, other.script);
    }

    public static SignificanceHeuristic parse(QueryParseContext context)
            throws IOException, QueryShardException {
        XContentParser parser = context.parser();
        String heuristicName = parser.currentName();
        Script script = null;
        XContentParser.Token token;
        String currentFieldName = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token.equals(XContentParser.Token.FIELD_NAME)) {
                currentFieldName = parser.currentName();
            } else {
                if (Script.SCRIPT_PARSE_FIELD.match(currentFieldName)) {
                    script = Script.parse(parser, context.getParseFieldMatcher(), context.getDefaultScriptLanguage());
                } else {
                    throw new ElasticsearchParseException("failed to parse [{}] significance heuristic. unknown object [{}]", heuristicName, currentFieldName);
                }
            }
        }

        if (script == null) {
            throw new ElasticsearchParseException("failed to parse [{}] significance heuristic. no script found in script_heuristic", heuristicName);
        }
        return new ScriptHeuristic(script);
    }

    public final class LongAccessor extends Number {
        public long value;
        @Override
        public int intValue() {
            return (int)value;
        }
        @Override
        public long longValue() {
            return value;
        }

        @Override
        public float floatValue() {
            return value;
        }

        @Override
        public double doubleValue() {
            return value;
        }

        @Override
        public String toString() {
            return Long.toString(value);
        }
    }
}

