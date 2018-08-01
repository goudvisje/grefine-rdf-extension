package org.deri.grefine.rdf.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.any23.http.HTTPClient;
import org.apache.any23.http.HTTPClientConfiguration;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyHttpClient implements HTTPClient {
	
	private static Logger log = LoggerFactory.getLogger(ProxyHttpClient.class);
	
    private static final Pattern ESCAPED_PATTERN = Pattern.compile("%[0-9a-f]{2}",Pattern.CASE_INSENSITIVE);

    private HTTPClientConfiguration configuration;

    private HttpClientBuilder builder = null;

    private long _contentLength = -1;

    private String actualDocumentIRI = null;

    private String contentType = null;

    public static final boolean isUrlEncoded(String url) {
        return ESCAPED_PATTERN.matcher(url).find();
    }

    public void init(HTTPClientConfiguration configuration) {
        if(configuration == null) throw new NullPointerException("Illegal configuration, cannot be null.");
        this.configuration = configuration;
    }

    /**
     *
     * Opens an {@link java.io.InputStream} from a given IRI.
     * It follows redirects.
     *
     * @param uri to be opened
     * @return {@link java.io.InputStream}
     * @throws IOException if there is an error opening the {@link java.io.InputStream}
     * located at the URI.
     */
    public InputStream openInputStream(String uri) throws IOException {
    	log.error("Initializing inputstream");
    	ensureBuilderInitialized();
    	log.error("Builder is set");
    	CloseableHttpClient httpclient = builder.build();
    	log.error("HttpClient is ready");
    	
    	log.error("Trying to retrieve " + uri);
        String uriStr;
        try {
            URI uriObj = new URI(uri, isUrlEncoded(uri));
            // [scheme:][//authority][path][?query][#fragment]
            uriStr = uriObj.toString();
        } catch (URIException e) {
            throw new IllegalArgumentException("Invalid IRI string.", e);
        }
        
        log.error("Found " + uriStr);
        HttpGet httpget = new HttpGet(uriStr);
        
        log.error("GET " + uriStr);
        CloseableHttpResponse response = httpclient.execute(httpget);
        StatusLine statusLine = response.getStatusLine();
        log.error("Retrieved statusline");
        try {
            
            if (statusLine.getStatusCode() >= 300) {
                throw new HttpResponseException(
                        statusLine.getStatusCode(),
                        statusLine.getReasonPhrase());
            }
            
            log.error("HttpEntity is here");
            HttpEntity entity = response.getEntity();
            if (entity != null) {
            	log.error("Creatuing bufferedHttpEntity");
                BufferedHttpEntity buffer = new BufferedHttpEntity(entity);
                return buffer.getContent();
            } else {
            	throw new IOException("No response for URI " + uri);
            }
        } finally {
            response.close();
        }
    }

    /**
     * Shuts down the connection manager.
     */
    public void close() {
    	//TODO
    	
    	//CloseableHttpClient#close()
    }

    public long getContentLength() {
        return _contentLength;
    }

    public String getActualDocumentIRI() {
        return actualDocumentIRI;
    }

    public String getContentType() {
        return contentType;
    }

    protected int getConnectionTimeout() {
        return configuration.getDefaultTimeout();
    }

    protected int getSoTimeout() {
        return configuration.getDefaultTimeout();
    }

    private void ensureBuilderInitialized() {
        if(configuration == null) throw new IllegalStateException("client must be initialized first.");
        if (builder != null) return;
        // One connection manager is used for concurrent HttpClient calls
        
        log.error("Define builder config");
        Builder config = HttpUtils.getDefaultConfig();
        config.setConnectTimeout(configuration.getDefaultTimeout());
        config.setSocketTimeout(configuration.getDefaultTimeout());
        
        log.error("createBuilder");
        builder = HttpUtils.createClientBuilder(config);
        builder.setMaxConnTotal(configuration.getMaxConnections());
        builder.setUserAgent(configuration.getUserAgent());
        
        log.error("createHeaders");
        Collection<BasicHeader> headers = new ArrayList<BasicHeader>();
        
        if (configuration.getAcceptHeader() != null) {
            headers.add(new BasicHeader("Accept", configuration.getAcceptHeader()));
        }
        headers.add(new BasicHeader("Accept-Language", "en-us,en-gb,en,*;q=0.3")); //TODO: this must become parametric.
        headers.add(new BasicHeader("Accept-Charset", "utf-8,iso-8859-1;q=0.7,*;q=0.5"));

        log.error("assignHeaders");
		// headers.add(new Header("Accept-Encoding", "x-gzip, gzip"));
        builder.setDefaultHeaders(headers);
    }
}
