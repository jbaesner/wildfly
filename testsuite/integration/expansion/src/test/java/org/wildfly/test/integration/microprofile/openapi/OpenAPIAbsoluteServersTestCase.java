/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.openapi;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.openapi.service.TestApplication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Validates usage of "mp.openapi.extensions.servers.relative" configuration property.
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@RunAsClient
public class OpenAPIAbsoluteServersTestCase {
    private static final String DEPLOYMENT_NAME = OpenAPIAbsoluteServersTestCase.class.getSimpleName() + ".war";

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml")
                .addPackage(TestApplication.class.getPackage())
                .addAsManifestResource(new StringAsset("mp.openapi.extensions.servers.relative=false"), "microprofile-config.properties")
                ;
    }

    @Test
    public void test(@ArquillianResource URL baseURL) throws IOException, URISyntaxException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = client.execute(new HttpGet(baseURL.toURI().resolve("/openapi")))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                List<String> urls = validateContent(response);
                // Ensure absolute urls are valid
                for (String url : urls) {
                    try (CloseableHttpResponse r = client.execute(new HttpGet(url + "/test/echo/foo"))) {
                        Assert.assertEquals(HttpServletResponse.SC_OK, r.getStatusLine().getStatusCode());
                        Assert.assertEquals("foo", EntityUtils.toString(r.getEntity()));
                    } catch (SSLHandshakeException ignored) {
                        // Ignore exception due to auto-generated self-signed certificate
                        // javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
                    }
                }
            }
        }
    }

    private static List<String> validateContent(HttpResponse response) throws IOException {
        Assert.assertEquals("application/yaml", response.getEntity().getContentType().getValue());

        JsonNode node = new ObjectMapper(new YAMLFactory()).reader().readTree(response.getEntity().getContent());
        JsonNode info = node.required("info");
        Assert.assertNotNull(info);
        Assert.assertEquals(DEPLOYMENT_NAME, info.required("title").asText());
        Assert.assertNull(info.get("description"));

        JsonNode servers = node.required("servers");
        List<String> result = new LinkedList<>();
        for (JsonNode server : servers) {
            result.add(server.required("url").asText());
        }
        Assert.assertFalse(result.isEmpty());
        return result;
    }
}
