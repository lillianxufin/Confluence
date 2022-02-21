package Model;

import Config.LoadYaml;

import java.io.IOException;

/*
  Wiki authentication class. Will get the basic information to acdess the confluence wiki.
 */
public class WikiAuthentication {
    private String baseUrl;
    private String username;
    private String password;
    private String encode;
    private String pageId;
    private LoadYaml loadYaml = new LoadYaml();

    public WikiAuthentication(String username, String password) throws IOException {
        baseUrl = loadYaml.getConfig("url");
        encode = loadYaml.getConfig("encode");
        pageId = loadYaml.getConfig("pageId");
        this.username = username;
        this.password = password;
    }

    public String getBaseUrl(){
        return baseUrl;
    }
    public String getPassword() {
        return password;
    }
    public String getUsername(){
        return username;
    }
    public String getEncode(){
        return encode;
    }
    public String getPageId () {return pageId; }
    public String toString(){
        return "Wiki authentication info: username: " + username + ", URL :" + baseUrl;
    }
}
