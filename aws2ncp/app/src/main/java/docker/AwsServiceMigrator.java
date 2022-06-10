package docker;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.eks.AmazonEKS;
import com.amazonaws.services.eks.AmazonEKSClient;
import com.amazonaws.services.eks.AmazonEKSClientBuilder;
import com.amazonaws.services.eks.model.CreateClusterRequest;
import com.amazonaws.services.eks.model.CreateClusterResult;
import com.amazonaws.services.eks.model.VpcConfigRequest;
import com.amazonaws.services.ecs.model.*;
import com.amazonaws.services.eks.model.ListClustersRequest;
import com.amazonaws.services.eks.model.ListClustersResult;

import java.util.*;

public class AwsServiceMigrator {

    private String roleArn = "";
    private String AwsRegion = "";

    public void ks_a2n(String aws_accessKey, String aws_secretKey, String ncp_accessKey, String ncp_secretKey, String clusterName, String region) throws Exception{
	AwsRegion = region;//.toLowerCase().replace("_", "-");
	System.out.println(AwsRegion);
        String clusterList = listEKSCluster(aws_accessKey, aws_secretKey);
        NcpServiceMigrator ncpServiceMigrator = new NcpServiceMigrator();
        
        
        
        if (clusterList.contains(clusterName))
            ncpServiceMigrator.createCluster(ncp_accessKey, ncp_secretKey, clusterName);
    }

    public void cs_a2n(String aws_accessKey, String aws_secretKey, String ncp_accessKey, String ncp_secretKey, String clusterName) throws Exception{

        String clusterList = listECSCluster(aws_accessKey, aws_secretKey);
        NcpServiceMigrator ncpServiceMigrator = new NcpServiceMigrator();

        if (clusterList.contains(clusterName))
            ncpServiceMigrator.createCluster(ncp_accessKey, ncp_secretKey, clusterName);
    }

    public AmazonEKS BuildEKSClient(String accessKey, String secretKey){
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);

        return AmazonEKSClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(Regions.valueOf(AwsRegion))
                .build();
    }

    public AmazonECS BuildECSClient(String accessKey, String secretKey){

        AwsServiceMigrator awsServiceMigrator = new AwsServiceMigrator();
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);

        return AmazonECSClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(Regions.valueOf(AwsRegion))
                .build();
    }

    public void createEKSCluster(String accessKey, String secretKey, String[] subnetID, String clusterName){
	AwsServiceMigrator awsServiceMigrator = new AwsServiceMigrator();
        AmazonEKS amazonEKS = awsServiceMigrator.BuildEKSClient(accessKey, secretKey);
        VpcConfigRequest vpcConfigRequest = new VpcConfigRequest();
        vpcConfigRequest.withSubnetIds(subnetID);

	System.out.println("Enter RoleArn(ex arn:aws:iam::000000000000:role/eksClusterRole): ");
	Scanner scanner = new Scanner(System.in);
	roleArn = scanner.nextLine();
        CreateClusterRequest request = new CreateClusterRequest();
        request.withName(clusterName)
                .withRoleArn(roleArn)
                .withResourcesVpcConfig(vpcConfigRequest);

        CreateClusterResult createClusterResult = amazonEKS.createCluster(request);
        System.out.println(createClusterResult);

    }



    public String listECSCluster(String accessKey, String secretKey){

        AwsServiceMigrator awsServiceMigrator = new AwsServiceMigrator();
        AmazonECS amazonECS = awsServiceMigrator.BuildECSClient(accessKey, secretKey);

        com.amazonaws.services.ecs.model.ListClustersRequest request = new com.amazonaws.services.ecs.model.ListClustersRequest();
        com.amazonaws.services.ecs.model.ListClustersResult listClustersResult = amazonECS.listClusters(request);

        String message = new StringBuilder()
                .append(listClustersResult)
                .toString();

        return message;
    }


    public String listEKSCluster(String accessKey, String secretKey){

        AwsServiceMigrator awsServiceMigrator = new AwsServiceMigrator();
        AmazonEKS amazonEKS = awsServiceMigrator.BuildEKSClient(accessKey, secretKey);

        ListClustersRequest request = new ListClustersRequest();
        ListClustersResult listClustersResult = amazonEKS.listClusters(request);

        String message = new StringBuilder()
                .append(listClustersResult)
                .toString();

        return message;
    }



}

