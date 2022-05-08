import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.eks.AmazonEKS;
import com.amazonaws.services.eks.model.CreateClusterResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class Migrator {

    public boolean migrate(String basedPlatformName, String targetPlatformName) throws Exception {

        if (Objects.equals(basedPlatformName, "AWS")) {
            if (Objects.equals(targetPlatformName, "NCP")) {
                awsToNcp();
                return true;
            } else {
                return false;
            }
        }

        if (Objects.equals(basedPlatformName, "NCP")) {
            if (Objects.equals(targetPlatformName, "AWS")) {
                ncpToAws();
                return true;
            } else {
                return false;
            }

        }
        return false;
    }

    // aws to ncp 변환
    public void awsToNcp() throws Exception {

        boolean flag = false;
        boolean flag2 = false;

        Scanner scanner = new Scanner(System.in);
        AwsMigrator awsMigrator = new AwsMigrator();
        NcpMigrator ncpMigrator = new NcpMigrator();
        String ncpAccessKey = null;
        String ncpSecretKey = null;


        // 1. 사용자로부터 서버가 있는 region을 입력 받음
        System.out.println("-----------------------------");
        Regions[] regions = Regions.values();

        for (Regions r : regions) {
            System.out.printf("|        %-18s |\n", r.name());
        }
        System.out.println("-----------------------------");

        System.out.println("Enter Region : ");
        String region = scanner.nextLine();
        System.out.println(region);


        // 2. 사용자로부터 awsAccessKey, awsSecretKey 입력 받음
        AmazonEC2 amazonEC2Client = null;
        while (!flag) {
            flag = true;

            System.out.println("Enter AWS Access Key : ");
            String accessKey = scanner.nextLine();
            System.out.println(accessKey);

            System.out.println("Enter AWS Secret Key : ");
            String secretKey = scanner.nextLine();
            System.out.println(secretKey);

            // 입력 받은 정보로 ec2 client 생성
            amazonEC2Client = awsMigrator.BuildEC2Client(accessKey, secretKey, Regions.valueOf(region));

            // 자격 증명에 오류가 있는지 검사
            if (!awsMigrator.checkGetEC2InstanceDetails(amazonEC2Client)) {
                System.out.println("Invalid Key!");
                flag = false;
            }
        }

        // 3. 입력 받은 계정 정보로 해당 region의 모든 Instance 정보를 가져옴
        List<Instance> instanceList = awsMigrator.getEC2InstanceDetails(amazonEC2Client);
        System.out.println(instanceList);

        // 4. 사용자에게 Instance 목록을 보여주고 이전할 Instance id를 입력 받음
        System.out.println("-----------------------------");
        for (Instance i : instanceList) {
            System.out.printf("|        %-18s |\n", i.getInstanceId());
        }
        System.out.println("-----------------------------");

        System.out.println("Enter Instance Id : ");
        String instanceId = scanner.nextLine();
        System.out.println(instanceId);

        List<String> instanceIds = new ArrayList<>();
        instanceIds.add(instanceId);


        // 5.target key 입력
        while(!flag2) {
            flag2 = true;

            System.out.println("Enter NCP Access Key : ");
            ncpAccessKey = scanner.nextLine();
            System.out.println(ncpAccessKey);

            System.out.println("Enter NCP Secret Key : ");
            ncpSecretKey = scanner.nextLine();
            System.out.println(ncpSecretKey);

            // 5-1. 자격 증명에 오류가 있는지 검사
            if(!ncpMigrator.checkGetVPCList(ncpAccessKey, ncpSecretKey, "")) {
                System.out.println("Invalid Key!");
                flag2 = false;
            }
        }


        //6. 해당 Instance 서버 정보를 호출
        List<Instance> selectedInstanceList = awsMigrator.getEC2InstanceDetails(amazonEC2Client, instanceIds);


        //7. 호출한 서버 정보를 공통 서버 정보로 변환
        List<ServerInfo> serverInfoList = awsMigrator.convertEC2InstanceDetailsToServerInfo(amazonEC2Client, selectedInstanceList, Regions.AP_NORTHEAST_2);


        //8. 공통 서버 정보를 이전할 서버 정보로 변환
        List<NcpServerInfo> ncpServerInfoList = new ArrayList<>();

        for(ServerInfo serverInfo: serverInfoList) {
            NcpServerInfo ncpServerInfo = ncpMigrator.convertServerInfoToNCPServerInfo(ncpAccessKey, ncpSecretKey, serverInfo);
            ncpServerInfoList.add(ncpServerInfo);
        }

        //9. 서버 실행 및 완료 메시지 출력
        for(NcpServerInfo ncpServerInfo: ncpServerInfoList) {
            ncpMigrator.runVPCInstance(ncpAccessKey, ncpSecretKey, ncpServerInfo);

            System.out.println(ncpServerInfo.serverName + "Migration completed");
        }
    }

    // ncp to aws 변환
    public void ncpToAws() throws Exception {

        boolean flag = false;
        boolean flag2 = false;

        Scanner scanner = new Scanner(System.in);
        AwsMigrator awsMigrator = new AwsMigrator();
        NcpMigrator ncpMigrator = new NcpMigrator();
        String ncpAccessKey = null;
        String ncpSecretKey = null;

        //1. 사용자로부터 ncpAccessKey, ncpSecretKey 입력 받음
        while(!flag) {
            flag2 = true;

            System.out.println("Enter NCP Access Key : ");
            ncpAccessKey = scanner.nextLine();
            System.out.println(ncpAccessKey);

            System.out.println("Enter NCP Secret Key : ");
            ncpSecretKey = scanner.nextLine();
            System.out.println(ncpSecretKey);

            // 5-1. 자격 증명에 오류가 있는지 검사
            if(!ncpMigrator.checkGetVPCList(ncpAccessKey, ncpSecretKey, "")) {
                System.out.println("Invalid Key!");
                flag = false;
            }
        }

        // 2. 입력 받은 계정 정보로 모든 InstanceNo 를 가져옴
        List<String> serverInstanceNoList = ncpMigrator.getServerInstanceNoList(ncpAccessKey, ncpSecretKey);


        // 3. 사용자에게 InstanceNo 목록을 보여주고 이전할 Instance no를 입력 받음
        System.out.println("-----------------------------");
        for (String serverInstanceNo : serverInstanceNoList) {
            System.out.printf("|        %-18s |\n", serverInstanceNo);
        }
        System.out.println("-----------------------------");


        System.out.println("Enter Instance No : ");
        String instanceNo = scanner.nextLine();
        System.out.println(instanceNo);

        List<String> instanceNos = new ArrayList<>();
        instanceNos.add(instanceNo);


        // 4. 사용자로부터 서버를 생성할 region을 입력 받음
        System.out.println("-----------------------------");
        Regions[] regions = Regions.values();

        for (Regions r : regions) {
            System.out.printf("|        %-18s |\n", r.name());
        }
        System.out.println("-----------------------------");

        System.out.println("Enter Region : ");
        String region = scanner.nextLine();
        System.out.println(region);


        // 5. 사용자로부터 awsAccessKey, awsSecretKey 입력 받음
        AmazonEC2 amazonEC2Client = null;
        while (!flag) {
            flag = true;

            System.out.println("Enter AWS Access Key : ");
            String accessKey = scanner.nextLine();
            System.out.println(accessKey);

            System.out.println("Enter AWS Secret Key : ");
            String secretKey = scanner.nextLine();
            System.out.println(secretKey);

            // 입력 받은 정보로 ec2 client 생성
            amazonEC2Client = awsMigrator.BuildEC2Client(accessKey, secretKey, Regions.valueOf(region));

            // 자격 증명에 오류가 있는지 검사
            if (!awsMigrator.checkGetEC2InstanceDetails(amazonEC2Client)) {
                System.out.println("Invalid Key!");
                flag = false;
            }
        }

        //6. 해당 Instance들의 정보를 공통 서버 정보로 변환
        List<ServerInfo> serverInfoList = ncpMigrator.convertNCPInstanceDetailsToServerInfo(ncpAccessKey,ncpSecretKey,instanceNos);


        //7. 공통 서버 정보를 이전할 서버 정보로 변환
        List<AwsServerInfo> awsServerInfoList = new ArrayList<>();
        
        for(ServerInfo serverInfo: serverInfoList) {
            awsServerInfoList.add(awsMigrator.convertServerInfoToAWSServerInfo(amazonEC2Client, serverInfo));
        }
        
        //8. 서버 실행 및 완료 메시지 출력
        for(AwsServerInfo awsServerInfo: awsServerInfoList) {
            awsMigrator.RunEc2Instance(amazonEC2Client, awsServerInfo);

            System.out.println(awsServerInfo.instanceName + "Migration completed");
        }
    }

    public void getAWSKey() {

    }

}