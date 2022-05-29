package docker;

public class NcpServerImageProduct {

    private String productCode;
    private String productName;
    private String productDescription;
    private Long baseBlockStorageSize;

    NcpServerImageProduct(String productCode, String productName,String productDescription, Long baseBlockStorageSize) {
        this.productCode = productCode;
        this.productName = productName;
        this.productDescription = productDescription;
        this.baseBlockStorageSize = baseBlockStorageSize;
    }

    public String getProductCode(){
        return productCode;
    }

    public String getProductName(){
        return productName;
    }

    public String getProductDescription(){
        return productDescription;
    }

    public Long getBaseBlockStorageSize(){
        return baseBlockStorageSize;
    }
}
