package docker;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ecr.AmazonECR;

import java.io.Console;
import java.util.*;

public class App {
    static Scanner scanner = new Scanner(System.in);
    static Migrator migrator = new Migrator();

    public static void main(String args[]) throws Exception {

        printLogo();

        Platform[] platforms = Platform.values();

        printCloudPlatformList(platforms);

        System.out.println("Based Cloud Select");
        String basedCloudName = getCloudPlatformInput(platforms);

        System.out.println("Target Cloud Select");
        String targetCloudName = getCloudPlatformInput(platforms);

        if(!migrator.migrate(basedCloudName, targetCloudName))
            System.out.println("Invalid Platform");
    }


    // 로고 출력
    private static void printLogo() {

        System.out.println("\n" +
                "            _                 _             \n" +
                "           (_)               | |            \n" +
                "  _ __ ___  _  __ _ _ __ __ _| |_ ___  _ __ \n" +
                " | '_ ` _ \\| |/ _` | '__/ _` | __/ _ \\| '__|\n" +
                " | | | | | | | (_| | | | (_| | || (_) | |   \n" +
                " |_| |_| |_|_|\\__, |_|  \\__,_|\\__\\___/|_|   \n" +
                "               __/ |                        \n" +
                "              |___/                         \n");
    }

    // 플랫폼 리스트 출력
    private static void printCloudPlatformList(Platform[] platforms) {

        System.out.println("Supported Cloud Platform List");


        System.out.println("---------------------------");
        for(int i=0; i<platforms.length; i++) {
            System.out.print("|         ");
            System.out.print(i + " : " + platforms[i].name());
            System.out.println("         |");
        }
        System.out.println("---------------------------");
    }

    // cloud platform 입력
    private static String getCloudPlatformInput(Platform[] platforms) {

        String cloudName = "";

        // cloud platform 입력
        while(true) {

            // 입력에 해당하는 값이 cloudplatformlist 에 있다면 (유효한 입력이라면)
            try {

                // cloud platform 입력
                System.out.println("Enter cloud platform number : ");
                int index = Integer.parseInt(scanner.nextLine());

                cloudName = platforms[index].name();
                return cloudName;

                // 유효하지 않은 입력이라면
            } catch(NumberFormatException | InputMismatchException | ArrayIndexOutOfBoundsException e) {
                System.out.println("유효하지 않은 입력입니다. 다시 입력해주세요.");
            }
        }
    }
}
