// This file is part of OpenTSDB.
// Copyright (C) 2018  The OpenTSDB Authors.
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
package net.opentsdb.query.idconverter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import net.opentsdb.core.MockTSDB;
import net.opentsdb.core.MockTSDBDefault;
import net.opentsdb.data.TimeSeriesDataSourceFactory;
import net.opentsdb.utils.JSON;

public class TestByteToStringIdConverterConfig {

  @Test
  public void builder() throws Exception {
    TimeSeriesDataSourceFactory m1 = mock(TimeSeriesDataSourceFactory.class);
    TimeSeriesDataSourceFactory m2 = mock(TimeSeriesDataSourceFactory.class);
    ByteToStringIdConverterConfig config = 
        (ByteToStringIdConverterConfig) ByteToStringIdConverterConfig.newBuilder()
        .addDataSource("m1", m1)
        .addDataSource("m2", m2)
        .setId("cvtr")
        .build();
    
    assertEquals("cvtr", config.getId());
    assertEquals(ByteToStringIdConverterFactory.TYPE, config.getType());
    assertSame(m1, config.getFactory("m1"));
    assertSame(m2, config.getFactory("m2"));
    assertNull(config.getFactory("m3"));
  }
  
  @Test
  public void serdes() throws Exception {
    ByteToStringIdConverterConfig config = 
        (ByteToStringIdConverterConfig) ByteToStringIdConverterConfig.newBuilder()
        .setId("cvtr")
        .build();
    
    String json = JSON.serializeToString(config);
    assertTrue(json.contains("\"id\":\"cvtr\""));
    assertTrue(json.contains("\"type\":\"ByteToStringIdConverter\""));
    
    MockTSDB tsdb = MockTSDBDefault.getMockTSDB();
    JsonNode node = JSON.getMapper().readTree(json);
    config = (ByteToStringIdConverterConfig) new ByteToStringIdConverterFactory()
        .parseConfig(JSON.getMapper(), tsdb, node);
    
    assertEquals("cvtr", config.getId());
    assertEquals(ByteToStringIdConverterFactory.TYPE, config.getType());
    
  }
  
}