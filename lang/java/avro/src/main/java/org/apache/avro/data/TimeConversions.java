/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.avro.data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.apache.avro.Conversion;
import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

public class TimeConversions {
  public static class DateConversion extends Conversion<LocalDate> {
    private static final LocalDate EPOCH_DATE = new LocalDate(1970, 1, 1);

    @Override
    public Class<LocalDate> getConvertedType() {
      return LocalDate.class;
    }

    @Override
    public String getLogicalTypeName() {
      return "date";
    }

    @Override
    public LocalDate fromInt(Integer daysFromEpoch, Schema schema, LogicalType type) {
      return EPOCH_DATE.plusDays(daysFromEpoch);
    }

    @Override
    public Integer toInt(LocalDate date, Schema schema, LogicalType type) {
      return Days.daysBetween(EPOCH_DATE, date).getDays();
    }

    @Override
    public Schema getRecommendedSchema() {
      return LogicalTypes.date().addToSchema(Schema.create(Schema.Type.INT));
    }
  }

  public static class TimeConversion extends Conversion<LocalTime> {
    @Override
    public Class<LocalTime> getConvertedType() {
      return LocalTime.class;
    }

    @Override
    public String getLogicalTypeName() {
      return "time-millis";
    }

    @Override
    public LocalTime fromInt(Integer millisFromMidnight, Schema schema, LogicalType type) {
      return LocalTime.fromMillisOfDay(millisFromMidnight);
    }

    @Override
    public Integer toInt(LocalTime time, Schema schema, LogicalType type) {
      return time.millisOfDay().get();
    }

    @Override
    public Schema getRecommendedSchema() {
      return LogicalTypes.timeMillis().addToSchema(Schema.create(Schema.Type.INT));
    }
}

  public static class TimeMicrosConversion extends Conversion<LocalTime> {
    @Override
    public Class<LocalTime> getConvertedType() {
      return LocalTime.class;
    }

    @Override
    public String getLogicalTypeName() {
      return "time-micros";
    }

    @Override
    public LocalTime fromLong(Long microsFromMidnight, Schema schema, LogicalType type) {
      return LocalTime.fromMillisOfDay(microsFromMidnight / 1000);
    }

    @Override
    public Schema getRecommendedSchema() {
      return LogicalTypes.timeMicros().addToSchema(Schema.create(Schema.Type.LONG));
    }
  }

  public static class LossyTimeMicrosConversion extends TimeMicrosConversion {
    @Override
    public Long toLong(LocalTime time, Schema schema, LogicalType type) {
      return 1000 * (long) time.millisOfDay().get();
    }
  }

  public static class TimestampConversion extends Conversion<DateTime> {
    @Override
    public Class<DateTime> getConvertedType() {
      return DateTime.class;
    }

    @Override
    public String getLogicalTypeName() {
      return "timestamp-millis";
    }

    @Override
    public DateTime fromLong(Long millisFromEpoch, Schema schema, LogicalType type) {
      return new DateTime(millisFromEpoch, DateTimeZone.UTC);
    }

    @Override
    public Long toLong(DateTime timestamp, Schema schema, LogicalType type) {
      return timestamp.getMillis();
    }

    @Override
    public Schema getRecommendedSchema() {
      return LogicalTypes.timestampMillis().addToSchema(Schema.create(Schema.Type.LONG));
    }
  }

  public static class TimestampMicrosConversion extends Conversion<DateTime> {
    @Override
    public Class<DateTime> getConvertedType() {
      return DateTime.class;
    }

    @Override
    public String getLogicalTypeName() {
      return "timestamp-micros";
    }

    @Override
    public DateTime fromLong(Long microsFromEpoch, Schema schema, LogicalType type) {
      return new DateTime(microsFromEpoch / 1000, DateTimeZone.UTC);
    }

    @Override
    public Schema getRecommendedSchema() {
      return LogicalTypes.timestampMicros().addToSchema(Schema.create(Schema.Type.LONG));
    }
}

  public static class LossyTimestampMicrosConversion extends TimestampMicrosConversion {
    @Override
    public Long toLong(DateTime timestamp, Schema schema, LogicalType type) {
      return 1000 * timestamp.getMillis();
    }
  }


  public static class LocalTimestampMillisConversion extends Conversion<LocalDateTime> {
    public Instant timestampMillisConversionFromLong(Long millisFromEpoch, Schema schema, LogicalType type) {
      return Instant.ofEpochMilli(millisFromEpoch);
    }

    public Long timestampMillisConversionToLong(Instant timestamp, Schema schema, LogicalType type) {
      return timestamp.toEpochMilli();
    }

    @Override
    public Class<LocalDateTime> getConvertedType() {
      return LocalDateTime.class;
    }

    @Override
    public String getLogicalTypeName() {
      return "local-timestamp-millis";
    }

    @Override
    public LocalDateTime fromLong(Long millisFromEpoch, Schema schema, LogicalType type) {
      Instant instant = timestampMillisConversionFromLong(millisFromEpoch, schema, type);
      return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    @Override
    public Long toLong(LocalDateTime timestamp, Schema schema, LogicalType type) {
      Instant instant = timestamp.toInstant(ZoneOffset.UTC);
      return timestampMillisConversionToLong(instant, schema, type);
    }

    @Override
    public Schema getRecommendedSchema() {
      return LogicalTypes.localTimestampMillis().addToSchema(Schema.create(Schema.Type.LONG));
    }
  }

  public static class LocalTimestampMicrosConversion extends Conversion<LocalDateTime> {

    private static Instant timestampMicrosConversionFromLong(Long microsFromEpoch) {
      long epochSeconds = microsFromEpoch / (1_000_000L);
      long nanoAdjustment = (microsFromEpoch % (1_000_000L)) * 1_000L;

      return Instant.ofEpochSecond(epochSeconds, nanoAdjustment);
    }

    private static Long timestampMicrosConversionToLong(Instant instant) {
      long seconds = instant.getEpochSecond();
      int nanos = instant.getNano();

      if (seconds < 0 && nanos > 0) {
        long micros = Math.multiplyExact(seconds + 1, 1_000_000L);
        long adjustment = (nanos / 1_000L) - 1_000_000;

        return Math.addExact(micros, adjustment);
      } else {
        long micros = Math.multiplyExact(seconds, 1_000_000L);

        return Math.addExact(micros, nanos / 1_000L);
      }
    }

    @Override
    public Class<LocalDateTime> getConvertedType() {
      return LocalDateTime.class;
    }

    @Override
    public String getLogicalTypeName() {
      return "local-timestamp-micros";
    }

    @Override
    public LocalDateTime fromLong(Long microsFromEpoch, Schema schema, LogicalType type) {
      Instant instant = timestampMicrosConversionFromLong(microsFromEpoch);
      return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    @Override
    public Long toLong(LocalDateTime timestamp, Schema schema, LogicalType type) {
      Instant instant = timestamp.toInstant(ZoneOffset.UTC);
      return timestampMicrosConversionToLong(instant);
    }

    @Override
    public Schema getRecommendedSchema() {
      return LogicalTypes.localTimestampMicros().addToSchema(Schema.create(Schema.Type.LONG));
    }
  }
}
