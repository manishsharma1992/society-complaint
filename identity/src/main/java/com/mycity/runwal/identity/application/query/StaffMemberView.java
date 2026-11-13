package com.mycity.runwal.identity.application.query;

import java.util.List;

/**
 * @param userId       the staff member, or a tower manager
 * @param towerIds     the towers they work in (or manage); empty when they work across the whole society
 * @param towerManager a tower admin, not staff: they pass the work on, e.g. to the lift company
 */
public record StaffMemberView(long userId, List<Long> towerIds, boolean towerManager) {
}
