package com.homework.banking.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servlet filter that enforces a sliding-window rate limit of 100 requests per minute per IP.
 *
 * <p>Each incoming IP gets an independent counter. On every request the filter prunes
 * timestamps older than 60 seconds, then atomically checks whether the remaining count
 * would exceed the limit. The prune, check, and add are synchronized on the per-IP list
 * to prevent a TOCTOU race where concurrent requests could both pass the check.</p>
 *
 * <p>Uses {@link CopyOnWriteArrayList} per IP. Synchronization on the list makes the
 * check-and-add atomic; the COW list is retained for the thread-safe iteration
 * semantics expected by the filter.</p>
 */
@Component
public class RateLimitingFilter implements Filter {

    private static final int MAX_REQUESTS = 100;
    private static final long WINDOW_MS = 60_000L;

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Long>> requestTimestamps =
            new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Checks the rate limit for the requesting IP. The prune-check-add sequence is
     * executed under a lock on the per-IP list, making it atomic. Forwards to the
     * next filter if the limit has not been reached; writes a 429 JSON response and
     * short-circuits if it has.
     *
     * @param request  the incoming servlet request
     * @param response the outgoing servlet response
     * @param chain    the remaining filter chain
     * @throws IOException      if writing the 429 response fails
     * @throws ServletException if the downstream filter chain throws
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String ip = httpRequest.getRemoteAddr();
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW_MS;

        CopyOnWriteArrayList<Long> timestamps = requestTimestamps.computeIfAbsent(ip,
                k -> new CopyOnWriteArrayList<>());

        boolean rateLimited;
        synchronized (timestamps) {
            timestamps.removeIf(t -> t < windowStart);
            if (timestamps.size() >= MAX_REQUESTS) {
                rateLimited = true;
            } else {
                timestamps.add(now);
                rateLimited = false;
            }
        }

        if (rateLimited) {
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(objectMapper.writeValueAsString(
                    Map.of("error", "Rate limit exceeded",
                           "details", List.of(Map.of("field", "request",
                                   "message", "Maximum 100 requests per minute per IP exceeded")))
            ));
            return;
        }

        chain.doFilter(request, response);
    }
}
