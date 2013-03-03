package org.jamesdbloom.mockserver;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.jamesdbloom.mockserver.client.MockServerClient;
import org.jamesdbloom.mockserver.matchers.Times;
import org.jamesdbloom.mockserver.model.*;
import org.jamesdbloom.mockserver.server.EmbeddedJettyRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author jamesdbloom
 */
public class ClientServerTest {

    private EmbeddedJettyRunner embeddedJettyRunner;
    private MockServerClient mockServerClient;

    @Before
    public void startServerAndCreateClient() {
        embeddedJettyRunner = new EmbeddedJettyRunner(8090);
        mockServerClient = new MockServerClient("localhost", 8090);
    }

    @After
    public void stopServer() throws Exception {
        embeddedJettyRunner.stop();
    }

    @Test
    public void clientCanCallServer() throws Exception {
        // when
        mockServerClient.when(new HttpRequest()).respond(new HttpResponse().withBody("somebody"));

        // then
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatus.OK_200)
                        .withHeaders(new Header("Content-Length", "" + "somebody".length()), new Header("Server", "Jetty(9.0.0.RC0)"))
                        .withBody("somebody"),
                makeRequest(new HttpRequest()));
    }

    @Test
    public void clientCanCallServerMatchPath() throws Exception {
        // when
        mockServerClient.when(new HttpRequest().withPath("/somepath1")).respond(new HttpResponse().withBody("somebody1"));
        mockServerClient.when(new HttpRequest().withPath("/somepath2")).respond(new HttpResponse().withBody("somebody2"));

        // then
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatus.OK_200)
                        .withHeaders(new Header("Content-Length", "" + "somebody2".length()), new Header("Server", "Jetty(9.0.0.RC0)"))
                        .withBody("somebody2"),
                makeRequest(new HttpRequest().withPath("/somepath2")));
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatus.OK_200)
                        .withHeaders(new Header("Content-Length", "" + "somebody1".length()), new Header("Server", "Jetty(9.0.0.RC0)"))
                        .withBody("somebody1"),
                makeRequest(new HttpRequest().withPath("/somepath1")));
    }

    @Test
    public void clientCanCallServerMatchPathXTimes() throws Exception {
        // when
        mockServerClient.when(new HttpRequest().withPath("/somepath"), Times.exactly(2)).respond(new HttpResponse().withBody("somebody"));

        // then
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatus.OK_200)
                        .withBody("somebody")
                        .withHeaders(new Header("Content-Length", "" + "somebody".length()), new Header("Server", "Jetty(9.0.0.RC0)")),
                makeRequest(new HttpRequest().withPath("/somepath")));
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatus.OK_200)
                        .withHeaders(new Header("Content-Length", "" + "somebody".length()), new Header("Server", "Jetty(9.0.0.RC0)"))
                        .withBody("somebody"),
                makeRequest(new HttpRequest().withPath("/somepath")));
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatus.NOT_FOUND_404)
                        .withHeaders(new Header("Content-Length", "0"), new Header("Server", "Jetty(9.0.0.RC0)")),
                makeRequest(new HttpRequest().withPath("/somepath")));
    }

    @Test
    public void clientCanCallServerPositionMatchEverything() throws Exception {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withMethod("POST")
                                .withPath("/somepath")
                                .withBody("somebody\nbodyParameterName=bodyParameterValue")
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                                .withQueryParameters(new Parameter("queryParameterName", "queryParameterValue"))
                                .withBodyParameters(new Parameter("bodyParameterName", "bodyParameterValue"))
                )
                .respond(
                        new HttpResponse()
                                .withStatusCode(HttpStatus.ACCEPTED_202)
                                .withBody("somebody")
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                );

        // then
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatus.ACCEPTED_202)
                        .withBody("somebody")
                        .withHeaders(new Header("Content-Length", "" + "somebody".length()), new Header("Server", "Jetty(9.0.0.RC0)")),
                makeRequest(
                        new HttpRequest()
                                .withMethod("POST")
                                .withPath("/somepath")
                                .withBody("somebody\nbodyParameterName=bodyParameterValue")
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                                .withQueryParameters(new Parameter("queryParameterName", "queryParameterValue"))
                                .withBodyParameters(new Parameter("bodyParameterName", "bodyParameterValue")))
        );
    }

    @Test
    public void clientCanCallServerNegativeMatchBodyOnly() throws Exception {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withMethod("GET")
                                .withPath("/somepath")
                                .withBody("somebody")
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                                .withQueryParameters(new Parameter("queryParameterName", "queryParameterValue"))
                                .withBodyParameters(new Parameter("bodyParameterName", "bodyParameterValue"))
                )
                .respond(
                        new HttpResponse()
                                .withStatusCode(HttpStatus.ACCEPTED_202)
                                .withBody("somebody")
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                );

        // then
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatus.NOT_FOUND_404)
                        .withHeaders(new Header("Content-Length", "0"), new Header("Server", "Jetty(9.0.0.RC0)")),
                makeRequest(
                        new HttpRequest()
                                .withPath("/somepath")
                                .withBody("someotherbody")
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                                .withQueryParameters(new Parameter("queryParameterName", "queryParameterValue"))
                                .withBodyParameters(new Parameter("bodyParameterName", "bodyParameterValue")))
        );
    }

    private HttpResponse makeRequest(HttpRequest httpRequest) throws Exception {
        HttpResponse httpResponse;
        HttpClient httpClient = new HttpClient();
        httpClient.start();
        String queryString = buildQueryString(httpRequest.getQueryParameters());
        if (queryString.length() > 0) {
            queryString = '?' + queryString;
        }
        String body = httpRequest.getBody() + '\n' + buildQueryString(httpRequest.getBodyParameters());
        Request request = httpClient.newRequest("http://localhost:8090" + (httpRequest.getPath().startsWith("/") ? "" : "/") + httpRequest.getPath() + queryString).method(HttpMethod.fromString(httpRequest.getMethod())).content(new StringContentProvider(body));
        for (Header header : httpRequest.getHeaders()) {
            for (String value : header.getValues()) {
                request.header(header.getName(), value);
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (Cookie cookie : httpRequest.getCookies()) {
            for (String value : cookie.getValues()) {
                stringBuilder.append(cookie.getName()).append("=").append(value).append("; ");
            }
        }
        if (stringBuilder.length() > 0) {
            request.header("Set-Cookie", stringBuilder.toString());
        }
        ContentResponse contentResponse = request.send();
        httpResponse = new HttpResponse();
        httpResponse.withBody(contentResponse.getContentAsString());
        httpResponse.withStatusCode(contentResponse.getStatus());
        List<Header> headers = new ArrayList<Header>();
        for (HttpField httpField : contentResponse.getHeaders()) {
            headers.add(new Header(httpField.getName(), httpField.getValue()));
        }
        if (headers.size() > 0) {
            httpResponse.withHeaders(headers);
        }
        return httpResponse;
    }

    private String buildQueryString(List<Parameter> queryParameters) {
        String queryString = "";
        for (Parameter parameter : queryParameters) {
            for (String parameterValue : parameter.getValues()) {
                queryString += parameter.getName() + '=' + parameterValue + '&';
            }
        }
        return StringUtils.removeEnd(queryString, "&");
    }
}