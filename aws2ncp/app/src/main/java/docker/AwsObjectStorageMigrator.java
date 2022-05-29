package docker;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;

import java.util.ArrayList;
import java.util.List;

public class AwsObjectStorageMigrator {

    public AmazonS3 BuildS3Client(String accessKey, String secretKey, Regions regions){
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);

        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(regions)
                .build();
    }

    public List<Bucket> getBucketList(AmazonS3 amazonS3) throws Exception {
        List<Bucket> buckets = amazonS3.listBuckets();
        for (Bucket b : buckets) {
            System.out.println("* " + b.getName());
        }
        return buckets;
    }

    // 인스턴스 목록 반환
    public List<Instance> getEC2InstanceDetails(AmazonEC2 amazonEC2) throws Exception{
        List<Instance> instanceList = new ArrayList<>();
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();

        DescribeInstancesResult describeInstancesResult = amazonEC2.describeInstances(describeInstancesRequest);
        for(Reservation reservation : describeInstancesResult.getReservations()){
            instanceList.addAll(reservation.getInstances());
        }
        return instanceList;
    }
}
