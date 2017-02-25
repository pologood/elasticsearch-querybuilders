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

package org.codelibs.elasticsearch.transport;

import org.codelibs.elasticsearch.cluster.node.DiscoveryNode;
import org.codelibs.elasticsearch.common.io.stream.StreamInput;

import java.io.IOException;

/**
 * An exception indicating that a message is sent to a node that is not connected.
 *
 *
 */
public class NodeNotConnectedException extends ConnectTransportException {

    public NodeNotConnectedException(DiscoveryNode node, String msg) {
        super(node, msg, (String)null);
    }

    public NodeNotConnectedException(StreamInput in) throws IOException {
        super(in);
    }
}
