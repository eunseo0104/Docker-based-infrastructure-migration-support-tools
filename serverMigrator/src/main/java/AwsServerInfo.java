import com.amazonaws.services.ec2.model.*;

public class AwsServerInfo {

    String instanceName;
    String imageId;

    String instanceType;

    String keyName;
    String securityGroupId;

    int maxCount;
    int minCount;
}
