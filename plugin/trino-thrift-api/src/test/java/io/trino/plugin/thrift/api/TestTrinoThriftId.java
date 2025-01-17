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
package io.trino.plugin.thrift.api;

import com.google.common.primitives.UnsignedBytes;
import org.junit.jupiter.api.Test;

import static io.trino.plugin.thrift.api.TrinoThriftId.summarize;
import static org.testng.Assert.assertEquals;

public class TestTrinoThriftId
{
    @Test
    public void testSummarize()
    {
        assertEquals(summarize(bytes()), "");
        assertEquals(summarize(bytes(1)), "01");
        assertEquals(summarize(bytes(255, 254, 253, 252, 251, 250, 249)), "FFFEFDFCFBFAF9");
        assertEquals(summarize(bytes(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 249, 250, 251, 252, 253, 254, 255)),
                "00010203040506070809F9FAFBFCFDFEFF");
        assertEquals(summarize(bytes(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 247, 248, 249, 250, 251, 252, 253, 254, 255)),
                "0001020304050607..F8F9FAFBFCFDFEFF");
    }

    private static byte[] bytes(int... values)
    {
        int length = values.length;
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = UnsignedBytes.checkedCast(values[i]);
        }
        return result;
    }
}
