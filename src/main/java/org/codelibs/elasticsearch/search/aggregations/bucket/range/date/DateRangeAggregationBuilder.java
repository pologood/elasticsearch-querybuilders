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

package org.codelibs.elasticsearch.search.aggregations.bucket.range.date;

import org.codelibs.elasticsearch.common.io.stream.StreamInput;
import org.codelibs.elasticsearch.common.xcontent.ObjectParser;
import org.codelibs.elasticsearch.common.xcontent.XContentParser;
import org.codelibs.elasticsearch.index.query.QueryParseContext;
import org.codelibs.elasticsearch.search.aggregations.AggregatorFactories.Builder;
import org.codelibs.elasticsearch.search.aggregations.AggregationBuilder;
import org.codelibs.elasticsearch.search.aggregations.AggregatorFactory;
import org.codelibs.elasticsearch.search.aggregations.InternalAggregation.Type;
import org.codelibs.elasticsearch.search.aggregations.bucket.range.AbstractRangeBuilder;
import org.codelibs.elasticsearch.search.aggregations.bucket.range.RangeAggregator;
import org.codelibs.elasticsearch.search.aggregations.bucket.range.RangeAggregator.Range;
import org.codelibs.elasticsearch.search.aggregations.support.ValuesSource.Numeric;
import org.codelibs.elasticsearch.search.aggregations.support.ValuesSourceConfig;
import org.codelibs.elasticsearch.search.aggregations.support.ValuesSourceParserHelper;
import org.codelibs.elasticsearch.search.internal.SearchContext;
import org.joda.time.DateTime;

import java.io.IOException;

public class DateRangeAggregationBuilder extends AbstractRangeBuilder<DateRangeAggregationBuilder, RangeAggregator.Range> {
    public static final String NAME = "date_range";
    static final Type TYPE = new Type(NAME);

    private static final ObjectParser<DateRangeAggregationBuilder, QueryParseContext> PARSER;
    static {
        PARSER = new ObjectParser<>(DateRangeAggregationBuilder.NAME);
        ValuesSourceParserHelper.declareNumericFields(PARSER, true, true, true);
        PARSER.declareBoolean(DateRangeAggregationBuilder::keyed, RangeAggregator.KEYED_FIELD);

        PARSER.declareObjectArray((agg, ranges) -> {
            for (Range range : ranges) {
                agg.addRange(range);
            }
        }, DateRangeAggregationBuilder::parseRange, RangeAggregator.RANGES_FIELD);
    }

    public static AggregationBuilder parse(String aggregationName, QueryParseContext context) throws IOException {
        return PARSER.parse(context.parser(), new DateRangeAggregationBuilder(aggregationName), context);
    }

    private static Range parseRange(XContentParser parser, QueryParseContext context) throws IOException {
        return Range.fromXContent(parser, context.getParseFieldMatcher());
    }

    public DateRangeAggregationBuilder(String name) {
        super(name, InternalDateRange.FACTORY);
    }

    /**
     * Read from a stream.
     */
    public DateRangeAggregationBuilder(StreamInput in) throws IOException {
        super(in, InternalDateRange.FACTORY, Range::new);
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    /**
     * Add a new range to this aggregation.
     *
     * @param key
     *            the key to use for this range in the response
     * @param from
     *            the lower bound on the dates, inclusive
     * @param to
     *            the upper bound on the dates, exclusive
     */
    public DateRangeAggregationBuilder addRange(String key, String from, String to) {
        addRange(new Range(key, from, to));
        return this;
    }

    /**
     * Same as {#addRange(String, String, String)} but the key will be
     * automatically generated based on <code>from</code> and <code>to</code>.
     */
    public DateRangeAggregationBuilder addRange(String from, String to) {
        return addRange(null, from, to);
    }

    /**
     * Add a new range with no lower bound.
     *
     * @param key
     *            the key to use for this range in the response
     * @param to
     *            the upper bound on the dates, exclusive
     */
    public DateRangeAggregationBuilder addUnboundedTo(String key, String to) {
        addRange(new Range(key, null, to));
        return this;
    }

    /**
     * Same as {#addUnboundedTo(String, String)} but the key will be
     * computed automatically.
     */
    public DateRangeAggregationBuilder addUnboundedTo(String to) {
        return addUnboundedTo(null, to);
    }

    /**
     * Add a new range with no upper bound.
     *
     * @param key
     *            the key to use for this range in the response
     * @param from
     *            the lower bound on the distances, inclusive
     */
    public DateRangeAggregationBuilder addUnboundedFrom(String key, String from) {
        addRange(new Range(key, from, null));
        return this;
    }

    /**
     * Same as {#addUnboundedFrom(String, String)} but the key will be
     * computed automatically.
     */
    public DateRangeAggregationBuilder addUnboundedFrom(String from) {
        return addUnboundedFrom(null, from);
    }

    /**
     * Add a new range to this aggregation.
     *
     * @param key
     *            the key to use for this range in the response
     * @param from
     *            the lower bound on the dates, inclusive
     * @param to
     *            the upper bound on the dates, exclusive
     */
    public DateRangeAggregationBuilder addRange(String key, double from, double to) {
        addRange(new Range(key, from, to));
        return this;
    }

    /**
     * Same as {#addRange(String, double, double)} but the key will be
     * automatically generated based on <code>from</code> and <code>to</code>.
     */
    public DateRangeAggregationBuilder addRange(double from, double to) {
        return addRange(null, from, to);
    }

    /**
     * Add a new range with no lower bound.
     *
     * @param key
     *            the key to use for this range in the response
     * @param to
     *            the upper bound on the dates, exclusive
     */
    public DateRangeAggregationBuilder addUnboundedTo(String key, double to) {
        addRange(new Range(key, null, to));
        return this;
    }

    /**
     * Same as {#addUnboundedTo(String, double)} but the key will be
     * computed automatically.
     */
    public DateRangeAggregationBuilder addUnboundedTo(double to) {
        return addUnboundedTo(null, to);
    }

    /**
     * Add a new range with no upper bound.
     *
     * @param key
     *            the key to use for this range in the response
     * @param from
     *            the lower bound on the distances, inclusive
     */
    public DateRangeAggregationBuilder addUnboundedFrom(String key, double from) {
        addRange(new Range(key, from, null));
        return this;
    }

    /**
     * Same as {#addUnboundedFrom(String, double)} but the key will be
     * computed automatically.
     */
    public DateRangeAggregationBuilder addUnboundedFrom(double from) {
        return addUnboundedFrom(null, from);
    }

    /**
     * Add a new range to this aggregation.
     *
     * @param key
     *            the key to use for this range in the response
     * @param from
     *            the lower bound on the dates, inclusive
     * @param to
     *            the upper bound on the dates, exclusive
     */
    public DateRangeAggregationBuilder addRange(String key, DateTime from, DateTime to) {
        addRange(new Range(key, convertDateTime(from), convertDateTime(to)));
        return this;
    }

    private Double convertDateTime(DateTime dateTime) {
        if (dateTime == null) {
            return null;
        } else {
            return (double) dateTime.getMillis();
        }
    }

    /**
     * Same as {#addRange(String, DateTime, DateTime)} but the key will be
     * automatically generated based on <code>from</code> and <code>to</code>.
     */
    public DateRangeAggregationBuilder addRange(DateTime from, DateTime to) {
        return addRange(null, from, to);
    }

    /**
     * Add a new range with no lower bound.
     *
     * @param key
     *            the key to use for this range in the response
     * @param to
     *            the upper bound on the dates, exclusive
     */
    public DateRangeAggregationBuilder addUnboundedTo(String key, DateTime to) {
        addRange(new Range(key, null, convertDateTime(to)));
        return this;
    }

    /**
     * Same as {#addUnboundedTo(String, DateTime)} but the key will be
     * computed automatically.
     */
    public DateRangeAggregationBuilder addUnboundedTo(DateTime to) {
        return addUnboundedTo(null, to);
    }

    /**
     * Add a new range with no upper bound.
     *
     * @param key
     *            the key to use for this range in the response
     * @param from
     *            the lower bound on the distances, inclusive
     */
    public DateRangeAggregationBuilder addUnboundedFrom(String key, DateTime from) {
        addRange(new Range(key, convertDateTime(from), null));
        return this;
    }

    /**
     * Same as {#addUnboundedFrom(String, DateTime)} but the key will be
     * computed automatically.
     */
    public DateRangeAggregationBuilder addUnboundedFrom(DateTime from) {
        return addUnboundedFrom(null, from);
    }

    @Override
    protected DateRangeAggregatorFactory innerBuild(SearchContext context, ValuesSourceConfig<Numeric> config,
            AggregatorFactory<?> parent, Builder subFactoriesBuilder) throws IOException {
        // We need to call processRanges here so they are parsed and we know whether `now` has been used before we make
        // the decision of whether to cache the request
        Range[] ranges = processRanges(context, config);
        return new DateRangeAggregatorFactory(name, type, config, ranges, keyed, rangeFactory, context, parent, subFactoriesBuilder,
                metaData);
    }
}
