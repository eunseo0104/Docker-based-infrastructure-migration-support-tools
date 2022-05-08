import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.ecr.AmazonECR;
import com.amazonaws.services.ecr.AmazonECRClientBuilder;
import com.amazonaws.services.ecr.model.ImageIdentifier;
import com.amazonaws.services.ecr.model.ListImagesRequest;
import com.amazonaws.services.ecr.model.ListImagesResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AwsMigrator {

    List<String> amiOsNameList = new ArrayList<>(Arrays.asList("amzn", "ubuntu","Windows_server"));

    public AmazonEC2 BuildEC2Client(String accessKey, String secretKey, Regions regions){
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);

        return AmazonEC2ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(regions)
                .build();
    }

    public void RunEc2Instance(AmazonEC2 amazonEC2Client) {
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

        runInstancesRequest.withImageId("ami-0eb7a369386789460")
                .withInstanceType(InstanceType.T2Micro)
                .withMinCount(1)
                .withMaxCount(1)
                .withKeyName("test_key")
                .withSecurityGroupIds("sg-0a0d3d63be3b062cc");

        RunInstancesResult runInstancesResult = amazonEC2Client.runInstances(runInstancesRequest);

        System.out.println(runInstancesResult);
    }

    public void RunEc2Instance(AmazonEC2 amazonEC2Client, AwsServerInfo awsServerInfo) {
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

        runInstancesRequest.withImageId(awsServerInfo.imageId)
                .withInstanceType(awsServerInfo.instanceType)
                .withMinCount(awsServerInfo.minCount)
                .withMaxCount(awsServerInfo.maxCount)
                .withKeyName(awsServerInfo.keyName)
                .withSecurityGroupIds(awsServerInfo.securityGroupId);

        RunInstancesResult runInstancesResult = amazonEC2Client.runInstances(runInstancesRequest);

        System.out.println(runInstancesResult);
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


    public List<Instance> getEC2InstanceDetails(AmazonEC2 amazonEC2, List<String> instanceIds){
        List<Instance> instanceList = new ArrayList<>();
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
        describeInstancesRequest.setInstanceIds(instanceIds);

        DescribeInstancesResult describeInstancesResult = amazonEC2.describeInstances(describeInstancesRequest);
        for(Reservation reservation : describeInstancesResult.getReservations()){
            for(Instance instance : reservation.getInstances()) {
                instanceList.add(instance);
            }
        }
        return instanceList;
    }

    public List<ServerInfo> convertEC2InstanceDetailsToServerInfo(AmazonEC2 amazonEC2, List<Instance> instanceList, Regions regions) {

        List<ServerInfo> serverInfoList = new ArrayList<>();

        for(Instance instance: instanceList) {

            // 객체 생성
            ServerInfo serverInfo = new ServerInfo();

            // 이미지 (DescribeImageRequest)
            DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest();
            describeImagesRequest.withImageIds(instance.getImageId());
            DescribeImagesResult describeImagesResult = amazonEC2.describeImages(describeImagesRequest);
            Image image = describeImagesResult.getImages().get(0);
            int lastIndexOfSlash = image.getName().lastIndexOf("/");
            String[] splitImageName = image.getName().substring(lastIndexOfSlash + 1).split("-");

            serverInfo.osArchitecture = image.getArchitecture();
            serverInfo.osName = splitImageName[0];
            switch (serverInfo.osName) {
                case "Windows_Server" -> {
                    serverInfo.osName = "Windows Server";
                    serverInfo.osVersion = splitImageName[1];
                }
                case "ubuntu" -> {
                    serverInfo.osName = "Ubuntu Server";
                    serverInfo.osVersion = splitImageName[2];
                }
                case "amzn", "amzn2" -> {
                    serverInfo.osName = "Amazon";
                    serverInfo.osVersion = splitImageName[3];
                }
            }

            // 인스턴스 타입 (DescribeInstanceTypesRequest)
            DescribeInstanceTypesRequest describeInstanceTypesRequest = new DescribeInstanceTypesRequest();
            describeInstanceTypesRequest.withInstanceTypes(instance.getInstanceType());
            DescribeInstanceTypesResult describeInstanceTypesResult = amazonEC2.describeInstanceTypes(describeInstanceTypesRequest);

            InstanceTypeInfo instanceTypeInfo = describeInstanceTypesResult.getInstanceTypes().get(0);
            serverInfo.vCpu = instanceTypeInfo.getVCpuInfo().getDefaultVCpus();
            serverInfo.memory = instanceTypeInfo.getMemoryInfo().getSizeInMiB() / 1024;

            switch (instanceTypeInfo.getInstanceType().split("\\.")[0]) {
                case "Mac", "T4g", "T3", "T3a", "t2", "M6g", "M6i", "M5", "M5a", "M5n", "M5zn", "M4", "A1" -> serverInfo.instanceType = "general";
                case "C6g", "C6gn", "C6i", "C5", "C5a", "C5n", "C4" -> serverInfo.instanceType = "computing";
                case "56g", "R5", "R5a", "R5b", "R5n", "R4", "X2gd", "X1e", "X1", "u", "z1d" -> serverInfo.instanceType = "memory";
                case "P4", "P3", "P2", "P1", "DL1", "Inf1", "G5", "G4dn", "G4ad", "G3", "F1", "VT1" -> serverInfo.instanceType = "intensive";
                case "I3", "I3en", "D2", "D3", "D3en", "H1" -> serverInfo.instanceType = "storage";
                default -> serverInfo.instanceType = "default";
            }

            // 리전
            serverInfo.region = regions.getDescription();

            // 서버 이름
            Tag tagName = instance.getTags().stream()
                    .filter(o -> o.getKey().equals("Name"))
                    .findFirst()
                    .orElse(new Tag("Name", "tag-not-found"));

            serverInfo.serverName = tagName.getValue();


            serverInfoList.add(serverInfo);

        }

        return serverInfoList;
    }

    // serverInfo -> AWS ServerInfo
    // 인스턴스 타입은 별도 클래스 호출
    public AwsServerInfo convertServerInfoToAWSServerInfo(AmazonEC2 amazonEC2, ServerInfo serverInfo) throws Exception {

        AwsServerInfo awsServerInfo = new AwsServerInfo();
        List<Filter> instanceFilters = new ArrayList<>();
        List<Filter> imageFilters = new ArrayList<>();

        awsServerInfo.instanceName = serverInfo.osName;

        serverInfo.vCpu = 2;
        serverInfo.memory= 4L;
        serverInfo.osName = "";

        Filter memoryFilter = new Filter().withName("memory-info.size-in-mib").withValues(String.valueOf(serverInfo.memory * 1024));
        Filter cpuFilter = new Filter().withName("vcpu-info.default-vcpus").withValues(String.valueOf(serverInfo.vCpu));

        instanceFilters.add(memoryFilter);
        instanceFilters.add(cpuFilter);

        // 인스턴스 타입 (DescribeInstanceTypesRequest)
        DescribeInstanceTypesRequest describeInstanceTypesRequest = new DescribeInstanceTypesRequest();
        describeInstanceTypesRequest.withFilters(instanceFilters);
        DescribeInstanceTypesResult describeInstanceTypesResult = amazonEC2.describeInstanceTypes(describeInstanceTypesRequest);

        List<String> instanceTypeList = new ArrayList<>();
        for(InstanceTypeInfo instanceTypeInfo: describeInstanceTypesResult.getInstanceTypes()){
            instanceTypeList.add(instanceTypeInfo.getInstanceType());
        }

        // 사용자에게 출력
        System.out.println(instanceTypeList);
        // 사용자로부터 입력
        awsServerInfo.instanceType = "T2.micro";



        // ---------- Image

        Filter osFilter = new Filter().withName("platform").withValues("windows");
        Filter architectureFilter = new Filter().withName("architecture").withValues("x86_64");
        Filter imageTypeFilter = new Filter().withName("image-type").withValues("machine");
        Filter ownerFilter = new Filter().withName("owner-alias").withValues("amazon");

        String filterOsName = "";
        String filterOsVersion = "";

        switch (serverInfo.osName) {
            case "Windows Server" -> filterOsName = "Windows_Server-";
            case "Ubuntu Server" -> filterOsName = "Ubuntu-";
            case "Centos" -> filterOsName = "centos";
        }
        Filter nameFilter = new Filter().withName("name").withValues(filterOsName+serverInfo.osVersion+"*");

        //imageFilters.add(osFilter);
        imageFilters.add(architectureFilter);
        imageFilters.add(imageTypeFilter);
        imageFilters.add(ownerFilter);
        imageFilters.add(nameFilter);

        DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest();
        describeImagesRequest.withFilters(imageFilters);
        DescribeImagesResult describeImagesResult = amazonEC2.describeImages(describeImagesRequest);

        List<String> imageList = new ArrayList<>();
        for(Image image: describeImagesResult.getImages()){
            imageList.add(image.getName());
        }

        // 사용자에게 출력
//        System.out.println(imageList);

        // 사용자로부터 입력
        awsServerInfo.imageId = "ami-0eb7a369386789460";


        //---------max count
        awsServerInfo.maxCount = 1;
        awsServerInfo.minCount = 1;

        //-------- Keypair
        DescribeKeyPairsRequest describeKeyPairsRequest = new DescribeKeyPairsRequest();
        DescribeKeyPairsResult describeKeyPairsResult = amazonEC2.describeKeyPairs(describeKeyPairsRequest);

        List<String> keyNameList = new ArrayList<>();
        for(KeyPairInfo keyPairInfo: describeKeyPairsResult.getKeyPairs()){
            keyNameList.add(keyPairInfo.getKeyName());
        }

        // 사용자에게 출력
        System.out.println(keyNameList);

        // 사용자로부터 입력
        awsServerInfo.keyName = "test_key";


        //--------- SecurityGroups
        DescribeSecurityGroupsRequest describeSecurityGroupsRequest = new DescribeSecurityGroupsRequest();
        DescribeSecurityGroupsResult describeSecurityGroupsResult = amazonEC2.describeSecurityGroups(describeSecurityGroupsRequest);

        List<String> securityGroupIdList = new ArrayList<>();
        for(SecurityGroup securityGroup: describeSecurityGroupsResult.getSecurityGroups()){
            securityGroupIdList.add(securityGroup.getGroupId());
        }

        // 사용자에게 출력
        System.out.println(securityGroupIdList);

        // 사용자로부터 입력
        awsServerInfo.securityGroupId = "sg-0a0d3d63be3b062cc";

        return awsServerInfo;
    }

    // 체크용
    // 인스턴스 목록 반환
    public Boolean checkGetEC2InstanceDetails(AmazonEC2 amazonEC2) throws Exception{
        List<Instance> instanceList = new ArrayList<>();
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();

        try {
            DescribeInstancesResult describeInstancesResult = amazonEC2.describeInstances(describeInstancesRequest);
            for(Reservation reservation : describeInstancesResult.getReservations()){
                instanceList.addAll(reservation.getInstances());
            }
        } catch(AmazonEC2Exception e) {
            return false;
        }

        return true;

    }


    public AmazonECR BuildECRClient(String accessKey, String secretKey, Regions regions){
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);


        return AmazonECRClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(regions)
                .build();
    }


    public List<ImageIdentifier> getlistImageResult(AmazonECR amazonECR) throws Exception{
        List<Image> imageList = new ArrayList<>();
        ListImagesRequest listImagesRequest = new ListImagesRequest();
        listImagesRequest.withRepositoryName("A");
        ListImagesResult listImagesResult = amazonECR.listImages(listImagesRequest);
        System.out.println(listImagesResult.getImageIds());
        return listImagesResult.getImageIds();
    }
}
