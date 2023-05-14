package ru.netology;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;

public class Request {
    private boolean hasHeaders;
    private String method;
    private String path;
    private HashMap<String, String> headers;
    private String body;
    private List<NameValuePair> queryParams;
    private List<NameValuePair> postParams;

    private Request() {
    }

    public boolean hasHeaders() {
        return hasHeaders;
    }

    public String getPath() {
        return path;
    }

    public String getHeader(String header) {
        if (!hasHeaders) {
            return null;
        }
        return headers.get(header);
    }

    public HashMap<String, String> getHeaders(){
        return headers;
    }

    public String getMethod() {
        return method;
    }

    public String getBody() {
        return body;
    }

    public List<NameValuePair> getPostParams() {
        return postParams;
    }

    public NameValuePair getPostParam(String name) {
        if (postParams == null) {
            return null;
        }

        for (NameValuePair pair : postParams) {
            if (pair.getName().equals(name)) {
                return pair;
            }
        }

        return null;
    }

    public List<NameValuePair> getQueryParams() {
        return queryParams;
    }

    public NameValuePair getQueryParam(String name) {
        if (queryParams == null) {
            return null;
        }

        for (NameValuePair pair : queryParams) {
            if (pair.getName().equals(name)) {
                return pair;
            }
        }

        return null;
    }

    public static Builder newBuilder() {
        return new Request().new Builder();
    }

    public class Builder {
        public static final String URLENCODED = "application/x-www-form-urlencoded";
        public static final String MULTIPART = "multipart/form-data";
        public static final String CONTENT_TYPE = "Content-Type";

        private Builder() {
        }

        public Builder setMethod(String method) {
            Request.this.method = method;
            return this;
        }

        public String getMethod() {
            return Request.this.getMethod();
        }

        public Builder setPath(String path) {
            try {
                URIBuilder uriB = new URIBuilder(path);
                Request.this.path = uriB.getPath();
                Request.this.queryParams = uriB.getQueryParams();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public String getHeader(String header) {
            return Request.this.getHeader(header);
        }

        public Builder addHeader(String header, String value) {
            if (Request.this.headers == null) {
                Request.this.headers = new HashMap<>();
            }
            Request.this.headers.put(header, value);
            Request.this.hasHeaders = true;
            return this;
        }

        public Builder addHeaders(List<String> headers) {
            if (Request.this.headers == null) {
                Request.this.headers = new HashMap<>();
            }

            for (String headerStr : headers) {
                int deliInd = headerStr.indexOf(':');
                String header = headerStr.substring(0, deliInd);
                String value = headerStr.substring(deliInd + 1).trim();
                Request.this.headers.put(header, value);
                Request.this.hasHeaders = true;
            }

            return this;
        }

        public void parseBodyUrlencoded() {
            try {
                URIBuilder urib = new URIBuilder("/?" + body);
                postParams = urib.getQueryParams();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        public void parseBodyMultipart(){

        }

        public void setBody(String body) {
            Request.this.body = body;
            String contentType = getHeader(CONTENT_TYPE);
            if (contentType != null) {
                if(contentType.equals(URLENCODED)){
                    parseBodyUrlencoded();
                } else if(contentType.equals(MULTIPART)){
                    parseBodyMultipart();
                }
            }
        }

        public Request build() {
            if (Request.this.headers == null) {
                Request.this.hasHeaders = false;
            } else if (Request.this.headers.isEmpty()) {
                Request.this.hasHeaders = false;
            } else {
                Request.this.hasHeaders = true;
            }
            return Request.this;
        }
    }
}
