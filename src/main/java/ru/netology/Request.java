package ru.netology;

import java.util.HashMap;

public class Request {
    private boolean hasHeaders;
    private String method;
    private String path;
    private HashMap<String, String> headers;
    private String body;

    private Request(){}

    public boolean hasHeaders(){
        return hasHeaders;
    }

    public String getPath() {
        return path;
    }

    public String getHeader(String header){
        if(!hasHeaders) {
            return null;
        }
        return headers.get(header);
    }

    public String getMethod(){
        return method;
    }

    public String getBody(){
        return body;
    }

    public static Builder newBuilder(){
        return new Request().new Builder();
    }

    public class Builder {

        private Builder(){}

        public Builder setMethod(String method){
            Request.this.method = method;
            return this;
        }

        public Builder setPath(String path){
            Request.this.path = path;
            return this;
        }

        public Builder addHeader(String header, String value){
            if(Request.this.headers == null) {
                Request.this.headers = new HashMap<>();
            }
            Request.this.headers.put(header, value);
            return this;
        }

        public void setBody(String body){
            Request.this.body = body;
        }

        public Request build(){
            if(Request.this.headers == null) {
                Request.this.hasHeaders = false;
            } else if(Request.this.headers.isEmpty()){
                Request.this.hasHeaders = false;
            } else {
                Request.this.hasHeaders = true;
            }
            return Request.this;
        }
    }
}
