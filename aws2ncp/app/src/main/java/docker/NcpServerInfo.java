package docker;
import java.util.List;

public class NcpServerInfo {

    /**
     * 리전 코드
     * 서버 인스턴가 생성될 리전(Region) 결정 가능
     * regionCode는 getRegionList 액션을 통해 획득 가능
     * Default : getRegionList 조회 결과의 첫 번째 리전을 선택
     */
    String regionCode;

    /**
     * - 회원 서버 이미지 인스턴스 번호
     * 직접 생성한 서버 이미지로부터 서버를 생성시 입력
     * 회원 서버 이미지 인스턴스 번호(memberServerImageInstanceNo)와 서버 이미지 상품 코드(serverImageProductCode) 중 반드시 한 개를 필수로 입력
     * memberServerImageInstanceNo는 getMemberServerImageInstanceList 액션을 통해 획득 가능
     */
    String memberServerImageInstanceNo;

    String serverImageProductCode;
    String vpcNo;
    String subnetNo;
    String serverProductCode;
    Boolean isEncryptedBaseBlockStorageVolume;
    String feeSystemTypeCode;
    int serverCreateCount;
    int serverCreateStartNo;
    String serverName;
    int networkInterfaceOrder;
    String networkInterfaceNo;
    String networkInterfaceSubnetNo;
    String networkInterfaceIp;
    List<String> accessControlGroupNoList;
    String placementGroupNo;
    Boolean isProtectServerTermination;
    String serverDescription;
    String initScriptNo;
    String loginKeyName;

    Boolean associateWithPublicIp;
    String responseFormatType;
}
