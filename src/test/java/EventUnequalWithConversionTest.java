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
import org.infai.ses.senergy.models.DeviceMessageModel;
import org.infai.ses.senergy.models.MessageModel;
import org.infai.ses.senergy.operators.Config;
import org.infai.ses.senergy.operators.Helper;
import org.infai.ses.senergy.operators.Message;
import org.infai.ses.senergy.testing.utils.JSONHelper;
import org.infai.ses.senergy.utils.ConfigProvider;
import org.json.JSONException;
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


public class EventUnequalWithConversionTest {
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

    private void test(String configuredValue, Object actualValue, boolean expectedToTrigger) throws IOException, JSONException {
        EventUnequalWithConversionTest.called = false;
        HttpServer server = TriggerServerMock.create(inputStream -> {
            JSONParser jsonParser = new JSONParser();
            try {
                JSONObject jsonObject = (JSONObject)jsonParser.parse(new InputStreamReader(inputStream, "UTF-8"));
                if(
                        jsonObject.containsKey("processVariablesLocal")
                        && ((JSONObject)jsonObject.get("processVariablesLocal")).containsKey("event")
                        && ((JSONObject)((JSONObject)jsonObject.get("processVariablesLocal")).get("event")).containsKey("value")
                ){
                    EventUnequalWithConversionTest.called = true;
                    EventUnequalWithConversionTest.processVariable = ((JSONObject)((JSONObject)jsonObject.get("processVariablesLocal")).get("event")).get("value");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        HttpServer converterServer = ConverterServerMock.create("/inCharacteristic/outCharacteristic");
        String mockUrl = "http://localhost:"+converterServer.getAddress().getPort();
        Converter converter = new Converter(mockUrl, mockUrl, "inCharacteristic", "outCharacteristic");
        EventUnequal events = new EventUnequal("", configuredValue, "http://localhost:"+server.getAddress().getPort()+"/endpoint", "test", converter);
        Config config = new Config(new JSONHelper().parseFile("config.json").toString());
        ConfigProvider.setConfig(config);
        MessageModel model = new MessageModel();
        Message message = new Message();
        events.configMessage(message);
        JSONObject m = new JSONHelper().parseFile("message.json");
        ((JSONObject)((JSONObject) m.get("value")).get("reading")).put("value", actualValue);
        DeviceMessageModel deviceMessageModel = JSONHelper.getObjectFromJSONString(m.toString(), DeviceMessageModel.class);
        assert deviceMessageModel != null;
        String topicName = config.getInputTopicsConfigs().get(0).getName();
        model.putMessage(topicName, Helper.deviceToInputMessageModel(deviceMessageModel, topicName));
        message.setMessage(model);
        events.run(message);
        server.stop(0);
        Assert.assertEquals(EventUnequalWithConversionTest.called, expectedToTrigger);
        if(expectedToTrigger){
            try {
                Object a = jsonNormalize(EventUnequalWithConversionTest.processVariable);
                Object b = jsonNormalize(actualValue);
                Assert.assertEquals(a, b);
            } catch (ParseException e) {
                Assert.fail(e.getMessage());
            }
        }
    }

    private void testWithConversion(String configuredValue, Object actualValue, String conversionResp, boolean expectedToTrigger) throws IOException, JSONException {
        EventUnequalWithConversionTest.called = false;
        HttpServer server = TriggerServerMock.create(inputStream -> {
            JSONParser jsonParser = new JSONParser();
            try {
                JSONObject jsonObject = (JSONObject)jsonParser.parse(new InputStreamReader(inputStream, "UTF-8"));
                if(
                        jsonObject.containsKey("processVariablesLocal")
                                && ((JSONObject)jsonObject.get("processVariablesLocal")).containsKey("event")
                                && ((JSONObject)((JSONObject)jsonObject.get("processVariablesLocal")).get("event")).containsKey("value")
                ){
                    EventUnequalWithConversionTest.called = true;
                    EventUnequalWithConversionTest.processVariable = ((JSONObject)((JSONObject)jsonObject.get("processVariablesLocal")).get("event")).get("value");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        HttpServer converterServer = ConverterServerMock.createWithResponse("/inCharacteristic/outCharacteristic", conversionResp);
        String mockUrl = "http://localhost:"+converterServer.getAddress().getPort();
        Converter converter = new Converter(mockUrl, mockUrl, "inCharacteristic", "outCharacteristic");
        EventUnequal events = new EventUnequal("", configuredValue, "http://localhost:"+server.getAddress().getPort()+"/endpoint", "test", converter);
        Config config = new Config(new JSONHelper().parseFile("config.json").toString());
        ConfigProvider.setConfig(config);
        MessageModel model = new MessageModel();
        Message message = new Message();
        events.configMessage(message);
        JSONObject m = new JSONHelper().parseFile("message.json");
        ((JSONObject)((JSONObject) m.get("value")).get("reading")).put("value", actualValue);
        DeviceMessageModel deviceMessageModel = JSONHelper.getObjectFromJSONString(m.toString(), DeviceMessageModel.class);
        assert deviceMessageModel != null;
        String topicName = config.getInputTopicsConfigs().get(0).getName();
        model.putMessage(topicName, Helper.deviceToInputMessageModel(deviceMessageModel, topicName));
        message.setMessage(model);
        events.run(message);
        server.stop(0);
        Assert.assertEquals(EventUnequalWithConversionTest.called, expectedToTrigger);
        if(expectedToTrigger){
            try {
                Object a = jsonNormalize(EventUnequalWithConversionTest.processVariable);
                Object b = jsonNormalize(actualValue);
                Assert.assertEquals(a, b);
            } catch (ParseException e) {
                Assert.fail(e.getMessage());
            }
        }
    }

    @Test
    public void stringEqualTrue() throws IOException, JSONException {
        test("\"foobar\"", "foobar",false);
    }

    @Test
    public void convertedStringEqualTrue() throws IOException, JSONException {
        testWithConversion("\"foobar\"", "foo", "\"foobar\"",false);
    }

    @Test
    public void convertedStringEqualFalse() throws IOException, JSONException {
        testWithConversion("\"foobar\"", "foo", "\"foo\"",true);
    }

    @Test
    public void stringEqualFalse() throws IOException, JSONException {
        test("\"foobar\"", "foo",true);
    }

    @Test
    public void numberEqualTrue() throws IOException, JSONException {
        test("42", 42,false);
    }

    @Test
    public void convertedNumberEqualTrue() throws IOException, JSONException {
        testWithConversion("42", 42, "42",false);
    }

    @Test
    public void convertedNumberEqualFalse() throws IOException, JSONException {
        testWithConversion("42", 13, "13",true);
    }

    @Test
    public void convertedNumberEqualTrue2() throws IOException, JSONException {
        testWithConversion("42", 13, "42",false);
    }

    @Test
    public void convertedNumberEqualFalse2() throws IOException, JSONException {
        testWithConversion("42", 13, "13",true);
    }

    @Test
    public void floatEqualTrue() throws IOException, JSONException {
        test("4.2", 4.2, false);
    }

    @Test
    public void floatEqualTrue2() throws IOException, JSONException {
        test("42.0", 42.0, false);
    }

    @Test
    public void floatEqualTrue3() throws IOException, JSONException {
        test("42", 42.0, false);
    }


    @Test
    public void floatEqualFalse() throws IOException, JSONException {
        test("4.2", 13, true);
    }

    @Test
    public void numberEqualFalse() throws IOException, JSONException {
        test("42", 13, true);
    }

    @Test
    @Ignore("this test can not be successful; operator will interpret number as string (\"42\"); test-helper compares with original")
    public void stringNumber() throws IOException, JSONException {
        test("\"foobar\"", 42, true);
    }

    @Test
    public void numberString() throws IOException, JSONException {
        test("42", "foo", true);
    }
}
