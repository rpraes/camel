/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.hazelcast;

import java.util.Arrays;
import java.util.Collection;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MultiMap;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.builder.RouteBuilder;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.*;

public class HazelcastMultimapProducerTest extends HazelcastCamelTestSupport {

    @Mock
    private MultiMap<Object, Object> map;

    @Override
    protected void trainHazelcastInstance(HazelcastInstance hazelcastInstance) {
        when(hazelcastInstance.getMultiMap("bar")).thenReturn(map);
    }

    @Override
    protected void verifyHazelcastInstance(HazelcastInstance hazelcastInstance) {
        verify(hazelcastInstance, atLeastOnce()).getMultiMap("bar");
    }

    @After
    public void verifyMapMock() {
        verifyNoMoreInteractions(map);
    }

    @Test(expected = CamelExecutionException.class)
    public void testWithInvalidOperation() {
        template.sendBodyAndHeader("direct:putInvalid", "my-foo", HazelcastConstants.OBJECT_ID, "4711");
    }

    @Test
    public void testPut() throws InterruptedException {
        template.sendBodyAndHeader("direct:put", "my-foo", HazelcastConstants.OBJECT_ID, "4711");
        verify(map).put("4711", "my-foo");
    }

    @Test
    public void testPutWithOperationName() throws InterruptedException {
        template.sendBodyAndHeader("direct:putWithOperationName", "my-foo", HazelcastConstants.OBJECT_ID, "4711");
        verify(map).put("4711", "my-foo");
    }

    @Test
    public void testPutWithOperationNumber() throws InterruptedException {
        template.sendBodyAndHeader("direct:putWithOperationNumber", "my-foo", HazelcastConstants.OBJECT_ID, "4711");
        verify(map).put("4711", "my-foo");
    }

    @Test
    public void testRemoveValue() {
        template.sendBodyAndHeader("direct:removevalue", "my-foo", HazelcastConstants.OBJECT_ID, "4711");
        verify(map).remove("4711", "my-foo");
    }

    @Test
    public void testGet() {
        when(map.get("4711")).thenReturn(Arrays.<Object>asList("my-foo"));
        template.sendBodyAndHeader("direct:get", null, HazelcastConstants.OBJECT_ID, "4711");
        verify(map).get("4711");
        Collection<?> body = consumer.receiveBody("seda:out", 5000, Collection.class);
        assertTrue(body.contains("my-foo"));
    }

    @Test
    public void testDelete() {
        template.sendBodyAndHeader("direct:delete", null, HazelcastConstants.OBJECT_ID, 4711);
        verify(map).remove(4711);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from("direct:putInvalid").setHeader(HazelcastConstants.OPERATION, constant("bogus")).to(String.format("hazelcast:%sbar", HazelcastConstants.MULTIMAP_PREFIX));

                from("direct:put").setHeader(HazelcastConstants.OPERATION, constant(HazelcastConstants.PUT_OPERATION)).to(String.format("hazelcast:%sbar", HazelcastConstants.MULTIMAP_PREFIX));

                from("direct:removevalue").setHeader(HazelcastConstants.OPERATION, constant(HazelcastConstants.REMOVEVALUE_OPERATION)).to(
                        String.format("hazelcast:%sbar", HazelcastConstants.MULTIMAP_PREFIX));

                from("direct:get").setHeader(HazelcastConstants.OPERATION, constant(HazelcastConstants.GET_OPERATION)).to(String.format("hazelcast:%sbar", HazelcastConstants.MULTIMAP_PREFIX))
                        .to("seda:out");

                from("direct:delete").setHeader(HazelcastConstants.OPERATION, constant(HazelcastConstants.DELETE_OPERATION)).to(String.format("hazelcast:%sbar", HazelcastConstants.MULTIMAP_PREFIX));

                from("direct:putWithOperationNumber").toF("hazelcast:%sbar?operation=%s", HazelcastConstants.MULTIMAP_PREFIX, HazelcastConstants.PUT_OPERATION);
                from("direct:putWithOperationName").toF("hazelcast:%sbar?operation=put", HazelcastConstants.MULTIMAP_PREFIX);
            }
        };
    }

}
