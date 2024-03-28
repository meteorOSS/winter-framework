package ioc;

import com.meteor.winter.ioc.resource.ResourceResolver;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

public class ResourceResolverTest {

    @Test
    public void testResourceResolver() throws URISyntaxException, IOException {
        String basePackage = "com.meteor.winter.ioc";
        ResourceResolver resourceResolver = new ResourceResolver(basePackage);
        resourceResolver.scan((r)->{
            return r.getName();
        });
    }

}
