package com.mycity.runwal.residency.interfaces.rest;

import com.mycity.runwal.residency.application.command.RemoveFlatMemberCommand;
import com.mycity.runwal.residency.application.service.FlatMembershipService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Links owners and tenants to flats. */
@RestController
@RequestMapping("/societies/{societyId}/flats/{flatId}/members")
class FlatMemberController {

    private final FlatMembershipService membershipService;

    FlatMemberController(FlatMembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @PostMapping
    ResponseEntity<FlatResponse> add(@PathVariable long societyId, @PathVariable long flatId,
                                     @Valid @RequestBody AddFlatMemberRequest request) {
        return ResponseEntity.of(membershipService.addMember(request.toCommand(societyId, flatId))
                .map(FlatResponse::from));
    }

    @DeleteMapping("/{memberId}")
    ResponseEntity<FlatResponse> remove(@PathVariable long societyId, @PathVariable long flatId,
                                        @PathVariable long memberId) {
        return ResponseEntity.of(membershipService.removeMember(
                        new RemoveFlatMemberCommand(societyId, flatId, memberId))
                .map(FlatResponse::from));
    }
}
