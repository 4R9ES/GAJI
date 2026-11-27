import { client } from './client'
import type { Location } from '../types/location'
export async function getLocations(): Promise<Location[]> {
  const rows = (
    await client.get<{ locationId: number; name: string }[]>('/locations')
  ).data
  return rows.map((row) => ({ id: String(row.locationId), name: row.name }))
}
