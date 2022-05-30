package docker;

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

import static docker.NcpMigrator.makeSignature;

public class NcpObjectStorageMigrator {

    private static String objectMigrationApiUrl = "https://objectmigration.apigw.ntruss.com";

    public String createMigrationJob(String accessKey, String secretKey, NcpMigrationJob ncpMigrationJob, String region) {

        String uri = "/migration-api/v1/policy";

        NcpMigrationJobSource ncpMigrationJobSource = ncpMigrationJob.getSource();
        NcpMigrationJobTarget ncpMigrationJobTarget = ncpMigrationJob.getTarget();

        String jsonInputString = "{\n" +
                "  \"title\": \""+ ncpMigrationJob.getTitle() + "\",\n" +
                "  \"source\": {\n" +
                "  \"bucketName\": \""+ ncpMigrationJobSource.getBucketName() + "\",\n" +
                "  \"sourceCspType\": \""+ ncpMigrationJobSource.getSourceCspType() + "\",\n" +
                "  \"accessKey\": \""+ ncpMigrationJobSource.getAccessKey() + "\",\n" +
                "  \"secretKey\": \""+ ncpMigrationJobSource.getSecretKey() + "\",\n" +
                "  \"regionName\": \""+ ncpMigrationJobSource.getRegionName() + "\"\n" +
                "  },\n" +
                "  \"target\": {\n" +
                "  \"bucketName\": \""+ ncpMigrationJobTarget.getBucketName() + "\"\n" +
                "  }\n" +
                "}" ;

        String response = clientApiPostCall(accessKey, secretKey, uri, jsonInputString, region);
        return ncpMigrationJob.getTitle();
    }


    public void startMigrationJob(String accessKey, String secretKey, NcpMigrationJob ncpMigrationJob, String region) {

        String title = ncpMigrationJob.getTitle();
        String uri = "/migration-api/v1/policy/" + title + "/start";

        String response = clientApiPostCall(accessKey, secretKey, uri, region);
    }

    public String clientApiPostCall(String accessKey, String secretKey, String uri, String region) {

        String timestamp = Long.toString(Instant.now().toEpochMilli());
        String body = "";

        try {
            HttpClient client = HttpClientBuilder.create().build(); // HttpClient 생성
            HttpPost postRequest = new HttpPost(objectMigrationApiUrl+uri); //Post 메소드 URL 생성
            postRequest.addHeader("x-ncp-apigw-timestamp", timestamp);
            postRequest.addHeader("x-ncp-iam-access-key", accessKey);
            postRequest.addHeader("x-ncp-apigw-signature-v2", makeSignature(accessKey, secretKey, timestamp, "POST", uri));
            postRequest.addHeader("x-ncp-region_code", region);


            HttpResponse response = client.execute(postRequest);

            //Response 출력
            if (response.getStatusLine().getStatusCode() == 200) {
                ResponseHandler<String> handler = new BasicResponseHandler();
                body = handler.handleResponse(response);
            } else {
                System.out.println("response is error : " + response.getStatusLine().getStatusCode());
            }

        } catch (Exception e){
            System.err.println(e.toString());
        }

        return body;
    }

    public String clientApiPostCall(String accessKey, String secretKey, String uri, String jsonInputString, String region) {

        String timestamp = Long.toString(Instant.now().toEpochMilli());
        String body = "";

        try {
            HttpClient client = HttpClientBuilder.create().build(); // HttpClient 생성
            HttpPost postRequest = new HttpPost(objectMigrationApiUrl+uri); //Post 메소드 URL 생성
            postRequest.addHeader("x-ncp-apigw-timestamp", timestamp);
            postRequest.addHeader("x-ncp-iam-access-key", accessKey);
            postRequest.addHeader("x-ncp-apigw-signature-v2", makeSignature(accessKey, secretKey, timestamp, "POST", uri));
            postRequest.addHeader("x-ncp-region_code", region);

            System.out.println(timestamp);
            System.out.println(makeSignature(accessKey, secretKey, timestamp, "POST", uri));
            System.out.println(jsonInputString);

            postRequest.setEntity(new StringEntity(jsonInputString));

            HttpResponse response = client.execute(postRequest);

            //Response 출력
            if (response.getStatusLine().getStatusCode() == 200) {
                ResponseHandler<String> handler = new BasicResponseHandler();
                body = handler.handleResponse(response);
            } else {
                System.out.println("response is error : " + response.getStatusLine().getStatusCode());
                System.out.println(response);
            }

        } catch (Exception e){
            System.err.println(e.toString());
        }

        return body;
    }

}
