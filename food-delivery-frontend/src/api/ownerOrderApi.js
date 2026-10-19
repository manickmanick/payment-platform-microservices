import api from "./axios";

export const getRestaurantOrders = () => {
  return api.get("/orders/restaurant?sort=createdAt,desc");
};

export const acceptOrder = (orderId) => {
  return api.put(`/orders/${orderId}/status`, { status: "ACCEPTED" });
};

export const updateOrderStatus = (orderId, status) => {
  return api.put(`/orders/${orderId}/status`, {
    status,
  });
};
