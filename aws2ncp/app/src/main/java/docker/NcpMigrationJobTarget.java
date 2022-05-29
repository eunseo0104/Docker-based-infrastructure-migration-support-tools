package docker;

public class NcpMigrationJobTarget {
    private String bucketName;
    private String prefix;

    NcpMigrationJobTarget(String bucketName, String prefix) {
        this.bucketName = bucketName;
        this.prefix = prefix;
    }
    public String getBucketName(){
        return bucketName;
    }
    public String getPrefix(){
        return prefix;
    }
}
