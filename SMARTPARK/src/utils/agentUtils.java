package utils;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

public class agentUtils {
    public static int[] getRandomLocation() {
        int max = 10;
        int min = 1;
        int range = max - min + 1;

        int[] location = {(int) (Math.random() * range) + min, (int) (Math.random() * range) + min};
        return location;
    }

    /**
     * Send data to the middleware server that holds information about agents.
     * @param name
     * @param type
     * @param location
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static void sendData(String name, String type, String location) throws ClientProtocolException, IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("http://localhost:3000/update-agent");

        String json = "{\"name\":\"" + name + "\",";
        json += "\"type\":\"" + type + "\",";
        json += "\"location\":\"" + location + "\"}";

//        String json = "{\"name\":\"parking1\",\"type\":\"parking\",\"location\":\"[0,1]\"}";
        System.out.println(json);
        StringEntity entity = new StringEntity(json);
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");

        CloseableHttpResponse response = client.execute(httpPost);
        client.close();
    }
}
