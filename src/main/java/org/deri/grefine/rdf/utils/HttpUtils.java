package org.deri.grefine.rdf.utils;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some HTTP utilities
 * 
 * @author Sergio Fernández <sergio.fernandez@salzburgresearch.at>
 *
 */
public class HttpUtils {
	
	private static Logger log = LoggerFactory.getLogger(HttpUtils.class);
	public static final String USER_AGENT = "Google Refine LMF Extension (beta)";
	public static final int CONNECTION_TIMEOUT = 10000;
	public static final int SO_TIMEOUT = 60000;
    private static final int MAX_REDIRECTS = 3;
    
//    public static PoolingHttpClientConnectionManager getPoolingHttpClientConnectionManager() {
//    	return PoolingHttpClientConnectionManagerSingleton.get();
//    }
    
    // Inner class to make sure that we have one PoolingHttpClientConnectionManager
    private static class PoolingHttpClientConnectionManagerSingleton extends PoolingHttpClientConnectionManager {
    	
    	private static PoolingHttpClientConnectionManager self;
    	
    	private PoolingHttpClientConnectionManagerSingleton() {
    		super();
    	}
    	
    	public static PoolingHttpClientConnectionManager get() {
    		if (self == null) {
    			self = new PoolingHttpClientConnectionManager();
    		}
    		return self;
    	}
    }
    
    public static Builder getDefaultConfig() {
    	return RequestConfig.custom()
        		.setSocketTimeout(SO_TIMEOUT)
        		.setConnectTimeout(CONNECTION_TIMEOUT)
        		.setRedirectsEnabled(true)
        		.setMaxRedirects(MAX_REDIRECTS);
    }
    
	public static HttpClientBuilder createClientBuilder(Builder config) {
		// https://stackoverflow.com/questions/36268092/how-to-use-httpclientbuilder-with-http-proxy

		HttpClients.custom().setUserAgent(USER_AGENT);
		HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        
        // Handle a http-proxy
        String proxyHost = System.getProperty( "http.proxyHost" );
        if ( proxyHost != null ) {
            int proxyPort = -1;
            String proxyPortStr = System.getProperty( "http.proxyPort" );
            if (proxyPortStr != null) {
                try {
                    proxyPort = Integer.parseInt( proxyPortStr );
                } catch (NumberFormatException e) {
                    log.warn("Invalid number for system property http.proxyPort ("+proxyPortStr+"), using default port instead");
                }
            }
            log.info("Use proxy: " +proxyHost +":" +proxyPort);
            HttpHost proxy = new HttpHost(proxyHost, proxyPort, "http");
            config.setProxy( proxy );
        } else {
        	throw new RuntimeException("Proxy is not used!! => " + System.getProperty( "http.proxyHost" ) + System.getProperty( "http.proxyPort" ));
        }

        httpClientBuilder.setDefaultRequestConfig(config.build());
        
        
        httpClientBuilder.setConnectionManager(PoolingHttpClientConnectionManagerSingleton.get());
        return httpClientBuilder;
    }
	
	public static HttpEntity get(String uri) throws IOException {
		log.debug("GET request over " + uri);
        HttpGet get = new HttpGet(uri);
        return get(get);
	}
	
	public static HttpEntity get(String uri, String accept) throws IOException {
		log.debug("GET request over " + uri);
        HttpGet get = new HttpGet(uri);
        get.setHeader("Accept", accept);
        return get(get);
	}
	
	private static HttpEntity get(HttpGet get) throws IOException {
		HttpClient client = createClientBuilder(getDefaultConfig()).build();
		HttpResponse response = client.execute(get);
		if (200 == response.getStatusLine().getStatusCode()) {
			return response.getEntity();
		} else {
			String msg = "Error performing GET request: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();
			log.error(msg);
			throw new ClientProtocolException(msg);
		}
	}
}
