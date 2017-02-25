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

package org.codelibs.elasticsearch.common.geo.builders;

import org.locationtech.spatial4j.shape.Point;
import com.vividsolutions.jts.geom.Coordinate;

import org.codelibs.elasticsearch.common.io.stream.StreamInput;
import org.codelibs.elasticsearch.common.io.stream.StreamOutput;
import org.codelibs.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Objects;

public class PointBuilder extends ShapeBuilder {
    public static final GeoShapeType TYPE = GeoShapeType.POINT;

    private Coordinate coordinate;

    /**
     * Create a point at [0.0,0.0]
     */
    public PointBuilder() {
        this.coordinate = ZERO_ZERO;
    }

    /**
     * Read from a stream.
     */
    public PointBuilder(StreamInput in) throws IOException {
        coordinate = readFromStream(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        writeCoordinateTo(coordinate, out);
    }

    public PointBuilder coordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
        return this;
    }

    public double longitude() {
        return coordinate.x;
    }

    public double latitude() {
        return coordinate.y;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
       builder.startObject();
       builder.field(FIELD_TYPE, TYPE.shapeName());
       builder.field(FIELD_COORDINATES);
       toXContent(builder, coordinate);
       return builder.endObject();
    }

    @Override
    public Point build() {
        return SPATIAL_CONTEXT.makePoint(coordinate.x, coordinate.y);
    }

    @Override
    public GeoShapeType type() {
        return TYPE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(coordinate);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PointBuilder other = (PointBuilder) obj;
        return Objects.equals(coordinate, other.coordinate);
    }
}
