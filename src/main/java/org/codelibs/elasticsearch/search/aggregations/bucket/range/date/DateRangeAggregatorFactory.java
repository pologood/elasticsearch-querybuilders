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

import org.codelibs.elasticsearch.search.aggregations.AggregatorFactories;
import org.codelibs.elasticsearch.search.aggregations.AggregatorFactory;
import org.codelibs.elasticsearch.search.aggregations.InternalAggregation.Type;
import org.codelibs.elasticsearch.search.aggregations.bucket.range.AbstractRangeAggregatorFactory;
import org.codelibs.elasticsearch.search.aggregations.bucket.range.InternalRange.Factory;
import org.codelibs.elasticsearch.search.aggregations.bucket.range.RangeAggregator.Range;
import org.codelibs.elasticsearch.search.aggregations.support.ValuesSource.Numeric;
import org.codelibs.elasticsearch.search.aggregations.support.ValuesSourceConfig;
import org.codelibs.elasticsearch.search.internal.SearchContext;

import java.io.IOException;
import java.util.Map;

public class DateRangeAggregatorFactory extends AbstractRangeAggregatorFactory<DateRangeAggregatorFactory, Range> {

    public DateRangeAggregatorFactory(String name, Type type, ValuesSourceConfig<Numeric> config, Range[] ranges, boolean keyed,
            Factory<?, ?> rangeFactory, SearchContext context, AggregatorFactory<?> parent,
            AggregatorFactories.Builder subFactoriesBuilder, Map<String, Object> metaData) throws IOException {
        super(name, type, config, ranges, keyed, rangeFactory, context, parent, subFactoriesBuilder, metaData);
    }

}
