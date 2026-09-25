export type PitchStatus = 'ACTIVE' | 'MAINTENANCE';

export interface PitchType {
  id: number;
  name: string;
  playerCapacity: number;
  description?: string;
}

export interface Pitch {
  id: number;
  name: string;
  pitchType: PitchType;
  status: PitchStatus;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  items: T[];
  content?: T[]; // backward-compatibility fallback
  pageNo: number;
  pageNumber?: number; // backward-compatibility fallback
  pageSize: number;
  totalElements: number;
  totalPages: number;
  isLast?: boolean;
  last?: boolean;
}

export interface PitchFilterParams {
  keyword?: string;
  pitchTypeId?: number | string;
  status?: PitchStatus | 'ALL' | '';
  page: number;
  size: number;
}

export interface PitchRequest {
  name: string;
  pitchTypeId: number;
  description?: string;
}

export interface UpdatePitchStatusRequest {
  status: PitchStatus;
}
