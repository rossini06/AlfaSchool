import axios from 'axios'

const axiosClient = axios.create({
  baseURL: '/',
  timeout: 10000,
})

axiosClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('authToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401) {
      localStorage.removeItem('authToken')
      localStorage.removeItem('refreshToken')
      window.location.reload()
    }
    return Promise.reject(error)
  },
)

export default axiosClient
