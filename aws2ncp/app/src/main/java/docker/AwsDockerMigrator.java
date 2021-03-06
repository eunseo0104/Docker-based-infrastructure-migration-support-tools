/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package docker;
import com.amazonaws.services.ecr.AmazonECR;
import com.amazonaws.services.ecr.AmazonECRClientBuilder;
import com.amazonaws.services.ecr.model.ImageIdentifier;
import com.amazonaws.services.ecr.model.ListImagesRequest;
import com.amazonaws.services.ecr.model.ListImagesResult;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import java.util.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
//aws to ncp
public class AwsDockerMigrator {
    
	
    public void migrateDocker(String args[], String[] aws){
        //args[0]: registry name
        //args[1]: access key
        //args[2]: secret key

	/*
        //ncp
        args = new String[3];
        args[0] = "";
        args[1] = "";
        args[2] = "";
        //aws
        aws = new String[5];
        aws[0] = ""; //ECS repository name
        aws[1] = ""; //AccessKey
        aws[2] = ""; //SecretKey
        aws[3] = ""; //region
        aws[4] = ""; //account id
	*/
	
        AmazonECR ecr = BuildECRClient(aws[1], aws[2], Regions.valueOf(aws[3]));
        try{
            List<ImageIdentifier> imageIdentifier = getlistImageResult(ecr, aws);
            String[] images = new String[imageIdentifier.size()];
            for(int i=0; i<imageIdentifier.size(); i++){
                images[i] = imageIdentifier.get(i).getImageTag();
            }
            downloadImages(aws, images);
        	uploadImages(args, aws, images);
        }
        catch(Exception e){
        	System.out.println(e);
        }
    }
    private static AmazonECR BuildECRClient(String accessKey, String secretKey, Regions regions){
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);

        return AmazonECRClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(regions)
                .build();
    }

    private static List<ImageIdentifier> getlistImageResult(AmazonECR amazonECR, String[] aws) throws Exception{
    	
        ListImagesRequest listImagesRequest = new ListImagesRequest();
        listImagesRequest.withRepositoryName(aws[0]);
        ListImagesResult listImagesResult = amazonECR.listImages(listImagesRequest);
                
        return listImagesResult.getImageIds();
    }

    private static void downloadImages(String[] aws, String[] images) throws Exception{
        String[] cmd = {"/bin/sh", "-c", "apt install awscli & aws configure set aws_access_key_id "+aws[1] 
        + "& aws configure set aws_secret_access_key "+aws[2]+ "& (aws ecr get-login-password --region "+aws[3]
        + " | docker login --username AWS --password-stdin "+aws[4]+".dkr.ecr."+aws[3]+".amazonaws.com)"};
        Process p = Runtime.getRuntime().exec(cmd);
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String s;
        while((s=br.readLine())!=null)
                System.out.println(s);
        
        for(int i=0; i<images.length; i++){
            System.out.println(images[i]);

            cmd[2] = "docker pull " + aws[4]+".dkr.ecr."+aws[3]+".amazonaws.com/"+aws[0]+":"+images[i];
            System.out.println(cmd[2]);
            p = Runtime.getRuntime().exec(cmd);
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while((s=br.readLine())!=null)
                System.out.println(s);
        }
        

    }

    private static void uploadImages(String[] args, String[] aws, String[] images) throws Exception{
        String region = "kr";
        String endpoint = args[0]+"."+region+".ncr.ntruss.com";
        String[] cmd = {"/bin/sh", "-c", "docker login -u "+args[1]+" "+endpoint + " -p "+args[2]};
        Process p = Runtime.getRuntime().exec(cmd);
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String s;
        while((s=br.readLine())!=null)
                System.out.println(s);

        for(int i=0; i<images.length; i++){
            cmd[2] = "docker tag "+aws[4]+".dkr.ecr."+aws[3]+".amazonaws.com/"+aws[0]+":"+images[i]+" "+endpoint+"/"+images[i];
            System.out.println(cmd[2]);
            p = Runtime.getRuntime().exec(cmd);

            cmd[2] = "docker push "+endpoint+"/"+images[i];
	    
	    
            p = Runtime.getRuntime().exec(cmd);
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while((s=br.readLine())!=null)
                System.out.println(s);
        }
    }
    
}


