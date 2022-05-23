package docker;

public class KeyPair {
    private String accessKey;
    private String secretKey;

    KeyPair(String x, String y) {
        this.accessKey = x;
        this.secretKey = y;
    }

    public String getAccessKey(){
        return accessKey;
    }

    public String getSecretKey(){
        return secretKey;
    }
}