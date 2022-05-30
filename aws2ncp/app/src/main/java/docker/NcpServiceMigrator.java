
package docker;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


// 네이버 api
public class NcpServiceMigrator {

    //ncr.apigw.ntruss.com/ncr/api
    private static String nksApiUrl = "https://nks.apigw.ntruss.com";


    public void ks_n2a(String ncp_accessKey, String ncp_secretKey, String aws_accessKey, String aws_secretKey, String clusterName) throws Exception {

        String clusterList = getClusterList(ncp_accessKey, ncp_secretKey);
        AwsServiceMigrator awsServiceMigrator = new AwsServiceMigrator();

        String[] subnetID = new String[2];
        subnetID[0] = "subnet-4068210d";
        subnetID[1] = "subnet-acaa10f3";

        if(clusterList.contains(clusterName))
            awsServiceMigrator.createEKSCluster(aws_accessKey, aws_secretKey, subnetID, clusterName);

    }

    //cluster 목록 조회
    public String getClusterList(String accessKey, String secretKey) throws Exception {

        String uri = "/vnks/v2/clusters";

        String response = clientApiCall(accessKey, secretKey, uri);
        return response;
    }

    //cluster 상세 조회
    public String getCluster(String accessKey, String secretKey) throws Exception {

        String uri = "/vnks/v2/clusters/4427138a-4e32-4fe7-9ce7-18d8943e4a59";

        String response = clientApiCall(accessKey, secretKey, uri);
        return response;
    }

    //cluster 생성
    public void createCluster(String accessKey, String secretKey, String clusterName) throws Exception{

        String uri = "/vnks/v2/clusters";


        String jsonInputString = "{\n" +
                "  \"name\": \""+ clusterName + "\",\n" +
                "  \"clusterType\": \"SVR.VNKS.STAND.C002.M008.NET.SSD.B050.G002\",\n" +
                "  \"loginKeyName\": \"egg\",\n" +
                "  \"regionCode\": \"KR\",\n" +
                "  \"vpcNo\": 18338,\n" +
                "  \"subnetNoList\": [\n" +
                "    41755\n" +
                "  ],\n" +
                "  \"subnetLbNo\": 41754,\n" +
                "  \"zoneCode\": \"KR-1\",\n" +
                "  \"zoneNo\": 2\n" +
                "}" ;

        String response = clientApiPostCall(accessKey, secretKey, uri, jsonInputString);
    }


    public String clientApiCall(String accessKey, String secretKey, String uri) {

        String timestamp = Long.toString(Instant.now().toEpochMilli());
        String body = "";

        try {
            HttpClient client = HttpClientBuilder.create().build(); // HttpClient 생성
            HttpGet getRequest = new HttpGet(nksApiUrl+uri); //GET 메소드 URL 생성
            getRequest.addHeader("x-ncp-apigw-timestamp", timestamp);
            getRequest.addHeader("x-ncp-iam-access-key", accessKey);
            getRequest.addHeader("x-ncp-apigw-signature-v2", makeSignature(accessKey, secretKey, timestamp, "GET", uri));


            HttpResponse response = client.execute(getRequest);

            //Response 출력
            if (response.getStatusLine().getStatusCode() == 200) {
                ResponseHandler<String> handler = new BasicResponseHandler();
                body = handler.handleResponse(response);
                System.out.println(body);
            } else {
                System.out.println("response is error : " + response.getStatusLine().getStatusCode());
            }

        } catch (Exception e){
            System.err.println(e.toString());
        }

        return body;
    }



    public String clientApiPostCall(String accessKey, String secretKey, String uri, String jsonMessage) {

        String timestamp = Long.toString(Instant.now().toEpochMilli());
        String body = "";

        try {
            HttpClient client = HttpClientBuilder.create().build(); // HttpClient 생성
            HttpPost postRequest = new HttpPost(nksApiUrl+uri); //GET 메소드 URL 생성
            postRequest.addHeader("x-ncp-apigw-timestamp", timestamp);
            postRequest.addHeader("x-ncp-iam-access-key", accessKey);
            postRequest.addHeader("x-ncp-apigw-signature-v2", makeSignature(accessKey, secretKey, timestamp, "POST", uri));
            postRequest.setHeader("Accept", "application/json");
            postRequest.setHeader("Connection", "keep-alive");
            postRequest.setHeader("Content-Type", "application/json");

            postRequest.setEntity(new StringEntity(jsonMessage));

            HttpResponse response = client.execute(postRequest);

            //Response 출력
            if (response.getStatusLine().getStatusCode() == 200) {
                ResponseHandler<String> handler = new BasicResponseHandler();
                body = handler.handleResponse(response);
                System.out.println(body);
            } else {
                System.out.println("response is error : " + response.getStatusLine().getStatusCode());
            }

        } catch (Exception e){
            System.err.println(e.toString());
        }

        return body;
    }


    // API 호출을 위한 Signature 생성
    public static String makeSignature(String accessKey, String secretKey, String timestamp, String method, String url) throws Exception {

        String space = " ";					// one space
        String newLine = "\n";					// new line


        String message = new StringBuilder()
                .append(method)
                .append(space)
                .append(url)
                .append(newLine)
                .append(timestamp)
                .append(newLine)
                .append(accessKey)
                .toString();

        SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(signingKey);

        byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        String encodeBase64String = Base64.encodeBase64String(rawHmac);

        return encodeBase64String;
    }


}

