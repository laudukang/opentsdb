// This file is part of OpenTSDB.
// Copyright (C)2019  The OpenTSDB Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package net.opentsdb.query.processor.summarizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import net.opentsdb.data.BaseTimeSeriesStringId;
import net.opentsdb.data.MockTimeSeries;
import net.opentsdb.data.SecondTimeStamp;
import net.opentsdb.data.TimeSeries;
import net.opentsdb.data.TimeSeriesValue;
import net.opentsdb.data.TimeSpecification;
import org.junit.Before;
import org.junit.Test;

import net.opentsdb.data.types.numeric.MutableNumericValue;
import net.opentsdb.data.types.numeric.NumericType;
import net.opentsdb.query.DefaultTimeSeriesDataSourceConfig;
import net.opentsdb.query.QueryMode;
import net.opentsdb.query.QueryPipelineContext;
import net.opentsdb.query.SemanticQuery;
import net.opentsdb.query.filter.MetricLiteralFilter;

public class TestSummarizerPassThroughNumericIterator {
  private static final int BASE_TIME = 1546300800;
  
  private SemanticQuery query;
  private QueryPipelineContext context;
  private SummarizerPassThroughResult result;
  
  @Before
  public void before() throws Exception {
    context = mock(QueryPipelineContext.class);
    query = SemanticQuery.newBuilder()
        .setStart(Integer.toString(BASE_TIME))
        .setEnd(Integer.toString(BASE_TIME * (3600 * 4)))
        .setMode(QueryMode.SINGLE)
        .addExecutionGraphNode(DefaultTimeSeriesDataSourceConfig.newBuilder()
            .setMetric(MetricLiteralFilter.newBuilder()
                .setMetric("sys.cpu.user")
                .build())
            .setId("m1")
            .build())
        .addExecutionGraphNode(SummarizerConfig.newBuilder()
            .addSummary("sum")
            .addSource("m1")
            .setId("summarizer")
            .build())
        .build();
    when(context.query()).thenReturn(query);
    result = mock(SummarizerPassThroughResult.class);
    Summarizer node = mock(Summarizer.class);
    when(node.pipelineContext()).thenReturn(context);
    when(result.summarizerNode()).thenReturn(node);
    
    TimeSpecification time_spec = mock(TimeSpecification.class);
    when(time_spec.start()).thenReturn(new SecondTimeStamp(BASE_TIME));
    when(time_spec.interval()).thenReturn(Duration.ofSeconds(60));
    when(result.timeSpecification()).thenReturn(time_spec);
  }
  
  @Test
  public void longs() throws Exception {
    TimeSeries series = new MockTimeSeries(new BaseTimeSeriesStringId.Builder()
        .setMetric("foo")
        .build());
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 0L), 42));
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 60L), 24));
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 120L), -8));
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 240L), 1));
    SummarizedTimeSeries sts = spy(new SummarizedTimeSeries(result, series));
    SummarizerPassThroughNumericIterator iterator = 
        new SummarizerPassThroughNumericIterator(sts);
    
    assertTrue(iterator.hasNext());
    TimeSeriesValue<NumericType> value = 
        (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME, value.timestamp().epoch());
    assertEquals(42, value.value().longValue());
    
    value = (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME + 60, value.timestamp().epoch());
    assertEquals(24, value.value().longValue());
   
    value = (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME + 120, value.timestamp().epoch());
    assertEquals(-8, value.value().longValue());
    
    value = (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME + 240, value.timestamp().epoch());
    assertEquals(1, value.value().longValue());
    
    verify(sts, never()).summarize(any(long[].class), anyInt(), anyInt());
    assertFalse(iterator.hasNext());
    
    verify(sts, times(1)).summarize(eq(new long[] { 42, 24, -8, 1, 0, 0, 0, 0 }), 
        eq(0), eq(4));
  }
  
  @Test
  public void doubles() throws Exception {
    TimeSeries series = new MockTimeSeries(new BaseTimeSeriesStringId.Builder()
        .setMetric("foo")
        .build());
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 0L), 42.75));
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 60L), 24.1));
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 120L), -8.98));
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 240L), 1.5));
    SummarizedTimeSeries sts = spy(new SummarizedTimeSeries(result, series));
    SummarizerPassThroughNumericIterator iterator = 
        new SummarizerPassThroughNumericIterator(sts);
    
    assertTrue(iterator.hasNext());
    TimeSeriesValue<NumericType> value = 
        (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME, value.timestamp().epoch());
    assertEquals(42.75, value.value().doubleValue(), 0.001);
    
    value = (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME + 60, value.timestamp().epoch());
    assertEquals(24.1, value.value().doubleValue(), 0.001);
   
    value = (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME + 120, value.timestamp().epoch());
    assertEquals(-8.98, value.value().doubleValue(), 0.001);
    
    value = (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME + 240, value.timestamp().epoch());
    assertEquals(1.5, value.value().doubleValue(), 0.001);
    
    verify(sts, never()).summarize(any(long[].class), anyInt(), anyInt());
    assertFalse(iterator.hasNext());
    
    verify(sts, times(1)).summarize(eq(new double[] { 42.75, 24.1, -8.98, 1.5, 0, 0, 0, 0 }), 
        eq(0), eq(4));
  }
  
  @Test
  public void mixed() throws Exception {
    TimeSeries series = new MockTimeSeries(new BaseTimeSeriesStringId.Builder()
        .setMetric("foo")
        .build());
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 0L), 42.75));
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 60L), 24));
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 120L), -8));
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 240L), 1.5));
    SummarizedTimeSeries sts = spy(new SummarizedTimeSeries(result, series));
    SummarizerPassThroughNumericIterator iterator = 
        new SummarizerPassThroughNumericIterator(sts);
    
    assertTrue(iterator.hasNext());
    TimeSeriesValue<NumericType> value = 
        (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME, value.timestamp().epoch());
    assertEquals(42.75, value.value().doubleValue(), 0.001);
    
    value = (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME + 60, value.timestamp().epoch());
    assertEquals(24, value.value().longValue());
   
    value = (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME + 120, value.timestamp().epoch());
    assertEquals(-8, value.value().longValue());
    
    value = (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME + 240, value.timestamp().epoch());
    assertEquals(1.5, value.value().doubleValue(), 0.001);
    
    verify(sts, never()).summarize(any(long[].class), anyInt(), anyInt());
    assertFalse(iterator.hasNext());
    
    verify(sts, times(1)).summarize(eq(new double[] { 42.75, 24, -8, 1.5, 0, 0, 0, 0 }), 
        eq(0), eq(4));
  }

  @Test
  public void filterMiddle() throws Exception {
    query = SemanticQuery.newBuilder()
        .setStart(Integer.toString(BASE_TIME + 60))
        .setEnd(Integer.toString(BASE_TIME + 180))
        .setMode(QueryMode.SINGLE)
        .addExecutionGraphNode(DefaultTimeSeriesDataSourceConfig.newBuilder()
            .setMetric(MetricLiteralFilter.newBuilder()
                .setMetric("sys.cpu.user")
                .build())
            .setId("m1")
            .build())
        .addExecutionGraphNode(SummarizerConfig.newBuilder()
            .addSummary("sum")
            .addSource("m1")
            .setId("summarizer")
            .build())
        .build();
    when(context.query()).thenReturn(query);
    
    TimeSeries series = new MockTimeSeries(new BaseTimeSeriesStringId.Builder()
        .setMetric("foo")
        .build());
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 0L), 42));
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 60L), 24));
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 120L), -8));
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 240L), 1));
    SummarizedTimeSeries sts = spy(new SummarizedTimeSeries(result, series));
    SummarizerPassThroughNumericIterator iterator = 
        new SummarizerPassThroughNumericIterator(sts);
    
    assertTrue(iterator.hasNext());
    TimeSeriesValue<NumericType> value = 
        (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME, value.timestamp().epoch());
    assertEquals(42, value.value().longValue());
    
    value = (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME + 60, value.timestamp().epoch());
    assertEquals(24, value.value().longValue());
   
    value = (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME + 120, value.timestamp().epoch());
    assertEquals(-8, value.value().longValue());
    
    value = (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME + 240, value.timestamp().epoch());
    assertEquals(1, value.value().longValue());
    
    verify(sts, never()).summarize(any(long[].class), anyInt(), anyInt());
    assertFalse(iterator.hasNext());
    
    verify(sts, times(1)).summarize(eq(new long[] { 24, -8, 0, 0, 0, 0, 0, 0 }), 
        eq(0), eq(2));
  }
  
  @Test
  public void filterEarly() throws Exception {
    query = SemanticQuery.newBuilder()
        .setStart(Integer.toString(BASE_TIME + 300))
        .setEnd(Integer.toString(BASE_TIME + 900))
        .setMode(QueryMode.SINGLE)
        .addExecutionGraphNode(DefaultTimeSeriesDataSourceConfig.newBuilder()
            .setMetric(MetricLiteralFilter.newBuilder()
                .setMetric("sys.cpu.user")
                .build())
            .setId("m1")
            .build())
        .addExecutionGraphNode(SummarizerConfig.newBuilder()
            .addSummary("sum")
            .addSource("m1")
            .setId("summarizer")
            .build())
        .build();
    when(context.query()).thenReturn(query);
    
    TimeSeries series = new MockTimeSeries(new BaseTimeSeriesStringId.Builder()
        .setMetric("foo")
        .build());
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 0L), 42));
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 60L), 24));
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 120L), -8));
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 240L), 1));
    SummarizedTimeSeries sts = spy(new SummarizedTimeSeries(result, series));
    SummarizerPassThroughNumericIterator iterator = 
        new SummarizerPassThroughNumericIterator(sts);
    
    assertTrue(iterator.hasNext());
    TimeSeriesValue<NumericType> value = 
        (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME, value.timestamp().epoch());
    assertEquals(42, value.value().longValue());
    
    value = (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME + 60, value.timestamp().epoch());
    assertEquals(24, value.value().longValue());
   
    value = (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME + 120, value.timestamp().epoch());
    assertEquals(-8, value.value().longValue());
    
    value = (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME + 240, value.timestamp().epoch());
    assertEquals(1, value.value().longValue());
    
    verify(sts, never()).summarize(any(long[].class), anyInt(), anyInt());
    assertFalse(iterator.hasNext());
    
    verify(sts, never()).summarize(any(long[].class), anyInt(), anyInt());
  }
  
  @Test
  public void filterLate() throws Exception {
    query = SemanticQuery.newBuilder()
        .setStart(Integer.toString(BASE_TIME - 900))
        .setEnd(Integer.toString(BASE_TIME - 60))
        .setMode(QueryMode.SINGLE)
        .addExecutionGraphNode(DefaultTimeSeriesDataSourceConfig.newBuilder()
            .setMetric(MetricLiteralFilter.newBuilder()
                .setMetric("sys.cpu.user")
                .build())
            .setId("m1")
            .build())
        .addExecutionGraphNode(SummarizerConfig.newBuilder()
            .addSummary("sum")
            .addSource("m1")
            .setId("summarizer")
            .build())
        .build();
    when(context.query()).thenReturn(query);
    
    TimeSeries series = new MockTimeSeries(new BaseTimeSeriesStringId.Builder()
        .setMetric("foo")
        .build());
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 0L), 42));
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 60L), 24));
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 120L), -8));
    ((MockTimeSeries) series).addValue(
        new MutableNumericValue(new SecondTimeStamp(BASE_TIME + 240L), 1));
    SummarizedTimeSeries sts = spy(new SummarizedTimeSeries(result, series));
    SummarizerPassThroughNumericIterator iterator = 
        new SummarizerPassThroughNumericIterator(sts);
    
    assertTrue(iterator.hasNext());
    TimeSeriesValue<NumericType> value = 
        (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME, value.timestamp().epoch());
    assertEquals(42, value.value().longValue());
    
    value = (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME + 60, value.timestamp().epoch());
    assertEquals(24, value.value().longValue());
   
    value = (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME + 120, value.timestamp().epoch());
    assertEquals(-8, value.value().longValue());
    
    value = (TimeSeriesValue<NumericType>) iterator.next();
    assertEquals(BASE_TIME + 240, value.timestamp().epoch());
    assertEquals(1, value.value().longValue());
    
    verify(sts, never()).summarize(any(long[].class), anyInt(), anyInt());
    assertFalse(iterator.hasNext());
    
    verify(sts, never()).summarize(any(long[].class), anyInt(), anyInt());
  }
  
}