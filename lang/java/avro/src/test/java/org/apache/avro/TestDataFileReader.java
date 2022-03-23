/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.avro;

import static org.junit.Assert.assertEquals;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.file.FileReader;
import org.apache.avro.file.SeekableFileInput;
import org.apache.avro.file.SeekableInput;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.junit.Test;

@SuppressWarnings("restriction")
public class TestDataFileReader {

  @Test
  // regression test for bug AVRO-2944
  public void testThrottledInputStream() throws IOException {
    // AVRO-2944 describes hanging/failure in reading Avro file with performing
    // magic header check. This happens with throttled input stream,
    // where we read into buffer less bytes than requested.

    Schema legacySchema = new Schema.Parser().setValidate(false).setValidateDefaults(false)
        .parse("{\"type\": \"record\", \"name\": \"TestSchema\", \"fields\": "
            + "[ {\"name\": \"id\", \"type\": [\"long\", \"null\"], \"default\": null}]}");
    File f = Files.createTempFile("testThrottledInputStream", ".avro").toFile();
    try (DataFileWriter<?> w = new DataFileWriter<>(new GenericDatumWriter<>())) {
      w.create(legacySchema, f);
      w.flush();
    }

    // Without checking for magic header, throttled input has no effect
    FileReader r = new DataFileReader(throttledInputStream(f), new GenericDatumReader<>());
    assertEquals("TestSchema", r.getSchema().getName());

    // With checking for magic header, throttled input should pass too.
    FileReader r2 = DataFileReader.openReader(throttledInputStream(f), new GenericDatumReader<>());
    assertEquals("TestSchema", r2.getSchema().getName());
  }

  private SeekableInput throttledInputStream(File f) throws IOException {
    SeekableFileInput input = new SeekableFileInput(f);
    return new SeekableInput() {
      @Override
      public void close() throws IOException {
        input.close();
      }

      @Override
      public void seek(long p) throws IOException {
        input.seek(p);
      }

      @Override
      public long tell() throws IOException {
        return input.tell();
      }

      @Override
      public long length() throws IOException {
        return input.length();
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        if (len == 1) {
          return input.read(b, off, len);
        } else {
          return input.read(b, off, len - 1);
        }
      }
    };
  }

  @Test(expected = EOFException.class)
  // another regression test for bug AVRO-2944, testing EOF case
  public void testInputStreamEOF() throws IOException {
    // AVRO-2944 describes hanging/failure in reading Avro file with performing
    // magic header check. This potentially happens with a defective input stream
    // where a -1 value is unexpectedly returned from a read.
    Schema legacySchema = new Schema.Parser().setValidate(false).setValidateDefaults(false)
        .parse("{\"type\": \"record\", \"name\": \"TestSchema\", \"fields\": "
            + "[ {\"name\": \"id\", \"type\": [\"long\", \"null\"], \"default\": null}]}");
    File f = Files.createTempFile("testInputStreamEOF", ".avro").toFile();
    try (DataFileWriter<?> w = new DataFileWriter<>(new GenericDatumWriter<>())) {
      w.create(legacySchema, f);
      w.flush();
    }

    // Should throw an EOFException
    DataFileReader.openReader(eofInputStream(f), new GenericDatumReader<>());
  }

  private SeekableInput eofInputStream(File f) throws IOException {
    SeekableFileInput input = new SeekableFileInput(f);
    return new SeekableInput() {
      @Override
      public void close() throws IOException {
        input.close();
      }

      @Override
      public void seek(long p) throws IOException {
        input.seek(p);
      }

      @Override
      public long tell() throws IOException {
        return input.tell();
      }

      @Override
      public long length() throws IOException {
        return input.length();
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        return -1;
      }
    };
  }

}
