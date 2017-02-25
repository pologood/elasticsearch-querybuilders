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

package org.codelibs.elasticsearch.search.fetch.subphase.highlight;

import org.codelibs.elasticsearch.common.ParsingException;
import org.codelibs.elasticsearch.common.io.stream.StreamInput;
import org.codelibs.elasticsearch.common.io.stream.StreamOutput;
import org.codelibs.elasticsearch.common.io.stream.Streamable;
import org.codelibs.elasticsearch.common.text.Text;
import org.codelibs.elasticsearch.common.xcontent.ToXContent;
import org.codelibs.elasticsearch.common.xcontent.XContentBuilder;
import org.codelibs.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A field highlighted with its highlighted fragments.
 */
public class HighlightField implements ToXContent, Streamable {

    private String name;

    private Text[] fragments;

    HighlightField() {
    }

    public HighlightField(String name, Text[] fragments) {
        this.name = Objects.requireNonNull(name, "missing highlight field name");
        this.fragments = fragments;
    }

    /**
     * The name of the field highlighted.
     */
    public String name() {
        return name;
    }

    /**
     * The name of the field highlighted.
     */
    public String getName() {
        return name();
    }

    /**
     * The highlighted fragments. <tt>null</tt> if failed to highlight (for example, the field is not stored).
     */
    public Text[] fragments() {
        return fragments;
    }

    /**
     * The highlighted fragments. <tt>null</tt> if failed to highlight (for example, the field is not stored).
     */
    public Text[] getFragments() {
        return fragments();
    }

    @Override
    public String toString() {
        return "[" + name + "], fragments[" + Arrays.toString(fragments) + "]";
    }

    public static HighlightField readHighlightField(StreamInput in) throws IOException {
        HighlightField field = new HighlightField();
        field.readFrom(in);
        return field;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        name = in.readString();
        if (in.readBoolean()) {
            int size = in.readVInt();
            if (size == 0) {
                fragments = Text.EMPTY_ARRAY;
            } else {
                fragments = new Text[size];
                for (int i = 0; i < size; i++) {
                    fragments[i] = in.readText();
                }
            }
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(name);
        if (fragments == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.writeVInt(fragments.length);
            for (Text fragment : fragments) {
                out.writeText(fragment);
            }
        }
    }

    public static HighlightField fromXContent(XContentParser parser) throws IOException {
        XContentParser.Token token = parser.nextToken();
        assert token == XContentParser.Token.FIELD_NAME;
        String fieldName = parser.currentName();
        Text[] fragments = null;
        token = parser.nextToken();
        if (token == XContentParser.Token.START_ARRAY) {
            fragments = parseValues(parser);
        } else if (token == XContentParser.Token.VALUE_NULL) {
            fragments = null;
        } else {
            throw new ParsingException(parser.getTokenLocation(),
                    "unexpected token type [" + token + "]");
        }
        return new HighlightField(fieldName, fragments);
    }

    private static Text[] parseValues(XContentParser parser) throws IOException {
        List<Text> values = new ArrayList<>();
        while (parser.nextToken() != XContentParser.Token.END_ARRAY) {
            values.add(new Text(parser.text()));
        }
        return values.toArray(new Text[values.size()]);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.field(name);
        if (fragments == null) {
            builder.nullValue();
        } else {
            builder.startArray();
            for (Text fragment : fragments) {
                builder.value(fragment);
            }
            builder.endArray();
        }
        return builder;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        HighlightField other = (HighlightField) obj;
        return Objects.equals(name, other.name) && Arrays.equals(fragments, other.fragments);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(name, Arrays.hashCode(fragments));
    }

}
