import api from "./axios";
export const getAdminReport = (params) => api.get("/admin/reports/summary", { params });
export const getRestaurantApplications = () => api.get("/catalog-admin/restaurants/admin/applications");
export const reviewRestaurantApplication = (id, decision) => api.put(`/catalog-admin/restaurants/admin/${id}/review`, { decision });
export const getUsers = () => api.get("/users");
export const createUser = (data) => api.post("/users", data);
export const deleteUser = (id) => api.delete(`/users/${id}`);
export const createCoupon = (data) => api.post("/coupons", data);
