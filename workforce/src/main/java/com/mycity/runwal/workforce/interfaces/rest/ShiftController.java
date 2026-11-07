package com.mycity.runwal.workforce.interfaces.rest;

import com.mycity.runwal.workforce.application.command.EndShiftCommand;
import com.mycity.runwal.workforce.application.command.StartShiftCommand;
import com.mycity.runwal.workforce.application.query.MyShiftsQuery;
import com.mycity.runwal.workforce.application.query.OnDutyQuery;
import com.mycity.runwal.workforce.application.query.ShiftHoursQuery;
import com.mycity.runwal.workforce.application.query.ShiftHoursView;
import com.mycity.runwal.workforce.application.query.ShiftView;
import com.mycity.runwal.workforce.application.service.ShiftQueryService;
import com.mycity.runwal.workforce.application.service.ShiftService;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Clock in / clock out for maintenance staff, and shift hours for admins. */
@RestController
@RequestMapping("/societies/{societyId}/shifts")
class ShiftController {

    private final ShiftService shiftService;
    private final ShiftQueryService queryService;

    ShiftController(ShiftService shiftService, ShiftQueryService queryService) {
        this.shiftService = shiftService;
        this.queryService = queryService;
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    ShiftResponse start(@PathVariable long societyId) {
        return ShiftResponse.of(shiftService.start(new StartShiftCommand(societyId)));
    }

    @PostMapping("/end")
    ShiftResponse end(@PathVariable long societyId) {
        return ShiftResponse.of(shiftService.end(new EndShiftCommand(societyId)));
    }

    /** The caller's own shifts in [from, to), newest first. */
    @GetMapping("/mine")
    List<ShiftResponse> mine(@PathVariable long societyId, @RequestParam Instant from, @RequestParam Instant to) {
        return queryService.mine(new MyShiftsQuery(societyId, from, to)).stream().map(ShiftResponse::of).toList();
    }

    @GetMapping("/on-duty")
    List<ShiftResponse> onDuty(@PathVariable long societyId) {
        return queryService.onDuty(new OnDutyQuery(societyId)).stream().map(ShiftResponse::of).toList();
    }

    @GetMapping("/hours")
    List<ShiftHoursResponse> hours(@PathVariable long societyId, @RequestParam Instant from,
                                   @RequestParam Instant to) {
        return queryService.hours(new ShiftHoursQuery(societyId, from, to)).stream()
                .map(ShiftHoursResponse::of)
                .toList();
    }

    record ShiftResponse(long id, long staffId, Instant startedAt, Instant endedAt, long minutesCounted) {
        static ShiftResponse of(ShiftView view) {
            return new ShiftResponse(view.id(), view.staffId(), view.startedAt(), view.endedAt(),
                    view.minutesCounted());
        }
    }

    record ShiftHoursResponse(long staffId, int shifts, long minutesWorked, boolean onDutyNow) {
        static ShiftHoursResponse of(ShiftHoursView view) {
            return new ShiftHoursResponse(view.staffId(), view.shifts(), view.minutesWorked(), view.onDutyNow());
        }
    }
}
