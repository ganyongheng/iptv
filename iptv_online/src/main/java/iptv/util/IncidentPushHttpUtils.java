package iptv.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 20181209 add 
 * @author zoucong
 *
 */
public class IncidentPushHttpUtils {
	
	private static SimpleDateFormat httpUtilsdateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");

	
	 private static PoolingHttpClientConnectionManager connMgr;
	    private static RequestConfig requestConfig;
	    private static final int MAX_TIMEOUT = 2000;
	    private static final Logger log = LoggerFactory.getLogger("httpUtilsLogger");
	    //private static final Logger log = Logger.getLogger(HttpUtils.class);
	    
	    private static final String logSuccessMsgFormat ="??????:[%s],????????????:[%s],??????:[%s],?????????:[%s],????????????:[%s],??????????????????[%s],??????????????????[%s]";
	    private static final String logErrorMsgFormat ="[??????]-??????:[%s],????????????:[%s],??????:[%s],????????????:[%s],??????????????????[%s],??????????????????[%s]";
	    static {
	        // ???????????????
	        connMgr = new PoolingHttpClientConnectionManager();
	        // ?????????????????????
	        connMgr.setMaxTotal(20);
	        connMgr.setDefaultMaxPerRoute(connMgr.getMaxTotal());

	        RequestConfig.Builder configBuilder = RequestConfig.custom();
	        // ??????????????????
	        configBuilder.setConnectTimeout(MAX_TIMEOUT);
	        // ??????????????????
	        configBuilder.setSocketTimeout(MAX_TIMEOUT);
	        // ?????????????????????????????????????????????
	        configBuilder.setConnectionRequestTimeout(MAX_TIMEOUT);
	        // ????????????????????? ????????????????????????
	        configBuilder.setStaleConnectionCheckEnabled(true);
	        requestConfig = configBuilder.build();
	    }

	    /**
	     * ?????? GET ?????????HTTP????????????????????????
	     * @param url
	     * @return
	     */
	    public static String doGet(String url) {
	        return doGet(url, new HashMap<String, Object>());
	    }
	    
	    /**
	     * ?????? GET ?????????HTTP????????????????????????,??????????????????
	     * @param url
	     * @return
	     */
	    public static String doGet(String url,Logger logger) {
	        return doGetWithLog(url, new HashMap<String, Object>(),logger);
	    }

	    /**
	     * ?????? GET ?????????HTTP??????K-V??????
	     * @param url
	     * @param params
	     * @return
	     */
	    public static String doGet(String url, Map<String, Object> params) {
	        String apiUrl = url;
	        StringBuffer param = new StringBuffer();
	        String result = null;
            Date stime=new Date();

	        try {
	        int i = 0;
	        for (String key : params.keySet()) {
	            if (i == 0)
	                param.append("?");
	            else
	                param.append("&");
	            param.append(key).append("=").append( (null == params.get(key) ? "" : URLEncoder.encode(params.get(key).toString())));
	            i++;
	        }
	        apiUrl += param;
	           HttpClient httpclient = new DefaultHttpClient();
	       
	            HttpGet httpPost = new HttpGet(apiUrl);
	            httpPost.setConfig(requestConfig);

	            HttpResponse response = httpclient.execute(httpPost);
	            int statusCode = response.getStatusLine().getStatusCode();
	            HttpEntity entity = response.getEntity();
	            if (entity != null) {
	                InputStream instream = entity.getContent();
	                result = IOUtils.toString(instream, "UTF-8");
	            }
	            log.info(String.format(IncidentPushHttpUtils.logSuccessMsgFormat,"get,K-V",url,param,statusCode,result,httpUtilsdateFormat.format(stime)
	            		,httpUtilsdateFormat.format(new Date())));
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error(String.format(IncidentPushHttpUtils.logErrorMsgFormat,"get,K-V",url,param,e.getMessage(),httpUtilsdateFormat.format(stime)
	            		,httpUtilsdateFormat.format(new Date())));
	        }
	        return result;
	    }
	    
	    /**
	     * ?????? GET ?????????HTTP??????K-V??????,??????????????????
	     * @param url
	     * @param params
	     * @return
	     */
	    public static String doGetWithLog(String url, Map<String, Object> params,Logger logger) {
	        String apiUrl = url;
	        StringBuffer param = new StringBuffer();
	        String result = null;
            Date stime=new Date();

	        try {
	        int i = 0;
	        for (String key : params.keySet()) {
	            if (i == 0)
	                param.append("?");
	            else
	                param.append("&");
	            param.append(key).append("=").append( (null == params.get(key) ? "" : URLEncoder.encode(params.get(key).toString())));
	            i++;
	        }
	        apiUrl += param;
	           HttpClient httpclient = new DefaultHttpClient();
	       
	            HttpGet httpPost = new HttpGet(apiUrl);
	            httpPost.setConfig(requestConfig);

	            HttpResponse response = httpclient.execute(httpPost);
	            int statusCode = response.getStatusLine().getStatusCode();
	            HttpEntity entity = response.getEntity();
	            if (entity != null) {
	                InputStream instream = entity.getContent();
	                result = IOUtils.toString(instream, "UTF-8");
	            }
	            logger.info(String.format(IncidentPushHttpUtils.logSuccessMsgFormat,"get,K-V",url,param,statusCode,result,httpUtilsdateFormat.format(stime)
	            		,httpUtilsdateFormat.format(new Date())));
	        } catch (Exception e) {
	            e.printStackTrace();
	            logger.error(String.format(IncidentPushHttpUtils.logErrorMsgFormat,"get,K-V",url,param,e.getMessage(),httpUtilsdateFormat.format(stime)
	            		,httpUtilsdateFormat.format(new Date())));
	        }
	        return result;
	    }
	  

	    /**
	     * ?????? POST ?????????HTTP????????????????????????
	     * @param apiUrl
	     * @return
	     */
	    public static String doPost(String apiUrl) {
	        return doPost(apiUrl, new HashMap<String, Object>());
	    }

	    /**
	     * ?????? POST ?????????HTTP??????K-V??????
	     * @param apiUrl API??????URL
	     * @param params ??????map
	     * @return
	     */
	    public static String doPost(String apiUrl, Map<String, Object> params) {
	        CloseableHttpClient httpClient = HttpClients.createDefault();
	        String httpStr = null;
	        HttpPost httpPost = new HttpPost(apiUrl);
	        CloseableHttpResponse response = null;

	        Date stime=new Date();
	        try {
	            httpPost.setConfig(requestConfig);
	            List<NameValuePair> pairList = new ArrayList<>(params.size());
	            for (Map.Entry<String, Object> entry : params.entrySet()) {
	                NameValuePair pair = new BasicNameValuePair(entry.getKey(), entry
	                        .getValue().toString());
	                pairList.add(pair);
	            }
	            httpPost.setEntity(new UrlEncodedFormEntity(pairList, Charset.forName("UTF-8")));
	            response = httpClient.execute(httpPost);

	            HttpEntity entity = response.getEntity();
	            httpStr = EntityUtils.toString(entity, "UTF-8");
	            log.info(String.format(IncidentPushHttpUtils.logSuccessMsgFormat,"Post,K-V",apiUrl, JSON.toJSONString(params),response.getStatusLine().getStatusCode(),httpStr,httpUtilsdateFormat.format(stime)
	            		,httpUtilsdateFormat.format(new Date())));
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error(String.format(IncidentPushHttpUtils.logErrorMsgFormat,"Post,K-V",apiUrl,JSON.toJSONString(params),e.getMessage(),httpUtilsdateFormat.format(stime)
	            		,httpUtilsdateFormat.format(new Date())));
	        } finally {
	            if (response != null) {
	                try {
	                    EntityUtils.consume(response.getEntity());
	                } catch (Exception e) {
	                    e.printStackTrace();
	                }
	            }
	        }
	        return httpStr;
	    }

	    /**
	     * ?????? POST ?????????HTTP??????JSON??????
	     * @param apiUrl
	     * @param json json??????
	     * @return
	     */
	    public static String doPost(String apiUrl, JSONObject json, Logger logger) {
	        CloseableHttpClient httpClient = HttpClients.createDefault();
	        String httpStr = null;
	        HttpPost httpPost = new HttpPost(apiUrl);
	        CloseableHttpResponse response = null;

	        Date stime=new Date();
	        try {
	            httpPost.setConfig(requestConfig);
	            httpPost.setHeader("Content-Type", "application/json");
	            StringEntity stringEntity = new StringEntity(json.toJSONString(),"UTF-8");//????????????????????????
	            stringEntity.setContentEncoding("UTF-8");
	            stringEntity.setContentType("application/json");
	            httpPost.setEntity(stringEntity);
	            response = httpClient.execute(httpPost);
	            HttpEntity entity = response.getEntity();

	            httpStr = EntityUtils.toString(entity, "UTF-8");
	            logger.info(String.format(IncidentPushHttpUtils.logSuccessMsgFormat,"Post,json",apiUrl, json.toJSONString(),response.getStatusLine().getStatusCode(),httpStr,httpUtilsdateFormat.format(stime)
	            		,httpUtilsdateFormat.format(new Date())));
	        } catch (Exception e) {
	        	e.printStackTrace();
	        	logger.error(String.format(IncidentPushHttpUtils.logErrorMsgFormat,"Post,json",apiUrl,json.toJSONString(),e.getMessage(),httpUtilsdateFormat.format(stime)
	            		,httpUtilsdateFormat.format(new Date())));
	        } finally {
	            if (response != null) {
	                try {
	                    EntityUtils.consume(response.getEntity());
	                } catch (Exception e) {
	                    e.printStackTrace();
	                }
	            }
	        }
	        
	        return httpStr;
	    }
	    
	    /**
	     * ?????? POST ?????????HTTP??????JSON??????
	     * @param apiUrl
	     * @param json json??????
	     * @return
	     */
	    public static String doPost(String apiUrl, JSONObject json) {
	        CloseableHttpClient httpClient = HttpClients.createDefault();
	        String httpStr = null;
	        HttpPost httpPost = new HttpPost(apiUrl);
	        CloseableHttpResponse response = null;

	        Date stime=new Date();
	        try {
	            httpPost.setConfig(requestConfig);
	            httpPost.setHeader("Content-Type", "application/json");
	            StringEntity stringEntity = new StringEntity(json.toJSONString(),"UTF-8");//????????????????????????
	            stringEntity.setContentEncoding("UTF-8");
	            stringEntity.setContentType("application/json");
	            httpPost.setEntity(stringEntity);
	            response = httpClient.execute(httpPost);
	            HttpEntity entity = response.getEntity();

	            httpStr = EntityUtils.toString(entity, "UTF-8");
	            log.info(String.format(IncidentPushHttpUtils.logSuccessMsgFormat,"Post,json",apiUrl, json.toJSONString(),response.getStatusLine().getStatusCode(),httpStr,httpUtilsdateFormat.format(stime)
	            		,httpUtilsdateFormat.format(new Date())));
	        } catch (Exception e) {	
	        	e.printStackTrace();
	            log.error(String.format(IncidentPushHttpUtils.logErrorMsgFormat,"Post,json",apiUrl,json.toJSONString(),e.getMessage(),httpUtilsdateFormat.format(stime)
	            		,httpUtilsdateFormat.format(new Date())),e);
	        } finally {
	            if (response != null) {
	                try {
	                    EntityUtils.consume(response.getEntity());
	                } catch (Exception e) {
	                    e.printStackTrace();
	                }
	            }
	        }
	        
	        return httpStr;
	    }	    

	    /**
	     * ?????? SSL POST ?????????HTTPS??????K-V??????
	     * @param apiUrl API??????URL
	     * @param params ??????map
	     * @return
	     */
	    public static String doPostSSL(String apiUrl, Map<String, Object> params) {
	        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(createSSLConnSocketFactory()).setConnectionManager(connMgr).setDefaultRequestConfig(requestConfig).build();
	        HttpPost httpPost = new HttpPost(apiUrl);
	        CloseableHttpResponse response = null;
	        String httpStr = null;

	        Date stime=new Date();
	        try {
	            httpPost.setConfig(requestConfig);
	            List<NameValuePair> pairList = new ArrayList<NameValuePair>(params.size());
	            for (Map.Entry<String, Object> entry : params.entrySet()) {
	                NameValuePair pair = new BasicNameValuePair(entry.getKey(), entry
	                        .getValue().toString());
	                pairList.add(pair);
	            }
	            httpPost.setEntity(new UrlEncodedFormEntity(pairList, Charset.forName("utf-8")));
	            response = httpClient.execute(httpPost);
	            int statusCode = response.getStatusLine().getStatusCode();
	            if (statusCode != HttpStatus.SC_OK) {
	                return null;
	            }
	            HttpEntity entity = response.getEntity();
	            if (entity == null) {
	                return null;
	            }
	            httpStr = EntityUtils.toString(entity, "utf-8");
	            log.info(String.format(IncidentPushHttpUtils.logSuccessMsgFormat,"POSTSSL,K-V",apiUrl,JSON.toJSONString(params),response.getStatusLine().getStatusCode(),httpStr,httpUtilsdateFormat.format(stime)
	            		,httpUtilsdateFormat.format(new Date())));
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error(String.format(IncidentPushHttpUtils.logErrorMsgFormat,"POSTSSL,K-V",apiUrl,JSON.toJSONString(params),e.getMessage(),httpUtilsdateFormat.format(stime)
	            		,httpUtilsdateFormat.format(new Date())));
	        } finally {
	            if (response != null) {
	                try {
	                    EntityUtils.consume(response.getEntity());
	                } catch (Exception e) {
	                    e.printStackTrace();
	                }
	            }
	        }
	        return httpStr;
	    }

	    /**
	     * ?????? SSL POST ?????????HTTPS??????JSON??????
	     * @param apiUrl API??????URL
	     * @param json JSON??????
	     * @return
	     */
	    public static String doPostSSL(String apiUrl, Object json) {
	        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(createSSLConnSocketFactory()).setConnectionManager(connMgr).setDefaultRequestConfig(requestConfig).build();
	        HttpPost httpPost = new HttpPost(apiUrl);
	        CloseableHttpResponse response = null;
	        String httpStr = null;
	        Date stime=new Date();

	        try {
	            httpPost.setConfig(requestConfig);
	            StringEntity stringEntity = new StringEntity(json.toString(),"UTF-8");//????????????????????????
	            stringEntity.setContentEncoding("UTF-8");
	            stringEntity.setContentType("application/json");
	            httpPost.setEntity(stringEntity);
	            response = httpClient.execute(httpPost);
	            int statusCode = response.getStatusLine().getStatusCode();
	            if (statusCode != HttpStatus.SC_OK) {
	                return null;
	            }
	            HttpEntity entity = response.getEntity();
	            if (entity == null) {
	                return null;
	            }
	            httpStr = EntityUtils.toString(entity, "utf-8");
	            log.info(String.format(IncidentPushHttpUtils.logSuccessMsgFormat,"POST,JSON",apiUrl,JSON.toJSONString(json),statusCode,httpStr,httpUtilsdateFormat.format(stime)
	            		,httpUtilsdateFormat.format(new Date())));
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error(String.format(IncidentPushHttpUtils.logErrorMsgFormat,"POST,JSON",apiUrl,JSON.toJSONString(json),e.getMessage(),httpUtilsdateFormat.format(stime)
	            		,httpUtilsdateFormat.format(new Date())));
	        } finally {
	            if (response != null) {
	                try {
	                    EntityUtils.consume(response.getEntity());
	                } catch (Exception e) {
	                    e.printStackTrace();
	                }
	            }
	        }
	        return httpStr;
	    }

	    /**
	     * ??????SSL????????????
	     *
	     * @return
	     */
	    private static SSLConnectionSocketFactory createSSLConnSocketFactory() {
	        SSLConnectionSocketFactory sslsf = null;
	        try {
	            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {

	                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
	                    return true;
	                }
	            }).build();
	            sslsf = new SSLConnectionSocketFactory(sslContext, new X509HostnameVerifier() {

	                @Override
	                public boolean verify(String arg0, SSLSession arg1) {
	                    return true;
	                }

	                @Override
	                public void verify(String host, SSLSocket ssl) throws IOException {
	                }

	                @Override
	                public void verify(String host, X509Certificate cert) throws SSLException {
	                }

	                @Override
	                public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
	                }
	            });
	        } catch (GeneralSecurityException e) {
	            e.printStackTrace();
	        }
	        return sslsf;
	    }
	    
	    
	    public static void main(String []args){
	    	String url="https://test.tv.video.qq.com/i-tvbin/open/get_token?version=1&format=json&appid=yms2plxvdr0airlo&appkey=6sKAgk0PStJFEUjMIaMHfejJOKxMgYfo";
	    	String result=IncidentPushHttpUtils.doGet(url);
	    	System.out.println(result);
	    }
}
