package org.jolokia.jvmagent.security;

import java.lang.reflect.Field;
import java.util.Arrays;

import javax.security.auth.Subject;

import com.sun.net.httpserver.*;
import org.jolokia.server.core.config.ConfigKey;
import org.jolokia.test.util.MockLoginContext;
import org.testng.annotations.*;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

public class JaasHttpAuthenticatorTest {

    private JaasHttpAuthenticator auth;

    @BeforeMethod
    public void setUp() throws Exception {
        auth = new JaasHttpAuthenticator("jolokia");
    }

    @AfterMethod
    public void checkThatThreadLocalIsRemoved() throws NoSuchFieldException, IllegalAccessException {
        Field field = auth.getClass().getDeclaredField("subjectThreadLocal");
        field.setAccessible(true);
        ThreadLocal<Subject> tl = (ThreadLocal<Subject>) field.get(auth);
        assertNull(tl.get());
        field.setAccessible(false);
    }

    @Test
    public void testAuthenticateNoAuthorizationHeader() throws Exception {
        Headers respHeader = new Headers();
        HttpExchange ex = createExchange(respHeader);

        Authenticator.Result res = auth.authenticate(ex);

        assertEquals(((Authenticator.Retry) res).getResponseCode(),401);
        assertTrue(respHeader.containsKey("WWW-Authenticate"));
        assertTrue(respHeader.getFirst("WWW-Authenticate").contains("jolokia"));
    }

    @Test
    public void testAuthenticateNoLoginModules() throws Exception {
        Headers respHeader = new Headers();
        HttpExchange ex = createExchange(respHeader,"Authorization", "Basic cm9sYW5kOnMhY3IhdA==");

        Authenticator.Result result = auth.authenticate(ex);

        assertEquals(((Authenticator.Failure) result).getResponseCode(), 401);
    }

    @Test
    public void testAuthenticateSuccess() throws Exception {
        Headers respHeader = new Headers();
        HttpExchange ex = createExchange(respHeader, MockLoginContext.SUBJECT, "Authorization", "Basic cm9sYW5kOnMhY3IhdA==");

        new MockLoginContext("jolokia",true);

        Authenticator.Result result = auth.authenticate(ex);
        HttpPrincipal principal = ((Authenticator.Success) result).getPrincipal();
        assertEquals(principal.getRealm(),"jolokia");
        assertEquals(principal.getUsername(),"roland");
    }


    private HttpExchange createExchange(Headers respHeaders, String... reqHeaderValues) {
        return createExchange(respHeaders,null,reqHeaderValues);
    }

    private HttpExchange createExchange(Headers respHeaders, Subject subject, String... reqHeaderValues) {
        HttpExchange ex = createMock(HttpExchange.class);
        Headers reqHeaders = new Headers();
        for (int i = 0; i < reqHeaderValues.length; i+=2) {
            reqHeaders.put(reqHeaderValues[i], Arrays.asList(reqHeaderValues[i+1]));
        }
        expect(ex.getResponseHeaders()).andStubReturn(respHeaders);
        expect(ex.getRequestHeaders()).andStubReturn(reqHeaders);
        if (subject != null) {
            ex.setAttribute(ConfigKey.JAAS_SUBJECT_REQUEST_ATTRIBUTE, subject);
        }
        replay(ex);
        return ex;
    }
}
