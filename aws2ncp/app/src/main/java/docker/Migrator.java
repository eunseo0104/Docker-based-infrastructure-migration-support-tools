package docker;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Instance;

import java.util.*;

public class Migrator {

    static Scanner scanner = new Scanner(System.in);
    static AwsMigrator awsMigrator = new AwsMigrator();
    static NcpMigrator ncpMigrator = new NcpMigrator();

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

        String ncpAccessKey = null;
        String ncpSecretKey = null;
        String ncpRegistryName = null;
        String awsRepositoryName = null;
	    String awsAccessKey = null;
	    String awsSecretKey = null;

        // 1. 사용자로부터 서버가 있는 region을 입력 받음
        Regions[] regions = Regions.values();
        printAwsRegionList(regions);

        String region = getAwsRegionInput(regions);

        
    	// 2. base Key 입력
        AmazonEC2 amazonEC2Client = null;

        while(amazonEC2Client == null) {

            // aws 키 입력
            KeyPair awsKeyPair = getAwsKeyInput();
            awsAccessKey = awsKeyPair.getAccessKey();
            awsSecretKey = awsKeyPair.getSecretKey();
            // 키로 클라이언트 생성 및 검증
            amazonEC2Client = buildEc2Client(awsKeyPair.getAccessKey(), awsKeyPair.getSecretKey(), region);
        }

        // 3.target key 입력
        KeyPair ncpKeyPair = getNcpKeyInput();
        ncpAccessKey = ncpKeyPair.getAccessKey();
        ncpSecretKey = ncpKeyPair.getSecretKey();

        System.out.println("Are you sure you want to migrate EC2 Instance?(Y/N): ");
        while(true){
            String answer = scanner.nextLine();
            if(answer.equals("Y")){

                // 3. 입력 받은 계정 정보로 해당 region의 모든 Instance 정보를 가져옴
                List<Instance> instanceList = awsMigrator.getEC2InstanceDetails(amazonEC2Client);
                System.out.println(instanceList);

                // 4. 사용자에게 Instance 목록을 보여주고 이전할 Instance id를 입력 받음

                printAwsInstanceIdList(instanceList);

                String instanceId = getAwsInstanceIdInput(instanceList);


                List<String> instanceIds = new ArrayList<>();
                instanceIds.add(instanceId);


                //6. 해당 Instance 서버 정보를 호출
                List<Instance> selectedInstanceList = awsMigrator.getEC2InstanceDetails(amazonEC2Client, instanceIds);


                //7. 호출한 서버 정보를 공통 서버 정보로 변환
                List<ServerInfo> serverInfoList = awsMigrator.convertEC2InstanceDetailsToServerInfo(amazonEC2Client, selectedInstanceList, Regions.valueOf(region));


                List<String> ncpRegionList = ncpMigrator.getRegionList(ncpAccessKey, ncpSecretKey);

                // target platform region list 출력
                printNcpRegionList(ncpRegionList);

                // target platform region 선택
                String regionCode = getNcpRegionCodeInput(ncpRegionList);


                List<NcpServerImageProduct> ncpServerImageProductList = ncpMigrator.getNcpServerImageProductList(ncpAccessKey, ncpSecretKey, regionCode);

                // server image product list 출력
                printServerImageProductList(ncpServerImageProductList);

                // server image product 선택
                NcpServerImageProduct ncpServerImageProduct = getNcpServerImageProductInput(ncpServerImageProductList);

                List<NcpServerProduct> serverProductList = ncpMigrator.getNcpServerProductList(ncpAccessKey, ncpSecretKey, regionCode, ncpServerImageProduct.getProductCode());

                // server image product 에 적용 가능한 server product list 출력
                printServerProductList(serverProductList);

                // server product 선택
                NcpServerProduct ncpServerProduct = getNcpServerProductInput(serverProductList);

                //8. 공통 서버 정보를 이전할 서버 정보로 변환
                List<NcpServerInfo> ncpServerInfoList = new ArrayList<>();

                for(ServerInfo serverInfo: serverInfoList) {
                    NcpServerInfo ncpServerInfo = ncpMigrator.convertServerInfoToNCPServerInfo(ncpAccessKey, ncpSecretKey, serverInfo, regionCode);
                    ncpServerInfo.serverImageProductCode = ncpServerImageProduct.getProductCode();
                    ncpServerInfo.serverProductCode = ncpServerProduct.getProductCode();
                    ncpServerInfoList.add(ncpServerInfo);
                }

                //9. 서버 실행 및 완료 메시지 출력
                for(NcpServerInfo ncpServerInfo: ncpServerInfoList) {
                    ncpMigrator.runVPCInstance(ncpAccessKey, ncpSecretKey, ncpServerInfo);

                    System.out.println(ncpServerInfo.serverName + "Migration completed");
                }

                System.out.println("Server Instance Migration Complete.");
                break;
            }
            else if(answer.equals("N")){
                System.out.println("Complete.");
                break;
            }
            else{
                System.out.println("(Y/N): ");
            }
        }

        
        //10. Docker image Migration
        System.out.println("Are you sure you want to migrate AWS Docker image?(Y/N): ");
        while(true){
            String answer = scanner.nextLine();
            if(answer.equals("Y")){
                AwsDockerMigrator awsDocker = new AwsDockerMigrator();
                System.out.println("Enter NCP Container Registry name: ");
                ncpRegistryName = scanner.nextLine();

                System.out.println("Enter AWS Container Repository name: ");
                awsRepositoryName = scanner.nextLine();

                System.out.println("Enter AWS Account id: ");
                String awsAcountId = scanner.nextLine();
	
                String[] ncpInfo = {ncpRegistryName, ncpAccessKey, ncpSecretKey};	
                String[] awsInfo = {awsRepositoryName, awsAccessKey, awsSecretKey, region, awsAcountId};
                awsDocker.migrateDocker(ncpInfo, awsInfo);
                System.out.println("Image Migration Complete.");
                
                break;
            }
            else if(answer.equals("N")){
                System.out.println("Complete.");
                break;
            }
            else{
                System.out.println("(Y/N): ");
            }
        }
        
        //11. kubernetes service
   	AwsServiceMigrator awsServiceMigrator = new AwsServiceMigrator();
        
        System.out.println("Enter clusterName: ");
        String clusterName = scanner.nextLine();
        
        awsServiceMigrator.ks_a2n(awsAccessKey, awsSecretKey, ncpAccessKey, ncpSecretKey, clusterName, region);
        
    }

    // ncp to aws 변환
    public void ncpToAws() throws Exception {

        String awsAccessKey = null;
        String awsSecretKey = null;
        String ncpRegistryName = null;
        String awsRepositoryName = null;

        // 1. 사용자로부터 ncpAccessKey, ncpSecretKey 입력 받음
        KeyPair ncpKeyPair = getNcpKeyInput();
        String ncpAccessKey = ncpKeyPair.getAccessKey();
        String ncpSecretKey = ncpKeyPair.getSecretKey();

        // 2. 입력 받은 계정 정보로 모든 InstanceNo 를 가져옴
        List<String> serverInstanceNoList = ncpMigrator.getServerInstanceNoList(ncpAccessKey, ncpSecretKey);

        // 3. 모든 InstanceNo 출력 및 이전할 InstanceNo 입력
        printNcpInstanceNoList(serverInstanceNoList);

        String instanceNo = getNcpInstanceNoInput(serverInstanceNoList);

        List<String> instanceNos = new ArrayList<>();
        instanceNos.add(instanceNo);

        // 4. 타겟 플랫폼 region 리스트 출력 및 입력
        Regions[] regions = Regions.values();
        printAwsRegionList(regions);

        // 5. 타겟 플랫폼 키 입력 및 클라이언트 생성
        String region = getAwsRegionInput(regions);

        AmazonEC2 amazonEC2Client = null;

        while(amazonEC2Client == null) {

            // aws 키 입력
            KeyPair awsKeyPair = getAwsKeyInput();

            // 키로 클라이언트 생성 및 검증
            amazonEC2Client = buildEc2Client(awsKeyPair.getAccessKey(), awsKeyPair.getSecretKey(), region);
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
        
        //9. Docker image 이전
        System.out.println("Are you sure you want to migrate NCP Docker image?(Y/N): ");
        while(true){
		String answer = scanner.nextLine();
		if(answer.equals("Y")){
			NcpDockerMigrator ncpDockerMigrator = new NcpDockerMigrator();
			
			System.out.println("Enter NCP Container Registry name: ");
			ncpRegistryName = scanner.nextLine();
			
			System.out.println("Enter AWS Container Repository name: ");
			awsRepositoryName = scanner.nextLine();
			
			System.out.println("Enter AWS Account id: ");
			String awsAcountId = scanner.nextLine();
			
			String[] ncpInfo = {ncpRegistryName, ncpAccessKey, ncpSecretKey};
			String[] awsInfo = {awsRepositoryName, awsAccessKey, awsSecretKey, region, awsAcountId};
			
			ncpDockerMigrator.migrateDocker(ncpInfo, awsInfo);
			System.out.println("Image Migration Complete.");
			break;
		}
		else if(answer.equals("N")){
			System.out.println("Complete.");
			break;
		}
		else{
			System.out.println("(Y/N): ");
		}
	}
	//10. Kubernetes service
   	NcpServiceMigrator ncpServiceMigrator = new NcpServiceMigrator();
   
   	System.out.println("Enter clusterName: ");
        String clusterName = scanner.nextLine();
   
   	ncpServiceMigrator.ks_n2a(ncpAccessKey, ncpSecretKey, awsAccessKey, awsSecretKey, clusterName);
    }

    // AWS Region 리스트 출력
    private static void printAwsRegionList(Regions[] regions) {

        System.out.println("Region List");

        System.out.println("-----------------------------");
        for(int i=0; i<regions.length; i++) {
            System.out.printf("|     %-22s |\n", i + " : " + regions[i].name());
        }
        System.out.println("-----------------------------");
    }

    // AWS Region 입력
    private static String getAwsRegionInput(Regions[] regions) {

        while(true) {
            try {
                // cloud platform 입력
                System.out.println("Enter region number : ");
                int index = Integer.parseInt(scanner.nextLine());

                return regions[index].name();

                // 유효하지 않은 입력이라면
            } catch(Exception e) {
                System.out.println("유효하지 않은 입력입니다. 다시 입력해주세요.");
            }
        }
    }

    // AWS Key 입력
    private static KeyPair getAwsKeyInput() {

        while(true) {

            try {
                System.out.println("Enter AWS Access Key : ");
                String awsAccessKey = scanner.nextLine();
                System.out.println(awsAccessKey);

                System.out.println("Enter AWS Secret Key : ");
                String awsSecretKey = scanner.nextLine();
                System.out.println(awsSecretKey);

                return new KeyPair(awsAccessKey, awsSecretKey);
            } catch (Exception e) {
                System.out.println("키가 유효하지 않습니다. 다시 입력해주세요.");
            }
        }
    }

    // AWS Client 생성
    private static AmazonEC2 buildEc2Client(String accessKey, String secretKey, String region) {

        try {
            // 입력 받은 정보로 ec2 client 생성
            AmazonEC2 amazonEC2Client = awsMigrator.BuildEC2Client(accessKey, secretKey, Regions.valueOf(region));

            // 자격 증명에 오류가 있는지 검사
            if (!awsMigrator.checkGetEC2InstanceDetails(amazonEC2Client)) {
                //System.out.println("키가 유효하지 않습니다. 다시 입력해주세요.");
                throw new Exception();
            }

            return amazonEC2Client;
        } catch (Exception e) {
            System.out.println("키가 유효하지 않습니다. 다시 입력해주세요.");
            return null;
        }
    }

    // AWS Instance ID 리스트 출력
    private static void printAwsInstanceIdList(List<Instance> instanceList) {

        System.out.println("Instance Id List");

        System.out.println("-------------------------------");
        for(int i=0; i<instanceList.size(); i++) {
            System.out.printf("|   %-22s   |\n", i + " : " + instanceList.get(i).getInstanceId());
        }
        System.out.println("-------------------------------");
    }

    // AWS Instance ID 입력
    private static String getAwsInstanceIdInput(List<Instance> instanceList) {

        while (true) {
            try {
                // Instance Id 번호 입력
                System.out.println("Enter instance id number : ");
                int index = Integer.parseInt(scanner.nextLine());

                return instanceList.get(index).getInstanceId();

                // 유효하지 않은 입력이라면
            } catch (Exception e) {
                System.out.println("유효하지 않은 입력입니다. 다시 입력해주세요.");
            }
        }
    }

    // NCP Key 입력
    private static KeyPair getNcpKeyInput() {

        while(true) {

            try {
                System.out.println("Enter NCP Access Key : ");
                String ncpAccessKey = scanner.nextLine();
                System.out.println(ncpAccessKey);

                System.out.println("Enter NCP Secret Key : ");
                String ncpSecretKey = scanner.nextLine();
                System.out.println(ncpSecretKey);

                // 5-1. 자격 증명에 오류가 있는지 검사
                if(!ncpMigrator.checkGetVPCList(ncpAccessKey, ncpSecretKey, "")) {
                    //System.out.println("Invalid Key!");
                    throw new Exception();
                }
                return new KeyPair(ncpAccessKey,ncpSecretKey);
            } catch (Exception e) {
                System.out.println("키가 유효하지 않습니다. 다시 입력해주세요.");
            }
        }
    }

    // NCP Instance Number 리스트 출력
    private static void printNcpInstanceNoList(List<String> instanceNoList) {

        System.out.println("Instance Number List");

        System.out.println("-----------------------------");
        for (int i=0; i<instanceNoList.size(); i++) {
            System.out.printf("|        %-18s |\n", i + " : " + instanceNoList.get(i));
        }
        System.out.println("-----------------------------");
    }

    // NCP Instance Number 입력
    private static String getNcpInstanceNoInput(List<String> instanceNoList) {

        while (true) {
            try {
                // Instance No 번호 입력
                System.out.println("Enter instance no number : ");
                int index = Integer.parseInt(scanner.nextLine());

                return instanceNoList.get(index);

                // 유효하지 않은 입력이라면
            } catch (Exception e) {
                System.out.println("유효하지 않은 입력입니다. 다시 입력해주세요.");
            }
        }
    }

    // NCP Region 리스트 출력
    private static void printNcpRegionList(List<String> regionList) {

        System.out.println("Region List");

        System.out.println("-------------------");
        for(int i=0; i<regionList.size(); i++) {
            System.out.printf("|      %-11s |\n", i + " : " + regionList.get(i));
        }
        System.out.println("-------------------");
    }

    // NCP Region 입력
    private static String getNcpRegionCodeInput(List<String> regionList) {

        while(true) {
            try {
                System.out.println("Enter region number : ");
                int index = Integer.parseInt(scanner.nextLine());

                return regionList.get(index);

                // 유효하지 않은 입력이라면
            } catch(Exception e) {
                System.out.println("유효하지 않은 입력입니다. 다시 입력해주세요.");
            }
        }
    }

    // NCP Server Image Product  리스트 출력
    private static void printServerImageProductList(List<NcpServerImageProduct> ncpServerImageProductList) {

        System.out.println("Server Image Product List");

        System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.printf("|   No   |                          Name                          |                                   Description                              |   Block Storage Size (GB)  |\n");

        for(int i=0; i<ncpServerImageProductList.size(); i++) {
            NcpServerImageProduct ncpServerImageProduct = ncpServerImageProductList.get(i);
            System.out.printf("|   %-5s", i);
            System.out.printf("|      %-50s", ncpServerImageProduct.getProductName());
            System.out.printf("|      %-70s", ncpServerImageProduct.getProductDescription());
            System.out.printf("|             %-14s |\n", ncpServerImageProduct.getBaseBlockStorageSize());

        }
        System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
    }


    // NCP Server Image Product 입력
    private static NcpServerImageProduct getNcpServerImageProductInput(List<NcpServerImageProduct> ncpServerImageProductList) {

        while(true) {
            try {
                System.out.println("Enter server image product number : ");
                int index = Integer.parseInt(scanner.nextLine());

                return ncpServerImageProductList.get(index);

                // 유효하지 않은 입력이라면
            } catch(Exception e) {
                System.out.println("유효하지 않은 입력입니다. 다시 입력해주세요.");
            }
        }
    }

    private void printServerProductList(List<NcpServerProduct> serverProductList) {

        System.out.println("Server Product List");

        System.out.println("----------------------------------------------------------------------------------------------------------------------------");
        System.out.printf("|   No   |                          Name                                                             |    Product Type     |\n");

        for(int i=0; i<serverProductList.size(); i++) {
            NcpServerProduct ncpServerProduct = serverProductList.get(i);
            System.out.printf("|   %-5s", i);
            System.out.printf("|      %-85s", ncpServerProduct.getProductName());
            System.out.printf("|      %-14s |\n", ncpServerProduct.getProductType());

        }
        System.out.println("----------------------------------------------------------------------------------------------------------------------------");
    }

    // NCP Server Product 입력
    private static NcpServerProduct getNcpServerProductInput(List<NcpServerProduct> ncpServerProductList) {

        while(true) {
            try {
                System.out.println("Enter server product number : ");
                int index = Integer.parseInt(scanner.nextLine());

                return ncpServerProductList.get(index);

                // 유효하지 않은 입력이라면
            } catch(Exception e) {
                System.out.println("유효하지 않은 입력입니다. 다시 입력해주세요.");
            }
        }
    }
}
