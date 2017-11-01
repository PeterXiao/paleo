/*
 * Copyright 2016 Rahel Lüthy
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

package ch.netzwerg.paleo.io;

import ch.netzwerg.paleo.*;
import ch.netzwerg.paleo.schema.Schema;
import io.vavr.collection.Array;
import io.vavr.collection.HashSet;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.time.Instant;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.function.Function;

import static ch.netzwerg.paleo.ColumnIds.*;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class ParserTest {

    private static final String CONTENTS_WITH_HEADER_TSV = "Name\tAge\tHeight\tVegetarian\tDate Of Birth\tGender\n" +
            "String\tInt\tDouble\tBoolean\tTimestamp\tCategory\n" +
            "Ada\t42\t1.74\ttrue\t19750826050916.111\tFemale\n" +
            "Homer\t99\t1.20\tF\t20060108050916.222\tMale\n" +
            "Hillary\t67\t1.70\t1\t19471026050916.333\tFemale\n";

    private static final String CONTENTS_WITH_HEADER_CSV = "Name,Age,Height,Vegetarian,Date Of Birth,Gender\n" +
            "String,Int,Double,Boolean,Timestamp,Category\n" +
            "Ada,42,1.74,true,19750826050916.111,Female\n" +
            "Homer,99,1.20,F,20060108050916.222,Male\n" +
            "Hillary,67,1.70,1,19471026050916.333,Female\n";

    private static final String FIELDS = "  \"fields\": [\n" +
            "    {\n" +
            "      \"name\": \"Name\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Age\",\n" +
            "      \"type\": \"Int\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Height\",\n" +
            "      \"type\": \"Double\",\n" +
            "      \"metaData\": {\"unit\":\"m\"}\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Vegetarian\",\n" +
            "      \"type\": \"Boolean\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Date Of Birth\",\n" +
            "      \"type\": \"Timestamp\",\n" +
            "      \"format\": \"yyyyMMddHHmmss.SSS\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Gender\",\n" +
            "      \"type\": \"Category\"\n" +
            "    }\n" +
            "  ]\n";

    private static final String META_DATA = "  \"metaData\": { \"author\": \"netzwerg\" }";

    private static final String SCHEMA_STREAM_BASED_TSV = "{\n" +
            "  \"dataFileName\": \"/data.tsv\",\n" +
            META_DATA + ",\n" +
            FIELDS +
            "}";

    private static final String SCHEMA_STREAM_BASED_CSV = "{\n" +
            "  \"dataFileName\": \"/data.csv\",\n" +
            META_DATA + ",\n" +
            FIELDS +
            "}";

    private static final String SCHEMA_FILE_BASED_TSV = "{\n" +
            "  \"dataFileName\": \"data.tsv\",\n" +
            META_DATA + ",\n" +
            FIELDS +
            "}";

    private static final String SCHEMA_FILE_BASED_CSV = "{\n" +
            "  \"dataFileName\": \"data.csv\",\n" +
            META_DATA + ",\n" +
            FIELDS +
            "}";


    private static final String SCHEMA_INCONSISTENT_COLUMN_COUNT = "{\n" +
            "  \"dataFileName\": \"/inconsistent-column-count.tsv\",\n" +
            FIELDS +
            "}";

    private static final String SCHEMA_WITH_CHARSET_NAME = "{\n" +
            "  \"dataFileName\": \"/iso-8859-1.txt\",\n" +
            "  \"charsetName\": \"ISO-8859-1\",\n" +
            "  \"fields\": [\n" +
            "    {\n" +
            "      \"demo\": \"Demo\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    
    // -- Tab Delimited

    @Test
    public void tsv() {
        StringReader reader = new StringReader(CONTENTS_WITH_HEADER_TSV);
        DataFrame df = Parser.tsv(reader, "yyyyMMddHHmmss.SSS");
        assertDataFrameParsedCorrectly(df);
    }

    @Test
    public void tsvFromSchemaStreamBased() throws IOException {
        StringReader schemaReader = new StringReader(SCHEMA_STREAM_BASED_TSV);
        Schema schema = Schema.parseJson(schemaReader);
        DataFrame df = Parser.tsv(schema);
        assertDataFrameParsedCorrectly(df);
        assertMetaDataParsedCorrectly(df);
    }

    @Test
    public void tsvFromSchemaFileBased() throws IOException {
        StringReader schemaReader = new StringReader(SCHEMA_FILE_BASED_TSV);
        Schema schema = Schema.parseJson(schemaReader);
        // some trickery to find physical location of resources folder...
        File resourceFolder = new File(ParserTest.class.getResource("/data.tsv").getPath()).getParentFile();
        DataFrame df = Parser.tsv(schema, resourceFolder);
        assertDataFrameParsedCorrectly(df);
        assertMetaDataParsedCorrectly(df);
    }

    @Test
    public void tsvFromSchemaInconsistentColumnCount() throws IOException {
        StringReader schemaReader = new StringReader(SCHEMA_INCONSISTENT_COLUMN_COUNT);
        Schema schema = Schema.parseJson(schemaReader);
        try {
            Parser.tsv(schema);
            fail("Exception expected");
        } catch (Exception e) {
            assertEquals("Row '2' contains '5' values (but should match column count '6')", e.getMessage());
        }
    }

    @Test
    public void tsvWithEmptyValues() {
        String validButWithEmptyValues =
                "First\tLast\n" +
                        "String\tString\n" +
                        "Barack\tObama\n" +
                        "Homer\t\n" +
                        "\tClinton\n";
        DataFrame df = Parser.tsv(new StringReader(validButWithEmptyValues));
        assertEquals(2, df.getColumnCount());
        assertEquals(3, df.getRowCount());
    }

    @Test
    public void tsvOffsetRowIndexInErrorMessage() {
        String invalid = "First\tLast\nString\tString\nBarack\n";
        try (Reader in = new StringReader(invalid)) {
            Parser.tsv(in);
        } catch (Exception e) {
            assertEquals("Row '3' contains '1' value (but should match column count '2')", e.getMessage());
        }
    }

    @Test
    public void tsvFromSchemaWithCharset() throws IOException {
        assertSchemaWithCharset(Parser::tsv);
    }

    // -- Comma Separated

    @Test
    public void csv() {
        StringReader reader = new StringReader(CONTENTS_WITH_HEADER_CSV);
        DataFrame df = Parser.csv(reader, "yyyyMMddHHmmss.SSS");
        assertDataFrameParsedCorrectly(df);
    }

    @Test
    public void csvFromSchemaStreamBased() throws IOException {
        StringReader schemaReader = new StringReader(SCHEMA_STREAM_BASED_CSV);
        Schema schema = Schema.parseJson(schemaReader);
        DataFrame df = Parser.csv(schema);
        assertDataFrameParsedCorrectly(df);
        assertMetaDataParsedCorrectly(df);
    }

    @Test
    public void csvFromSchemaFileBased() throws IOException {
        StringReader schemaReader = new StringReader(SCHEMA_FILE_BASED_CSV);
        Schema schema = Schema.parseJson(schemaReader);
        // some trickery to find physical location of resources folder...
        File resourceFolder = new File(ParserTest.class.getResource("/data.csv").getPath()).getParentFile();
        DataFrame df = Parser.csv(schema, resourceFolder);
        assertDataFrameParsedCorrectly(df);
        assertMetaDataParsedCorrectly(df);
    }

    @Test
    public void csvWithEmptyValues() {
        String validButWithEmptyValues =
                "First,Last\n" +
                        "String,String\n" +
                        "Barack,Obama\n" +
                        "Homer,\n" +
                        ",Clinton\n";
        DataFrame df = Parser.csv(new StringReader(validButWithEmptyValues));
        assertEquals(2, df.getColumnCount());
        assertEquals(3, df.getRowCount());
    }

    @Test
    public void csvFromSchemaWithCharset() throws IOException {
        assertSchemaWithCharset(Parser::csv);
    }

    // -- Generic

    private static void assertDataFrameParsedCorrectly(DataFrame df) {
        assertEquals(6, df.getColumnCount());
        assertEquals(Array.of("Name", "Age", "Height", "Vegetarian", "Date Of Birth", "Gender"), df.getColumnNames());

        StringColumnId nameColumnId = df.getColumnId(0, ColumnType.STRING);
        StringColumn nameColumn = df.getColumn(nameColumnId);
        assertEquals(Array.of("Ada", "Homer", "Hillary"), nameColumn.getValues());

        IntColumnId ageColumnId = df.getColumnId(1, ColumnType.INT);
        IntColumn ageColumn = df.getColumn(ageColumnId);
        assertArrayEquals(new int[]{42, 99, 67}, ageColumn.valueStream().toArray());

        DoubleColumnId heightColumnId = df.getColumnId(2, ColumnType.DOUBLE);
        DoubleColumn heightColumn = df.getColumn(heightColumnId);
        assertArrayEquals(new double[]{1.74, 1.20, 1.70}, heightColumn.valueStream().toArray(), 0.01);

        BooleanColumnId vegetarianColumnId = df.getColumnId(3, ColumnType.BOOLEAN);
        BooleanColumn vegetarianColumn = df.getColumn(vegetarianColumnId);
        assertEquals(Array.of(true, false, false), vegetarianColumn.valueStream().toArray());

        TimestampColumnId dateOfBirthColumnId = df.getColumnId(4, ColumnType.TIMESTAMP);
        TimestampColumn dateOfBirthColumn = df.getColumn(dateOfBirthColumnId);
        Function<? super Instant, Month> toMonth = instant -> instant.atZone(ZoneId.from(ZoneOffset.UTC)).getMonth();
        assertEquals(asList(Month.AUGUST, Month.JANUARY, Month.OCTOBER), dateOfBirthColumn.getValues().map(toMonth).toJavaList());
        assertEquals(111, dateOfBirthColumn.getValueAt(0).getLong(ChronoField.MILLI_OF_SECOND));
        assertEquals(222, dateOfBirthColumn.getValueAt(1).getLong(ChronoField.MILLI_OF_SECOND));
        assertEquals(333, dateOfBirthColumn.getValueAt(2).getLong(ChronoField.MILLI_OF_SECOND));

        CategoryColumnId genderColumnId = df.getColumnId(5, ColumnType.CATEGORY);
        CategoryColumn genderColumn = df.getColumn(genderColumnId);
        assertEquals(HashSet.of("Female", "Male"), genderColumn.getCategories());

        // typed random access for String values
        String stringValue = df.getValueAt(0, nameColumnId);
        assertEquals("Ada", stringValue);

        // typed random access for primitive ints
        int intValue = df.getValueAt(1, ageColumnId);
        assertEquals(99, intValue);

        // typed random access for primitive doubles
        double doubleValue = df.getValueAt(0, heightColumnId);
        assertEquals(1.74, doubleValue, 0.01);

        // typed random access for booleans
        boolean booleanValue = df.getValueAt(2, vegetarianColumnId);
        assertFalse(booleanValue);

        // typed random access for categories
        String categoryValue = df.getValueAt(2, genderColumnId);
        assertEquals("Female", categoryValue);
    }

    private static void assertMetaDataParsedCorrectly(DataFrame df) {
        Map<String, String> dataFrameMetaData = df.getMetaData();
        assertEquals(1, dataFrameMetaData.size());
        assertEquals(Option.of("netzwerg"), dataFrameMetaData.get("author"));

        Map<String, String> columnMetaData = df.getColumn(df.getColumnId(2, ColumnType.DOUBLE)).getMetaData();
        assertEquals(1, columnMetaData.size());
        assertEquals(Option.of("m"), columnMetaData.get("unit"));
    }

    private static void assertSchemaWithCharset(Function<Schema, DataFrame> parseLogic) throws IOException {
        StringReader schemaReader = new StringReader(SCHEMA_WITH_CHARSET_NAME);
        Schema schema = Schema.parseJson(schemaReader);
        DataFrame dataFrame = parseLogic.apply(schema);
        StringColumnId columnId = dataFrame.getColumnId(0, ColumnType.STRING);
        assertEquals("°", dataFrame.getValueAt(0, columnId));
    }

}