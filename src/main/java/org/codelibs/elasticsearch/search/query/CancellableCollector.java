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
package org.codelibs.elasticsearch.search.query;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.FilterCollector;
import org.apache.lucene.search.FilterLeafCollector;
import org.apache.lucene.search.LeafCollector;
import org.codelibs.elasticsearch.common.inject.Provider;
import org.codelibs.elasticsearch.tasks.TaskCancelledException;

import java.io.IOException;

/**
 * Collector that checks if the task it is executed under is cancelled.
 */
public class CancellableCollector extends FilterCollector {
    private final Provider<Boolean> cancelled;
    private final boolean leafLevel;

    /**
     * Constructor
     * @param cancelled supplier of the cancellation flag, the supplier will be called for each segment if lowLevelCancellation is set
     *                  to false and for each collected record if lowLevelCancellation is set to true. In other words this class assumes
     *                  that the supplier is fast, with performance on the order of a volatile read.
     * @param lowLevelCancellation true if collector should check for cancellation for each collected record, false if check should be
     *                             performed only once per segment
     * @param in wrapped collector
     */
    public CancellableCollector(Provider<Boolean> cancelled, boolean lowLevelCancellation, Collector in) {
        super(in);
        this.cancelled = cancelled;
        this.leafLevel = lowLevelCancellation;
    }

    @Override
    public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
        if (cancelled.get()) {
            throw new TaskCancelledException("cancelled");
        }
        if (leafLevel) {
            return new CancellableLeafCollector(super.getLeafCollector(context));
        } else {
            return super.getLeafCollector(context);
        }
    }

    private class CancellableLeafCollector extends FilterLeafCollector {
        private CancellableLeafCollector(LeafCollector in) {
            super(in);
        }

        @Override
        public void collect(int doc) throws IOException {
            if (cancelled.get()) {
                throw new TaskCancelledException("cancelled");
            }
            super.collect(doc);
        }
    }
}
