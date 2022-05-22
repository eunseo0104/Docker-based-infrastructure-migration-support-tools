import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


// 네이버 서버 생성 API
public class NcpMigrator {

    private static String serverApiUrl = "https://ncloud.apigw.ntruss.com";



    // NCP ServerInfo -> ServerInfo
    public List<ServerInfo> convertNCPInstanceDetailsToServerInfo(String accessKey, String secretKey, List<String> serverInstanceNoList) throws Exception {

        List<ServerInfo> serverInfoList = new ArrayList<>();

        for (String no: serverInstanceNoList) {
            ServerInfo serverInfo = new ServerInfo();

            String uri = "/vserver/v2/getServerInstanceDetail" + "&serverInstanceNo=" + no;
            StringBuilder response = NCPApiCall(accessKey, secretKey, uri);

            // 응답 xml 파싱
            Document document = XmlParser.stringBuilderToDocument(response);

            String totalRows = document.getElementsByTagName("totalRows").item(0).getTextContent();

            // serverInstanceDetail -> ServerInfo 변환
            if(!totalRows.equals("0")) {
                NodeList nList = document.getElementsByTagName("serverInstance");
                for (int temp = 0; temp < nList.getLength(); temp++) {
                    Node nNode = nList.item(temp);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) nNode;

                        String serverImageProductCode = document.getElementsByTagName("serverImageProductCode").item(0).getTextContent();
                        String serverProductCode = document.getElementsByTagName("serverProductCode").item(0).getTextContent();
                        StringBuilder response2 = getVPCServerImageProductList(accessKey, secretKey, serverProductCode);
                        Document document2 = XmlParser.stringBuilderToDocument(response2);

                        String totalRows2 = document2.getElementsByTagName("totalRows").item(0).getTextContent();

                        if(!totalRows2.equals("0")) {
                            NodeList nList2 = document2.getElementsByTagName("product");
                            for (int temp2 = 0; temp2 < nList2.getLength(); temp2++) {
                                Node nNode2 = nList2.item(temp2);
                                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                                    Element eElement2 = (Element) nNode2;

                                    String productCode = document2.getElementsByTagName("productCode").item(0).getTextContent();
                                    if(Objects.equals(productCode, serverImageProductCode)) {
                                        String productName = document2.getElementsByTagName("productName").item(0).getTextContent();

                                        serverInfo.osName = productName.split("-")[0];
                                        serverInfo.osVersion = productName.split("-")[1];
                                        serverInfo.osArchitecture = productName.split("-")[2];

                                        break;
                                    }
                                }
                            }
                        }

                        serverInfo.serverName =  document.getElementsByTagName("serverName").item(0).getTextContent();

                        String serverInstanceType = document.getElementsByTagName("serverInstanceType").item(0).getTextContent();
                        switch (serverInstanceType) {
                            case "STAND":
                                serverInfo.instanceType = "general";
                                break;
                            default:
                                serverInfo.instanceType = "default";
                                break;
                        }

                        serverInfo.memory = Long.parseLong(eElement.getElementsByTagName("memorySize").item(0).getTextContent())/1024/1024/1024;
                        serverInfo.vCpu =  Integer.parseInt(eElement.getElementsByTagName("cpuCount").item(0).getTextContent());
                        serverInfo.region = document.getElementsByTagName("regionCode").item(0).getTextContent();;

                        System.out.println(no);
                        serverInfoList.add(serverInfo);
                    }
                }
            }
        }
        return serverInfoList;
    }

    // ServerInfo -> NCP ServerInfo
    public NcpServerInfo convertServerInfoToNCPServerInfo(String accessKey, String secretKey, ServerInfo serverInfo) throws Exception {

        NcpServerInfo ncpServerInfo = new NcpServerInfo();

        ncpServerInfo.serverName = serverInfo.serverName;

        ncpServerInfo.regionCode = "KR"; // 수정 필요

        ncpServerInfo.serverImageProductCode = convertVPCServerImageProductCode(accessKey, secretKey, serverInfo.osName, serverInfo.osVersion);

        System.out.println(serverInfo.osName + " " + serverInfo.osVersion);
        System.out.println(ncpServerInfo.serverImageProductCode);

        ncpServerInfo.vpcNo = getVPCList(accessKey, secretKey, ncpServerInfo.regionCode);
        System.out.println(ncpServerInfo.vpcNo);

        ncpServerInfo.subnetNo = getSubnetList(accessKey, secretKey, ncpServerInfo.regionCode);
        System.out.println(ncpServerInfo.subnetNo);

        System.out.println(serverInfo.vCpu);
        System.out.println(serverInfo.memory);

        ncpServerInfo.serverProductCode = convertVPCServerProductCode(accessKey, secretKey, ncpServerInfo.regionCode,
                ncpServerInfo.serverImageProductCode, serverInfo.instanceType, serverInfo.memory, serverInfo.vCpu);

        ncpServerInfo.networkInterfaceOrder = 0;

        ncpServerInfo.accessControlGroupNoList = getAccessControlGroupList(accessKey, secretKey);
        return ncpServerInfo;
    }

    private String convertVPCServerProductCode(String accessKey, String secretKey, String regionCode, String serverImageProductCode, String instanceType, Long memory, int vCpu) throws Exception {

        regionCode = "KR";

        String uri = "/vserver/v2/getServerProductList?" + regionCode + "&serverImageProductCode=" + serverImageProductCode;

        StringBuilder response = NCPApiCall(accessKey, secretKey, uri);

        String ncpInstanceType = "";
        switch (instanceType) {
            case "general":
                ncpInstanceType = "STAND";
                break;
            case "computing":
                ncpInstanceType = "HICPU";
                break;
            case "memory":
                ncpInstanceType = "HIMEM";
                break;
            case "intensive":
                ncpInstanceType = "CPU";
                break;
            case "storage":
                ncpInstanceType = "NONE";
                break;
        }

        // 응답 xml 파싱
        Document document = XmlParser.stringBuilderToDocument(response);

        String totalRows = document.getElementsByTagName("totalRows").item(0).getTextContent();
        System.out.println(totalRows);

        // instanceType, memory, vCpu가 일치하면 반환
        if(!totalRows.equals("0")) {
            NodeList nList = document.getElementsByTagName("product");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;

                    String productType = eElement.getElementsByTagName("productType").item(0).getTextContent().split("        ")[1];
                    int cpuCount = Integer.parseInt(eElement.getElementsByTagName("cpuCount").item(0).getTextContent());
                    long memorySize = Long.parseLong(eElement.getElementsByTagName("memorySize").item(0).getTextContent())/1024/1024/1024;

                    // type, cpu, memory 같다면 productCode 반환
                    if(productType.equals(ncpInstanceType) && cpuCount == vCpu && memory == memorySize ) {
                        String productCode = eElement.getElementsByTagName("productCode").item(0).getTextContent();
                        return productCode;
                    }
                }
            }
        }
        return "Server Product Code does not exist";
    }

    private String getSubnetList(String accessKey, String secretKey, String regionCode) throws Exception {

        regionCode = "KR";

        String uri = "/vpc/v2/getSubnetList?" + regionCode;
        StringBuilder response = NCPApiCall(accessKey, secretKey, uri);

        // 응답 xml 파싱
        Document document = XmlParser.stringBuilderToDocument(response);

        String totalRows = document.getElementsByTagName("totalRows").item(0).getTextContent();

        // subnet 이 생성되어있을 경우 첫 subnetNo 반환
        if(!totalRows.equals("0")) {
            NodeList nList = document.getElementsByTagName("subnetList");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;

                    String vpcNo = eElement.getElementsByTagName("subnetNo").item(0).getTextContent();
                    return vpcNo;
                }
            }
        }
        return "Subnet does not exist";
    }

    public void runVPCInstance(String accessKey, String secretKey, NcpServerInfo ncpServerInfo) throws Exception {

        String acgString = "";
        for(int i =1; i < ncpServerInfo.accessControlGroupNoList.size() + 1; i++) {
            acgString += "&networkInterfaceList.1.accessControlGroupNoList." + i + "=" + ncpServerInfo.accessControlGroupNoList.get(i - 1);
        }


        String uri = "/vserver/v2/createServerInstances?serverImageProductCode=" + ncpServerInfo.serverImageProductCode
                + "&serverProductCode=" + ncpServerInfo.serverProductCode
                + "&vpcNo=" + ncpServerInfo.vpcNo
                + "&subnetNo=" + ncpServerInfo.subnetNo
                + "&serverName=" + ncpServerInfo.serverName
                + "&networkInterfaceList.1.networkInterfaceOrder=" + ncpServerInfo.networkInterfaceOrder
                + acgString;


        StringBuilder response = NCPApiCall(accessKey, secretKey, uri);
    }

    public void getVPCRegionList(String accessKey, String secretKey) {

        String uri = "/vserver/v2/getRegionList";

        StringBuilder response = NCPApiCall(accessKey, secretKey, uri);
    }

    public List<String> getServerInstanceNoList(String accessKey, String secretKey) throws Exception {

        List<String> serverInstanceNoList = new ArrayList<>();

        String uri = "/vserver/v2/getServerInstanceList";
        StringBuilder response = NCPApiCall(accessKey, secretKey, uri);

        // 응답 xml 파싱
        Document document = XmlParser.stringBuilderToDocument(response);

        String totalRows = document.getElementsByTagName("totalRows").item(0).getTextContent();

        // serverInstanceNo 리스트 반환
        if(!totalRows.equals("0")) {
            NodeList nList = document.getElementsByTagName("serverInstanceList");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;

                    String no = document.getElementsByTagName("serverInstanceNo").item(0).getTextContent();
                    System.out.println(no);
                    serverInstanceNoList.add(no);
                }
            }
        }
        return serverInstanceNoList;
    }

    public List<String> getAccessControlGroupList(String accessKey, String secretKey) throws Exception {

        List<String> acgList = new ArrayList<>();

        String uri = "/vserver/v2/getAccessControlGroupList";

        StringBuilder response = NCPApiCall(accessKey, secretKey, uri);

        // 응답 xml 파싱
        Document document = XmlParser.stringBuilderToDocument(response);

        String totalRows = document.getElementsByTagName("totalRows").item(0).getTextContent();

        // vpc 가 생성되어있을 경우 첫 vpcNo 반환
        if(!totalRows.equals("0")) {
            NodeList nList = document.getElementsByTagName("accessControlGroupList");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;

                    String acg = document.getElementsByTagName("accessControlGroupNo").item(0).getTextContent();
                    System.out.println(acg);
                    acgList.add(acg);
                }
            }
        }
        return acgList;
    }

    // VPC 리스트 조회
    public String getVPCList(String accessKey, String secretKey, String regionCode) throws Exception {

        regionCode = "KR";

        String uri = "/vpc/v2/getVpcList?" + regionCode;
        StringBuilder response = NCPApiCall(accessKey, secretKey, uri);

        // 응답 xml 파싱
        Document document = XmlParser.stringBuilderToDocument(response);

        String totalRows = document.getElementsByTagName("totalRows").item(0).getTextContent();

        // vpc 가 생성되어있을 경우 첫 vpcNo 반환
        if(!totalRows.equals("0")) {
            NodeList nList = document.getElementsByTagName("vpcList");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;

                    String vpcNo = eElement.getElementsByTagName("vpcNo").item(0).getTextContent();
                    return vpcNo;
                }
            }
        }
        return "vpc does not exist";
    }

    public void getVPCZoneList(String accessKey, String secretKey) {

        String regionCode = "KR";

        String uri = "/vserver/v2/getZoneList?regionCode=" + regionCode;

        StringBuilder response = NCPApiCall(accessKey, secretKey, uri);

    }

    // 서버 이미지 프로덕트 코드 변환
    public String convertVPCServerImageProductCode(String accessKey, String secretKey, String osName, String osVersion) throws Exception {

        // getVPCServerImageProductList API 호출
        StringBuilder response = getVPCServerImageProductList(accessKey, secretKey);

        // 응답 xml 파싱
        // serverInfo의 os 정보와 일치하는 product description이 있으면 해당 productCode 반환
        String xml = response.toString();
        Document document = XmlParser.loadXMLFromString(xml);
        document.getDocumentElement().normalize();

        NodeList nList = document.getElementsByTagName("product");

        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;

                String productOsName = "";
                String productOsVersion = "";
                String productDescription = eElement.getElementsByTagName("productDescription").item(0).getTextContent();

                for(int i = 0; i < productDescription.length(); i++) {

                    char c = productDescription.charAt(i);

                    // 0~첫 숫자 이전: OS명, 첫 숫자~다음 공백: OS 버전
                    if(c >= '0' && c <= '9') {
                        productOsName = productDescription.substring(0,i-1);
                        productOsVersion = productDescription.substring(i).split(" ")[0];
                        break;
                    }
                }
                System.out.println(productOsName + " " + productOsVersion);
                // OS가 일치하는지 확인
                if(productOsName.equalsIgnoreCase(osName) && productOsVersion.equalsIgnoreCase(osVersion)) {

                    return eElement.getElementsByTagName("productCode").item(0).getTextContent();
                }
            }
        }
        return "not exist";
    }

    // VPC 서버 이미지 프로덕트
    public StringBuilder getVPCServerImageProductList(String accessKey, String secretKey) {

        // 수정 필요
        String regionCode = "KR";
        String uri = "/vserver/v2/getServerImageProductList?regionCode=" + regionCode;

        StringBuilder response = NCPApiCall(accessKey, secretKey, uri);
        return response;
    }

    // VPC 서버 이미지 프로덕트 with serverProductCode
    public StringBuilder getVPCServerImageProductList(String accessKey, String secretKey, String serverProductCode) {

        // 수정 필요
        String regionCode = "KR";
        String uri = "/vserver/v2/getServerImageProductList?regionCode=" + regionCode + "&productCode=" + serverProductCode;

        StringBuilder response = NCPApiCall(accessKey, secretKey, uri);
        return response;
    }

    // API 호출을 위한 Signature 생성
    public static String makeSignature(String accessKey, String secretKey, String timestamp, String method, String url) throws Exception {

        String space = " ";					// one space
        String newLine = "\n";					// new line

        String message = new StringBuilder()
                .append(method)
                .append(space)
                .append(url)
                .append(newLine)
                .append(timestamp)
                .append(newLine)
                .append(accessKey)
                .toString();

        SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(signingKey);

        byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        String encodeBase64String = Base64.encodeBase64String(rawHmac);

        return encodeBase64String;
    }

    // API 호출
    public StringBuilder NCPApiCall(String accessKey, String secretKey, String uri) {

        String timestamp = Long.toString(Instant.now().toEpochMilli());

        StringBuilder response = new StringBuilder();
        try {
            URL url = new URL(serverApiUrl + uri);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("GET");

            con.setRequestProperty("x-ncp-apigw-timestamp", timestamp);
            con.setRequestProperty("x-ncp-iam-access-key", accessKey);
            con.setRequestProperty("x-ncp-apigw-signature-v2", makeSignature(accessKey, secretKey, timestamp, "GET", uri));

            int responseCode = con.getResponseCode();
            BufferedReader br;
            if(responseCode==200) { // 정상 호출
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            } else {  // 에러 발생
                br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }
            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();
            System.out.println(response);

        } catch (Exception e) {
            System.out.println(e);
        }

        return response;
    }


    // VPC 리스트 조회
    public boolean checkGetVPCList(String accessKey, String secretKey, String regionCode) throws Exception {

        regionCode = "KR";

        String uri = "/vpc/v2/getVpcList?" + regionCode;
        StringBuilder response = NCPApiCall(accessKey, secretKey, uri);

        // 응답 xml 파싱
        Document document = XmlParser.stringBuilderToDocument(response);

        String totalRows = document.getElementsByTagName("totalRows").item(0).getTextContent();

        // vpc 가 생성되어있을 경우 첫 vpcNo 반환
        if(!totalRows.equals("0")) {
            NodeList nList = document.getElementsByTagName("vpcList");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;

                    String vpcNo = eElement.getElementsByTagName("vpcNo").item(0).getTextContent();
                    return true;
                }
            }
        }
        return false;
    }

}