/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.orc.stream;

import io.airlift.slice.Slice;
import io.airlift.slice.SliceOutput;
import io.airlift.slice.Slices;
import io.trino.orc.OrcDataSourceId;
import org.junit.jupiter.api.Test;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import static io.trino.memory.context.AggregatedMemoryContext.newSimpleAggregatedMemoryContext;
import static io.trino.orc.stream.LongDecode.readVInt;
import static io.trino.orc.stream.LongDecode.writeVLong;
import static org.testng.Assert.assertEquals;

public class TestLongDecode
{
    @Test
    public void testVInt()
            throws Exception
    {
        Slice slice = Slices.allocate(100);
        SliceOutput output = slice.getOutput();

        assertVIntRoundTrip(output, 0);
        assertVIntRoundTrip(output, 1);
        assertVIntRoundTrip(output, -1);
        assertVIntRoundTrip(output, Integer.MAX_VALUE);
        assertVIntRoundTrip(output, Integer.MAX_VALUE + 1L);
        assertVIntRoundTrip(output, Integer.MAX_VALUE - 1L);
        assertVIntRoundTrip(output, Integer.MIN_VALUE);
        assertVIntRoundTrip(output, Integer.MIN_VALUE + 1L);
        assertVIntRoundTrip(output, Integer.MIN_VALUE - 1L);
        assertVIntRoundTrip(output, Long.MAX_VALUE);
        assertVIntRoundTrip(output, Long.MAX_VALUE - 1);
        assertVIntRoundTrip(output, Long.MIN_VALUE + 1);

        for (int value = -100_000; value < 100_000; value++) {
            assertVIntRoundTrip(output, value);
        }
    }

    private static void assertVIntRoundTrip(SliceOutput output, long value)
            throws IOException
    {
        assertVIntRoundTrip(output, value, true);
        assertVIntRoundTrip(output, value, false);
    }

    private static void assertVIntRoundTrip(SliceOutput output, long value, boolean signed)
            throws IOException
    {
        // write using Hive's code
        output.reset();
        if (signed) {
            writeVslong(output, value);
        }
        else {
            writeVulong(output, value);
        }
        Slice hiveBytes = output.slice().copy();

        // write using Trino's code, and verify they are the same
        output.reset();
        writeVLong(output, value, signed);
        Slice trinoBytes = output.slice().copy();
        if (!trinoBytes.equals(hiveBytes)) {
            assertEquals(trinoBytes, hiveBytes);
        }

        // read using Hive's code
        if (signed) {
            long readValueOld = readVslong(hiveBytes.getInput());
            assertEquals(readValueOld, value);
        }
        else {
            long readValueOld = readVulong(hiveBytes.getInput());
            assertEquals(readValueOld, value);
        }

        // read using Trino's code
        long readValueNew = readVInt(signed, new OrcInputStream(OrcChunkLoader.create(new OrcDataSourceId("test"), hiveBytes, Optional.empty(), newSimpleAggregatedMemoryContext())));
        assertEquals(readValueNew, value);
    }

    //
    // The following was copied from package private org.apache.hadoop.hive.ql.io.orc.SerializationUtils

    private static void writeVulong(OutputStream output, long value)
            throws IOException
    {
        while (true) {
            if ((value & ~0x7f) == 0) {
                output.write((byte) value);
                return;
            }
            output.write((byte) (0x80 | (value & 0x7f)));
            value >>>= 7;
        }
    }

    private static void writeVslong(OutputStream output, long value)
            throws IOException
    {
        writeVulong(output, (value << 1) ^ (value >> 63));
    }

    private static long readVulong(InputStream in)
            throws IOException
    {
        long result = 0;
        long b;
        int offset = 0;
        do {
            b = in.read();
            if (b == -1) {
                throw new EOFException("Reading Vulong past EOF");
            }
            result |= (0x7f & b) << offset;
            offset += 7;
        }
        while (b >= 0x80);
        return result;
    }

    private static long readVslong(InputStream in)
            throws IOException
    {
        long result = readVulong(in);
        return (result >>> 1) ^ -(result & 1);
    }
}
