package docker;
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
import java.util.*;

public class AwsMigrator {

    List<String> amiOsNameList = new ArrayList<>(Arrays.asList("amzn", "ubuntu","Windows_server"));
    Scanner scanner = new Scanner(System.in);
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
                case "Windows_Server":
                    serverInfo.osName = "Windows Server";
                    serverInfo.osVersion = splitImageName[1];
                    break;
                case "ubuntu":
                    serverInfo.osName = "Ubuntu Server";
                    serverInfo.osVersion = splitImageName[2];
                    break;
                case "amzn":
                case "amaz2":
                    serverInfo.osName = "Amazon";
                    serverInfo.osVersion = splitImageName[3];
                    break;
            }

            // 인스턴스 타입 (DescribeInstanceTypesRequest)
            DescribeInstanceTypesRequest describeInstanceTypesRequest = new DescribeInstanceTypesRequest();
            describeInstanceTypesRequest.withInstanceTypes(instance.getInstanceType());
            DescribeInstanceTypesResult describeInstanceTypesResult = amazonEC2.describeInstanceTypes(describeInstanceTypesRequest);

            InstanceTypeInfo instanceTypeInfo = describeInstanceTypesResult.getInstanceTypes().get(0);
            serverInfo.vCpu = instanceTypeInfo.getVCpuInfo().getDefaultVCpus();
            serverInfo.memory = instanceTypeInfo.getMemoryInfo().getSizeInMiB() / 1024;

            switch (instanceTypeInfo.getInstanceType().split("\\.")[0]) {
                case "Mac":
                case "T4g":
                case "T3":
                case "T3a":
                case "t2":
                case "M6g":
                case "M6i":
                case "M5":
                case "M5a":
                case "M5n":
                case "M5zn":
                case "M4":
                case "A1":
                    serverInfo.instanceType = "general";
                    break;
                case "C6g":
                case "C6gn":
                case "C6i":
                case "C5":
                case "C5a":
                case "C5n":
                case "C4":
                    serverInfo.instanceType = "computing";
                    break;
                case "56g":
                case "R5":
                case "R5a":
                case "R5b":
                case "R5n":
                case "R4":
                case "X2gd":
                case "X1e":
                case "X1":
                case "u":
                case "z1d":
                    serverInfo.instanceType = "memory";
                    break;
                case "P4":
                case "P3":
                case "P2":
                case "P1":
                case "DL1":
                case "Inf1":
                case "G5":
                case "G4dn":
                case "G4ad":
                case "G3":
                case "F1":
                case "VT1":
                    serverInfo.instanceType = "intensive";
                    break;
                case "I3":
                case "I3en":
                case "D2":
                case "D3":
                case "D3en":
                case "H1":
                    serverInfo.instanceType = "storage";
                    break;
                default:
                    serverInfo.instanceType = "default";
                    break;
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
        System.out.println("Select Instance Type: ");
        awsServerInfo.instanceType = scanner.nextLine(); //"T2.micro";



        // ---------- Image

        Filter osFilter = new Filter().withName("platform").withValues("windows");
        Filter architectureFilter = new Filter().withName("architecture").withValues("x86_64");
        Filter imageTypeFilter = new Filter().withName("image-type").withValues("machine");
        Filter ownerFilter = new Filter().withName("owner-alias").withValues("amazon");

        String filterOsName = "";
        String filterOsVersion = "";

        switch (serverInfo.osName) {
            case "Windows Server":
                filterOsName = "Windows_Server-";
                break;
            case "Ubuntu Server":
                filterOsName = "Ubuntu-";
                break;
            case "Centos":
                filterOsName = "centos";
                break;
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
	System.out.println("Input Image List(ami-00000000000000000): ");
        // 사용자로부터 입력
        awsServerInfo.imageId = scanner.nextLine(); //"ami-0eb7a369386789460";


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
        System.out.println("Key List");

        System.out.println("-------------------");
        for(int i=0; i<keyNameList.size(); i++) {
            System.out.printf("|      %-11s |\n", i + " : " + keyNameList.get(i));
        }
        System.out.println("-------------------");

        // Key Number 입력
        while (true) {
            try {
                System.out.println("Key name number : ");
                int index = Integer.parseInt(scanner.nextLine());

                awsServerInfo.keyName= keyNameList.get(index);
                break;
                // 유효하지 않은 입력이라면
            } catch (Exception e) {
                System.out.println("유효하지 않은 입력입니다. 다시 입력해주세요.");
            }
        }


        //--------- SecurityGroups
        DescribeSecurityGroupsRequest describeSecurityGroupsRequest = new DescribeSecurityGroupsRequest();
        DescribeSecurityGroupsResult describeSecurityGroupsResult = amazonEC2.describeSecurityGroups(describeSecurityGroupsRequest);

        List<String> securityGroupIdList = new ArrayList<>();
        for(SecurityGroup securityGroup: describeSecurityGroupsResult.getSecurityGroups()){
            securityGroupIdList.add(securityGroup.getGroupId());
        }

        // 사용자에게 출력
        System.out.println("Security Group Id List");

        System.out.println("-------------------------------------");
        for(int i=0; i<securityGroupIdList.size(); i++) {
            System.out.printf("|      %-30s |\n", i + " : " + securityGroupIdList.get(i));
        }
        System.out.println("-------------------------------------");


        // Security Group Number 입력
        while (true) {
            try {
                System.out.println("Security group number : ");
                int index = Integer.parseInt(scanner.nextLine());

                awsServerInfo.securityGroupId= securityGroupIdList.get(index);
                break;
                // 유효하지 않은 입력이라면
            } catch (Exception e) {
                System.out.println("유효하지 않은 입력입니다. 다시 입력해주세요.");
            }
        }

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
            System.out.println(e.getMessage());
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
