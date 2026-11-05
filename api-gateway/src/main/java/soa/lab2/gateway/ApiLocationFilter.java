package soa.lab2.gateway;

import com.netflix.util.Pair;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Component
public class ApiLocationFilter extends ZuulFilter {
    @Override
    public String filterType() {
        return FilterConstants.POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.SEND_RESPONSE_FILTER_ORDER - 1;
    }

    @Override
    public boolean shouldFilter() {
        Object route = RequestContext.getCurrentContext().get(FilterConstants.PROXY_KEY);
        return "collection-api".equals(route) || "starship-api".equals(route);
    }

    @Override
    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();
        String service = "collection-api".equals(context.get(FilterConstants.PROXY_KEY))
                ? "collection-service"
                : "starship-service";
        List<Pair<String, String>> headers = context.getZuulResponseHeaders();
        for (int i = 0; i < headers.size(); i++) {
            Pair<String, String> header = headers.get(i);
            if (!"Location".equalsIgnoreCase(header.first()))
                continue;
            URI uri = URI.create(header.second());
            String path = uri.getRawPath();
            if (path != null && (path.equals("/api/v1") || path.startsWith("/api/v1/"))) {
                String location = UriComponentsBuilder.newInstance()
                        .path("/api/" + service + path.substring("/api/v1".length()))
                        .query(uri.getRawQuery()).fragment(uri.getRawFragment()).build(true).toUriString();
                headers.set(i, new Pair<>(header.first(), location));
            }
        }
        return null;
    }
}
