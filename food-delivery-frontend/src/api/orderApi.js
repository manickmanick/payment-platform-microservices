import api from "./axios";

export const placeOrder = (deliveryAddress, couponCode, coordinates, customerPhone, smsOptIn) => {
  let key = sessionStorage.getItem("checkoutIdempotencyKey");
  if (!key) {
    key = crypto.randomUUID();
    sessionStorage.setItem("checkoutIdempotencyKey", key);
  }
  return api.post("/orders/checkout", {
    deliveryAddress,
    couponCode: couponCode || null,
    latitude: coordinates?.latitude ?? null,
    longitude: coordinates?.longitude ?? null,
    customerPhone: customerPhone || null,
    smsOptIn: Boolean(smsOptIn),
  }, { headers: { "Idempotency-Key": key } });
};

export const getMyOrders = () => api.get("/orders?sort=createdAt,desc");

export const getOrderById = (id) => api.get(`/orders/${id}`);

export const getDriverLocation = (id) => api.get(`/orders/${id}/driver-location`);
export const confirmOrder = (id) => api.put(`/orders/${id}/confirm`);
export const cancelOrder = (id) => api.post(`/orders/${id}/cancel`);
