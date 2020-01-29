/*
 * Copyright 2020 InfAI (CC SES)
 *
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

import com.sun.net.httpserver.HttpServer;
import org.infai.seits.sepl.operators.Message;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;


public class EventUnequalTest {
    public static boolean called = false;
    private static Object processVariable = null;

    private Object jsonNormalize(Object in) throws ParseException {
        Map<String, Object> wrapper = new HashMap<String, Object>();
        wrapper.put("value", in);
        JSONObject temp = new JSONObject(wrapper);
        Object candidate = ((JSONObject)(new JSONParser().parse(temp.toJSONString()))).get("value");
        if(candidate instanceof Long){
            candidate = Double.valueOf((Long)candidate);
        }
        return candidate;
    }

    private void test(String configuredValue, Object messageValue, boolean expectedToTrigger) throws IOException {
        EventUnequalTest.called = false;
        HttpServer server = TriggerServerMock.create(inputStream -> {
            JSONParser jsonParser = new JSONParser();
            try {
                JSONObject jsonObject = (JSONObject)jsonParser.parse(new InputStreamReader(inputStream, "UTF-8"));
                if(
                        jsonObject.containsKey("localVariables")
                        && ((JSONObject)jsonObject.get("localVariables")).containsKey("event")
                        && ((JSONObject)((JSONObject)jsonObject.get("localVariables")).get("event")).containsKey("value")
                ){
                    EventUnequalTest.called = true;
                    EventUnequalTest.processVariable = ((JSONObject)((JSONObject)jsonObject.get("localVariables")).get("event")).get("value");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        EventUnequal events = new EventUnequal(configuredValue, "http://localhost:"+server.getAddress().getPort()+"/endpoint", "test");
        Message msg = TestMessageProvider.getTestMessage(messageValue);
        events.config(msg);
        events.run(msg);
        server.stop(0);
        Assert.assertEquals(EventUnequalTest.called, expectedToTrigger);
        if(expectedToTrigger){
            try {
                Object a = jsonNormalize(EventUnequalTest.processVariable);
                Object b = jsonNormalize(messageValue);
                Assert.assertEquals(a, b);
            } catch (ParseException e) {
                Assert.fail(e.getMessage());
            }
        }
    }

    @Test
    public void stringUnequalFalse() throws IOException {
        test("\"foobar\"", "foobar",false);
    }

    @Test
    public void stringUnequalTrue() throws IOException {
        test("\"foobar\"", "foo",true);
    }

    @Test
    public void numberUnequalFalse() throws IOException {
        test("42", 42,false);
    }

    @Test
    public void floatUnequalFalse() throws IOException {
        test("4.2", 4.2, false);
    }

    @Test
    public void floatUnequalFalse2() throws IOException {
        test("42.0", 42.0, false);
    }

    @Test
    public void floatUnequalFalse3() throws IOException {
        test("42", 42.0, false);
    }


    @Test
    public void floatUnequalTrue() throws IOException {
        test("4.2", 13, true);
    }

    @Test
    public void numberUnequalTrue() throws IOException {
        test("42", 13, true);
    }

    @Test
    @Ignore("this test can not be successful; operator will interpret number as string (\"42\"); test-helper compares with original")
    public void stringNumber() throws IOException {
        test("\"foobar\"", 42, true);
    }

    @Test
    public void numberString() throws IOException {
        //string "foo" can not be interpreted as number -> no event trigger
        test("42", "foo", false);
    }
}