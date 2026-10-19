import api from "./axios";

export const registerUser = (data) => {
  return api.post("/auth/register", data);
};

export const loginUser = (data) => {
  return api.post("/auth/login", data);
};

export const getCurrentUser = () => {
  return api.get("/users/me");
};
export const verifyEmail = (token) => api.post("/auth/email/verify", { token });
export const resendVerification = (email) => api.post("/auth/email/resend", { email });
export const requestPasswordReset = (email) => api.post("/auth/password/forgot", { email });
export const resetPassword = (token, password) => api.post("/auth/password/reset", { token, password });
