package com.mycity.runwal.complaint.infrastructure.config;

import com.mycity.runwal.complaint.application.port.ComplaintsAddOn;
import com.mycity.runwal.shared.id.SocietyId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Refuses every request about a society that has not switched the Complaints add-on on (403, with the reason).
 * The society's data stays; switching the add-on on again brings everything back.
 */
class ComplaintAddOnInterceptor implements HandlerInterceptor {

    private final ComplaintsAddOn addOn;

    ComplaintAddOnInterceptor(ComplaintsAddOn addOn) {
        this.addOn = addOn;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(variables instanceof Map<?, ?> map) || !(map.get("societyId") instanceof String id)
                || !id.matches("[1-9][0-9]{0,17}") || addOn.isOnFor(new SocietyId(Long.parseLong(id)))) {
            return true;
        }
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("{\"title\":\"Add-on off\",\"status\":403,"
                + "\"detail\":\"This society has not switched on the Complaints add-on\"}");
        return false;
    }
}
