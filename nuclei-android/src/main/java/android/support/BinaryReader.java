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

import okio.BufferedSource;

public final class BinaryReader {

    final BufferedSource input;

    public BinaryReader(BufferedSource input) {
        this.input = input;
    }

    public BufferedSource getInput() {
        return input;
    }

    public boolean isNull() throws IOException {
        return input.readByte() == BinaryWriter.NULL;
    }

    public String nextString() throws IOException {
        int len = input.readInt();
        return input.readByteString(len).utf8();
    }

    public boolean nextBoolean() throws IOException {
        return input.readByte() == 1;
    }

    public double nextDouble() throws IOException {
        return Double.longBitsToDouble(input.readLong());
    }

    public long nextLong() throws IOException {
        return input.readDecimalLong();
    }

    public int nextInt() throws IOException {
        return input.readInt();
    }

    public int nextInteger() throws IOException {
        return input.readInt();
    }

    public void read(byte[] buf) throws IOException {
        input.readFully(buf);
    }

}
