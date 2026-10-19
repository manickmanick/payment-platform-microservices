import api from "./axios";

export const getAvailableDriverOrders = () => api.get("/drivers/orders/available");
export const getDriverOrders = () => api.get("/drivers/orders/me");
export const acceptDriverOrder = (orderId) => api.post(`/drivers/orders/${orderId}/accept`);
export const updateDriverLocation = (orderId, latitude, longitude) =>
  api.post("/drivers/location", { orderId, latitude, longitude });
export const updateDriverPresence = (latitude, longitude, available) =>
  api.put("/drivers/presence", { latitude, longitude, available });
export const updateDriverOrderStatus = (orderId, status) =>
  api.put(`/orders/${orderId}/status`, { status });
