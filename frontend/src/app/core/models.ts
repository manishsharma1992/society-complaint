export type ComplaintStatus = 'OPEN' | 'ASSIGNED' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'REJECTED';
export type Scope = 'FLAT' | 'COMMON_AREA';

export interface StatusChange {
  fromStatus: ComplaintStatus | null;
  toStatus: ComplaintStatus;
  changedBy: number;
  changedAt: string;
  note: string | null;
}

export interface Photo {
  url: string;
  contentType: string;
  sizeBytes: number;
  uploadedBy: number;
  uploadedAt: string;
}

export interface Comment {
  sequence: number;
  authorId: number;
  text: string;
  postedAt: string;
}

export interface Complaint {
  id: number;
  societyId: number;
  scope: Scope;
  towerId: number | null;
  flatId: number | null;
  categoryId: number;
  title: string;
  description: string | null;
  status: ComplaintStatus;
  raisedBy: number;
  assignedTo: number | null;
  raisedAt: string;
  dueAt: string;
  history: StatusChange[];
  photos: Photo[];
  comments: Comment[];
}

export interface Category {
  id: number;
  name: string;
  slaHours: number;
}

export interface Tower {
  id: number;
  code: string;
  name: string;
  residentialFloors: number;
}

export interface Flat {
  id: number;
  towerId: number;
  towerCode: string;
  floor: number;
  number: number;
  label: string;
}

/** A complaint is still being worked on until it is closed or rejected. */
export const isOngoing = (status: ComplaintStatus) => status !== 'CLOSED' && status !== 'REJECTED';
