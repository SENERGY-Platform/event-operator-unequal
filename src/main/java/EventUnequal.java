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

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.infai.seits.sepl.operators.Input;
import org.infai.seits.sepl.operators.Message;
import org.infai.seits.sepl.operators.OperatorInterface;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;


public class EventUnequal implements OperatorInterface {
    private Object value;
    private String url;
    private String eventId;
    private Converter converter;

    public EventUnequal(String valueString, String url, String eventId, Converter converter) {
        this.value = new JSONTokener(valueString).nextValue();
        this.url = url;
        this.eventId = eventId;
        this.converter = converter;
    }

    @Override
    public void run(Message message) {
        try{
            Input input = message.getInput("value");
            if(this.operator(input)){
                this.trigger(input);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private boolean operator(Input input) throws IOException {
        Object value;
        if(this.value instanceof String){
            value = input.getString();
        }else if(this.value instanceof Double){
            value = input.getValue();
        }else if(this.value instanceof Integer){
            value = input.getValue().intValue();
        }else if(this.value instanceof Float){
            value = input.getValue().floatValue();
        }else{
            value = null;
        }
        value = this.converter.convert(value);
        return !this.value.equals(value);
    }


    private void trigger(Input input){
        Object value;

        if(this.value instanceof String){
            value = input.getString();
        }else if(this.value instanceof Double){
            value = input.getValue();
        }else if(this.value instanceof Integer){
            value = input.getValue().intValue();
        }else if(this.value instanceof Float){
            value = input.getValue().floatValue();
        }else{
            value = input.getValue();
        }

        JSONObject json = new JSONObject()
                .put("messageName", this.eventId)
                .put("all", true)
                .put("resultEnabled", false)
                .put("processVariablesLocal", new JSONObject()
                        .put("event", new JSONObject()
                                .put("value", value)
                        )
                );

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        try {
            HttpPost request = new HttpPost(this.url);
            StringEntity params = new StringEntity(json.toString());
            request.addHeader("content-type", "application/json");
            request.setEntity(params);
            CloseableHttpResponse resp = httpClient.execute(request);
            resp.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void config(Message message) {
        message.addInput("value");
    }
}
