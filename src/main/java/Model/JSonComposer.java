package Model;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/*
   Class to manage confluence related operations.
 */
public class JSonComposer {
    private String BASE_URL;
    private String USERNAME;
    private String PASSWORD;
    private  String ENCODING;
    private String pageId;

    public JSonComposer(WikiAuthentication wikiAuthentication) throws IOException {
        BASE_URL = wikiAuthentication.getBaseUrl();
        USERNAME = wikiAuthentication.getUsername();
        PASSWORD = wikiAuthentication.getPassword();
        ENCODING = wikiAuthentication.getEncode();
        pageId = wikiAuthentication.getPageId();
    }

    /**
     * Compose the URL to download attachment content by given page id and attachment name.
     * @param contentId
     * @param fileName
     * @param expansions
     * @return
     * @throws UnsupportedEncodingException
     */
    private String getAttachmentRestUrl(String contentId, String fileName, String[] expansions) throws UnsupportedEncodingException
    {
        final String expand = URLEncoder.encode(StringUtils.join(expansions, ","), ENCODING);
        // Example: https://wiki.globalrelay.net/download/attachments/144985221/coverage.json?api=v2
        return String.format("%s/download/attachments/%s/%s?api=v2?expand=%s&os_authType=basic&os_username=%s&os_password=%s", BASE_URL, contentId, fileName, expand, URLEncoder.encode(USERNAME, ENCODING), URLEncoder.encode(PASSWORD, ENCODING));
    }

    /**
     * Method to get attachment, and parse the content to json format.
     *
     * @param fileName
     * @return
     * @throws Exception
     */
    public JSONArray getCurrentJasonArrayFromWikiAttachment(String fileName) throws Exception
    {
        HttpClient client = HttpClientBuilder.create().build();

        // Get current page version
        String pageObj = null;
        HttpEntity pageEntity = null;
        JSONArray jsonArr = null;
        try
        {
            HttpGet getPageRequest = new HttpGet(getAttachmentRestUrl(pageId, fileName, new String[] {"body.storage", "version", "ancestors"}));
            HttpResponse getPageResponse = client.execute(getPageRequest);
            pageEntity = getPageResponse.getEntity();
            pageObj = IOUtils.toString(pageEntity.getContent());
            jsonArr = new JSONArray(pageObj);

        } catch(JSONException e) {
            //System.out.println("Cannot get wiki attachment: " + fileName);
        }
        finally
        {
            if (pageEntity != null)
            {
                EntityUtils.consume(pageEntity);
            }
        }

        return jsonArr;
    }

    /**
     * Get Confluence wiki attachment id by attachment file name.
     *
     * @param fileName
     * @return
     * @throws Exception
     */
    public String getAllWikiAttachmentId(String fileName) throws Exception
    {
        String auth = new String(Base64.encodeBase64((USERNAME + ":" + PASSWORD).getBytes()));

        HttpClient client = HttpClientBuilder.create().build();
        // Compose get request url
        HttpGet httpget = new HttpGet("https://wiki.globalrelay.net/rest/api/content/144985221/child/attachment?filename=" + fileName);
        httpget.setHeader("X-Atlassian-Token", "nocheck");
        httpget.setHeader("Authorization", "Basic " + auth);

        HttpResponse response = null;
        HttpEntity pageEntity = null;
        try {
            response = client.execute(httpget);
            pageEntity = response.getEntity();
            String pageObj = IOUtils.toString(pageEntity.getContent());
            //Retrieve attachment id from response
            JSONObject jsonArr = new JSONObject(pageObj);
            String pageObj2 = jsonArr.get("results").toString();
            JSONArray jsonArr2 = new JSONArray(pageObj2);
            JSONObject jsonObject2 = (JSONObject) jsonArr2.get(0);
            return (String) jsonObject2.get("id");
        } finally {
            if (response != null) {
                EntityUtils.consume(pageEntity);
            }
        }

    }

    /**
     * Send post request to confluence to update the attachment file with the new content.
     *
     * @param fileName
     * @param reportResults
     * @throws Exception
     */
    public void postNewJsonFile(String fileName, Map<String, String> reportResults) throws Exception {
        File newAttachment = constructNewJsonFile(fileName, reportResults);
        if(newAttachment != null) {
            String attachmentId = getAllWikiAttachmentId(fileName);
            String auth = new String(org.apache.commons.codec.binary.Base64.encodeBase64((USERNAME + ":" + PASSWORD).getBytes()));


            HttpClient client = HttpClientBuilder.create().build();
            HttpPost httppost = new HttpPost(BASE_URL + "/rest/api/content/" + pageId + "/child/attachment/" + attachmentId +"/data");
            httppost.setHeader("X-Atlassian-Token", "nocheck");
            httppost.setHeader("Authorization", "Basic " + auth);

            //Build file
            FileBody fileBody = new FileBody(newAttachment, "application/json");
            MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                    .addPart("file", fileBody);
            HttpEntity multiPartEntity = builder.build();
            httppost.setEntity(multiPartEntity);

            HttpResponse response = null;
            HttpEntity pageEntity = null;
            try {
                response = client.execute(httppost);
                pageEntity = response.getEntity();
            } finally {
                if (response != null) {
                    EntityUtils.consume(pageEntity);
                }
            }

            // Print out post result
            if (response.getStatusLine().getStatusCode() == 200) {
                System.out.println("Posted.");
            } else {
                System.out.println("Post Failed -> Error : " + response.getStatusLine().getStatusCode());
            }
        } else {
            System.out.println("Failed to post new attachment to wiki: " + fileName + ", please make sure your file name is correct or your credential to wiki is correct.");
        }
    }

    /**
     * Construct the new json file that will be posted to confluence wiki page.
     *
     * @param fileName
     * @param reportResults
     * @return
     * @throws Exception
     */
    public File constructNewJsonFile(String fileName, Map<String, String> reportResults) throws Exception {
        JSONArray currentJsonArray = getCurrentJasonArrayFromWikiAttachment(fileName);
        if(currentJsonArray != null) {
            JSONObject newJsonObject = composeNewJsonObject(reportResults);
            currentJsonArray.put(newJsonObject);

            File newJsonFile = new File(fileName);
            FileUtils.writeStringToFile(newJsonFile, currentJsonArray.toString());

            return newJsonFile;
        } else return null;
    }

    /**
     * Compose the new json object which represents the new item will be posted to confluence wiki page table.
     *
     * @param reportResults
     * @return
     */
    public JSONObject composeNewJsonObject(Map<String, String> reportResults) {
        JSONObject jsonObject = new JSONObject(reportResults);
        return jsonObject;
    }


}
