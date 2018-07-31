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
	
	public static HttpClient createClient() {
		// https://stackoverflow.com/questions/36268092/how-to-use-httpclientbuilder-with-http-proxy

		HttpClients.custom().setUserAgent(USER_AGENT);
		HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

        Builder config = RequestConfig.custom()
        		.setSocketTimeout(SO_TIMEOUT)
        		.setConnectTimeout(CONNECTION_TIMEOUT)
        		.setRedirectsEnabled(true)
        		.setMaxRedirects(MAX_REDIRECTS);
        
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
            HttpHost proxy = new HttpHost(proxyHost, proxyPort, "http");
            config.setProxy( proxy );
        }

        httpClientBuilder.setDefaultRequestConfig(config.build());
        return httpClientBuilder.build();
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
		HttpClient client = createClient();
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
