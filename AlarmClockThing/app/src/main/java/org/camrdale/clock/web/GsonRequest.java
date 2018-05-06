package org.camrdale.clock.web;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.UnsupportedEncodingException;
import java.util.Map;

class GsonRequest<Req, Resp> extends Request<Resp> {
    private static final String TAG = GsonRequest.class.getSimpleName();

    private final Gson gson;
    private final Class<Resp> responseClass;
    private final Map<String, String> headers;
    private final Req request;
    private final Response.Listener<Resp> listener;

    /**
     * Make a POST request and return a parsed object from JSON.
     *
     * @param url URL of the request to make
     * @param responseClass Relevant class object, for Gson's reflection
     * @param headers Map of request headers
     */
    GsonRequest(Gson gson, String url, Class<Resp> responseClass,
                Map<String, String> headers, Req request,
                Response.Listener<Resp> listener, Response.ErrorListener errorListener) {
        super(Method.POST, url, errorListener);
        this.gson = gson;
        this.responseClass = responseClass;
        this.headers = headers;
        this.request = request;
        this.listener = listener;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return headers != null ? headers : super.getHeaders();
    }

    @Override
    public String getBodyContentType() {
        return "application/json";
    }

    @Override
    public byte[] getBody() {
        String json = gson.toJson(request);
        Log.i(TAG, "Sending JSON request to " + getUrl() + ": " + json);
        return json.getBytes(Charsets.UTF_8);
    }

    @Override
    protected void deliverResponse(Resp response) {
        listener.onResponse(response);
    }

    @Override
    protected Response<Resp> parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(
                    response.data,
                    HttpHeaderParser.parseCharset(response.headers));
            Log.i(TAG, "Received JSON response from " + getUrl() + ": " + json);
            return Response.success(
                    gson.fromJson(json, responseClass),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | JsonSyntaxException e) {
            return Response.error(new ParseError(e));
        }
    }
}
