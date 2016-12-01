/**
 * Copyright 2016 YouVersion
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.support;

import java.io.IOException;

import okio.BufferedSink;
import okio.ByteString;

public class BinaryWriter {

    static final byte NULL = 1;
    static final byte NOT_NULL = 2;

    final BufferedSink output;

    public BinaryWriter(BufferedSink output) {
        this.output = output;
    }

    public BufferedSink getOutput() {
        return output;
    }

    /**
     * Encodes {@code value}.
     *
     * @param value the literal string value, or null to encode a null literal.
     * @return this writer.
     */
    public BinaryWriter value(String value) throws IOException {
        if (value == null) {
            return nullValue();
        }
        output.writeByte(NOT_NULL);
        ByteString str = ByteString.encodeUtf8(value);
        output.writeInt(str.size());
        output.write(str);
        return this;
    }

    /**
     * Encodes {@code null}.
     *
     * @return this writer.
     */
    public BinaryWriter nullValue() throws IOException {
        output.writeByte(NULL);
        return this;
    }

    /**
     * Encodes {@code value}.
     *
     * @return this writer.
     */
    public BinaryWriter value(boolean value) throws IOException {
        output.writeByte(NOT_NULL);
        output.writeByte(value ? 1 : 0);
        return this;
    }

    /**
     * Encodes {@code value}.
     *
     * @param value a finite value. May not be {@link Double#isNaN() NaNs} or
     *              {@link Double#isInfinite() infinities} unless this writer is lenient.
     * @return this writer.
     */
    public BinaryWriter value(double value) throws IOException {
        output.writeByte(NOT_NULL);
        output.writeLong(Double.doubleToLongBits(value));
        return this;
    }

    /**
     * Encodes {@code value}.
     *
     * @return this writer.
     */
    public BinaryWriter value(long value) throws IOException {
        output.writeByte(NOT_NULL);
        output.writeDecimalLong(value);
        return this;
    }

    /**
     * Encodes {@code value}.
     *
     * @return this writer.
     */
    public BinaryWriter value(int value) throws IOException {
        output.writeByte(NOT_NULL);
        output.writeInt(value);
        return this;
    }

    /**
     * Encodes {@code value}.
     *
     * @return this writer.
     */
    public BinaryWriter value(byte[] value) throws IOException {
        output.write(value);
        return this;
    }

}
