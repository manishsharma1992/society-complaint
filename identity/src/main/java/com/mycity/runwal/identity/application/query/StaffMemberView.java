package com.mycity.runwal.identity.application.query;

import java.util.List;

/**
 * @param userId   the staff member
 * @param towerIds the towers they work in; empty when they work across the whole society
 */
public record StaffMemberView(long userId, List<Long> towerIds) {
}
