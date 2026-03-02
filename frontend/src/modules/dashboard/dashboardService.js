import axiosClient from '../../shared/api/axiosClient'

export async function getDashboard() {
  const response = await axiosClient.get('/api/v1/dashboard')
  return response.data
}
