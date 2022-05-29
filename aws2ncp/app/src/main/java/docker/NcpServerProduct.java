package docker;

public class NcpServerProduct {
    private String productCode;
    private String productName;
    private String productDescription;
    private String productType;

    NcpServerProduct(String productCode, String productName,String productDescription, String productType) {
        this.productCode = productCode;
        this.productName = productName;
        this.productDescription = productDescription;
        this.productType = productType;
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

    public String getProductType(){
        return productType;
    }
}
