import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { authApi } from '../api/auth'
import { getLocations } from '../api/locations'
export function useMe() {
  return useQuery({ queryKey: ['me'], queryFn: authApi.me, retry: false })
}
export function useLocations() {
  return useQuery({
    queryKey: ['locations'],
    queryFn: getLocations,
    staleTime: Infinity,
  })
}
export function useAction<T, R>(
  action: (input: T) => Promise<R>,
  onSuccess?: (result: R) => void,
) {
  const cache = useQueryClient()
  return useMutation({
    mutationFn: action,
    onSuccess: async (result) => {
      await cache.invalidateQueries()
      onSuccess?.(result)
    },
  })
}
