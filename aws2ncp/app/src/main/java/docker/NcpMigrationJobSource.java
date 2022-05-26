package docker;

public class NcpMigrationJobSource {
    private String bucketName;
    private String sourceCspType;
    private String accessKey;
    private String secretKey;
    private String regionName;
    private String prefix;
    NcpMigrationJobSource(String bucketName, String sourceCspType, String accessKey, String secretKey, String regionName, String prefix) {
        this.accessKey = accessKey;
        this.sourceCspType = sourceCspType;
        this.bucketName = bucketName;
        this.secretKey = secretKey;
        this.regionName = regionName;
        this.prefix = prefix;
    }

    public String getBucketName(){
        return bucketName;
    }
    public String getSourceCspType(){
        return sourceCspType;
    }
    public String getAccessKey(){
        return accessKey;
    }
    public String getSecretKey(){
        return secretKey;
    }
    public String getRegionName(){
        return regionName;
    }
    public String getPrefix(){
        return prefix;
    }


}