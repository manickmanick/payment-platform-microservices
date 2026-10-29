import axios from "axios";

const baseURL =
  import.meta.env.VITE_API_BASE_URL ||
  `${window.location.protocol}//${window.location.hostname}:8084/api/v1`;

// Public requests: no Authorization token
export const publicApi = axios.create({
  baseURL,
  timeout: 15000,
});

// Authenticated requests: attach token
const api = axios.create({
  baseURL,
  timeout: 15000,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

export default api;
