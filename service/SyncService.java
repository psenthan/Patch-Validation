/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
*/

package org.wso2.patchvalidator.service;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;
import org.wso2.patchvalidator.interfaces.CommonValidator;
import org.wso2.patchvalidator.validators.PatchValidateFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.patchvalidator.validators.PatchValidateService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.Objects;
import org.wso2.patchvalidator.validators.UpdateValidateService;


@Path("/{directory}")
public class SyncService {

    @GET
    @Path("/{patchId}/{state}/{type}")

            public void getType(@PathParam("patchId") String patchId,
                                  @PathParam("directory") String version,
                                  @PathParam("state") Integer state,
                                  @PathParam("type") Integer type) throws IOException, InterruptedException {

        if (type == 1) {
            PatchValidateService runPatchValidation = new PatchValidateService();
            runPatchValidation.zipPatchValidate(patchId,version,state,type);
        }
        else if(type ==2) {
            UpdateValidateService runUpdateValidation = new UpdateValidateService();
            runUpdateValidation.zipUpdateValidate(patchId,version,state,type);
        }
    }



}
