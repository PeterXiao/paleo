/*
 * Copyright 2015 Rahel Lüthy
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

package ch.fhnw.ima.paleo;

import org.junit.Test;

import static ch.fhnw.ima.paleo.ColumnIds.*;
import static org.junit.Assert.*;

public class StringColumnTest {

    @Test
    public void build() {
        StringColumnId id = stringCol("test");
        StringColumn.Builder builder = StringColumn.builder(id);
        builder.add("bli").addAll("bla", "blu").add("zzz");
        StringColumn column = builder.build();
        assertEquals(id, column.getId());
        assertEquals(4, column.getRowCount());
        assertEquals("bli", column.getValueAt(0));
        assertEquals("zzz", column.getValueAt(column.getRowCount() - 1));
    }

}