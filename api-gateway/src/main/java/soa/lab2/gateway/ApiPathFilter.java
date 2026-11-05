package soa.lab2.gateway;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;

@Component
public class ApiPathFilter extends ZuulFilter {
    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.PRE_DECORATION_FILTER_ORDER + 1;
    }

    @Override
    public boolean shouldFilter() {
        Object route = RequestContext.getCurrentContext().get(FilterConstants.PROXY_KEY);
        return "collection-api".equals(route) || "starship-api".equals(route);
    }

    @Override
    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();
        String path = (String) context.get(FilterConstants.REQUEST_URI_KEY);
        context.put(FilterConstants.REQUEST_URI_KEY, "/api/v1" + path);
        return null;
    }
}
