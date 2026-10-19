import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || `${window.location.protocol}//${window.location.hostname}:8080/api/v1`,
});

api.interceptors.request.use((config) => {
    const token = localStorage.getItem("token");

    if (token) {
      config.headers.Authorization =
        `Bearer ${token}`;
    }

    config.timeout = config.timeout || 15000;
    return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isLoginRequest = /\/auth\/(login|register)$/.test(error.config?.url || "");
    if (error.response?.status === 401 && !isLoginRequest) {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
    }
    return Promise.reject(error);
  },
);

export default api;
