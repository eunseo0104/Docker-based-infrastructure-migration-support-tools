package docker;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;

import java.time.Instant;
import java.util.HashMap;
import java.util.Scanner;

import static docker.NcpMigrator.makeSignature;

public class NcpObjectStorageMigrator {

    static Scanner scanner = new Scanner(System.in);
    static AwsMigrator awsMigrator = new AwsMigrator();
    static NcpMigrator ncpMigrator = new NcpMigrator();
    private static String objectMigrationApiUrl = "https://objectmigration.apigw.ntruss.com";

    public String createMigrationJob(String accessKey, String secretKey, NcpMigrationJob ncpMigrationJob) {

        String uri = "/migration-api/v1/policy";

        HashMap<String,String> hashMap = new HashMap<>();
        HashMap<String,String> sourceHashMap = new HashMap<>();
        HashMap<String,String> targetHashMap = new HashMap<>();
        NcpMigrationJobSource ncpMigrationJobSource = ncpMigrationJob.getSource();
        NcpMigrationJobTarget ncpMigrationJobTarget = ncpMigrationJob.getTarget();

        sourceHashMap.put("bucketName", ncpMigrationJobSource.getBucketName());
        sourceHashMap.put("sourceCspType", ncpMigrationJobSource.getSourceCspType());
        sourceHashMap.put("accessKey", ncpMigrationJobSource.getAccessKey());
        sourceHashMap.put("secretKey", ncpMigrationJobSource.getSecretKey());
        sourceHashMap.put("regionName", ncpMigrationJobSource.getRegionName());
        sourceHashMap.put("prefix", ncpMigrationJobSource.getPrefix());
        JSONObject sourceJsonObject = new JSONObject(sourceHashMap);

        targetHashMap.put("bucketName", ncpMigrationJobTarget.getBucketName());
        targetHashMap.put("prefix", ncpMigrationJobTarget.getPrefix());
        JSONObject targetJsonObject = new JSONObject(targetHashMap);

        hashMap.put("title", ncpMigrationJob.getTitle());
        hashMap.put("source", sourceJsonObject.toJSONString());
        hashMap.put("target", targetJsonObject.toJSONString());
        JSONObject migrationJsonObject = new JSONObject(hashMap);

        String response = clientApiPostCall(accessKey, secretKey, uri, migrationJsonObject);
        System.out.println(response);
        return ncpMigrationJob.getTitle();
    }


    public String startMigrationJob(String accessKey, String secretKey, NcpMigrationJob ncpMigrationJob) {

        String title = ncpMigrationJob.getTitle();
        String uri = "/migration-api/v1/policy/" + title + "/start";

        String response = clientApiPostCall(accessKey, secretKey, uri);
        return ncpMigrationJob.getTitle();
    }

    public String clientApiPostCall(String accessKey, String secretKey, String uri) {

        String timestamp = Long.toString(Instant.now().toEpochMilli());
        String body = "";

        try {
            HttpClient client = HttpClientBuilder.create().build(); // HttpClient 생성
            HttpPost postRequest = new HttpPost(objectMigrationApiUrl+uri); //Post 메소드 URL 생성
            postRequest.addHeader("x-ncp-apigw-timestamp", timestamp);
            postRequest.addHeader("x-ncp-iam-access-key", accessKey);
            postRequest.addHeader("x-ncp-apigw-signature-v2", makeSignature(accessKey, secretKey, timestamp, "GET", uri));

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

    public String clientApiPostCall(String accessKey, String secretKey, String uri, JSONObject jsonObject) {

        String timestamp = Long.toString(Instant.now().toEpochMilli());
        String body = "";

        try {
            HttpClient client = HttpClientBuilder.create().build(); // HttpClient 생성
            HttpPost postRequest = new HttpPost(objectMigrationApiUrl+uri); //Post 메소드 URL 생성
            postRequest.addHeader("x-ncp-apigw-timestamp", timestamp);
            postRequest.addHeader("x-ncp-iam-access-key", accessKey);
            postRequest.addHeader("x-ncp-apigw-signature-v2", makeSignature(accessKey, secretKey, timestamp, "GET", uri));

            postRequest.setEntity((HttpEntity) jsonObject);

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

}
