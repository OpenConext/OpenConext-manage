package manage.web;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SessionAliveFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain
            filterChain) throws ServletException, IOException {
        // add this header as an indication to the JS-client that this is a regular, non-session-expired response.
        response.addHeader("X-SESSION-ALIVE", "true");
        filterChain.doFilter(request, response);
    }
}
