/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.patchvalidator.validators;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Properties;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.patchvalidator.interfaces.CommonValidator;
import org.wso2.patchvalidator.service.SyncService;

/**
 * TODO: Class level comment.
 */
public class UpdateValidateService {

    public static PatchValidateFactory getPatchValidateFactory(String filepath) {
        if (filepath.endsWith(".zip")) {
            return new PatchValidateFactory();
        }
        return null;
    }
    private Properties prop = new Properties();
    private Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    private static final Logger LOG = LoggerFactory.getLogger(SyncService.class);


    public String zipUpdateValidate(String updateId, String version, int state, int type) throws IOException, InterruptedException {

        LOG.info("Sync Service running\n");
        prop.load( SyncService.class.getClassLoader().getResourceAsStream("application.properties"));
       // https://svn.wso2.com/wso2/custom/projects/projects/carbon/wilkes/updates/update2019/

        final String organization = prop.getProperty("organization");
        final String destFilePath = prop.getProperty("destFilePath")+"/update/";
        final String staticURL = prop.getProperty("staticURL");
        final String updateUrl = staticURL + version + "/updates/update" + updateId + "/";
        String errorMessage = "";
        String timeStamp = String.valueOf(timestamp.getTime());

        version = prop.getProperty(version);
        if(version==null){
            return "Incorrect directory";
        }
        String updateDestination = destFilePath + version + "/" + timeStamp+ "/update" + updateId + "/";
        String updateName = organization + version + "-" + updateId;

        String filepath = updateDestination + updateName + ".zip";
        String unzippedFolderPath = updateDestination + organization + version + "-" + updateId + "/";



        PatchValidateFactory patchValidateFactory = PatchValidateService.getPatchValidateFactory(filepath);
        assert patchValidateFactory != null;

        CommonValidator commonValidator = patchValidateFactory.getCommonValidation(filepath);


        String result = commonValidator.downloadZipFile(updateUrl, version, updateId, updateDestination);
        if (!Objects.equals(result, "")) {
            LOG.info(result);
            return result + "\n" + errorMessage;
        }


        File fl = new File(updateDestination);

        for (File file : fl.listFiles()) {
            if (file.getName().endsWith(".md5") || file.getName().endsWith((".asc"))
                    || file.getName().endsWith((".sha1"))) {
                /*
                todo: sendRequest()
                ("WSO2-CARBON-PATCH-4.0.0-0591","ReleasedNotInPublicSVN",true,"Promote");
                */
                errorMessage = "update" + updateId + " is already signed\n";
                FileUtils.deleteDirectory(new File(destFilePath));
                LOG.info(errorMessage + "\n");
                return errorMessage + "\n";
            }
        }
        try {
            LOG.info(filepath);
            LOG.info(updateDestination);
            LOG.info(unzippedFolderPath);

            commonValidator.unZip(new File(filepath), updateDestination);

              String updateValidateScriptPath =prop.getProperty("updateValidateScriptPath");

        try {
            Process validateUpdate = new ProcessBuilder("/bin/bash", updateValidateScriptPath).start();
            validateUpdate.waitFor();
            System.out.println("Script Successfully Executed!!!!!!!!!");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



        } catch (IOException e) {
            LOG.error("unzipping failed", e);
            errorMessage = errorMessage + "File unzipping failed\n";
        }


        if (Objects.equals(errorMessage, "SUCCESS")) {
            String updateStatus;
            if (state == 1) updateStatus = "ReleasedNotInPublicSVN";
            else if (state == 2) updateStatus = "ReleasedNotAutomated";
            else if (state == 3) updateStatus = "Promote";
            else updateStatus = "Error in update status";
            System.out.println("updateStatus = " + updateStatus);
            //todo: commit keys
            commonValidator.commitKeys(updateUrl, updateDestination);
            sendRequest(updateName, updateStatus, true,updateStatus);
        }

        FileUtils.deleteDirectory(new File(destFilePath));
        LOG.info(errorMessage + "\n");
        return errorMessage + "\n";



    }
    private void sendRequest(String updateName,String state, boolean isSuccess, String AdminTestPromote)
            throws IOException {
        String successState;
        if(isSuccess) successState = "true";
        else successState = "false";

        JSONObject json = new JSONObject();
        json.put("updateName", updateName);
        json.put("state", state);
        json.put("isSuccess",successState);
        json.put("AdminTestPromote",AdminTestPromote);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPost request = new HttpPost(prop.getProperty("httpUri"));
            LOG.info(String.valueOf(request));
            StringEntity params = new StringEntity(json.toString());
            params.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, prop.getProperty("content-type")));
            request.addHeader("Authentication", prop.getProperty("Authentication"));
            request.addHeader("Cache-Control", prop.getProperty("Cache-Control"));
            request.setEntity(params);
            httpClient.execute(request);
        } catch (Exception ex) {
            LOG.error("Error at sending Request", ex);
        }
    }



}


