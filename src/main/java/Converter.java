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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.infai.ses.senergy.operators.FlexInput;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Converter {
    private String url;
    private String from;
    private String to;
    private Map<String, List<PathAndCharacteristic>> topicToPathAndCharacteristic;

    public Converter(String url, String from, String to) {
        this(url, from, to, "");
    }

    public Converter(String url, String from, String to, String topicToPathAndCharacteristic) {
        this.url = url;
        this.from = from;
        this.to = to;
        this.topicToPathAndCharacteristic = this.parseTopicToPathAndCharacteristic(topicToPathAndCharacteristic);
    }


    public Object convert(Object in) throws IOException {
        return this.convert(this.from, this.to, in);
    }

    public Object convert(FlexInput input, Object value) throws IOException {
        String fromCharacteristic = this.from;
        if (fromCharacteristic.equals("")) {
            String topic = input.getCurrentInputTopic();
            List<PathAndCharacteristic> list = this.topicToPathAndCharacteristic.getOrDefault(topic, new ArrayList<PathAndCharacteristic>());
            if (list.size() > 0) {
                fromCharacteristic = list.get(0).characteristic_id;
            }
        }
        return this.convert(fromCharacteristic, this.to, value);
    }

    public Object convert(String fromCharacteristic, String to, Object toCharacteristic) throws IOException {
        if(this.useConverter(fromCharacteristic, to)) {
            StringEntity entity = new StringEntity(this.objToJsonStr(toCharacteristic));
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost(this.url + "/" + fromCharacteristic + "/" + to);
            request.setEntity(entity);
            request.addHeader("content-type", "application/json");
            CloseableHttpResponse resp = httpClient.execute(request);
            String respStr = new BasicResponseHandler().handleResponse(resp);
            return this.jsonStrToObject(respStr);
        }else{
            return toCharacteristic;
        }
    }

    public boolean useConverter(String fromCharacteristic, String toCharacteristic) {
        if (this.url.equals("")) {
            return false;
        }
        if (fromCharacteristic.equals("") || toCharacteristic.equals("")) {
            return false;
        }
        if (fromCharacteristic.equals(toCharacteristic)) {
            return false;
        }
        return true;
    }

    private String objToJsonStr(Object in) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, in);
        return writer.toString();
    }

    private Object jsonStrToObject(String in) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(in, Object.class);
    }

    private Map<String,List<PathAndCharacteristic>> parseTopicToPathAndCharacteristic(String topicToPathAndCharacteristic){
        topicToPathAndCharacteristic = topicToPathAndCharacteristic.trim();
        if (topicToPathAndCharacteristic.equals("")) {
            return new HashMap<String,List<PathAndCharacteristic>>();
        }
        Gson g = new Gson();
        Type mapType = new TypeToken<Map<String,List<PathAndCharacteristic>>>() {}.getType();
        return g.fromJson(topicToPathAndCharacteristic, mapType);
    }

}