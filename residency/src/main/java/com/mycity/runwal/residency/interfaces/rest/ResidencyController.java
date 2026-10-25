package com.mycity.runwal.residency.interfaces.rest;

import com.mycity.runwal.residency.application.query.FlatsOfMemberQuery;
import com.mycity.runwal.residency.application.query.GetFlatQuery;
import com.mycity.runwal.residency.application.query.GetSocietyQuery;
import com.mycity.runwal.residency.application.query.ListFlatsQuery;
import com.mycity.runwal.residency.application.query.ListTowersQuery;
import com.mycity.runwal.residency.application.service.ResidencyQueryService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read access to a society, its towers and flats. */
@RestController
@RequestMapping("/societies/{societyId}")
class ResidencyController {

    private final ResidencyQueryService queryService;

    ResidencyController(ResidencyQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    ResponseEntity<SocietyResponse> society(@PathVariable long societyId) {
        return ResponseEntity.of(queryService.society(new GetSocietyQuery(societyId)).map(SocietyResponse::from));
    }

    @GetMapping("/towers")
    List<TowerResponse> towers(@PathVariable long societyId) {
        return queryService.towers(new ListTowersQuery(societyId)).stream().map(TowerResponse::from).toList();
    }

    @GetMapping("/towers/{towerId}/flats")
    ResponseEntity<List<FlatResponse>> flatsOfTower(@PathVariable long societyId, @PathVariable long towerId) {
        return ResponseEntity.of(queryService.flats(new ListFlatsQuery(societyId, towerId))
                .map(flats -> flats.stream().map(FlatResponse::from).toList()));
    }

    @GetMapping("/flats/{flatId}")
    ResponseEntity<FlatResponse> flat(@PathVariable long societyId, @PathVariable long flatId) {
        return ResponseEntity.of(queryService.flat(new GetFlatQuery(societyId, flatId)).map(FlatResponse::from));
    }

    /** The flats a person is linked to, e.g. GET /flats?memberId=10. */
    @GetMapping("/flats")
    List<FlatResponse> flatsOfMember(@PathVariable long societyId, @RequestParam long memberId) {
        return queryService.flatsOfMember(new FlatsOfMemberQuery(societyId, memberId)).stream()
                .map(FlatResponse::from)
                .toList();
    }
}
