import { capabilitySettings, capabilityUpdate } from './generated/endpoints';
import type { Setting } from './generated/models';
import { unwrapResponse } from './unwrap';

export function getCapabilitySettings() {
  return capabilitySettings().then(response => unwrapResponse<Setting[]>(response));
}
export function updateCapabilitySetting(key: string, enabled: boolean, version: number, reason: string) {
  return capabilityUpdate(key, { enabled, version, reason }).then(response => unwrapResponse<Setting[]>(response));
}
