package com.mycity.runwal.complaint.interfaces.rest;

import jakarta.validation.constraints.Size;

record ResolveComplaintRequest(@Size(max = 500) String note) {
}
