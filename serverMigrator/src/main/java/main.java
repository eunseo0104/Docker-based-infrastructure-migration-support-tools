import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ecr.AmazonECR;

import java.io.Console;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class main {
    public static void main(String args[]) throws Exception {

        Scanner scanner = new Scanner(System.in);
        Migrator migrator = new Migrator();

        AwsMigrator awsMigrator = new AwsMigrator();
        NcpMigrator ncpMigrator = new NcpMigrator();

        String awsAccessKey = "AKIA6CU5MHVS2ZQMCCNC";
        String awsSecretKey = "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";
        Regions regions = Regions.AP_NORTHEAST_2;

        AmazonECR amazonECR = awsMigrator.BuildECRClient(awsAccessKey, awsSecretKey, regions);

        ServerInfo serverInfo = new ServerInfo();

        boolean flag1 = false; // 베이스 클라우드 입력
        boolean flag2 = false; // 타겟 클라우드 입력


        System.out.println("\n" +
                "            _                 _             \n" +
                "           (_)               | |            \n" +
                "  _ __ ___  _  __ _ _ __ __ _| |_ ___  _ __ \n" +
                " | '_ ` _ \\| |/ _` | '__/ _` | __/ _ \\| '__|\n" +
                " | | | | | | | (_| | | | (_| | || (_) | |   \n" +
                " |_| |_| |_|_|\\__, |_|  \\__,_|\\__\\___/|_|   \n" +
                "               __/ |                        \n" +
                "              |___/                         \n");


        System.out.println("Supported Cloud Platform List");

        Platform[] platforms = Platform.values();

        System.out.println("-----------------------------");
        for(Platform p : platforms) {
            System.out.print("|            ");
            System.out.print(p.name());
            System.out.println("            |");
        }
        System.out.println("-----------------------------");

        String basedCloudName = "";
        String targetCloudName = "";

        // base cloud platform 입력
        while(!flag1) {

            // base cloud platform 입력
            System.out.println("Enter based cloud platform : ");
            String basedCloud = scanner.nextLine();

            // 입력에 해당하는 값이 cloudplatformlist 에 있다면 (유효한 입력이라면)
            try {
                basedCloudName = Platform.valueOf(basedCloud).name();
                flag1 = true;
            } catch(IllegalArgumentException e) {
                System.out.println("Invalid input");
            }
        }

        // target Cloud Platform 입력
        while(!flag2) {
            // target cloud platform 입력
            System.out.println("Enter target cloud platform : ");
            String targetCloud = scanner.nextLine();

            // 입력에 해당하는 값이 cloudplatformlist 에 있다면 (유효한 입력이라면)
            try {
                targetCloudName = Platform.valueOf(targetCloud).name();
                flag2 = true;
            } catch(IllegalArgumentException e) {
                System.out.println("Invalid Input");
            }
        }

        if(!migrator.migrate(basedCloudName, targetCloudName))
            System.out.println("Invalid Platform");




        String naverAccessKey = "peEjtbF0CEY4e8xN9fgi";
        String naverSecretKey = "gm2dEU9I8bjRGVshHxMFhWEpxmtj0VX2ecmdwn3A";

    }
}
