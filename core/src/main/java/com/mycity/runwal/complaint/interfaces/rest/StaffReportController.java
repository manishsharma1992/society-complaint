package com.mycity.runwal.complaint.interfaces.rest;

import com.mycity.runwal.complaint.application.query.StaffPerformanceQuery;
import com.mycity.runwal.complaint.application.query.StaffPerformanceView;
import com.mycity.runwal.complaint.application.service.StaffReportService;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** How the maintenance staff performed on the complaints raised in a period. */
@RestController
@RequestMapping("/societies/{societyId}/reports/staff")
class StaffReportController {

    private final StaffReportService service;

    StaffReportController(StaffReportService service) {
        this.service = service;
    }

    @GetMapping
    List<StaffPerformanceResponse> report(@PathVariable long societyId, @RequestParam Instant from,
                                          @RequestParam Instant to, @RequestParam(required = false) Long towerId) {
        return service.report(new StaffPerformanceQuery(societyId, from, to, towerId)).stream()
                .map(StaffPerformanceResponse::of)
                .toList();
    }

    record StaffPerformanceResponse(long staffId, int assigned, int started, int resolved, int rejected,
                                    Long medianMinutesToStart, Long medianMinutesToResolve, int resolvedWithinSla,
                                    int openNow, int overdueNow) {
        static StaffPerformanceResponse of(StaffPerformanceView view) {
            return new StaffPerformanceResponse(view.staffId(), view.assigned(), view.started(), view.resolved(),
                    view.rejected(), view.medianMinutesToStart(), view.medianMinutesToResolve(),
                    view.resolvedWithinSla(), view.openNow(), view.overdueNow());
        }
    }
}
