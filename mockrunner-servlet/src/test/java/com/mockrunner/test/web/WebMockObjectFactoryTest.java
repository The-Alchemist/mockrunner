package com.mockrunner.test.web;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspEngineInfo;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.PageContext;

import com.mockrunner.mock.web.MockFilterChain;
import com.mockrunner.mock.web.MockFilterConfig;
import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;
import com.mockrunner.mock.web.MockHttpSession;
import com.mockrunner.mock.web.MockJspFactory;
import com.mockrunner.mock.web.MockPageContext;
import com.mockrunner.mock.web.MockServletConfig;
import com.mockrunner.mock.web.MockServletContext;
import com.mockrunner.mock.web.WebMockObjectFactory;

import junit.framework.TestCase;

public class WebMockObjectFactoryTest extends TestCase
{
    public void testDifferentMockObjects()
    {
        WebMockObjectFactory factory1 = new WebMockObjectFactory();
        WebMockObjectFactory factory2 = new WebMockObjectFactory();
        assertNotSame(factory1.getMockRequest(), factory2.getMockRequest());
        assertNotSame(factory1.getMockResponse(), factory2.getMockResponse());
        assertNotSame(factory1.getMockSession(), factory2.getMockSession());
        assertNotSame(factory1.getMockServletConfig(), factory2.getMockServletConfig());
        assertNotSame(factory1.getMockServletContext(), factory2.getMockServletContext());
    }
    
    public void testMockObjectsWithSameContext()
    {
        WebMockObjectFactory factory1 = new WebMockObjectFactory();
        WebMockObjectFactory factory2 = new WebMockObjectFactory(factory1);
        assertNotSame(factory1.getMockRequest(), factory2.getMockRequest());
        assertNotSame(factory1.getMockResponse(), factory2.getMockResponse());
        assertNotSame(factory1.getMockSession(), factory2.getMockSession());
        assertNotSame(factory1.getMockServletConfig(), factory2.getMockServletConfig());
        assertSame(factory1.getMockServletContext(), factory2.getMockServletContext());
    }
    
    public void testMockObjectsWithSameSessionAndContext()
    {
        WebMockObjectFactory factory1 = new WebMockObjectFactory();
        WebMockObjectFactory factory2 = new WebMockObjectFactory(factory1, false);
        assertNotSame(factory1.getMockRequest(), factory2.getMockRequest());
        assertNotSame(factory1.getMockResponse(), factory2.getMockResponse());
        assertSame(factory1.getMockSession(), factory2.getMockSession());
        assertNotSame(factory1.getMockServletConfig(), factory2.getMockServletConfig());
        assertSame(factory1.getMockServletContext(), factory2.getMockServletContext());
        factory2 = new WebMockObjectFactory(factory1, true);
        assertNotSame(factory1.getMockRequest(), factory2.getMockRequest());
        assertNotSame(factory1.getMockResponse(), factory2.getMockResponse());
        assertNotSame(factory1.getMockSession(), factory2.getMockSession());
        assertNotSame(factory1.getMockServletConfig(), factory2.getMockServletConfig());
        assertSame(factory1.getMockServletContext(), factory2.getMockServletContext());
    }
    
    public void testSetDefaultJspFactory()
    {
        WebMockObjectFactory factory = new WebMockObjectFactory();
        assertSame(factory.getMockJspFactory(), JspFactory.getDefaultFactory());
        assertSame(factory.getJspFactory(), JspFactory.getDefaultFactory());
        assertSame(factory.getMockPageContext(), factory.getMockJspFactory().getPageContext());
        TestJspFactory testJspFactory = new TestJspFactory();
        factory.setDefaultJspFactory(testJspFactory);
        assertNull(factory.getMockJspFactory());
        assertSame(testJspFactory, JspFactory.getDefaultFactory());
        assertSame(factory.getJspFactory(), JspFactory.getDefaultFactory());
        MockJspFactory testMockJspFactory = new MockJspFactory(){};
        factory.setDefaultJspFactory(testMockJspFactory);
        assertSame(testMockJspFactory, JspFactory.getDefaultFactory());
        assertSame(factory.getMockJspFactory(), JspFactory.getDefaultFactory());
        assertSame(factory.getJspFactory(), JspFactory.getDefaultFactory());
        assertSame(factory.getMockPageContext(), factory.getMockJspFactory().getPageContext());
        factory.setDefaultJspFactory(null);
        assertNull(factory.getMockJspFactory());
        assertNull(factory.getJspFactory());
    }
    
    public void testAddRequestWrapper()
    {
        WebMockObjectFactory factory = new WebMockObjectFactory();
        factory.getMockRequest().setupAddParameter("test", "test");
        factory.addRequestWrapper(HttpServletRequestWrapper.class);
        HttpServletRequest request = factory.getWrappedRequest();
        assertTrue(request instanceof HttpServletRequestWrapper);
        assertEquals("test", request.getParameter("test"));
        HttpServletRequestWrapper requestWrapper = (HttpServletRequestWrapper)factory.getWrappedRequest();
        assertSame(factory.getMockRequest(), requestWrapper.getRequest());
    }
    
    public void testAddResponseWrapper() throws Exception
    {
        WebMockObjectFactory factory = new WebMockObjectFactory();
        factory.addResponseWrapper(HttpServletResponseWrapper.class);
        HttpServletResponse response = factory.getWrappedResponse();
        assertTrue(response instanceof HttpServletResponseWrapper);
        response.getWriter().print("test");
        response.getWriter().flush();
        assertEquals("test", factory.getMockResponse().getOutputStreamContent());    
        HttpServletResponseWrapper responseWrapper = (HttpServletResponseWrapper)factory.getWrappedResponse();
        assertSame(factory.getMockResponse(), responseWrapper.getResponse());
    }
    
    public void testAddRequestAndResponseWrapperClasses()
    {
        WebMockObjectFactory factory = new WebMockObjectFactory();
        factory.addRequestWrapper(TestRequestWrapper.class);
        assertTrue(factory.getWrappedRequest() instanceof TestRequestWrapper);
        assertSame(factory.getMockRequest(), ((TestRequestWrapper)factory.getWrappedRequest()).getRequest());
        factory.addResponseWrapper(TestResponseWrapper.class);
        assertTrue(factory.getWrappedResponse() instanceof TestResponseWrapper);
        assertSame(factory.getMockResponse(), ((TestResponseWrapper)factory.getWrappedResponse()).getResponse());
    }
    
    public void testRefresh() throws Exception
    {
        WebMockObjectFactory factory = new WebMockObjectFactory();
        HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper(factory.getMockRequest());
        HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper(factory.getMockResponse());
        factory.addRequestWrapper(requestWrapper);
        factory.addResponseWrapper(responseWrapper);
        MockPageContext pageContext = factory.getMockPageContext();
        assertSame(factory.getMockRequest(), pageContext.getRequest());
        assertSame(factory.getMockResponse(), pageContext.getResponse());
        factory.refresh();
        pageContext = factory.getMockPageContext();
        assertSame(requestWrapper, pageContext.getRequest());
        assertSame(responseWrapper, pageContext.getResponse());
        assertSame(pageContext, factory.getMockJspFactory().getPageContext());
    }
    
    public void testOverrideCreate()
    {
        WebMockObjectFactory factory = new TestWebMockObjectFactory();
        assertNotSame(factory.getMockRequest().getClass(), MockHttpServletRequest.class);
        assertNotSame(factory.getMockResponse().getClass(), MockHttpServletResponse.class);
        assertNotSame(factory.getMockFilterChain().getClass(), MockFilterChain.class);
        assertNotSame(factory.getMockFilterConfig().getClass(), MockFilterConfig.class);
        assertNotSame(factory.getMockPageContext().getClass(), MockPageContext.class);
        assertNotSame(factory.getMockServletConfig().getClass(), MockServletConfig.class);
        assertNotSame(factory.getMockServletContext().getClass(), MockServletContext.class);
        assertNotSame(factory.getMockSession().getClass(), MockHttpSession.class);
    }
    
    public static class TestRequestWrapper extends MockHttpServletRequest
    {
        private HttpServletRequest request;
        
        public TestRequestWrapper(HttpServletRequest request)
        {
            this.request = request;
        }
        
        public HttpServletRequest getRequest()
        {
            return request;
        }
    }
    
    public static class TestResponseWrapper extends MockHttpServletResponse
    {
        private HttpServletResponse response;
        
        public TestResponseWrapper(HttpServletResponse response)
        {
            this.response = response;
        }
        
        public HttpServletResponse getResponse()
        {
            return response;
        }
    }
    
    public static class TestWebMockObjectFactory extends WebMockObjectFactory
    {
        public MockFilterChain createMockFilterChain()
        {
            return new MockFilterChain() {};
        }

        public MockFilterConfig createMockFilterConfig()
        {
            return new MockFilterConfig() {};
        }
        
        public MockPageContext createMockPageContext()
        {
            return new MockPageContext() {};
        }

        public MockHttpServletRequest createMockRequest()
        {
  
            return new MockHttpServletRequest() {};
        }

        public MockHttpServletResponse createMockResponse()
        {
            return new MockHttpServletResponse() {};
        }

        public MockServletConfig createMockServletConfig()
        {
            return new MockServletConfig() {};
        }

        public MockServletContext createMockServletContext()
        {
            return new MockServletContext() {};
        }

        public MockHttpSession createMockSession()
        {
            return new MockHttpSession() {};
        }
    }
    
    public static class TestJspFactory extends JspFactory
    {
        public JspEngineInfo getEngineInfo()
        {
            return null;
        }

        public JspApplicationContext getJspApplicationContext(ServletContext context)
        {
            return null;
        }

        public PageContext getPageContext(Servlet servlet, ServletRequest request, ServletResponse response, String errorPageURL, boolean needsSession, int buffer, boolean autoflush)
        {
            return null;
        }

        public void releasePageContext(PageContext pageContext)
        {
            
        }     
    }
}
